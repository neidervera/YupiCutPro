package com.example.data

import android.content.Context
import com.example.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ProjectStorage(private val context: Context) {

    private val projectsDir: File by lazy {
        File(context.filesDir, "yupicut_projects").apply { if (!exists()) mkdirs() }
    }

    fun getAllProjects(): List<Project> {
        val list = mutableListOf<Project>()
        val files = projectsDir.listFiles { _, name -> name.endsWith(".json") }
        if (files.isNullOrEmpty()) {
            // Seed default demo project
            val demo = SampleMediaRepository.createInitialDemoProject()
            saveProject(demo)
            list.add(demo)
        } else {
            files.sortedByDescending { it.lastModified() }.forEach { file ->
                try {
                    val jsonStr = file.readText()
                    val project = parseProject(jsonStr)
                    list.add(project)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return list
    }

    fun getProject(id: String): Project? {
        val file = File(projectsDir, "$id.json")
        if (!file.exists()) return null
        return try {
            parseProject(file.readText())
        } catch (e: Exception) {
            null
        }
    }

    fun saveProject(project: Project) {
        val file = File(projectsDir, "${project.id}.json")
        val json = serializeProject(project)
        file.writeText(json.toString(2))
    }

    fun deleteProject(id: String): Boolean {
        val file = File(projectsDir, "$id.json")
        return if (file.exists()) file.delete() else false
    }

    fun duplicateProject(project: Project): Project {
        val newProj = project.copy(
            id = java.util.UUID.randomUUID().toString(),
            name = "${project.name} (Copia)",
            lastModified = System.currentTimeMillis()
        )
        saveProject(newProj)
        return newProj
    }

    private fun serializeProject(project: Project): JSONObject {
        val root = JSONObject()
        root.put("id", project.id)
        root.put("name", project.name)
        root.put("lastModified", project.lastModified)
        root.put("aspectRatio", project.aspectRatio.name)
        root.put("exportResolution", project.exportResolution)
        root.put("exportFps", project.exportFps)
        root.put("exportBitrateKbps", project.exportBitrateKbps)
        root.put("exportCodec", project.exportCodec)
        root.put("exportFormat", project.exportFormat)
        root.put("watermark", project.watermark)

        // Video clips
        val vidArray = JSONArray()
        project.videoClips.forEach { clip ->
            val vObj = JSONObject()
            vObj.put("id", clip.id)
            vObj.put("title", clip.title)
            vObj.put("uriString", clip.uriString ?: "")
            vObj.put("durationMs", clip.durationMs)
            vObj.put("startTrimMs", clip.startTrimMs)
            vObj.put("endTrimMs", clip.endTrimMs)
            vObj.put("timelineStartMs", clip.timelineStartMs)
            vObj.put("sampleType", clip.sampleType)
            vObj.put("filter", clip.filter.name)
            vObj.put("effect", clip.effect.name)
            vObj.put("transition", clip.transition.name)
            vObj.put("transitionDurationMs", clip.transitionDurationMs)
            vObj.put("volume", clip.volume.toDouble())

            val adjObj = JSONObject()
            adjObj.put("brightness", clip.adjustments.brightness.toDouble())
            adjObj.put("contrast", clip.adjustments.contrast.toDouble())
            adjObj.put("saturation", clip.adjustments.saturation.toDouble())
            adjObj.put("temperature", clip.adjustments.temperature.toDouble())
            adjObj.put("exposure", clip.adjustments.exposure.toDouble())
            adjObj.put("sharpness", clip.adjustments.sharpness.toDouble())
            adjObj.put("vignette", clip.adjustments.vignette.toDouble())
            adjObj.put("speed", clip.adjustments.speed.toDouble())
            adjObj.put("rotation", clip.adjustments.rotation)
            adjObj.put("flipHorizontal", clip.adjustments.flipHorizontal)
            adjObj.put("flipVertical", clip.adjustments.flipVertical)
            vObj.put("adjustments", adjObj)

            vidArray.put(vObj)
        }
        root.put("videoClips", vidArray)

        // Audio clips
        val audArray = JSONArray()
        project.audioClips.forEach { clip ->
            val aObj = JSONObject()
            aObj.put("id", clip.id)
            aObj.put("title", clip.title)
            aObj.put("uriString", clip.uriString ?: "")
            aObj.put("durationMs", clip.durationMs)
            aObj.put("startTrimMs", clip.startTrimMs)
            aObj.put("endTrimMs", clip.endTrimMs)
            aObj.put("timelineStartMs", clip.timelineStartMs)
            aObj.put("volume", clip.volume.toDouble())
            aObj.put("fadeInMs", clip.fadeInMs)
            aObj.put("fadeOutMs", clip.fadeOutMs)
            aObj.put("speed", clip.speed.toDouble())
            aObj.put("sampleTrack", clip.sampleTrack ?: "")
            audArray.put(aObj)
        }
        root.put("audioClips", audArray)

        // Text clips
        val txtArray = JSONArray()
        project.textClips.forEach { clip ->
            val tObj = JSONObject()
            tObj.put("id", clip.id)
            tObj.put("text", clip.text)
            tObj.put("timelineStartMs", clip.timelineStartMs)
            tObj.put("durationMs", clip.durationMs)
            tObj.put("fontSizeSp", clip.fontSizeSp)
            tObj.put("textColorHex", clip.textColorHex)
            tObj.put("bgColorHex", clip.bgColorHex ?: "")
            tObj.put("outlineColorHex", clip.outlineColorHex ?: "")
            tObj.put("shadow", clip.shadow)
            tObj.put("animation", clip.animation.name)
            tObj.put("positionX", clip.positionX.toDouble())
            tObj.put("positionY", clip.positionY.toDouble())
            txtArray.put(tObj)
        }
        root.put("textClips", txtArray)

        return root
    }

    private fun parseProject(jsonStr: String): Project {
        val root = JSONObject(jsonStr)
        val id = root.optString("id", java.util.UUID.randomUUID().toString())
        val name = root.optString("name", "Proyecto")
        val lastModified = root.optLong("lastModified", System.currentTimeMillis())
        val aspect = try {
            AspectRatio.valueOf(root.optString("aspectRatio", AspectRatio.RATIO_9_16.name))
        } catch (_: Exception) {
            AspectRatio.RATIO_9_16
        }
        val exportResolution = root.optString("exportResolution", "1080p")
        val exportFps = root.optInt("exportFps", 30)
        val exportBitrateKbps = root.optInt("exportBitrateKbps", 12000)
        val exportCodec = root.optString("exportCodec", "H.264")
        val exportFormat = root.optString("exportFormat", "mp4")
        val watermark = root.optBoolean("watermark", false)

        val vClips = mutableListOf<VideoClip>()
        val vidArray = root.optJSONArray("videoClips")
        if (vidArray != null) {
            for (i in 0 until vidArray.length()) {
                val vObj = vidArray.getJSONObject(i)
                val adjObj = vObj.optJSONObject("adjustments")
                val adj = if (adjObj != null) {
                    VideoAdjustment(
                        brightness = adjObj.optDouble("brightness", 0.0).toFloat(),
                        contrast = adjObj.optDouble("contrast", 1.0).toFloat(),
                        saturation = adjObj.optDouble("saturation", 1.0).toFloat(),
                        temperature = adjObj.optDouble("temperature", 0.0).toFloat(),
                        exposure = adjObj.optDouble("exposure", 0.0).toFloat(),
                        sharpness = adjObj.optDouble("sharpness", 0.0).toFloat(),
                        vignette = adjObj.optDouble("vignette", 0.0).toFloat(),
                        speed = adjObj.optDouble("speed", 1.0).toFloat(),
                        rotation = adjObj.optInt("rotation", 0),
                        flipHorizontal = adjObj.optBoolean("flipHorizontal", false),
                        flipVertical = adjObj.optBoolean("flipVertical", false)
                    )
                } else VideoAdjustment()

                vClips.add(
                    VideoClip(
                        id = vObj.optString("id", java.util.UUID.randomUUID().toString()),
                        title = vObj.optString("title", "Clip"),
                        uriString = vObj.optString("uriString").takeIf { it.isNotEmpty() },
                        durationMs = vObj.optLong("durationMs", 4000L),
                        startTrimMs = vObj.optLong("startTrimMs", 0L),
                        endTrimMs = vObj.optLong("endTrimMs", 4000L),
                        timelineStartMs = vObj.optLong("timelineStartMs", 0L),
                        sampleType = vObj.optString("sampleType", "cyber_city"),
                        filter = try { FilterType.valueOf(vObj.optString("filter", "NONE")) } catch (_: Exception) { FilterType.NONE },
                        effect = try { EffectType.valueOf(vObj.optString("effect", "NONE")) } catch (_: Exception) { EffectType.NONE },
                        transition = try { TransitionType.valueOf(vObj.optString("transition", "NONE")) } catch (_: Exception) { TransitionType.NONE },
                        transitionDurationMs = vObj.optLong("transitionDurationMs", 800L),
                        volume = vObj.optDouble("volume", 1.0).toFloat(),
                        adjustments = adj
                    )
                )
            }
        }

        val aClips = mutableListOf<AudioClip>()
        val audArray = root.optJSONArray("audioClips")
        if (audArray != null) {
            for (i in 0 until audArray.length()) {
                val aObj = audArray.getJSONObject(i)
                aClips.add(
                    AudioClip(
                        id = aObj.optString("id", java.util.UUID.randomUUID().toString()),
                        title = aObj.optString("title", "Audio"),
                        uriString = aObj.optString("uriString").takeIf { it.isNotEmpty() },
                        durationMs = aObj.optLong("durationMs", 6000L),
                        startTrimMs = aObj.optLong("startTrimMs", 0L),
                        endTrimMs = aObj.optLong("endTrimMs", 6000L),
                        timelineStartMs = aObj.optLong("timelineStartMs", 0L),
                        volume = aObj.optDouble("volume", 1.0).toFloat(),
                        fadeInMs = aObj.optLong("fadeInMs", 500L),
                        fadeOutMs = aObj.optLong("fadeOutMs", 500L),
                        speed = aObj.optDouble("speed", 1.0).toFloat(),
                        waveform = SampleMediaRepository.generateWaveform(40),
                        sampleTrack = aObj.optString("sampleTrack").takeIf { it.isNotEmpty() }
                    )
                )
            }
        }

        val tClips = mutableListOf<TextClip>()
        val txtArray = root.optJSONArray("textClips")
        if (txtArray != null) {
            for (i in 0 until txtArray.length()) {
                val tObj = txtArray.getJSONObject(i)
                tClips.add(
                    TextClip(
                        id = tObj.optString("id", java.util.UUID.randomUUID().toString()),
                        text = tObj.optString("text", "Texto"),
                        timelineStartMs = tObj.optLong("timelineStartMs", 0L),
                        durationMs = tObj.optLong("durationMs", 3000L),
                        fontSizeSp = tObj.optInt("fontSizeSp", 24),
                        textColorHex = tObj.optString("textColorHex", "#FFFFFF"),
                        bgColorHex = tObj.optString("bgColorHex").takeIf { it.isNotEmpty() },
                        outlineColorHex = tObj.optString("outlineColorHex").takeIf { it.isNotEmpty() },
                        shadow = tObj.optBoolean("shadow", true),
                        animation = try { TextAnimation.valueOf(tObj.optString("animation", "FADE")) } catch (_: Exception) { TextAnimation.FADE },
                        positionX = tObj.optDouble("positionX", 0.0).toFloat(),
                        positionY = tObj.optDouble("positionY", 0.6).toFloat()
                    )
                )
            }
        }

        return Project(
            id = id,
            name = name,
            lastModified = lastModified,
            aspectRatio = aspect,
            videoClips = vClips,
            audioClips = aClips,
            textClips = tClips,
            exportResolution = exportResolution,
            exportFps = exportFps,
            exportBitrateKbps = exportBitrateKbps,
            exportCodec = exportCodec,
            exportFormat = exportFormat,
            watermark = watermark
        )
    }
}
