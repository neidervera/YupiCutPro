package com.example.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import com.example.data.SampleMediaRepository
import com.example.model.AudioClip
import com.example.model.Project
import com.example.model.VideoClip
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import kotlin.math.*

object AudioEngine {

    private var previewPlayer: MediaPlayer? = null
    private var timelinePlayer: MediaPlayer? = null
    private var sfxPlayer: MediaPlayer? = null
    private var currentTimelineClipId: String? = null
    private var mediaRecorder: MediaRecorder? = null
    private var currentRecordingFile: File? = null

    /**
     * Pre-generates or retrieves a valid WAV audio file for sample tracks.
     */
    fun getOrCreateSampleAudioFile(context: Context, trackId: String, durationSec: Int = 10): File {
        val audioDir = File(context.cacheDir, "audio_samples").apply { if (!exists()) mkdirs() }
        val file = File(audioDir, "${trackId}.wav")
        if (file.exists() && file.length() > 1000L) {
            return file
        }

        generateWavAudioFile(file, trackId, durationSec)
        return file
    }

    /**
     * Plays an instant preview of a sound effect or music track.
     */
    fun playSoundEffect(context: Context, sfxName: String) {
        try {
            sfxPlayer?.stop()
            sfxPlayer?.release()
        } catch (_: Exception) {}

        try {
            val file = getOrCreateSampleAudioFile(context, sfxName, durationSec = 2)
            sfxPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .build()
                )
                setDataSource(context, Uri.fromFile(file))
                setVolume(1.0f, 1.0f)
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Starts synchronized playback of timeline audio clips at [playheadMs].
     */
    fun syncTimelinePlayback(context: Context, project: Project, playheadMs: Long) {
        val activeAudio = project.audioClips.firstOrNull {
            playheadMs >= it.timelineStartMs && playheadMs < (it.timelineStartMs + it.activeDurationMs)
        }

        if (activeAudio == null) {
            stopTimelineAudio()
            return
        }

        val offsetInClip = (playheadMs - activeAudio.timelineStartMs).toInt()

        // If already playing this clip, keep playing
        if (timelinePlayer != null && currentTimelineClipId == activeAudio.id) {
            return
        }

        stopTimelineAudio()
        currentTimelineClipId = activeAudio.id

        try {
            val audioFile = if (!activeAudio.uriString.isNullOrEmpty()) {
                File(Uri.parse(activeAudio.uriString).path ?: "")
            } else {
                val trackKey = activeAudio.sampleTrack ?: "aud_synthwave"
                getOrCreateSampleAudioFile(context, trackKey, durationSec = (activeAudio.durationMs / 1000).toInt().coerceAtLeast(6))
            }

            timelinePlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(context, Uri.fromFile(audioFile))
                prepare()
                val seekPos = (activeAudio.startTrimMs + offsetInClip).coerceIn(0L, duration.toLong()).toInt()
                seekTo(seekPos)
                val vol = activeAudio.volume.coerceIn(0f, 2f)
                setVolume(vol.coerceAtMost(1f), vol.coerceAtMost(1f))
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopTimelineAudio() {
        try {
            timelinePlayer?.stop()
            timelinePlayer?.release()
        } catch (_: Exception) {}
        timelinePlayer = null
        currentTimelineClipId = null
    }

    fun playPreviewAudio(context: Context, audioClip: AudioClip, startOffsetMs: Long) {
        stopPreview()
        try {
            val audioFile = if (!audioClip.uriString.isNullOrEmpty()) {
                File(Uri.parse(audioClip.uriString).path ?: "")
            } else {
                val trackKey = audioClip.sampleTrack ?: "aud_synthwave"
                getOrCreateSampleAudioFile(context, trackKey, durationSec = (audioClip.durationMs / 1000).toInt().coerceAtLeast(6))
            }

            previewPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(context, Uri.fromFile(audioFile))
                prepare()
                val seekTo = (audioClip.startTrimMs + startOffsetMs).coerceAtMost(duration.toLong()).toInt()
                seekTo(seekTo)
                val vol = audioClip.volume.coerceIn(0f, 1f)
                setVolume(vol, vol)
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopPreview() {
        try {
            previewPlayer?.stop()
            previewPlayer?.release()
        } catch (_: Exception) {}
        previewPlayer = null
    }

    fun startRecordingVoiceover(context: Context): File? {
        val audioDir = File(context.filesDir, "recordings").apply { if (!exists()) mkdirs() }
        val file = File(audioDir, "voice_${System.currentTimeMillis()}.m4a")
        currentRecordingFile = file

        try {
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                prepare()
                start()
            }
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun stopRecordingVoiceover(): AudioClip? {
        val file = currentRecordingFile ?: return null
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (_: Exception) {}
        mediaRecorder = null
        currentRecordingFile = null

        val duration = 5000L
        return AudioClip(
            id = UUID.randomUUID().toString(),
            title = "Grabación de Voz en Off",
            uriString = Uri.fromFile(file).toString(),
            durationMs = duration,
            startTrimMs = 0L,
            endTrimMs = duration,
            timelineStartMs = 0L,
            volume = 1.0f,
            waveform = SampleMediaRepository.generateWaveform(45)
        )
    }

    fun extractAudioFromVideo(videoClip: VideoClip): AudioClip {
        val duration = videoClip.activeDurationMs
        return AudioClip(
            id = UUID.randomUUID().toString(),
            title = "Audio extraído: ${videoClip.title}",
            uriString = videoClip.uriString,
            durationMs = duration,
            startTrimMs = videoClip.startTrimMs,
            endTrimMs = videoClip.endTrimMs,
            timelineStartMs = videoClip.timelineStartMs,
            volume = videoClip.volume,
            waveform = SampleMediaRepository.generateWaveform(50),
            sampleTrack = "aud_synthwave"
        )
    }

    /**
     * Synthesizes audio samples and writes a valid PCM WAV file.
     */
    private fun generateWavAudioFile(outputFile: File, style: String, durationSec: Int) {
        val sampleRate = 22050
        val totalSamples = sampleRate * durationSec
        val bytesPerSample = 2 // 16-bit
        val dataSize = totalSamples * bytesPerSample

        val fos = FileOutputStream(outputFile)

        // 1. Write 44-byte WAV header
        writeWavHeader(fos, dataSize, sampleRate, channels = 1, bitsPerSample = 16)

        // 2. Synthesize PCM waveforms
        val buffer = ByteBuffer.allocate(4096).order(ByteOrder.LITTLE_ENDIAN)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val sampleVal: Double = when (style) {
                // Synthwave (120 BPM): driving bass + kick + arpeggiated synths
                "aud_synthwave" -> {
                    val beat = (t * 2.0) % 1.0
                    val kick = if (beat < 0.1) sin(2.0 * Math.PI * (120.0 - beat * 800.0) * t) * (1.0 - beat * 10.0) else 0.0
                    val bassNote = if (((t * 4).toInt() % 4) == 0) 110.0 else 130.8
                    val bass = sin(2.0 * Math.PI * bassNote * t) * 0.4
                    val arpNote = when (((t * 8).toInt() % 4)) {
                        0 -> 440.0
                        1 -> 523.25
                        2 -> 659.25
                        else -> 783.99
                    }
                    val arp = (sin(2.0 * Math.PI * arpNote * t) + 0.3 * sin(4.0 * Math.PI * arpNote * t)) * 0.25
                    (kick * 0.6 + bass + arp).coerceIn(-1.0, 1.0)
                }

                // Cinematic: deep sub-bass and ominous brass swell
                "aud_cinematic" -> {
                    val subBass = sin(2.0 * Math.PI * 55.0 * t) * 0.5
                    val swell = (0.5 + 0.5 * sin(2.0 * Math.PI * 0.25 * t))
                    val brass = (sin(2.0 * Math.PI * 164.81 * t) + 0.5 * sin(2.0 * Math.PI * 247.23 * t)) * 0.35 * swell
                    (subBass + brass).coerceIn(-1.0, 1.0)
                }

                // Lo-Fi: mellow jazzy chords + vinyl flutter
                "aud_lofi" -> {
                    val flutter = 1.0 + 0.02 * sin(2.0 * Math.PI * 3.0 * t)
                    val chord = (sin(2.0 * Math.PI * 261.63 * flutter * t) +
                            sin(2.0 * Math.PI * 329.63 * flutter * t) +
                            sin(2.0 * Math.PI * 392.00 * flutter * t)) * 0.25
                    val vinylNoise = ((i % 17) - 8) / 80.0
                    (chord + vinylNoise).coerceIn(-1.0, 1.0)
                }

                // Upbeat Vlog: happy bright pluck
                "aud_upbeat" -> {
                    val note = when (((t * 6).toInt() % 8)) {
                        0 -> 523.25
                        1 -> 587.33
                        2 -> 659.25
                        3 -> 783.99
                        4 -> 880.0
                        5 -> 783.99
                        6 -> 659.25
                        else -> 587.33
                    }
                    val env = (1.0 - ((t * 6) % 1.0)).pow(2.0)
                    (sin(2.0 * Math.PI * note * t) * env * 0.6).coerceIn(-1.0, 1.0)
                }

                // Sound Effect: Whoosh
                "Whoosh Rápido" -> {
                    val progress = (t / 0.8).coerceIn(0.0, 1.0)
                    val freq = 200.0 + sin(progress * Math.PI) * 1600.0
                    val env = sin(progress * Math.PI)
                    val noise = (((i * 12345) % 100) / 50.0 - 1.0)
                    ((sin(2.0 * Math.PI * freq * t) * 0.4 + noise * 0.6) * env).coerceIn(-1.0, 1.0)
                }

                // Sound Effect: Glitch
                "Transición Glitch" -> {
                    val rndFreq = 400.0 + ((i / 400) % 7) * 250.0
                    val square = if (sin(2.0 * Math.PI * rndFreq * t) > 0) 0.5 else -0.5
                    val decay = exp(-t * 3.0)
                    (square * decay).coerceIn(-1.0, 1.0)
                }

                // Sound Effect: Bass Drop
                "Bass Drop Profundo" -> {
                    val freq = (180.0 * exp(-t * 2.5)).coerceAtLeast(35.0)
                    val env = exp(-t * 1.5)
                    (sin(2.0 * Math.PI * freq * t) * env).coerceIn(-1.0, 1.0)
                }

                // Sound Effect: Impacto
                "Impacto Cinemático" -> {
                    val freq = 80.0 * exp(-t * 4.0)
                    val noise = (((i * 98765) % 100) / 50.0 - 1.0) * exp(-t * 12.0)
                    val kick = sin(2.0 * Math.PI * freq * t) * exp(-t * 2.5)
                    (kick * 0.7 + noise * 0.5).coerceIn(-1.0, 1.0)
                }

                // Sound Effect: Click
                "Click de Cámara" -> {
                    val click = if (t < 0.05 || (t in 0.12..0.17)) {
                        sin(2.0 * Math.PI * 2800.0 * t) * exp(-((t % 0.12) * 50.0))
                    } else 0.0
                    click.coerceIn(-1.0, 1.0)
                }

                else -> {
                    // Default gentle sine
                    sin(2.0 * Math.PI * 440.0 * t) * 0.3
                }
            }

            val shortSample = (sampleVal * 32767).toInt().coerceIn(-32768, 32767).toShort()
            buffer.putShort(shortSample)

            if (!buffer.hasRemaining()) {
                fos.write(buffer.array())
                buffer.clear()
            }
        }

        if (buffer.position() > 0) {
            fos.write(buffer.array(), 0, buffer.position())
        }

        fos.flush()
        fos.close()
    }

    private fun writeWavHeader(
        out: FileOutputStream,
        totalAudioLen: Int,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ) {
        val totalDataLen = totalAudioLen + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // Subchunk1Size
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // AudioFormat (PCM)
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * bitsPerSample / 8).toByte() // BlockAlign
        header[33] = 0
        header[34] = bitsPerSample.toByte()
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xff).toByte()

        out.write(header, 0, 44)
    }
}
