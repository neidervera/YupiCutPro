package com.example.engine

import android.content.Context
import android.graphics.*
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import com.example.model.*
import kotlin.math.*

object VideoRenderEngine {

    private val frameCache = LruCache<String, Bitmap>(20)
    private val retrieverCache = mutableMapOf<String, MediaMetadataRetriever>()

    fun getRetriever(context: Context, uriString: String): MediaMetadataRetriever? {
        return retrieverCache.getOrPut(uriString) {
            try {
                val r = MediaMetadataRetriever()
                r.setDataSource(context, Uri.parse(uriString))
                r
            } catch (e: Exception) {
                return null
            }
        }
    }

    /**
     * Renders a complete composited frame at [playheadMs] on the given [canvas] of size [width] x [height].
     */
    fun renderFrame(
        context: Context,
        project: Project,
        playheadMs: Long,
        canvas: Canvas,
        width: Int,
        height: Int,
        renderWatermark: Boolean = false
    ) {
        // Deep Pitch Black background
        canvas.drawColor(Color.BLACK)

        // Find active clip(s)
        val activeClipIndex = project.videoClips.indexOfFirst {
            playheadMs >= it.timelineStartMs && playheadMs < (it.timelineStartMs + it.activeDurationMs)
        }

        if (activeClipIndex >= 0) {
            val clip = project.videoClips[activeClipIndex]
            val clipTimeMs = (playheadMs - clip.timelineStartMs)
            val sourceTimeMs = (clip.startTrimMs + (clipTimeMs * clip.adjustments.speed).toLong()).coerceIn(clip.startTrimMs, clip.endTrimMs)

            // Check if there's a transition from previous clip
            val isTransitioning = clip.transition != TransitionType.NONE &&
                    clipTimeMs < clip.transitionDurationMs &&
                    activeClipIndex > 0

            if (isTransitioning) {
                val prevClip = project.videoClips[activeClipIndex - 1]
                val prevSourceTime = prevClip.endTrimMs - (clip.transitionDurationMs - clipTimeMs)
                val progress = clipTimeMs.toFloat() / clip.transitionDurationMs.toFloat()
                renderTransition(
                    context, prevClip, prevSourceTime,
                    clip, sourceTimeMs, progress, clip.transition,
                    canvas, width, height
                )
            } else {
                renderSingleClip(context, clip, sourceTimeMs, canvas, width, height)
            }
        } else {
            // No video clip at this timestamp: draw clean placeholder or end slate
            val p = Paint().apply {
                color = Color.rgb(0, 230, 118) // Cyber green
                textSize = 34f
                typeface = Typeface.MONOSPACE
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("YupiCut • Línea de tiempo vacía", width / 2f, height / 2f, p)
        }

        // Render Active Text Overlays
        project.textClips.forEach { textClip ->
            if (playheadMs >= textClip.timelineStartMs && playheadMs < (textClip.timelineStartMs + textClip.durationMs)) {
                val textProgress = (playheadMs - textClip.timelineStartMs).toFloat() / textClip.durationMs.toFloat()
                renderTextOverlay(textClip, textProgress, canvas, width, height)
            }
        }

        // Render Watermark if requested
        if (renderWatermark || project.watermark) {
            val wmPaint = Paint().apply {
                color = Color.argb(190, 0, 255, 102) // Neon green
                textSize = 28f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText("YupiCut Pro", width - 180f, height - 40f, wmPaint)
        }
    }

    private fun renderSingleClip(
        context: Context,
        clip: VideoClip,
        sourceTimeMs: Long,
        canvas: Canvas,
        width: Int,
        height: Int,
        alpha: Float = 1f
    ) {
        val baseBitmap = getClipBitmap(context, clip, sourceTimeMs, width, height)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        paint.alpha = (alpha.coerceIn(0f, 1f) * 255).toInt()

        // Apply Color Adjustments + LUT Matrix
        val colorMatrix = createAdjustmentMatrix(clip.adjustments, clip.filter)
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)

        canvas.save()

        // Handle Rotation & Mirroring
        val cx = width / 2f
        val cy = height / 2f
        canvas.translate(cx, cy)

        if (clip.adjustments.rotation != 0) {
            canvas.rotate(clip.adjustments.rotation.toFloat())
        }
        val sx = if (clip.adjustments.flipHorizontal) -1f else 1f
        val sy = if (clip.adjustments.flipVertical) -1f else 1f
        canvas.scale(sx, sy)

        // Handle Zoom Pulse Effect
        var scale = 1f
        if (clip.effect == EffectType.ZOOM_PULSE) {
            val pulse = (sin(sourceTimeMs / 140.0) * 0.22).toFloat()
            scale += pulse.coerceAtLeast(0f)
            canvas.scale(scale, scale)
        }

        val dstRect = RectF(-cx, -cy, cx, cy)

        // Draw blurred base if BLUR effect active
        if (clip.effect == EffectType.BLUR) {
            renderBlurEffect(canvas, baseBitmap, dstRect, paint)
        } else {
            canvas.drawBitmap(baseBitmap, null, dstRect, paint)
        }

        // Post-processing real-time Effects (Glitch, RGB Split, Fade to Black, VHS overlay)
        when (clip.effect) {
            EffectType.GLITCH -> {
                renderGlitchEffect(canvas, baseBitmap, dstRect, sourceTimeMs)
            }
            EffectType.RGB_SPLIT -> {
                renderRgbSplitEffect(canvas, baseBitmap, dstRect, sourceTimeMs)
            }
            EffectType.FADE_BLACK -> {
                val fadeRatio = (0.5f + 0.5f * sin(sourceTimeMs / 400.0).toFloat()).coerceIn(0f, 0.95f)
                val fadePaint = Paint().apply { color = Color.argb((fadeRatio * 255).toInt(), 0, 0, 0) }
                canvas.drawRect(dstRect, fadePaint)
            }
            else -> {}
        }

        // Retro VHS scanlines and timestamp if retro filter active
        if (clip.filter == FilterType.RETRO_VHS) {
            renderVhsScanlines(canvas, dstRect, sourceTimeMs)
        }

        // Vignette
        if (clip.adjustments.vignette > 0f) {
            renderVignette(canvas, dstRect, clip.adjustments.vignette)
        }

        canvas.restore()
    }

    private fun renderBlurEffect(canvas: Canvas, bitmap: Bitmap, dst: RectF, paint: Paint) {
        // Multi-layered offset rendering creates a genuine frosted soft focus blur
        val offsets = floatArrayOf(-8f, 0f, 8f)
        paint.alpha = 85
        for (ox in offsets) {
            for (oy in offsets) {
                val offsetRect = RectF(dst.left + ox, dst.top + oy, dst.right + ox, dst.bottom + oy)
                canvas.drawBitmap(bitmap, null, offsetRect, paint)
            }
        }
    }

    private fun renderGlitchEffect(canvas: Canvas, bitmap: Bitmap, dst: RectF, timeMs: Long) {
        val glitchP = Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
            alpha = 180
        }
        val shift = (sin(timeMs / 60.0) * 28.0).toFloat()
        val rDst = RectF(dst.left + shift, dst.top, dst.right + shift, dst.bottom)
        val bDst = RectF(dst.left - shift, dst.top, dst.right - shift, dst.bottom)

        // Red channel offset
        val rMatrix = ColorMatrix(floatArrayOf(
            1.4f, 0f, 0f, 0f, 20f,
            0f, 0f, 0f, 0f, 0f,
            0f, 0f, 0f, 0f, 0f,
            0f, 0f, 0f, 0.8f, 0f
        ))
        glitchP.colorFilter = ColorMatrixColorFilter(rMatrix)
        canvas.drawBitmap(bitmap, null, rDst, glitchP)

        // Cyan / Blue channel offset
        val cMatrix = ColorMatrix(floatArrayOf(
            0f, 0f, 0f, 0f, 0f,
            0f, 1.2f, 0f, 0f, 15f,
            0f, 0f, 1.5f, 0f, 30f,
            0f, 0f, 0f, 0.8f, 0f
        ))
        glitchP.colorFilter = ColorMatrixColorFilter(cMatrix)
        canvas.drawBitmap(bitmap, null, bDst, glitchP)

        // Horizontal displacement slices
        val sliceP = Paint().apply { color = Color.argb(120, 0, 255, 120) }
        val numSlices = 4
        for (s in 0 until numSlices) {
            val sy = dst.top + ((timeMs * 3 + s * 130) % dst.height().toLong()).toFloat()
            val sh = 12f
            canvas.drawRect(dst.left, sy, dst.right, sy + sh, sliceP)
        }
    }

