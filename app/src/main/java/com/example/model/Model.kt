package com.example.model

import java.util.UUID

enum class AspectRatio(val label: String, val ratio: Float, val iconName: String) {
    RATIO_16_9("16:9 (Horizontal)", 16f / 9f, "16:9"),
    RATIO_9_16("9:16 (TikTok/Reels)", 9f / 16f, "9:16"),
    RATIO_1_1("1:1 (Cuadrado)", 1f, "1:1"),
    RATIO_4_5("4:5 (Instagram)", 4f / 5f, "4:5"),
    RATIO_21_9("21:9 (Cine)", 21f / 9f, "21:9")
}

data class VideoAdjustment(
    val brightness: Float = 0f,      // -100 to 100 mapped to -1f to 1f
    val contrast: Float = 1f,        // 0.5f to 2f (1f is default)
    val saturation: Float = 1f,      // 0f to 2f (1f is default)
    val temperature: Float = 0f,     // -1f (cool/blue) to 1f (warm/amber)
    val exposure: Float = 0f,        // -1f to 1f
    val sharpness: Float = 0f,       // 0f to 1f
    val vignette: Float = 0f,        // 0f to 1f
    val speed: Float = 1f,           // 0.25f to 4f
    val rotation: Int = 0,           // 0, 90, 180, 270
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false
)

enum class FilterType(val displayName: String, val description: String) {
    NONE("Original", "Sin filtro"),
    CYBERPUNK("Cyberpunk", "Tonos cian y magenta futuristas"),
    TEAL_ORANGE("Teal & Orange", "Look cinematográfico de Hollywood"),
    NOIR("Noir Blanco y Negro", "Alto contraste dramático"),
    GOLDEN_HOUR("Golden Hour", "Calidez dorada de atardecer"),
    RETRO_VHS("Retro VHS", "Estilo vintage años 90"),
    MATRIX("Matrix Neo", "Tinte verdoso de código sci-fi"),
    VIVID("Vivid Pop", "Colores ultrabrillantes y saturados")
}

enum class EffectType(val displayName: String) {
    NONE("Ninguno"),
    GLITCH("Glitch RGB"),
    BLUR("Desenfoque"),
    ZOOM_PULSE("Zoom Pulse"),
    FADE_BLACK("Fundido a Negro"),
    RGB_SPLIT("Aberración Cromática")
}

enum class TransitionType(val displayName: String) {
    NONE("Corte"),
    DISSOLVE("Disolución"),
    SLIDE_LEFT("Deslizar Izq"),
    SLIDE_RIGHT("Deslizar Der"),
    WIPE("Barrido"),
    ZOOM_IN("Zoom In"),
    FLASH_WHITE("Flash Blanco"),
    GLITCH("Transición Glitch")
}

enum class TextAnimation(val displayName: String) {
    NONE("Estático"),
    FADE("Desvanecer"),
    TYPEWRITER("Máquina de escribir"),
    SLIDE_UP("Subir"),
    BOUNCE("Rebote"),
    GLOW("Resplandor Neón")
}

data class VideoClip(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val uriString: String? = null,
    val durationMs: Long = 4000L,
    val startTrimMs: Long = 0L,
    val endTrimMs: Long = 4000L,
    val timelineStartMs: Long = 0L,
    val adjustments: VideoAdjustment = VideoAdjustment(),
    val filter: FilterType = FilterType.NONE,
    val effect: EffectType = EffectType.NONE,
    val transition: TransitionType = TransitionType.NONE,
    val transitionDurationMs: Long = 800L,
    val volume: Float = 1.0f,
    val sampleType: String = "cyber_city" // Built-in sample renderer
) {
    val activeDurationMs: Long
        get() = ((endTrimMs - startTrimMs) / adjustments.speed).toLong().coerceAtLeast(200L)
}

data class AudioClip(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val uriString: String? = null,
    val durationMs: Long = 6000L,
    val startTrimMs: Long = 0L,
    val endTrimMs: Long = 6000L,
    val timelineStartMs: Long = 0L,
    val volume: Float = 1.0f,
    val fadeInMs: Long = 500L,
    val fadeOutMs: Long = 500L,
    val speed: Float = 1.0f,
    val waveform: List<Float> = emptyList(),
    val sampleTrack: String? = null
) {
    val activeDurationMs: Long
        get() = ((endTrimMs - startTrimMs) / speed).toLong().coerceAtLeast(200L)
}

data class TextClip(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val timelineStartMs: Long = 0L,
    val durationMs: Long = 3000L,
    val fontSizeSp: Int = 24,
    val textColorHex: String = "#FFFFFF",
    val bgColorHex: String? = null,
    val outlineColorHex: String? = "#000000",
    val shadow: Boolean = true,
    val animation: TextAnimation = TextAnimation.FADE,
    val positionX: Float = 0f, // -1f (left) to 1f (right)
    val positionY: Float = 0.6f // -1f (top) to 1f (bottom)
)

data class StickerClip(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val emoji: String,
    val timelineStartMs: Long = 0L,
    val durationMs: Long = 3000L,
    val positionX: Float = 0f,
    val positionY: Float = 0f,
    val scale: Float = 1.0f
)

data class Project(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Proyecto Nuevo",
    val lastModified: Long = System.currentTimeMillis(),
    val aspectRatio: AspectRatio = AspectRatio.RATIO_9_16,
    val videoClips: List<VideoClip> = emptyList(),
    val audioClips: List<AudioClip> = emptyList(),
    val textClips: List<TextClip> = emptyList(),
    val stickerClips: List<StickerClip> = emptyList(),
    val exportResolution: String = "1080p",
    val exportFps: Int = 30,
    val exportBitrateKbps: Int = 12000,
    val exportCodec: String = "H.264",
    val exportFormat: String = "mp4",
    val watermark: Boolean = false
) {
    val totalDurationMs: Long
        get() {
            var maxEnd = 0L
            videoClips.forEach {
                val end = it.timelineStartMs + it.activeDurationMs
                if (end > maxEnd) maxEnd = end
            }
            audioClips.forEach {
                val end = it.timelineStartMs + it.activeDurationMs
                if (end > maxEnd) maxEnd = end
            }
            textClips.forEach {
                val end = it.timelineStartMs + it.durationMs
                if (end > maxEnd) maxEnd = end
            }
            stickerClips.forEach {
                val end = it.timelineStartMs + it.durationMs
                if (end > maxEnd) maxEnd = end
            }
            return if (maxEnd > 0L) maxEnd else 5000L
        }
}
