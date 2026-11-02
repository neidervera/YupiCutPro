package com.example.ui.editor

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AspectRatio
import com.example.model.Project
import com.example.ui.editor.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val project by viewModel.project.collectAsStateWithLifecycle()
    val playheadMs by viewModel.playheadMs.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val selectedClipId by viewModel.selectedClipId.collectAsStateWithLifecycle()
    val selectedTrackType by viewModel.selectedTrackType.collectAsStateWithLifecycle()
    val activePanel by viewModel.activePanel.collectAsStateWithLifecycle()
    val timelineZoom by viewModel.timelineZoom.collectAsStateWithLifecycle()
    val exportProgress by viewModel.exportProgress.collectAsStateWithLifecycle()
    val isExporting by viewModel.isExporting.collectAsStateWithLifecycle()
    val isRecordingVoice by viewModel.isRecordingVoice.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()

    var showExportDialog by remember { mutableStateOf(false) }
    var showAspectRatioMenu by remember { mutableStateOf(false) }

    // Media Picker launcher for videos and audios
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val type = it.toString()
            if (type.contains("audio") || type.endsWith(".mp3") || type.endsWith(".wav")) {
                viewModel.addAudioClipFromUri(it, "Audio importado")
            } else {
                viewModel.addVideoClipFromUri(it, "Vídeo importado")
            }
        }
    }

    BackHandler {
        if (activePanel != ActivePanel.NONE) {
            viewModel.setActivePanel(ActivePanel.NONE)
        } else {
            onNavigateBack()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = project.name,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = SuccessGreen.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Auto saved",
                                color = SuccessGreen,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                actions = {
                    // Undo
                    IconButton(onClick = { viewModel.undo() }, enabled = canUndo) {
                        Icon(
                            Icons.Default.Undo,
                            contentDescription = "Deshacer",
                            tint = if (canUndo) Color.White else TextTertiary
                        )
                    }

                    // Redo
                    IconButton(onClick = { viewModel.redo() }, enabled = canRedo) {
                        Icon(
                            Icons.Default.Redo,
                            contentDescription = "Rehacer",
                            tint = if (canRedo) Color.White else TextTertiary
                        )
                    }

                    // Aspect Ratio Button
                    Box {
                        IconButton(onClick = { showAspectRatioMenu = true }) {
                            Icon(Icons.Default.AspectRatio, contentDescription = "Formato", tint = CyberGreen)
                        }
                        DropdownMenu(
                            expanded = showAspectRatioMenu,
                            onDismissRequest = { showAspectRatioMenu = false },
                            modifier = Modifier.background(SurfaceElevated)
                        ) {
                            AspectRatio.values().forEach { ratio ->
                                DropdownMenuItem(
                                    text = { Text(ratio.label, color = Color.White) },
                                    onClick = {
                                        viewModel.setAspectRatio(ratio)
                                        showAspectRatioMenu = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Primary Export Action Button
                    Button(
                        onClick = { showExportDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = BackgroundDark),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("export_button")
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = "Exportar", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Exportar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        bottomBar = {
            // Main Bottom Navigation Toolbar
            Column(modifier = Modifier.fillMaxWidth().background(SurfaceDark)) {
                // Secondary Active Tool Panel if open
                if (activePanel != ActivePanel.NONE) {
                    Box(modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp)) {
                        when (activePanel) {
                            ActivePanel.MEDIA_ADD -> MediaAddPanel(
                                onAddSampleVideo = { viewModel.addVideoClipFromSample(it) },
                                onAddSampleAudio = { viewModel.addAudioClipFromSample(it) },
                                onPlaySampleAudio = { viewModel.playSampleAudioPreview(it) },
                                onAddSoundEffect = { name, dur -> viewModel.addSoundEffectClip(name, dur) },
                                onPlaySoundEffect = { name -> viewModel.playSoundEffectPreview(name) },
                                onPickDeviceMedia = { mediaPickerLauncher.launch("*/*") }
                            )
                            ActivePanel.ADJUSTMENTS -> {
                                val selectedVideo = project.videoClips.find { it.id == selectedClipId }
                                    ?: project.videoClips.firstOrNull()
                                if (selectedVideo != null) {
                                    AdjustmentsPanel(
                                        clip = selectedVideo,
                                        onUpdateAdjustments = { viewModel.updateSelectedVideoAdjustments(it) }
                                    )
                                } else {
                                    Text("Selecciona un clip de vídeo para ajustar", color = TextSecondary, modifier = Modifier.padding(16.dp))
                                }
                            }
                            ActivePanel.FILTERS -> {
                                val currentFilter = project.videoClips.find { it.id == selectedClipId }?.filter
                                    ?: project.videoClips.firstOrNull()?.filter ?: com.example.model.FilterType.NONE
                                FiltersPanel(
                                    selectedFilter = currentFilter,
                                    onSelectFilter = { viewModel.updateSelectedVideoFilter(it) }
                                )
                            }
                            ActivePanel.TRANSITIONS -> {
                                val currentTr = project.videoClips.find { it.id == selectedClipId }?.transition
                                    ?: project.videoClips.firstOrNull()?.transition ?: com.example.model.TransitionType.NONE
                                TransitionsPanel(
                                    selectedTransition = currentTr,
                                    onSelectTransition = { viewModel.updateSelectedVideoTransition(it) }
                                )
                            }
                            ActivePanel.EFFECTS -> {
                                val currentEff = project.videoClips.find { it.id == selectedClipId }?.effect
                                    ?: project.videoClips.firstOrNull()?.effect ?: com.example.model.EffectType.NONE
                                EffectsPanel(
                                    selectedEffect = currentEff,
                                    onSelectEffect = { viewModel.updateSelectedVideoEffect(it) }
                                )
                            }
                            ActivePanel.AUDIO_TOOLS -> {
                                val selectedAudio = project.audioClips.find { it.id == selectedClipId }
                                    ?: project.audioClips.firstOrNull()
                                AudioToolsPanel(
                                    audioClip = selectedAudio,
                                    onUpdateVolume = { viewModel.updateSelectedAudioVolume(it) },
                                    onUpdateSpeed = { viewModel.updateSelectedAudioSpeed(it) },
                                    onExtractAudio = { viewModel.extractAudioFromSelectedVideo() },
                                    onToggleRecording = {
                                        // Voice recording toggle
                                    },
                                    isRecording = isRecordingVoice
                                )
                            }
                            ActivePanel.TEXT_EDITOR -> {
                                val selectedText = project.textClips.find { it.id == selectedClipId }
                                TextEditorPanel(
                                    selectedClip = selectedText,
                                    onAddText = { viewModel.addTextClip(it) },
                                    onUpdateTextClip = { viewModel.updateSelectedTextClip(it) }
                                )
                            }
                            ActivePanel.AI_TOOLS -> {
                                AiToolsPanel(
                                    project = project,
                                    onAutoSubtitles = { viewModel.applyAiAutoSubtitles() },
                                    onRemoveSilences = { viewModel.applyAiSilenceRemoval() },
                                    onSceneSplit = { viewModel.applyAiSceneSplit() },
                                    onEnhanceAudio = { viewModel.applyAiAudioEnhancement() },
                                    onColorGrade = { viewModel.applyAiColorGrading() },
                                    onAutoTransitions = { viewModel.applyAiTransitions() }
                                )
                            }
                            else -> {}
                        }
                    }
                    Divider(color = SurfaceBorder)
                }

                // Primary Bottom Tab Icons Row (CapCut / Filmora inspired)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BottomNavToolItem(
                        title = "Media",
                        icon = Icons.Default.AddPhotoAlternate,
                        isActive = activePanel == ActivePanel.MEDIA_ADD,
                        onClick = { viewModel.setActivePanel(ActivePanel.MEDIA_ADD) }
                    )
                    BottomNavToolItem(
                        title = "Ajustar",
                        icon = Icons.Default.Tune,
                        isActive = activePanel == ActivePanel.ADJUSTMENTS,
                        onClick = { viewModel.setActivePanel(ActivePanel.ADJUSTMENTS) }
                    )
                    BottomNavToolItem(
                        title = "Filtros",
                        icon = Icons.Default.AutoFixHigh,
                        isActive = activePanel == ActivePanel.FILTERS,
                        onClick = { viewModel.setActivePanel(ActivePanel.FILTERS) }
                    )
                    BottomNavToolItem(
                        title = "Transición",
                        icon = Icons.Default.SwapCalls,
                        isActive = activePanel == ActivePanel.TRANSITIONS,
                        onClick = { viewModel.setActivePanel(ActivePanel.TRANSITIONS) }
                    )
                    BottomNavToolItem(
                        title = "Efectos",
                        icon = Icons.Default.Grain,
                        isActive = activePanel == ActivePanel.EFFECTS,
                        onClick = { viewModel.setActivePanel(ActivePanel.EFFECTS) }
                    )
                    BottomNavToolItem(
                        title = "Audio",
                        icon = Icons.Default.MusicNote,
                        isActive = activePanel == ActivePanel.AUDIO_TOOLS,
                        onClick = { viewModel.setActivePanel(ActivePanel.AUDIO_TOOLS) }
                    )
                    BottomNavToolItem(
                        title = "Texto",
                        icon = Icons.Default.Title,
                        isActive = activePanel == ActivePanel.TEXT_EDITOR,
                        onClick = { viewModel.setActivePanel(ActivePanel.TEXT_EDITOR) }
                    )
                    BottomNavToolItem(
                        title = "IA Pro",
                        icon = Icons.Default.AutoAwesome,
                        isActive = activePanel == ActivePanel.AI_TOOLS,
                        highlight = true,
                        onClick = { viewModel.setActivePanel(ActivePanel.AI_TOOLS) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Video Player Canvas (~40% of vertical space)
            VideoPlayerView(
                project = project,
                playheadMs = playheadMs,
                isPlaying = isPlaying,
                onTogglePlay = { viewModel.togglePlayPause() },
                onChangeAspectRatio = { showAspectRatioMenu = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.9f)
            )

            // 2. Multi-track Timeline (~60% of space)
            TimelineView(
                project = project,
                playheadMs = playheadMs,
                selectedClipId = selectedClipId,
                selectedTrackType = selectedTrackType,
                timelineZoom = timelineZoom,
                onSeek = { viewModel.seekTo(it) },
                onSelectVideoClip = { viewModel.selectVideoClip(it) },
                onSelectAudioClip = { viewModel.selectAudioClip(it) },
                onSelectTextClip = { viewModel.selectTextClip(it) },
                onSplitClip = { viewModel.splitCurrentClip() },
                onDeleteClip = { viewModel.deleteSelectedClip() },
                onDuplicateClip = { viewModel.duplicateSelectedClip() },
                onZoomChange = { viewModel.setTimelineZoom(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.1f)
            )
        }
    }

    // Export Dialog
    if (showExportDialog) {
        ExportDialog(
            project = project,
            isExporting = isExporting,
            exportProgress = exportProgress,
            onDismiss = { showExportDialog = false },
            onStartExport = { viewModel.startExport() },
            onCancelExport = { viewModel.cancelExport() },
            onShareExportedVideo = { viewModel.shareExportedVideo(it) },
            onUpdateExportSettings = { res, fps, bitrate, codec, fmt, wm ->
                viewModel.updateExportSettings(res, fps, bitrate, codec, fmt, wm)
            }
        )
    }
}

@Composable
private fun BottomNavToolItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    highlight: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        color = if (isActive) Color(0xFF0F3820) else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .width(60.dp)
            .fillMaxHeight()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = when {
                    isActive -> CyberGreen
                    highlight -> NeonGreen
                    else -> TextSecondary
                },
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                color = when {
                    isActive -> CyberGreen
                    highlight -> NeonGreen
                    else -> TextSecondary
                },
                fontSize = 10.sp,
                fontWeight = if (isActive || highlight) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}
