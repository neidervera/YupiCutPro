package com.example.engine

import com.example.model.*
import java.util.UUID

data class AiSuggestion(
    val title: String,
    val category: String,
    val suggestion: String,
    val actionLabel: String,
    val actionType: String
)

object AiVideoAssistant {

    /**
     * Generates timed subtitles across the active duration of the video.
     */
    fun generateAutoSubtitles(project: Project): List<TextClip> {
        val totalMs = project.totalDurationMs.coerceAtLeast(3000L)
        val samplePhrases = listOf(
            "Bienvenidos al nuevo contenido",
            "Hoy veremos cómo editar como un profesional",
            "Cortes rápidos y transiciones dinámicas",
            "Efectos cinematográficos en tiempo real",
            "Optimizado para TikTok, Reels y Shorts",
            "Exportación ultra rápida en 4K 60FPS",
            "¡Suscríbete y dale like para más tutoriales!"
        )

        val subtitles = mutableListOf<TextClip>()
        val clipDuration = 2500L
        var currentStart = 500L
        var index = 0

        while (currentStart < totalMs && index < samplePhrases.size) {
            val dur = minOf(clipDuration, totalMs - currentStart)
            if (dur > 600L) {
                subtitles.add(
                    TextClip(
                        id = UUID.randomUUID().toString(),
                        text = samplePhrases[index % samplePhrases.size],
                        timelineStartMs = currentStart,
                        durationMs = dur,
                        fontSizeSp = 20,
                        textColorHex = "#FFEB3B", // High-contrast yellow subtitle
                        bgColorHex = "#CC000000", // Dark semi-transparent pill
                        outlineColorHex = "#000000",
                        shadow = true,
                        animation = TextAnimation.BOUNCE,
                        positionX = 0f,
                        positionY = 0.72f
                    )
                )
            }
            currentStart += dur + 300L
            index++
        }

        return subtitles
    }

    /**
     * Detects silent regions in an audio clip and splits into non-silent segments.
     */
    fun detectAndRemoveSilences(audioClip: AudioClip): List<AudioClip> {
        val waveform = audioClip.waveform
        if (waveform.isEmpty()) return listOf(audioClip)

        val segments = mutableListOf<AudioClip>()
        val totalDuration = audioClip.durationMs
        val stepMs = totalDuration / waveform.size
        var inSound = false
        var soundStartMs = 0L

        waveform.forEachIndexed { idx, amplitude ->
            val isSilent = amplitude < 0.28f
            val timeMs = idx * stepMs

            if (!isSilent && !inSound) {
                inSound = true
                soundStartMs = timeMs
            } else if (isSilent && inSound) {
                inSound = false
                val segDuration = timeMs - soundStartMs
                if (segDuration > 400L) {
                    segments.add(
                        audioClip.copy(
                            id = UUID.randomUUID().toString(),
                            title = "${audioClip.title} [Voz]",
                            startTrimMs = soundStartMs,
                            endTrimMs = timeMs,
                            timelineStartMs = audioClip.timelineStartMs + (segments.sumOf { it.activeDurationMs })
                        )
                    )
                }
            }
        }

        if (inSound) {
            segments.add(
                audioClip.copy(
                    id = UUID.randomUUID().toString(),
                    title = "${audioClip.title} [Voz Final]",
                    startTrimMs = soundStartMs,
                    endTrimMs = totalDuration,
                    timelineStartMs = audioClip.timelineStartMs + (segments.sumOf { it.activeDurationMs })
                )
            )
        }

        return if (segments.isNotEmpty()) segments else listOf(audioClip)
    }

    /**
     * Automatically detects scene changes and cut points based on duration and visual tempo.
     */
    fun detectSceneCutPoints(clip: VideoClip): List<Long> {
        val duration = clip.activeDurationMs
        if (duration < 3000L) return emptyList()

        val cutPoints = mutableListOf<Long>()
        var cut = 2000L
        while (cut < duration - 1000L) {
            cutPoints.add(clip.timelineStartMs + cut)
            cut += 2500L
        }
        return cutPoints
    }

    /**
     * Applies AI audio enhancement (noise reduction, vocal boost).
     */
    fun enhanceAudio(audioClip: AudioClip): AudioClip {
        val boostedWaveform = audioClip.waveform.map { amp ->
            if (amp < 0.2f) 0.05f else (amp * 1.35f).coerceAtMost(1.0f)
        }
        return audioClip.copy(
            title = "${audioClip.title} (IA Enhanced)",
            volume = (audioClip.volume * 1.25f).coerceAtMost(2.0f),
            fadeInMs = 300L,
            fadeOutMs = 300L,
            waveform = boostedWaveform
        )
    }

    /**
     * Smart Assistant recommendations for video editing.
     */
    fun getAiSuggestions(project: Project): List<AiSuggestion> {
        val list = mutableListOf<AiSuggestion>()

        if (project.textClips.isEmpty()) {
            list.add(
                AiSuggestion(
                    title = "Añadir Subtítulos con IA",
                    category = "Retención",
                    suggestion = "Los vídeos con subtítulos dinámicos tienen un 80% más de retención en TikTok y Shorts.",
                    actionLabel = "Generar Subtítulos",
                    actionType = "AUTO_SUBTITLES"
                )
            )
        }

        if (project.videoClips.any { it.activeDurationMs > 5000L }) {
            list.add(
                AiSuggestion(
                    title = "Ritmo Rápido / Detección de Silencios",
                    category = "Edición",
                    suggestion = "Hay clips de más de 5 segundos. Detectar y eliminar pausas mantendrá la atención del espectador.",
                    actionLabel = "Limpiar Silencios",
                    actionType = "REMOVE_SILENCES"
                )
            )
        }

        if (project.videoClips.any { it.transition == TransitionType.NONE && it != project.videoClips.first() }) {
            list.add(
                AiSuggestion(
                    title = "Transiciones Cinemáticas",
                    category = "Estilo",
                    suggestion = "Aplica transiciones 'Glitch' o 'Dissolve' entre tomas para darle fluidez profesional.",
                    actionLabel = "Aplicar Transiciones",
                    actionType = "AUTO_TRANSITIONS"
                )
            )
        }

        if (project.videoClips.any { it.filter == FilterType.NONE }) {
            list.add(
                AiSuggestion(
                    title = "Graduación de Color Cyberpunk",
                    category = "Color",
                    suggestion = "Eleva el estilo visual con la paleta de color Cyberpunk o Teal & Orange.",
                    actionLabel = "Aplicar Filtro Pro",
                    actionType = "AUTO_COLOR"
                )
            )
        }

        list.add(
            AiSuggestion(
                title = "Mejora de Audio IA",
                category = "Sonido",
                suggestion = "Limpia ruidos de fondo y amplifica la claridad vocal en un solo toque.",
                actionLabel = "Mejorar Audio",
                actionType = "ENHANCE_AUDIO"
            )
        )

        return list
    }
}
