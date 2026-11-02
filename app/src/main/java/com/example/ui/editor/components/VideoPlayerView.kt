package com.example.ui.editor.components

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.VideoRenderEngine
import com.example.model.AspectRatio
import com.example.model.Project
import com.example.ui.theme.*

@Composable
fun VideoPlayerView(
    project: Project,
    playheadMs: Long,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onChangeAspectRatio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(BackgroundDark),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Player Canvas Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            // Aspect Ratio Constrained Frame
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(project.aspectRatio.ratio, matchHeightConstraintsFirst = true)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black)
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Live Video Canvas
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val w = size.width.toInt().coerceAtLeast(100)
                    val h = size.height.toInt().coerceAtLeast(100)

                    drawIntoCanvas { composeCanvas ->
                        val native = composeCanvas.nativeCanvas
                        VideoRenderEngine.renderFrame(
                            context = context,
                            project = project,
                            playheadMs = playheadMs,
                            canvas = native,
                            width = w,
                            height = h,
                            renderWatermark = project.watermark
                        )
                    }
                }

                // Aspect Ratio indicator badge on canvas corner
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clickable { onChangeAspectRatio() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            Icons.Default.AspectRatio,
                            contentDescription = "Formato de pantalla",
                            tint = CyberGreen,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = project.aspectRatio.label.substringBefore(" "),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Floating Transport Control Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Current Timecode / Total Duration
            val curSec = playheadMs / 1000f
            val totSec = project.totalDurationMs / 1000f
            val timeText = String.format("%02d:%05.2f / %02d:%05.2f",
                (curSec / 60).toInt(), curSec % 60,
                (totSec / 60).toInt(), totSec % 60
            )

            Text(
                text = timeText,
                color = CyberGreen,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            // Play / Pause Button
            IconButton(
                onClick = onTogglePlay,
                modifier = Modifier
                    .size(40.dp)
                    .background(CyberGreen, CircleShape)
                    .testTag("play_pause_button")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                    tint = BackgroundDark,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Quick Resolution Badge
            Surface(
                color = SurfaceElevated,
                shape = RoundedCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
            ) {
                Text(
                    text = "${project.exportResolution} ${project.exportFps}fps",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
    }
}
