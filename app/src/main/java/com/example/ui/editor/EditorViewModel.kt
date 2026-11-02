package com.example.ui.editor

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ProjectStorage
import com.example.data.SampleAudioItem
import com.example.data.SampleMediaItem
import com.example.data.SampleMediaRepository
import com.example.engine.*
import com.example.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

enum class TrackType {
    NONE, VIDEO, AUDIO, TEXT
}

enum class ActivePanel {
    NONE,
    MEDIA_ADD,
    ADJUSTMENTS,
    FILTERS,
    TRANSITIONS,
    EFFECTS,
    AUDIO_TOOLS,
    TEXT_EDITOR,
    CANVAS_RATIO,
    AI_TOOLS,
    EXPORT_MODAL
}

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val storage = ProjectStorage(application)

    private val _project = MutableStateFlow(SampleMediaRepository.createInitialDemoProject())
    val project: StateFlow<Project> = _project.asStateFlow()

    private val _playheadMs = MutableStateFlow(0L)
    val playheadMs: StateFlow<Long> = _playheadMs.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _selectedClipId = MutableStateFlow<String?>(null)
    val selectedClipId: StateFlow<String?> = _selectedClipId.asStateFlow()

    private val _selectedTrackType = MutableStateFlow(TrackType.NONE)
    val selectedTrackType: StateFlow<TrackType> = _selectedTrackType.asStateFlow()

    private val _activePanel = MutableStateFlow(ActivePanel.NONE)
    val activePanel: StateFlow<ActivePanel> = _activePanel.asStateFlow()

    private val _timelineZoom = MutableStateFlow(1.0f)
    val timelineZoom: StateFlow<Float> = _timelineZoom.asStateFlow()

    private val _exportProgress = MutableStateFlow<ExportProgress?>(null)
    val exportProgress: StateFlow<ExportProgress?> = _exportProgress.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _isRecordingVoice = MutableStateFlow(false)
    val isRecordingVoice: StateFlow<Boolean> = _isRecordingVoice.asStateFlow()

    // Undo & Redo History
    private val undoStack = ArrayDeque<Project>()
    private val redoStack = ArrayDeque<Project>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private var playbackJob: Job? = null
    private var exportJob: Job? = null

    init {
        val allProjects = storage.getAllProjects()
        if (allProjects.isNotEmpty()) {
            _project.value = allProjects.first()
        }
        recalculateTimelineStarts()
    }

    fun loadProject(loadedProject: Project) {
        pause()
        _project.value = loadedProject
        _playheadMs.value = 0L
        _selectedClipId.value = null
        _selectedTrackType.value = TrackType.NONE
        undoStack.clear()
        redoStack.clear()
        updateUndoRedoStates()
        recalculateTimelineStarts()
    }

    private fun pushHistoryState() {
        undoStack.addLast(_project.value)
        if (undoStack.size > 25) undoStack.removeFirst()
        redoStack.clear()
        updateUndoRedoStates()
    }

    private fun updateUndoRedoStates() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack.addLast(_project.value)
            _project.value = undoStack.removeLast()
            updateUndoRedoStates()
            storage.saveProject(_project.value)
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack.addLast(_project.value)
            _project.value = redoStack.removeLast()
            updateUndoRedoStates()
            storage.saveProject(_project.value)
        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            play()
        }
    }

    fun play() {
        _isPlaying.value = true
        // Start synchronized audio playback
        AudioEngine.syncTimelinePlayback(getApplication(), _project.value, _playheadMs.value)

        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis() - _playheadMs.value
            while (_isPlaying.value) {
                val current = System.currentTimeMillis() - startTime
                if (current >= _project.value.totalDurationMs) {
                    _playheadMs.value = 0L
                    pause()
                    break
                } else {
                    _playheadMs.value = current
                    AudioEngine.syncTimelinePlayback(getApplication(), _project.value, current)
                }
                delay(33) // ~30 FPS preview updates
            }
        }
    }

    fun pause() {
        _isPlaying.value = false
        AudioEngine.stopTimelineAudio()
        playbackJob?.cancel()
        playbackJob = null
    }

    fun seekTo(timeMs: Long) {
        val bounded = timeMs.coerceIn(0L, _project.value.totalDurationMs)
        _playheadMs.value = bounded
        if (_isPlaying.value) {
            AudioEngine.syncTimelinePlayback(getApplication(), _project.value, bounded)
        }
    }

    fun setTimelineZoom(zoom: Float) {
        _timelineZoom.value = zoom.coerceIn(0.5f, 4.0f)
    }

    fun setActivePanel(panel: ActivePanel) {
        _activePanel.value = if (_activePanel.value == panel) ActivePanel.NONE else panel
    }

    fun selectVideoClip(clipId: String) {
        _selectedClipId.value = clipId
        _selectedTrackType.value = TrackType.VIDEO
    }

    fun selectAudioClip(clipId: String) {
        _selectedClipId.value = clipId
        _selectedTrackType.value = TrackType.AUDIO
    }

    fun selectTextClip(clipId: String) {
        _selectedClipId.value = clipId
        _selectedTrackType.value = TrackType.TEXT
    }

    fun clearSelection() {
        _selectedClipId.value = null
        _selectedTrackType.value = TrackType.NONE
    }

    // Helper: Gets the target video clip ID (either currently selected or under the playhead)
    private fun getTargetVideoClipId(): String? {
        if (_selectedClipId.value != null && _selectedTrackType.value == TrackType.VIDEO) {
            return _selectedClipId.value
        }
        val curTime = _playheadMs.value
        val clipAtPlayhead = _project.value.videoClips.firstOrNull {
            curTime >= it.timelineStartMs && curTime < (it.timelineStartMs + it.activeDurationMs)
        }
        val target = clipAtPlayhead ?: _project.value.videoClips.firstOrNull()
        if (target != null) {
            _selectedClipId.value = target.id
            _selectedTrackType.value = TrackType.VIDEO
        }
        return target?.id
    }

    // --- Editing Tools ---

    fun splitCurrentClip() {
        val selectedId = _selectedClipId.value ?: getTargetVideoClipId() ?: return
        val currentPlay = _playheadMs.value

        when (_selectedTrackType.value) {
            TrackType.VIDEO -> {
                val clip = _project.value.videoClips.find { it.id == selectedId } ?: return
                if (currentPlay > clip.timelineStartMs && currentPlay < (clip.timelineStartMs + clip.activeDurationMs)) {
                    pushHistoryState()
                    val offsetInClip = ((currentPlay - clip.timelineStartMs) * clip.adjustments.speed).toLong()
                    val splitPointSource = clip.startTrimMs + offsetInClip

                    val firstPart = clip.copy(
                        endTrimMs = splitPointSource
                    )
                    val secondPart = clip.copy(
                        id = UUID.randomUUID().toString(),
                        startTrimMs = splitPointSource,
                        timelineStartMs = currentPlay
                    )

                    val newClips = _project.value.videoClips.toMutableList()
                    val idx = newClips.indexOfFirst { it.id == selectedId }
                    if (idx >= 0) {
                        newClips[idx] = firstPart
                        newClips.add(idx + 1, secondPart)
                        updateProject { it.copy(videoClips = newClips) }
                        _selectedClipId.value = secondPart.id
                    }
                }
            }
            TrackType.AUDIO -> {
                val clip = _project.value.audioClips.find { it.id == selectedId } ?: return
                if (currentPlay > clip.timelineStartMs && currentPlay < (clip.timelineStartMs + clip.activeDurationMs)) {
                    pushHistoryState()
                    val offsetInClip = ((currentPlay - clip.timelineStartMs) * clip.speed).toLong()
                    val splitPoint = clip.startTrimMs + offsetInClip

                    val firstPart = clip.copy(endTrimMs = splitPoint)
                    val secondPart = clip.copy(
                        id = UUID.randomUUID().toString(),
                        startTrimMs = splitPoint,
                        timelineStartMs = currentPlay
                    )
                    val newClips = _project.value.audioClips.toMutableList()
                    val idx = newClips.indexOfFirst { it.id == selectedId }
                    if (idx >= 0) {
                        newClips[idx] = firstPart
                        newClips.add(idx + 1, secondPart)
                        updateProject { it.copy(audioClips = newClips) }
                        _selectedClipId.value = secondPart.id
                    }
                }
            }
            else -> {}
        }
    }

    fun deleteSelectedClip() {
        val selectedId = _selectedClipId.value ?: return
        pushHistoryState()

        when (_selectedTrackType.value) {
            TrackType.VIDEO -> {
                val newClips = _project.value.videoClips.filterNot { it.id == selectedId }
                updateProject { it.copy(videoClips = newClips) }
            }
            TrackType.AUDIO -> {
                val newClips = _project.value.audioClips.filterNot { it.id == selectedId }
                updateProject { it.copy(audioClips = newClips) }
            }
            TrackType.TEXT -> {
                val newClips = _project.value.textClips.filterNot { it.id == selectedId }
                updateProject { it.copy(textClips = newClips) }
            }
            else -> {}
        }

        _selectedClipId.value = null
        _selectedTrackType.value = TrackType.NONE
    }

    fun duplicateSelectedClip() {
        val selectedId = _selectedClipId.value ?: return
        pushHistoryState()

        when (_selectedTrackType.value) {
            TrackType.VIDEO -> {
                val clip = _project.value.videoClips.find { it.id == selectedId } ?: return
                val newClip = clip.copy(
                    id = UUID.randomUUID().toString(),
                    title = "${clip.title} (Copia)",
                    timelineStartMs = clip.timelineStartMs + clip.activeDurationMs
                )
                val newClips = _project.value.videoClips.toMutableList()
                val idx = newClips.indexOfFirst { it.id == selectedId }
                newClips.add(idx + 1, newClip)
                updateProject { it.copy(videoClips = newClips) }
                _selectedClipId.value = newClip.id
            }
            TrackType.AUDIO -> {
                val clip = _project.value.audioClips.find { it.id == selectedId } ?: return
                val newClip = clip.copy(
                    id = UUID.randomUUID().toString(),
                    title = "${clip.title} (Copia)",
                    timelineStartMs = clip.timelineStartMs + clip.activeDurationMs
                )
                val newClips = _project.value.audioClips.toMutableList()
                val idx = newClips.indexOfFirst { it.id == selectedId }
                newClips.add(idx + 1, newClip)
                updateProject { it.copy(audioClips = newClips) }
                _selectedClipId.value = newClip.id
            }
            TrackType.TEXT -> {
                val clip = _project.value.textClips.find { it.id == selectedId } ?: return
                val newClip = clip.copy(
                    id = UUID.randomUUID().toString(),
                    timelineStartMs = clip.timelineStartMs + clip.durationMs
                )
                val newClips = _project.value.textClips.toMutableList().apply { add(newClip) }
                updateProject { it.copy(textClips = newClips) }
                _selectedClipId.value = newClip.id
            }
            else -> {}
        }
    }

    fun updateSelectedVideoAdjustments(adjustments: VideoAdjustment) {
        val selectedId = getTargetVideoClipId() ?: return
        val newClips = _project.value.videoClips.map {
            if (it.id == selectedId) it.copy(adjustments = adjustments) else it
        }
        updateProject { it.copy(videoClips = newClips) }
    }

    fun updateSelectedVideoFilter(filter: FilterType) {
        val selectedId = getTargetVideoClipId() ?: return
        pushHistoryState()
        val newClips = _project.value.videoClips.map {
            if (it.id == selectedId) it.copy(filter = filter) else it
        }
        updateProject { it.copy(videoClips = newClips) }
    }

    fun updateSelectedVideoEffect(effect: EffectType) {
        val selectedId = getTargetVideoClipId() ?: return
        pushHistoryState()
        val newClips = _project.value.videoClips.map {
            if (it.id == selectedId) it.copy(effect = effect) else it
        }
        updateProject { it.copy(videoClips = newClips) }
    }

    fun updateSelectedVideoTransition(transition: TransitionType) {
        val selectedId = getTargetVideoClipId() ?: return
        pushHistoryState()
        val newClips = _project.value.videoClips.map {
            if (it.id == selectedId) it.copy(transition = transition) else it
        }
        updateProject { it.copy(videoClips = newClips) }
    }

    fun updateSelectedAudioVolume(volume: Float) {
        val selectedId = _selectedClipId.value ?: return
        val newClips = _project.value.audioClips.map {
            if (it.id == selectedId) it.copy(volume = volume) else it
        }
        updateProject { it.copy(audioClips = newClips) }
    }

    fun updateSelectedAudioSpeed(speed: Float) {
        val selectedId = _selectedClipId.value ?: return
        val newClips = _project.value.audioClips.map {
            if (it.id == selectedId) it.copy(speed = speed) else it
        }
        updateProject { it.copy(audioClips = newClips) }
    }

    fun updateSelectedAudioFades(fadeInMs: Long, fadeOutMs: Long) {
        val selectedId = _selectedClipId.value ?: return
        val newClips = _project.value.audioClips.map {
            if (it.id == selectedId) it.copy(fadeInMs = fadeInMs, fadeOutMs = fadeOutMs) else it
        }
        updateProject { it.copy(audioClips = newClips) }
    }

    fun updateSelectedTextClip(updated: TextClip) {
        val newClips = _project.value.textClips.map {
            if (it.id == updated.id) updated else it
        }
        updateProject { it.copy(textClips = newClips) }
    }

    fun setAspectRatio(ratio: AspectRatio) {
        pushHistoryState()
        updateProject { it.copy(aspectRatio = ratio) }
    }

    fun addVideoClipFromSample(sample: SampleMediaItem) {
        pushHistoryState()
        val lastEnd = _project.value.videoClips.maxOfOrNull { it.timelineStartMs + it.activeDurationMs } ?: 0L
        val newClip = VideoClip(
            id = UUID.randomUUID().toString(),
            title = sample.title,
            durationMs = sample.durationMs,
            startTrimMs = 0L,
            endTrimMs = sample.durationMs,
            timelineStartMs = lastEnd,
            sampleType = sample.sampleType
        )
        updateProject { it.copy(videoClips = it.videoClips + newClip) }
        _selectedClipId.value = newClip.id
        _selectedTrackType.value = TrackType.VIDEO
    }

    fun addVideoClipFromUri(uri: Uri, title: String) {
        pushHistoryState()
        val lastEnd = _project.value.videoClips.maxOfOrNull { it.timelineStartMs + it.activeDurationMs } ?: 0L
        val newClip = VideoClip(
            id = UUID.randomUUID().toString(),
            title = title,
            uriString = uri.toString(),
            durationMs = 6000L,
            startTrimMs = 0L,
            endTrimMs = 6000L,
            timelineStartMs = lastEnd,
            sampleType = "custom"
        )
        updateProject { it.copy(videoClips = it.videoClips + newClip) }
        _selectedClipId.value = newClip.id
        _selectedTrackType.value = TrackType.VIDEO
    }

    fun playSoundEffectPreview(sfxName: String) {
        AudioEngine.playSoundEffect(getApplication(), sfxName)
    }

    fun playSampleAudioPreview(sample: SampleAudioItem) {
        AudioEngine.playSoundEffect(getApplication(), sample.id)
    }

    fun addAudioClipFromSample(sample: SampleAudioItem) {
        pushHistoryState()
        val newClip = AudioClip(
            id = UUID.randomUUID().toString(),
            title = sample.title,
            durationMs = sample.durationMs,
            startTrimMs = 0L,
            endTrimMs = sample.durationMs,
            timelineStartMs = _playheadMs.value,
            volume = 0.9f,
            waveform = sample.waveform,
            sampleTrack = sample.id
        )
        updateProject { it.copy(audioClips = it.audioClips + newClip) }
        _selectedClipId.value = newClip.id
        _selectedTrackType.value = TrackType.AUDIO
        // Play instant audible preview
        playSampleAudioPreview(sample)
    }

    fun addAudioClipFromUri(uri: Uri, title: String) {
        pushHistoryState()
        val newClip = AudioClip(
            id = UUID.randomUUID().toString(),
            title = title,
            uriString = uri.toString(),
            durationMs = 8000L,
            startTrimMs = 0L,
            endTrimMs = 8000L,
            timelineStartMs = _playheadMs.value,
            volume = 1.0f,
            waveform = SampleMediaRepository.generateWaveform(45)
        )
        updateProject { it.copy(audioClips = it.audioClips + newClip) }
        _selectedClipId.value = newClip.id
        _selectedTrackType.value = TrackType.AUDIO
    }

    fun addSoundEffectClip(sfxName: String, durationMs: Long) {
        pushHistoryState()
        val newClip = AudioClip(
            id = UUID.randomUUID().toString(),
            title = sfxName,
            durationMs = durationMs,
            startTrimMs = 0L,
            endTrimMs = durationMs,
            timelineStartMs = _playheadMs.value,
            volume = 1.0f,
            waveform = SampleMediaRepository.generateWaveform(25),
            sampleTrack = sfxName
        )
        updateProject { it.copy(audioClips = it.audioClips + newClip) }
        _selectedClipId.value = newClip.id
        _selectedTrackType.value = TrackType.AUDIO
        playSoundEffectPreview(sfxName)
    }

    fun addTextClip(text: String = "Título de Vídeo") {
        pushHistoryState()
        val newClip = TextClip(
            id = UUID.randomUUID().toString(),
            text = text,
            timelineStartMs = _playheadMs.value,
            durationMs = 3000L,
            fontSizeSp = 24,
            textColorHex = "#00FF66", // Cyber neon green
            shadow = true,
            animation = TextAnimation.BOUNCE,
            positionX = 0f,
            positionY = 0.6f
        )
        updateProject { it.copy(textClips = it.textClips + newClip) }
        _selectedClipId.value = newClip.id
        _selectedTrackType.value = TrackType.TEXT
    }

    fun extractAudioFromSelectedVideo() {
        val selectedId = getTargetVideoClipId() ?: return
        val clip = _project.value.videoClips.find { it.id == selectedId } ?: return
        pushHistoryState()
        val audioClip = AudioEngine.extractAudioFromVideo(clip)
        updateProject { it.copy(audioClips = it.audioClips + audioClip) }
        _selectedClipId.value = audioClip.id
        _selectedTrackType.value = TrackType.AUDIO
    }

    // --- AI Tools Implementation ---

    fun applyAiAutoSubtitles() {
        pushHistoryState()
        val subs = AiVideoAssistant.generateAutoSubtitles(_project.value)
        updateProject { it.copy(textClips = it.textClips + subs) }
    }

    fun applyAiSilenceRemoval() {
        val selectedId = _selectedClipId.value
        val targetClip = if (selectedId != null) {
            _project.value.audioClips.find { it.id == selectedId }
        } else {
            _project.value.audioClips.firstOrNull()
        } ?: return

        pushHistoryState()
        val cleaned = AiVideoAssistant.detectAndRemoveSilences(targetClip)
        val newAudio = _project.value.audioClips.toMutableList()
        val idx = newAudio.indexOfFirst { it.id == targetClip.id }
        if (idx >= 0) {
            newAudio.removeAt(idx)
            newAudio.addAll(idx, cleaned)
            updateProject { it.copy(audioClips = newAudio) }
        }
    }

    fun applyAiSceneSplit() {
        val selectedId = getTargetVideoClipId() ?: return
        val clip = _project.value.videoClips.find { it.id == selectedId } ?: return

        val cuts = AiVideoAssistant.detectSceneCutPoints(clip)
        if (cuts.isEmpty()) return

        pushHistoryState()
        var remainingClip = clip
        val resultClips = mutableListOf<VideoClip>()

        cuts.forEach { cutTimestamp ->
            val cutInSource = remainingClip.startTrimMs + (cutTimestamp - remainingClip.timelineStartMs)
            if (cutInSource < remainingClip.endTrimMs) {
                val segment = remainingClip.copy(
                    id = UUID.randomUUID().toString(),
                    endTrimMs = cutInSource
                )
                resultClips.add(segment)
                remainingClip = remainingClip.copy(
                    id = UUID.randomUUID().toString(),
                    startTrimMs = cutInSource,
                    timelineStartMs = cutTimestamp
                )
            }
        }
        resultClips.add(remainingClip)

        val newVideoClips = _project.value.videoClips.toMutableList()
        val idx = newVideoClips.indexOfFirst { it.id == clip.id }
        if (idx >= 0) {
            newVideoClips.removeAt(idx)
            newVideoClips.addAll(idx, resultClips)
            updateProject { it.copy(videoClips = newVideoClips) }
        }
    }

    fun applyAiAudioEnhancement() {
        val selectedId = _selectedClipId.value
        val clip = if (selectedId != null) {
            _project.value.audioClips.find { it.id == selectedId }
        } else {
            _project.value.audioClips.firstOrNull()
        } ?: return

        pushHistoryState()
        val enhanced = AiVideoAssistant.enhanceAudio(clip)
        val newAudios = _project.value.audioClips.map {
            if (it.id == clip.id) enhanced else it
        }
        updateProject { it.copy(audioClips = newAudios) }
    }

    fun applyAiColorGrading() {
        val selectedId = getTargetVideoClipId() ?: return
        pushHistoryState()
        val newClips = _project.value.videoClips.map {
            if (it.id == selectedId) it.copy(
                filter = FilterType.CYBERPUNK,
                adjustments = it.adjustments.copy(contrast = 1.25f, saturation = 1.35f)
            ) else it
        }
        updateProject { it.copy(videoClips = newClips) }
    }

    fun applyAiTransitions() {
        pushHistoryState()
        val newClips = _project.value.videoClips.mapIndexed { idx, clip ->
            if (idx > 0 && clip.transition == TransitionType.NONE) {
                clip.copy(transition = if (idx % 2 == 0) TransitionType.GLITCH else TransitionType.DISSOLVE)
            } else clip
        }
        updateProject { it.copy(videoClips = newClips) }
    }

    // --- Export Management ---

    fun updateExportSettings(
        resolution: String = _project.value.exportResolution,
        fps: Int = _project.value.exportFps,
        bitrateKbps: Int = _project.value.exportBitrateKbps,
        codec: String = _project.value.exportCodec,
        format: String = _project.value.exportFormat,
        watermark: Boolean = _project.value.watermark
    ) {
        updateProject {
            it.copy(
                exportResolution = resolution,
                exportFps = fps,
                exportBitrateKbps = bitrateKbps,
                exportCodec = codec,
                exportFormat = format,
                watermark = watermark
            )
        }
    }

    fun startExport() {
        pause()
        _isExporting.value = true
        _exportProgress.value = ExportProgress(
            progressPercent = 0,
            currentFrame = 0,
            totalFrames = 100,
            statusText = "Preparando renderizado de vídeo..."
        )

        exportJob?.cancel()
        exportJob = viewModelScope.launch {
            val file = VideoExportEngine.exportProject(getApplication(), _project.value) { prog ->
                _exportProgress.value = prog
            }
            _isExporting.value = false
        }
    }

    fun cancelExport() {
        VideoExportEngine.cancelExport()
        exportJob?.cancel()
        _isExporting.value = false
        _exportProgress.value = null
    }

    fun shareExportedVideo(file: File) {
        VideoExportEngine.shareExportedVideo(getApplication(), file)
    }

    private fun updateProject(transform: (Project) -> Project) {
        val updated = transform(_project.value).copy(lastModified = System.currentTimeMillis())
        _project.value = updated
        recalculateTimelineStarts()
        storage.saveProject(updated)
    }

    private fun recalculateTimelineStarts() {
        var currentStart = 0L
        val updatedVideos = _project.value.videoClips.map { clip ->
            val updated = clip.copy(timelineStartMs = currentStart)
            currentStart += clip.activeDurationMs
            updated
        }
        _project.value = _project.value.copy(videoClips = updatedVideos)
    }
}
