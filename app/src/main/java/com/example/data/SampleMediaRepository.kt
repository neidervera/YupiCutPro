package com.example.data

import com.example.model.*
import java.util.UUID
import kotlin.random.Random

data class SampleMediaItem(
    val id: String,
    val title: String,
    val category: String,
    val durationMs: Long,
    val sampleType: String,
    val thumbnailDesc: String
)

data class SampleAudioItem(
    val id: String,
    val title: String,
    val artist: String,
    val category: String,
    val durationMs: Long,
    val bpm: Int,
    val waveform: List<Float>
)

object SampleMediaRepository {

    val sampleVideos = listOf(
        SampleMediaItem(
            id = "vid_cyber_city",
            title = "Cyber City Neon 4K",
            category = "Sci-Fi",
            durationMs = 8000L,
            sampleType = "cyber_city",
            thumbnailDesc = "Rascacielos con luces de neón cian y magenta"
        ),
        SampleMediaItem(
            id = "vid_sunset_coast",
            title = "Sunset Ocean Waves",
            category = "Naturaleza",
            durationMs = 7000L,
            sampleType = "sunset_coast",
            thumbnailDesc = "Atardecer dorado sobre olas del océano"
        ),
        SampleMediaItem(
            id = "vid_matrix_code",
            title = "Digital Matrix Stream",
            category = "Tecnología",
            durationMs = 6000L,
            sampleType = "matrix_code",
            thumbnailDesc = "Lluvia de código binario y datos cuánticos"
        ),
        SampleMediaItem(
            id = "vid_epic_lights",
            title = "Studio Flare & Particles",
            category = "Cinemático",
            durationMs = 5000L,
            sampleType = "epic_lights",
            thumbnailDesc = "Focos cinemáticos con polvo brillante"
        ),
        SampleMediaItem(
            id = "vid_tech_glitch",
            title = "Cyber Glitch Grid",
            category = "Efectos",
            durationMs = 5500L,
            sampleType = "tech_glitch",
            thumbnailDesc = "Rejilla 3D de alta velocidad con distorsión"
        )
    )

    fun generateWaveform(count: Int = 40): List<Float> {
        val rand = Random(42)
        return (0 until count).map {
            (0.15f + rand.nextFloat() * 0.85f)
        }
    }

    val sampleAudios = listOf(
        SampleAudioItem(
            id = "aud_synthwave",
            title = "Midnight Cyber Drive",
            artist = "Yupi Beats",
            category = "Synthwave",
            durationMs = 12000L,
            bpm = 124,
            waveform = generateWaveform(60)
        ),
        SampleAudioItem(
            id = "aud_cinematic",
            title = "Hyperion Rising",
            artist = "Orchestral FX",
            category = "Épico",
            durationMs = 10000L,
            bpm = 95,
            waveform = generateWaveform(50)
        ),
        SampleAudioItem(
            id = "aud_lofi",
            title = "Coffee & Memories",
            artist = "Chill Wave",
            category = "Lo-Fi",
            durationMs = 14000L,
            bpm = 82,
            waveform = generateWaveform(70)
        ),
        SampleAudioItem(
            id = "aud_upbeat",
            title = "Summer Vlog Energy",
            artist = "Pop Groove",
            category = "Vlog",
            durationMs = 9000L,
            bpm = 128,
            waveform = generateWaveform(45)
        )
    )

    val soundEffects = listOf(
        "Whoosh Rápido" to 600L,
        "Transición Glitch" to 800L,
        "Bass Drop Profundo" to 1400L,
        "Impacto Cinemático" to 1200L,
        "Click de Cámara" to 300L,
        "Rewind Tape" to 900L
    )

    fun createInitialDemoProject(): Project {
        val clip1 = VideoClip(
            id = UUID.randomUUID().toString(),
            title = "Cyber City Neon",
            durationMs = 5000L,
            startTrimMs = 0L,
            endTrimMs = 5000L,
            timelineStartMs = 0L,
            sampleType = "cyber_city",
            adjustments = VideoAdjustment(brightness = 0.05f, contrast = 1.15f, saturation = 1.25f),
            filter = FilterType.CYBERPUNK
        )

        val clip2 = VideoClip(
            id = UUID.randomUUID().toString(),
            title = "Sunset Coast",
            durationMs = 6000L,
            startTrimMs = 0L,
            endTrimMs = 6000L,
            timelineStartMs = 5000L,
            sampleType = "sunset_coast",
            transition = TransitionType.DISSOLVE,
            transitionDurationMs = 800L,
            adjustments = VideoAdjustment(temperature = 0.3f, saturation = 1.1f),
            filter = FilterType.GOLDEN_HOUR
        )

        val clip3 = VideoClip(
            id = UUID.randomUUID().toString(),
            title = "Studio Flares",
            durationMs = 4000L,
            startTrimMs = 0L,
            endTrimMs = 4000L,
            timelineStartMs = 11000L,
            sampleType = "epic_lights",
            transition = TransitionType.GLITCH,
            transitionDurationMs = 600L,
            filter = FilterType.TEAL_ORANGE
        )

        val musicClip = AudioClip(
            id = UUID.randomUUID().toString(),
            title = "Midnight Cyber Drive",
            durationMs = 15000L,
            startTrimMs = 0L,
            endTrimMs = 15000L,
            timelineStartMs = 0L,
            volume = 0.85f,
            fadeInMs = 800L,
            fadeOutMs = 1200L,
            waveform = generateWaveform(75),
            sampleTrack = "aud_synthwave"
        )

        val titleClip = TextClip(
            id = UUID.randomUUID().toString(),
            text = "YUPICUT PRO 2026",
            timelineStartMs = 500L,
            durationMs = 3500L,
            fontSizeSp = 28,
            textColorHex = "#00FF66",
            outlineColorHex = "#070A08",
            shadow = true,
            animation = TextAnimation.BOUNCE,
            positionX = 0f,
            positionY = -0.5f
        )

        val subtitleClip = TextClip(
            id = UUID.randomUUID().toString(),
            text = "Edición cinematográfica al instante",
            timelineStartMs = 4500L,
            durationMs = 3000L,
            fontSizeSp = 18,
            textColorHex = "#FFFFFF",
            bgColorHex = "#80000000",
            animation = TextAnimation.FADE,
            positionX = 0f,
            positionY = 0.65f
        )

        return Project(
            id = "demo_project_1",
            name = "Edición Cyberpunk 4K",
            lastModified = System.currentTimeMillis(),
            aspectRatio = AspectRatio.RATIO_9_16,
            videoClips = listOf(clip1, clip2, clip3),
            audioClips = listOf(musicClip),
            textClips = listOf(titleClip, subtitleClip),
            exportResolution = "1080p",
            exportFps = 30,
            exportBitrateKbps = 12000
        )
    }
}
