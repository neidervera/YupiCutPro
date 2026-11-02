package com.example.ui.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.engine.ExportProgress
import com.example.model.Project
import com.example.ui.theme.*
import java.io.File

@Composable
fun ExportDialog(
    project: Project,
    isExporting: Boolean,
    exportProgress: ExportProgress?,
    onDismiss: () -> Unit,
    onStartExport: () -> Unit,
    onCancelExport: () -> Unit,
    onShareExportedVideo: (File) -> Unit,
    onUpdateExportSettings: (resolution: String, fps: Int, bitrateKbps: Int, codec: String, format: String, watermark: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(project.name) }
    var resolution by remember { mutableStateOf(project.exportResolution) }
    var fps by remember { mutableStateOf(project.exportFps) }
    var bitrateKbps by remember { mutableStateOf(project.exportBitrateKbps) }
    var isVbr by remember { mutableStateOf(true) }
    var codec by remember { mutableStateOf(project.exportCodec) }
    var format by remember { mutableStateOf(project.exportFormat) }
    var watermark by remember { mutableStateOf(project.watermark) }

    val durationSec = (project.totalDurationMs / 1000f).coerceAtLeast(1f)
    val estimatedMb = (durationSec * (bitrateKbps / 8000f)).toInt().coerceAtLeast(1)

    Dialog(onDismissRequest = { if (!isExporting) onDismiss() }) {
        Surface(
            color = SurfaceDark,
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header (Matching CapCut style)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Upload, contentDescription = "Exportar", tint = CyberGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exportar Vídeo", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    if (!isExporting) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = TextSecondary)
                        }
                    }
                }

                Divider(color = SurfaceBorder, modifier = Modifier.padding(vertical = 12.dp))

                if (isExporting || exportProgress != null) {
                    // Export Progress State
                    val prog = exportProgress
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (prog?.isComplete == true) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Completo",
                                tint = SuccessGreen,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("¡Vídeo Exportado!", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(prog.outputFile?.name ?: "", color = TextSecondary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(16.dp))

                            prog.outputFile?.let { file ->
                                Button(
                                    onClick = { onShareExportedVideo(file) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = BackgroundDark),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Compartir")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Compartir / Abrir Vídeo", fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = onDismiss,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Volver al Editor")
                            }
                        } else {
                            val pct = prog?.progressPercent ?: 0
                            CircularProgressIndicator(
                                progress = { pct / 100f },
                                color = CyberGreen,
                                trackColor = SurfaceBorder,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("$pct%", color = CyberGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = prog?.statusText ?: "Renderizando fotogramas...",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = onCancelExport,
                                colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = Color.White),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Cancelar Exportación")
                            }
                        }
                    }
                } else {
                    // Settings Form (CapCut inspired)
                    // 1. File Name
                    Text("Nombre del archivo", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberGreen,
                            unfocusedBorderColor = SurfaceBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. Resolution Selection
                    Text("Resolución", color = TextSecondary, fontSize = 12.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("480p", "720p", "1080p", "1440p", "4K").forEach { res ->
                            FilterChip(
                                selected = resolution == res,
                                onClick = {
                                    resolution = res
                                    onUpdateExportSettings(res, fps, bitrateKbps, codec, format, watermark)
                                },
                                label = { Text(res, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberGreen,
                                    selectedLabelColor = BackgroundDark
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3. Frame Rate
                    Text("Tasa de Fotogramas (FPS)", color = TextSecondary, fontSize = 12.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(24, 30, 60).forEach { f ->
                            FilterChip(
                                selected = fps == f,
                                onClick = {
                                    fps = f
                                    onUpdateExportSettings(resolution, f, bitrateKbps, codec, format, watermark)
                                },
                                label = { Text("${f}fps", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberGreen,
                                    selectedLabelColor = BackgroundDark
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 4. Bitrate & Rate Control
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Bitrate: $bitrateKbps Kbps", color = TextSecondary, fontSize = 12.sp)
                        Row {
                            Text(
                                text = if (isVbr) "VBR (Variable)" else "CBR (Estático)",
                                color = CyberGreen,
                                fontSize = 11.sp,
                                modifier = Modifier.clickable { isVbr = !isVbr }
                            )
                        }
                    }
                    Slider(
                        value = bitrateKbps.toFloat(),
                        onValueChange = {
                            bitrateKbps = it.toInt()
                            onUpdateExportSettings(resolution, fps, it.toInt(), codec, format, watermark)
                        },
                        valueRange = 4000f..30000f,
                        colors = SliderDefaults.colors(
                            thumbColor = CyberGreen,
                            activeTrackColor = CyberGreen,
                            inactiveTrackColor = SurfaceBorder
                        )
                    )

                    // 5. Codec & Format
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Códec", color = TextSecondary, fontSize = 12.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("H.264", "HEVC").forEach { c ->
                                    FilterChip(
                                        selected = codec == c,
                                        onClick = {
                                            codec = c
                                            onUpdateExportSettings(resolution, fps, bitrateKbps, c, format, watermark)
                                        },
                                        label = { Text(c, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Formato", color = TextSecondary, fontSize = 12.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("mp4", "webm").forEach { fmt ->
                                    FilterChip(
                                        selected = format == fmt,
                                        onClick = {
                                            format = fmt
                                            onUpdateExportSettings(resolution, fps, bitrateKbps, codec, fmt, watermark)
                                        },
                                        label = { Text(fmt.uppercase(), fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Watermark Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Marca de agua YupiCut", color = TextPrimary, fontSize = 12.sp)
                        Switch(
                            checked = watermark,
                            onCheckedChange = {
                                watermark = it
                                onUpdateExportSettings(resolution, fps, bitrateKbps, codec, format, it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BackgroundDark,
                                checkedTrackColor = CyberGreen
                            )
                        )
                    }

                    Divider(color = SurfaceBorder, modifier = Modifier.padding(vertical = 12.dp))

                    // Estimation Label (CapCut reference: "Duration: 52s | Size: about 52 MB")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Duración: ${durationSec.toInt()}s | Tamaño: ~$estimatedMb MB",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancelar")
                        }

                        Button(
                            onClick = onStartExport,
                            colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = BackgroundDark),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("start_export_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Exportar", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Exportar Vídeo", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