    private fun renderRgbSplitEffect(canvas: Canvas, bitmap: Bitmap, dst: RectF, timeMs: Long) {
        val splitP = Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
            alpha = 190
        }
        val angle = (timeMs / 120.0)
        val offX = (cos(angle) * 18.0).toFloat()
        val offY = (sin(angle) * 18.0).toFloat()

        // Red
        val rMat = ColorMatrix(floatArrayOf(
            1.5f, 0f, 0f, 0f, 0f,
            0f, 0f, 0f, 0f, 0f,
            0f, 0f, 0f, 0f, 0f,
            0f, 0f, 0f, 0.7f, 0f
        ))
        splitP.colorFilter = ColorMatrixColorFilter(rMat)
        canvas.drawBitmap(bitmap, null, RectF(dst.left + offX, dst.top + offY, dst.right + offX, dst.bottom + offY), splitP)

        // Green
        val gMat = ColorMatrix(floatArrayOf(
            0f, 0f, 0f, 0f, 0f,
            0f, 1.5f, 0f, 0f, 0f,
            0f, 0f, 0f, 0f, 0f,
            0f, 0f, 0f, 0.7f, 0f
        ))
        splitP.colorFilter = ColorMatrixColorFilter(gMat)
        canvas.drawBitmap(bitmap, null, RectF(dst.left - offX, dst.top - offY, dst.right - offX, dst.bottom - offY), splitP)
    }

    private fun renderVhsScanlines(canvas: Canvas, dst: RectF, timeMs: Long) {
        val lineP = Paint().apply {
            color = Color.argb(45, 0, 0, 0)
            strokeWidth = 2f
        }
        var y = dst.top
        while (y < dst.bottom) {
            canvas.drawLine(dst.left, y, dst.right, y, lineP)
            y += 6f
        }

        // VHS REC OSD
        val osdP = Paint().apply {
            color = Color.rgb(0, 255, 102)
            textSize = 24f
            typeface = Typeface.MONOSPACE
            isAntiAlias = true
        }
        canvas.drawText("● REC  SP 00:02:14", dst.left + 24f, dst.top + 40f, osdP)
    }

    private fun renderTransition(
        context: Context,
        clipA: VideoClip,
        timeA: Long,
        clipB: VideoClip,
        timeB: Long,
        progress: Float,
        type: TransitionType,
        canvas: Canvas,
        width: Int,
        height: Int
    ) {
        when (type) {
            TransitionType.DISSOLVE -> {
                renderSingleClip(context, clipA, timeA, canvas, width, height, alpha = 1f - progress)
                renderSingleClip(context, clipB, timeB, canvas, width, height, alpha = progress)
            }
            TransitionType.SLIDE_LEFT -> {
                val offset = width * progress
                canvas.save()
                canvas.translate(-offset, 0f)
                renderSingleClip(context, clipA, timeA, canvas, width, height)
                canvas.restore()

                canvas.save()
                canvas.translate(width - offset, 0f)
                renderSingleClip(context, clipB, timeB, canvas, width, height)
                canvas.restore()
            }
            TransitionType.SLIDE_RIGHT -> {
                val offset = width * progress
                canvas.save()
                canvas.translate(offset, 0f)
                renderSingleClip(context, clipA, timeA, canvas, width, height)
                canvas.restore()

                canvas.save()
                canvas.translate(-width + offset, 0f)
                renderSingleClip(context, clipB, timeB, canvas, width, height)
                canvas.restore()
            }
            TransitionType.WIPE -> {
                val splitX = width * progress
                canvas.save()
                canvas.clipRect(0f, 0f, width - splitX, height.toFloat())
                renderSingleClip(context, clipA, timeA, canvas, width, height)
                canvas.restore()

                canvas.save()
                canvas.clipRect(width - splitX, 0f, width.toFloat(), height.toFloat())
                renderSingleClip(context, clipB, timeB, canvas, width, height)
                canvas.restore()
            }
            TransitionType.FLASH_WHITE -> {
                if (progress < 0.5f) {
                    renderSingleClip(context, clipA, timeA, canvas, width, height)
                    val flashAlpha = (progress * 2 * 255).toInt()
                    canvas.drawColor(Color.argb(flashAlpha, 255, 255, 255))
                } else {
                    renderSingleClip(context, clipB, timeB, canvas, width, height)
                    val flashAlpha = ((1f - progress) * 2 * 255).toInt()
                    canvas.drawColor(Color.argb(flashAlpha, 255, 255, 255))
                }
            }
            TransitionType.ZOOM_IN -> {
                val scaleA = 1f + progress * 0.6f
                canvas.save()
                canvas.scale(scaleA, scaleA, width / 2f, height / 2f)
                renderSingleClip(context, clipA, timeA, canvas, width, height, alpha = 1f - progress)
                canvas.restore()

                val scaleB = 0.5f + progress * 0.5f
                canvas.save()
                canvas.scale(scaleB, scaleB, width / 2f, height / 2f)
                renderSingleClip(context, clipB, timeB, canvas, width, height, alpha = progress)
                canvas.restore()
            }
            TransitionType.GLITCH -> {
                val glitchOffset = (sin(progress * 35f) * 35f).toFloat()
                canvas.save()
                canvas.translate(glitchOffset, 0f)
                if (progress < 0.5f) {
                    renderSingleClip(context, clipA, timeA, canvas, width, height)
                } else {
                    renderSingleClip(context, clipB, timeB, canvas, width, height)
                }
                canvas.restore()
            }
            TransitionType.NONE -> {
                renderSingleClip(context, clipB, timeB, canvas, width, height)
            }
        }
    }

    private fun renderVignette(canvas: Canvas, dst: RectF, intensity: Float) {
        val radius = max(dst.width(), dst.height()) * 0.7f
        val gradient = RadialGradient(
            0f, 0f, radius,
            intArrayOf(Color.TRANSPARENT, Color.argb((intensity * 235).toInt(), 0, 0, 0)),
            floatArrayOf(0.4f, 1f),
            Shader.TileMode.CLAMP
        )
        val vPaint = Paint().apply { shader = gradient }
        canvas.drawRect(dst, vPaint)
    }

    private fun renderTextOverlay(clip: TextClip, progress: Float, canvas: Canvas, width: Int, height: Int) {
        var displayText = clip.text
        var alpha = 1.0f
        var animOffsetY = 0f
        var animScale = 1.0f

        when (clip.animation) {
            TextAnimation.TYPEWRITER -> {
                val charsToShow = (progress * clip.text.length).toInt().coerceIn(0, clip.text.length)
                displayText = clip.text.substring(0, charsToShow)
            }
            TextAnimation.FADE -> {
                alpha = if (progress < 0.2f) progress / 0.2f
                else if (progress > 0.8f) (1f - progress) / 0.2f
                else 1.0f
            }
            TextAnimation.SLIDE_UP -> {
                if (progress < 0.2f) {
                    animOffsetY = (1f - (progress / 0.2f)) * 50f
                }
            }
            TextAnimation.BOUNCE -> {
                if (progress < 0.3f) {
                    val p = progress / 0.3f
                    animScale = 0.7f + sin(p * Math.PI).toFloat() * 0.4f
                }
            }
            TextAnimation.GLOW -> {
                animScale = 1.0f + (sin(progress * 20.0).toFloat() * 0.05f)
            }
            TextAnimation.NONE -> {}
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = clip.fontSizeSp * 2.5f * animScale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            color = try { Color.parseColor(clip.textColorHex) } catch (_: Exception) { Color.WHITE }
            setAlpha((alpha.coerceIn(0f, 1f) * 255).toInt())
        }

        val x = (width / 2f) + (clip.positionX * (width / 2f))
        val y = (height / 2f) + (clip.positionY * (height / 2f)) + animOffsetY

        clip.bgColorHex?.let { bgHex ->
            try {
                val bounds = Rect()
                textPaint.getTextBounds(displayText, 0, displayText.length, bounds)
                val padH = 24f
                val padV = 16f
                val bgPaint = Paint().apply {
                    color = Color.parseColor(bgHex)
                    setAlpha((alpha.coerceIn(0f, 1f) * 200).toInt())
                }
                val bgRect = RectF(
                    x + bounds.left - padH,
                    y + bounds.top - padV,
                    x + bounds.right + padH,
                    y + bounds.bottom + padV
                )
                canvas.drawRoundRect(bgRect, 16f, 16f, bgPaint)
            } catch (_: Exception) {}
        }

        if (clip.shadow) {
            textPaint.setShadowLayer(14f, 4f, 4f, Color.argb((alpha * 200).toInt(), 0, 0, 0))
        }

        canvas.drawText(displayText, x, y, textPaint)
    }

    private fun createAdjustmentMatrix(adj: VideoAdjustment, filter: FilterType): ColorMatrix {
        val matrix = ColorMatrix()

        // 1. Brightness & Exposure
        val bShift = (adj.brightness + adj.exposure) * 255f
        val bMatrix = ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, bShift,
            0f, 1f, 0f, 0f, bShift,
            0f, 0f, 1f, 0f, bShift,
            0f, 0f, 0f, 1f, 0f
        ))
        matrix.postConcat(bMatrix)

        // 2. Contrast
        val c = adj.contrast
        val cShift = (1f - c) * 128f
        val cMatrix = ColorMatrix(floatArrayOf(
            c, 0f, 0f, 0f, cShift,
            0f, c, 0f, 0f, cShift,
            0f, 0f, c, 0f, cShift,
            0f, 0f, 0f, 1f, 0f
        ))
        matrix.postConcat(cMatrix)

        // 3. Saturation
        val satMatrix = ColorMatrix().apply { setSaturation(adj.saturation) }
        matrix.postConcat(satMatrix)

        // 4. Temperature
        if (adj.temperature != 0f) {
            val rScale = 1f + (adj.temperature * 0.25f)
            val bScale = 1f - (adj.temperature * 0.25f)
            val tMatrix = ColorMatrix(floatArrayOf(
                rScale, 0f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, bScale, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
            matrix.postConcat(tMatrix)
        }

        // 5. Preset Filter LUTs
        when (filter) {
            FilterType.CYBERPUNK -> {
                val fMat = ColorMatrix(floatArrayOf(
                    1.4f, 0f, 0.4f, 0f, 15f,
                    0f, 0.8f, 0.2f, 0f, -15f,
                    0.2f, 0.3f, 1.6f, 0f, 35f,
                    0f, 0f, 0f, 1f, 0f
                ))
                matrix.postConcat(fMat)
            }
            FilterType.TEAL_ORANGE -> {
                val fMat = ColorMatrix(floatArrayOf(
                    1.4f, 0f, 0f, 0f, 25f,
                    0f, 1.1f, 0.1f, 0f, 5f,
                    0f, 0.2f, 1.35f, 0f, 30f,
                    0f, 0f, 0f, 1f, 0f
                ))
                matrix.postConcat(fMat)
            }
            FilterType.NOIR -> {
                val bwMat = ColorMatrix().apply { setSaturation(0f) }
                matrix.postConcat(bwMat)
                val boost = ColorMatrix(floatArrayOf(
                    1.4f, 0f, 0f, 0f, -25f,
                    0f, 1.4f, 0f, 0f, -25f,
                    0f, 0f, 1.4f, 0f, -25f,
                    0f, 0f, 0f, 1f, 0f
                ))
                matrix.postConcat(boost)
            }
            FilterType.GOLDEN_HOUR -> {
                val fMat = ColorMatrix(floatArrayOf(
                    1.45f, 0f, 0f, 0f, 30f,
                    0.1f, 1.2f, 0f, 0f, 20f,
                    0f, 0f, 0.7f, 0f, -20f,
                    0f, 0f, 0f, 1f, 0f
                ))
                matrix.postConcat(fMat)
            }
            FilterType.RETRO_VHS -> {
                val fMat = ColorMatrix(floatArrayOf(
                    1.2f, 0.1f, 0f, 0f, 20f,
                    0f, 1.25f, 0.1f, 0f, 10f,
                    0.1f, 0f, 0.8f, 0f, 15f,
                    0f, 0f, 0f, 1f, 0f
                ))
                matrix.postConcat(fMat)
            }
            FilterType.MATRIX -> {
                val fMat = ColorMatrix(floatArrayOf(
                    0.4f, 0f, 0f, 0f, -15f,
                    0.1f, 1.6f, 0.1f, 0f, 30f,
                    0f, 0f, 0.5f, 0f, -15f,
                    0f, 0f, 0f, 1f, 0f
                ))
                matrix.postConcat(fMat)
            }
            FilterType.VIVID -> {
                val satBoost = ColorMatrix().apply { setSaturation(1.8f) }
                matrix.postConcat(satBoost)
            }
            FilterType.NONE -> {}
        }

        return matrix
    }

    private fun getClipBitmap(
        context: Context,
        clip: VideoClip,
        sourceTimeMs: Long,
        width: Int,
        height: Int
    ): Bitmap {
        if (!clip.uriString.isNullOrEmpty()) {
            val cacheKey = "${clip.uriString}_${sourceTimeMs / 200}"
            frameCache.get(cacheKey)?.let { return it }

            val retriever = getRetriever(context, clip.uriString)
            if (retriever != null) {
                try {
                    val frame = retriever.getFrameAtTime(
                        sourceTimeMs * 1000L,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    )
                    if (frame != null) {
                        frameCache.put(cacheKey, frame)
                        return frame
                    }
                } catch (_: Exception) {}
            }
        }

        return generateProceduralFrame(clip.sampleType, sourceTimeMs, width, height)
    }

    fun generateProceduralFrame(sampleType: String, timeMs: Long, width: Int, height: Int): Bitmap {
        val bmp = Bitmap.createBitmap(width.coerceAtLeast(320), height.coerceAtLeast(180), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val t = timeMs / 1000f

        when (sampleType) {
            "cyber_city" -> {
                // Cyberpunk city skyline with neon green & emerald grid
                val sky = LinearGradient(0f, 0f, 0f, height.toFloat(),
                    Color.rgb(5, 12, 8), Color.rgb(2, 35, 18), Shader.TileMode.CLAMP)
                canvas.drawPaint(Paint().apply { shader = sky })

                // Neon Green Grid Horizon
                val gridPaint = Paint().apply {
                    color = Color.argb(120, 0, 230, 118)
                    strokeWidth = 2.5f
                }
                val horizonY = height * 0.65f
                for (i in 0..10) {
                    val y = horizonY + (i * i * (height - horizonY) / 100f)
                    canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
                }
                for (i in -10..10) {
                    val xVanishing = width / 2f
                    val xBottom = (width / 2f) + (i * (width / 10f))
                    canvas.drawLine(xVanishing, horizonY, xBottom, height.toFloat(), gridPaint)
                }

                // Buildings
                val bPaint = Paint().apply { color = Color.rgb(6, 16, 10) }
                val numBuildings = 8
                for (b in 0 until numBuildings) {
                    val bx = b * (width / (numBuildings - 1f)) - 30f
                    val bWidth = (width / numBuildings.toFloat()) * 1.3f
                    val bHeight = height * (0.3f + ((b * 73) % 100) / 300f)
                    val by = horizonY - bHeight
                    canvas.drawRect(bx, by, bx + bWidth, horizonY, bPaint)

                    // Neon Windows
                    val winPaint = Paint().apply {
                        color = if (b % 2 == 0) Color.rgb(0, 255, 102) else Color.rgb(0, 230, 118)
                    }
                    val winCount = 12
                    for (w in 0 until winCount) {
                        val wx = bx + 15f + (w % 3) * 20f
                        val wy = by + 20f + (w / 3) * 35f
                        val blink = sin((t * 4f) + (b * 5) + w).toFloat()
                        if (blink > -0.2f) {
                            winPaint.alpha = (160 + blink * 95).toInt().coerceIn(40, 255)
                            canvas.drawRect(wx, wy, wx + 10f, wy + 16f, winPaint)
                        }
                    }
                }

                // Flying neon speeder
                val speederX = ((t * 420f) % (width + 200)) - 100f
                val speederY = height * 0.35f + sin(t * 3f) * 20f
                val speederPaint = Paint().apply {
                    color = Color.rgb(0, 255, 102)
                    strokeWidth = 6f
                }
                canvas.drawLine(speederX - 60f, speederY, speederX, speederY, speederPaint)
            }
            "sunset_coast" -> {
                val sunGradient = LinearGradient(0f, 0f, 0f, height.toFloat(),
                    intArrayOf(Color.rgb(255, 100, 0), Color.rgb(255, 180, 0), Color.rgb(10, 45, 20)),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawPaint(Paint().apply { shader = sunGradient })

                val sunY = height * 0.45f
                val sunPaint = Paint().apply {
                    color = Color.rgb(255, 245, 160)
                    isAntiAlias = true
                }
                canvas.drawCircle(width / 2f, sunY, height * 0.18f, sunPaint)

                val waterY = height * 0.55f
                val waterPaint = Paint().apply { color = Color.rgb(8, 30, 20) }
                canvas.drawRect(0f, waterY, width.toFloat(), height.toFloat(), waterPaint)

                val wavePaint = Paint().apply {
                    color = Color.argb(160, 0, 255, 120)
                    strokeWidth = 3f
                    isAntiAlias = true
                }
                for (w in 0..12) {
                    val wy = waterY + (w * 25f)
                    val waveOffset = sin((t * 2f) + w) * 20f
                    canvas.drawLine(width * 0.25f - waveOffset, wy, width * 0.75f + waveOffset, wy, wavePaint)
                }
            }
            "matrix_code" -> {
                canvas.drawColor(Color.BLACK)
                val codePaint = Paint().apply {
                    color = Color.rgb(0, 255, 102)
                    textSize = 24f
                    typeface = Typeface.MONOSPACE
                    isAntiAlias = true
                }
                val cols = 22
                val colWidth = width.toFloat() / cols
                val chars = "01010101789ABCDEFYUPICUTMATRIXPRO"
                for (c in 0 until cols) {
                    val speed = 160f + ((c * 37) % 150)
                    val yHead = ((t * speed) + (c * 80)) % (height + 300) - 50f
                    for (row in 0..15) {
                        val cy = yHead - (row * 24f)
                        if (cy in 0f..height.toFloat()) {
                            val charIdx = ((c * 5) + row + (t * 10).toInt()) % chars.length
                            val char = chars[charIdx.coerceAtLeast(0)]
                            codePaint.alpha = if (row == 0) 255 else (255 - row * 16).coerceAtLeast(30)
                            codePaint.color = if (row == 0) Color.WHITE else Color.rgb(0, 255, 102)
                            canvas.drawText(char.toString(), c * colWidth + 8f, cy, codePaint)
                        }
                    }
                }
            }
            "epic_lights" -> {
                canvas.drawColor(Color.rgb(4, 12, 8))
                val flareX = (width / 2f) + sin(t * 1.5f) * (width * 0.25f)
                val flareY = height * 0.4f
                val flareGrad = RadialGradient(
                    flareX, flareY, width * 0.5f,
                    intArrayOf(Color.argb(230, 0, 255, 120), Color.argb(90, 0, 180, 80), Color.TRANSPARENT),
                    floatArrayOf(0f, 0.4f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawPaint(Paint().apply { shader = flareGrad })

                val beamPaint = Paint().apply {
                    color = Color.argb(70, 0, 255, 120)
                    strokeWidth = 4f
                }
                canvas.drawLine(flareX, flareY, 0f, height.toFloat(), beamPaint)
                canvas.drawLine(flareX, flareY, width.toFloat(), height.toFloat(), beamPaint)
                canvas.drawLine(flareX, flareY, width / 2f, height.toFloat(), beamPaint)
            }
            else -> {
                canvas.drawColor(Color.rgb(8, 18, 12))
                val gPaint = Paint().apply {
                    color = Color.rgb(0, 230, 118)
                    strokeWidth = 2f
                }
                val step = 40f
                var x = 0f
                while (x < width) {
                    canvas.drawLine(x, 0f, x, height.toFloat(), gPaint)
                    x += step
                }
                var y = 0f
                while (y < height) {
                    canvas.drawLine(0f, y, width.toFloat(), y, gPaint)
                    y += step
                }
            }
        }

        return bmp
    }
}
