package com.example.ui.editor.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.editor.TrackType
import com.example.ui.theme.*

@Composable
fun TimelineView(
    project: Project,
    playheadMs: Long,
    selectedClipId: String?,
    selectedTrackType: TrackType,
    timelineZoom: Float,
    onSeek: (Long) -> Unit,
    onSelectVideoClip: (String) -> Unit,
    onSelectAudioClip: (String) -> Unit,
    onSelectTextClip: (String) -> Unit,
    onSplitClip: () -> Unit,
    onDeleteClip: () -> Unit,
    onDuplicateClip: () -> Unit,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalMs = project.totalDurationMs.coerceAtLeast(6000L)
    // Base pixel width per second (e.g., 60.dp per second * zoom)
    val msPerDp = (1000f / (60f * timelineZoom)).coerceAtLeast(1f)
    val totalWidthDp = (totalMs / msPerDp).dp + 300.dp
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .border(androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder))
    ) {
        // Quick Action Bar on top of timeline (Split, Delete, Duplicate, Zoom)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceElevated)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Split Action
                Button(
                    onClick = onSplitClip,
                    enabled = selectedClipId != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedClipId != null) Color(0xFF0F3820) else SurfaceBorder,
                        contentColor = if (selectedClipId != null) CyberGreen else TextTertiary
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("split_clip_button")
                ) {
                    Icon(Icons.Default.ContentCut, contentDescription = "Dividir", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Dividir", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Delete Action
                IconButton(
                    onClick = onDeleteClip,
                    enabled = selectedClipId != null,
                    modifier = Modifier
                        .size(32.dp)
                        .background(if (selectedClipId != null) DangerRed.copy(alpha = 0.2f) else Color.Transparent, RoundedCornerShape(4.dp))
                        .testTag("delete_clip_button")
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = if (selectedClipId != null) DangerRed else TextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Duplicate Action
                IconButton(
                    onClick = onDuplicateClip,
                    enabled = selectedClipId != null,
                    modifier = Modifier
                        .size(32.dp)
                        .background(if (selectedClipId != null) SurfaceBorder else Color.Transparent, RoundedCornerShape(4.dp))
                        .testTag("duplicate_clip_button")
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Duplicar",
                        tint = if (selectedClipId != null) CyberGreen else TextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Timeline Zoom Slider
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ZoomOut, contentDescription = "Alejar", tint = TextSecondary, modifier = Modifier.size(16.dp))
                Slider(
                    value = timelineZoom,
                    onValueChange = onZoomChange,
                    valueRange = 0.5f..3.0f,
                    modifier = Modifier
                        .width(100.dp)
                        .height(24.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = CyberGreen,
                        activeTrackColor = CyberGreen,
                        inactiveTrackColor = SurfaceBorder
                    )
                )
                Icon(Icons.Default.ZoomIn, contentDescription = "Acercar", tint = TextSecondary, modifier = Modifier.size(16.dp))
            }
        }

        // Multi-track Timeline Scrollable Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .horizontalScroll(scrollState)
        ) {
            Box(
                modifier = Modifier
                    .width(totalWidthDp)
                    .fillMaxHeight()
                    .pointerInput(totalMs, msPerDp) {
                        detectTapGestures { offset ->
                            val clickedMs = (offset.x / density * msPerDp).toLong().coerceIn(0L, totalMs)
                            onSeek(clickedMs)
                        }
                    }
                    .pointerInput(totalMs, msPerDp) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val draggedMs = (change.position.x / density * msPerDp).toLong().coerceIn(0L, totalMs)
                            onSeek(draggedMs)
                        }
                    }
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 1. Time Ruler
                    TimelineRuler(totalMs = totalMs, msPerDp = msPerDp, modifier = Modifier.height(26.dp))

                    // 2. Video Track
                    Text(
                        text = "VÍDEO",
                        color = TextTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    )
                    VideoTrackRow(
                        clips = project.videoClips,
                        selectedClipId = selectedClipId,
                        msPerDp = msPerDp,
                        onSelectClip = onSelectVideoClip,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .padding(vertical = 4.dp)
                    )

                    // 3. Audio Track
                    Text(
                        text = "AUDIO & MÚSICA",
                        color = TextTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    )
                    AudioTrackRow(
                        clips = project.audioClips,
                        selectedClipId = selectedClipId,
                        msPerDp = msPerDp,
                        onSelectClip = onSelectAudioClip,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .padding(vertical = 4.dp)
                    )

                    // 4. Text & Subtitles Track
                    Text(
                        text = "TEXTO & SUBTÍTULOS",
                        color = TextTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    )
                    TextTrackRow(
                        clips = project.textClips,
                        selectedClipId = selectedClipId,
                        msPerDp = msPerDp,
                        onSelectClip = onSelectTextClip,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .padding(vertical = 4.dp)
                    )
                }

                // Red/Cyan Playhead Indicator Line
                val playheadOffsetDp = (playheadMs / msPerDp).dp
                Box(
                    modifier = Modifier
                        .offset(x = playheadOffsetDp)
                        .fillMaxHeight()
                        .width(2.dp)
                        .background(PlayheadColor)
                ) {
                    // Playhead Top Cap Tag
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .offset(x = (-4).dp, y = 0.dp)
                            .background(PlayheadColor, RoundedCornerShape(2.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineRuler(
    totalMs: Long,
    msPerDp: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxWidth()) {
        val totalSec = (totalMs / 1000).toInt() + 2
        for (sec in 0..totalSec) {
            val xDp = (sec * 1000f) / msPerDp
            val xPx = xDp * density

            // Major tick mark every second
            drawLine(
                color = SurfaceBorder,
                start = Offset(xPx, size.height * 0.4f),
                end = Offset(xPx, size.height),
                strokeWidth = 2f
            )

            // Half-second tick
            val halfXPx = ((sec * 1000f + 500f) / msPerDp) * density
            drawLine(
                color = SurfaceBorder.copy(alpha = 0.5f),
                start = Offset(halfXPx, size.height * 0.7f),
                end = Offset(halfXPx, size.height),
                strokeWidth = 1f
            )
        }
    }
}

@Composable
private fun VideoTrackRow(
    clips: List<VideoClip>,
    selectedClipId: String?,
    msPerDp: Float,
    onSelectClip: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(SurfaceDark.copy(alpha = 0.5f))
    ) {
        clips.forEach { clip ->
            val startDp = (clip.timelineStartMs / msPerDp).dp
            val widthDp = (clip.activeDurationMs / msPerDp).dp.coerceAtLeast(36.dp)
            val isSelected = clip.id == selectedClipId

            Box(
                modifier = Modifier
                    .offset(x = startDp)
                    .width(widthDp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(VideoTrackColor)
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) CyberGreen else VideoTrackBorder,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable { onSelectClip(clip.id) }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = clip.title,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        if (clip.transition != TransitionType.NONE) {
                            Surface(
                                color = CyberGreen.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(2.dp)
                            ) {
                                Text(
                                    text = clip.transition.displayName,
                                    color = CyberGreen,
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(2.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = String.format("%.1fs", clip.activeDurationMs / 1000f),
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                        if (clip.filter != FilterType.NONE) {
                            Text(
                                text = clip.filter.displayName,
                                color = NeonGreen,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioTrackRow(
    clips: List<AudioClip>,
    selectedClipId: String?,
    msPerDp: Float,
    onSelectClip: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(SurfaceDark.copy(alpha = 0.5f))
    ) {
        clips.forEach { clip ->
            val startDp = (clip.timelineStartMs / msPerDp).dp
            val widthDp = (clip.activeDurationMs / msPerDp).dp.coerceAtLeast(36.dp)
            val isSelected = clip.id == selectedClipId

            Box(
                modifier = Modifier
                    .offset(x = startDp)
                    .width(widthDp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(AudioTrackColor)
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) AudioTrackBorder else AudioTrackBorder.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable { onSelectClip(clip.id) }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = "Pista de audio",
                        tint = AudioTrackBorder,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = clip.title,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    // Mini waveform preview
                    Canvas(
                        modifier = Modifier
                            .width(60.dp)
                            .height(18.dp)
                    ) {
                        val barCount = 12
                        val barW = size.width / barCount
                        for (b in 0 until barCount) {
                            val amp = if (b < clip.waveform.size) clip.waveform[b] else 0.5f
                            val h = size.height * amp
                            drawRect(
                                color = AudioTrackBorder,
                                topLeft = Offset(b * barW, (size.height - h) / 2f),
                                size = androidx.compose.ui.geometry.Size(barW * 0.7f, h)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TextTrackRow(
    clips: List<TextClip>,
    selectedClipId: String?,
    msPerDp: Float,
    onSelectClip: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(SurfaceDark.copy(alpha = 0.5f))
    ) {
        clips.forEach { clip ->
            val startDp = (clip.timelineStartMs / msPerDp).dp
            val widthDp = (clip.durationMs / msPerDp).dp.coerceAtLeast(36.dp)
            val isSelected = clip.id == selectedClipId

            Box(
                modifier = Modifier
                    .offset(x = startDp)
                    .width(widthDp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(TextTrackColor)
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) TextTrackBorder else TextTrackBorder.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable { onSelectClip(clip.id) }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Title,
                        contentDescription = "Texto",
                        tint = TextTrackBorder,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = clip.text,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
