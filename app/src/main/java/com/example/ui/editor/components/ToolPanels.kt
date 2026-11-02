package com.example.ui.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.example.data.SampleAudioItem
import com.example.data.SampleMediaItem
import com.example.data.SampleMediaRepository
import com.example.engine.AiSuggestion
import com.example.engine.AiVideoAssistant
import com.example.model.*
import com.example.ui.theme.*

@Composable
fun AdjustmentsPanel(
    clip: VideoClip,
    onUpdateAdjustments: (VideoAdjustment) -> Unit,
    modifier: Modifier = Modifier
) {
    val adj = clip.adjustments
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text("AJUSTES DE VÍDEO", color = CyberGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        // Speed Toggles
        Text("Velocidad: ${adj.speed}x", color = TextPrimary, fontSize = 12.sp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(0.25f, 0.5f, 1.0f, 1.5f, 2.0f, 4.0f).forEach { spd ->
                FilterChip(
                    selected = adj.speed == spd,
                    onClick = { onUpdateAdjustments(adj.copy(speed = spd)) },
                    label = { Text("${spd}x", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyberGreen,
                        selectedLabelColor = BackgroundDark,
                        containerColor = SurfaceElevated,
                        labelColor = TextPrimary
                    )
                )
            }
        }

        // Rotation & Flip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { onUpdateAdjustments(adj.copy(rotation = (adj.rotation + 90) % 360)) },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberGreen)
            ) {
                Icon(Icons.Default.RotateRight, contentDescription = "Rotar", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Rotar (${adj.rotation}°)")
            }

            OutlinedButton(
                onClick = { onUpdateAdjustments(adj.copy(flipHorizontal = !adj.flipHorizontal)) },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (adj.flipHorizontal) CyberGreen else TextSecondary
                )
            ) {
                Icon(Icons.Default.Flip, contentDescription = "Voltear H", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Voltear H")
            }

            OutlinedButton(
                onClick = { onUpdateAdjustments(adj.copy(flipVertical = !adj.flipVertical)) },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (adj.flipVertical) CyberGreen else TextSecondary
                )
            ) {
                Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Voltear V", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Voltear V")
            }
        }

        Divider(color = SurfaceBorder, modifier = Modifier.padding(vertical = 8.dp))

        // Sliders
        AdjustmentSlider(
            label = "Brillo",
            value = adj.brightness,
            valueRange = -1f..1f,
            onValueChange = { onUpdateAdjustments(adj.copy(brightness = it)) },
            displayFormat = "%+d"
        )

        AdjustmentSlider(
            label = "Contraste",
            value = adj.contrast,
            valueRange = 0.5f..2.0f,
            onValueChange = { onUpdateAdjustments(adj.copy(contrast = it)) },
            displayFormat = "%.2f"
        )

        AdjustmentSlider(
            label = "Saturación",
            value = adj.saturation,
            valueRange = 0f..2.0f,
            onValueChange = { onUpdateAdjustments(adj.copy(saturation = it)) },
            displayFormat = "%.2f"
        )

        AdjustmentSlider(
            label = "Temperatura (Cálido / Frío)",
            value = adj.temperature,
            valueRange = -1f..1f,
            onValueChange = { onUpdateAdjustments(adj.copy(temperature = it)) },
            displayFormat = "%+d"
        )

        AdjustmentSlider(
            label = "Exposición",
            value = adj.exposure,
            valueRange = -1f..1f,
            onValueChange = { onUpdateAdjustments(adj.copy(exposure = it)) },
            displayFormat = "%+d"
        )

        AdjustmentSlider(
            label = "Viñeta",
            value = adj.vignette,
            valueRange = 0f..1f,
            onValueChange = { onUpdateAdjustments(adj.copy(vignette = it)) },
            displayFormat = "%.2f"
        )
    }
}

@Composable
private fun AdjustmentSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    displayFormat: String
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = TextPrimary, fontSize = 12.sp)
            val formatted = if (displayFormat.contains("d")) {
                String.format(displayFormat, (value * 100).toInt())
            } else {
                String.format(displayFormat, value)
            }
            Text(formatted, color = CyberGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = CyberGreen,
                activeTrackColor = CyberGreen,
                inactiveTrackColor = SurfaceBorder
            )
        )
    }
}

@Composable
fun FiltersPanel(
    selectedFilter: FilterType,
    onSelectFilter: (FilterType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .padding(16.dp)
    ) {
        Text("FILTROS CINEMATOGRÁFICOS & LUTS", color = CyberGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(FilterType.values()) { filter ->
                val isSelected = filter == selectedFilter
                Surface(
                    color = if (isSelected) Color(0xFF0F3820) else SurfaceElevated,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) CyberGreen else SurfaceBorder
                    ),
                    modifier = Modifier
                        .width(100.dp)
                        .height(110.dp)
                        .clickable { onSelectFilter(filter) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    when (filter) {
                                        FilterType.CYBERPUNK -> Color(0xFF00FF88)
                                        FilterType.TEAL_ORANGE -> Color(0xFFFFB300)
                                        FilterType.NOIR -> Color.White
                                        FilterType.GOLDEN_HOUR -> Color(0xFFFF9100)
                                        FilterType.RETRO_VHS -> Color(0xFF00E676)
                                        FilterType.MATRIX -> Color(0xFF76FF03)
                                        FilterType.VIVID -> Color(0xFF00FF66)
                                        FilterType.NONE -> Color.Gray
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = filter.displayName,
                                tint = BackgroundDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Text(
                            text = filter.displayName,
                            color = if (isSelected) CyberGreen else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransitionsPanel(
    selectedTransition: TransitionType,
    onSelectTransition: (TransitionType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .padding(16.dp)
    ) {
        Text("TRANSICIONES ENTRE CLIPS", color = CyberGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(TransitionType.values()) { tr ->
                val isSelected = tr == selectedTransition
                Surface(
                    color = if (isSelected) Color(0xFF0F3820) else SurfaceElevated,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) CyberGreen else SurfaceBorder
                    ),
                    modifier = Modifier
                        .width(95.dp)
                        .height(85.dp)
                        .clickable { onSelectTransition(tr) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = when (tr) {
                                TransitionType.DISSOLVE -> Icons.Default.BlurOn
                                TransitionType.SLIDE_LEFT, TransitionType.SLIDE_RIGHT -> Icons.Default.SwapHoriz
                                TransitionType.WIPE -> Icons.Default.Swipe
                                TransitionType.ZOOM_IN -> Icons.Default.ZoomIn
                                TransitionType.FLASH_WHITE -> Icons.Default.FlashOn
                                TransitionType.GLITCH -> Icons.Default.Grain
                                TransitionType.NONE -> Icons.Default.Close
                            },
                            contentDescription = tr.displayName,
                            tint = if (isSelected) CyberGreen else TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = tr.displayName,
                            color = if (isSelected) CyberGreen else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EffectsPanel(
    selectedEffect: EffectType,
    onSelectEffect: (EffectType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .padding(16.dp)
    ) {
        Text("EFECTOS EN TIEMPO REAL", color = CyberGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(EffectType.values()) { eff ->
                val isSelected = eff == selectedEffect
                Surface(
                    color = if (isSelected) Color(0xFF0F3820) else SurfaceElevated,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) CyberGreen else SurfaceBorder
                    ),
                    modifier = Modifier
                        .width(105.dp)
                        .height(90.dp)
                        .clickable { onSelectEffect(eff) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = when (eff) {
                                EffectType.GLITCH -> Icons.Default.Grain
                                EffectType.BLUR -> Icons.Default.BlurCircular
                                EffectType.ZOOM_PULSE -> Icons.Default.Transform
                                EffectType.FADE_BLACK -> Icons.Default.Nightlight
                                EffectType.RGB_SPLIT -> Icons.Default.ColorLens
                                EffectType.NONE -> Icons.Default.Close
                            },
                            contentDescription = eff.displayName,
                            tint = if (isSelected) NeonGreen else CyberGreen,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = eff.displayName,
                            color = if (isSelected) CyberGreen else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AudioToolsPanel(
    audioClip: AudioClip?,
    onUpdateVolume: (Float) -> Unit,
    onUpdateSpeed: (Float) -> Unit,
    onExtractAudio: () -> Unit,
    onToggleRecording: () -> Unit,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .padding(16.dp)
    ) {
        Text("HERRAMIENTAS DE AUDIO", color = CyberGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        if (audioClip != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Volumen de Audio", color = TextPrimary, fontSize = 12.sp)
                Text("${(audioClip.volume * 100).toInt()}%", color = CyberGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = audioClip.volume,
                onValueChange = onUpdateVolume,
                valueRange = 0f..2.0f,
                colors = SliderDefaults.colors(
                    thumbColor = CyberGreen,
                    activeTrackColor = CyberGreen,
                    inactiveTrackColor = SurfaceBorder
                )
            )

            Text("Velocidad de Audio: ${audioClip.speed}x", color = TextPrimary, fontSize = 12.sp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0.5f, 1.0f, 1.5f, 2.0f).forEach { spd ->
                    FilterChip(
                        selected = audioClip.speed == spd,
                        onClick = { onUpdateSpeed(spd) },
                        label = { Text("${spd}x", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberGreen,
                            selectedLabelColor = BackgroundDark,
                            containerColor = SurfaceElevated,
                            labelColor = TextPrimary
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onToggleRecording,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) DangerRed else Color(0xFF0F3820),
                    contentColor = if (isRecording) Color.White else CyberGreen
                ),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = "Grabar Voz",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isRecording) "Detener" else "Grabar Voz en Off", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = onExtractAudio,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberGreen),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.GraphicEq, contentDescription = "Separar", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Separar de Vídeo", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun TextEditorPanel(
    selectedClip: TextClip?,
    onAddText: (String) -> Unit,
    onUpdateTextClip: (TextClip) -> Unit,
    modifier: Modifier = Modifier
) {
    var textInput by remember(selectedClip) { mutableStateOf(selectedClip?.text ?: "YupiCut Pro") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .padding(16.dp)
    ) {
        Text("EDITOR DE TEXTO & TÍTULOS", color = CyberGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = textInput,
            onValueChange = {
                textInput = it
                if (selectedClip != null) {
                    onUpdateTextClip(selectedClip.copy(text = it))
                }
            },
            label = { Text("Contenido del Texto", color = TextSecondary) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyberGreen,
                unfocusedBorderColor = SurfaceBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (selectedClip != null) {
            Text("Color del Texto", color = TextPrimary, fontSize = 12.sp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("#00FF66", "#00E676", "#FFFFFF", "#FFFF00", "#FF0055", "#00E5FF").forEach { hex ->
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(hex)))
                            .border(
                                width = if (selectedClip.textColorHex == hex) 2.dp else 1.dp,
                                color = if (selectedClip.textColorHex == hex) Color.White else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { onUpdateTextClip(selectedClip.copy(textColorHex = hex)) }
                    )
                }
            }

            Text("Animación de Entrada", color = TextPrimary, fontSize = 12.sp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextAnimation.values().forEach { anim ->
                    FilterChip(
                        selected = selectedClip.animation == anim,
                        onClick = { onUpdateTextClip(selectedClip.copy(animation = anim)) },
                        label = { Text(anim.displayName, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberGreen,
                            selectedLabelColor = BackgroundDark,
                            containerColor = SurfaceElevated,
                            labelColor = TextPrimary
                        )
                    )
                }
            }
        } else {
            Button(
                onClick = { onAddText(textInput) },
                colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = BackgroundDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Insertar Texto en Línea de Tiempo", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AiToolsPanel(
    project: Project,
    onAutoSubtitles: () -> Unit,
    onRemoveSilences: () -> Unit,
    onSceneSplit: () -> Unit,
    onEnhanceAudio: () -> Unit,
    onColorGrade: () -> Unit,
    onAutoTransitions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val suggestions = remember(project) { AiVideoAssistant.getAiSuggestions(project) }
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, contentDescription = "IA", tint = CyberGreen, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("HERRAMIENTAS DE INTELIGENCIA ARTIFICIAL", color = CyberGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AiFeatureButton(
                title = "Auto-Subtítulos",
                icon = Icons.Default.Subtitles,
                onClick = onAutoSubtitles,
                modifier = Modifier.weight(1f)
            )
            AiFeatureButton(
                title = "Quitar Silencios",
                icon = Icons.Default.GraphicEq,
                onClick = onRemoveSilences,
                modifier = Modifier.weight(1f)
            )
            AiFeatureButton(
                title = "Detectar Escenas",
                icon = Icons.Default.MovieFilter,
                onClick = onSceneSplit,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AiFeatureButton(
                title = "Mejora Audio",
                icon = Icons.Default.VolumeUp,
                onClick = onEnhanceAudio,
                modifier = Modifier.weight(1f)
            )
            AiFeatureButton(
                title = "Color IA Pro",
                icon = Icons.Default.Palette,
                onClick = onColorGrade,
                modifier = Modifier.weight(1f)
            )
            AiFeatureButton(
                title = "Transiciones IA",
                icon = Icons.Default.Transform,
                onClick = onAutoTransitions,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("RECOMENDACIONES DEL ASISTENTE INTELIGENTE", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        suggestions.forEach { sug ->
            Surface(
                color = SurfaceElevated,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(sug.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Surface(
                            color = CyberGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(sug.category, color = CyberGreen, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(sug.suggestion, color = TextSecondary, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            when (sug.actionType) {
                                "AUTO_SUBTITLES" -> onAutoSubtitles()
                                "REMOVE_SILENCES" -> onRemoveSilences()
                                "AUTO_TRANSITIONS" -> onAutoTransitions()
                                "AUTO_COLOR" -> onColorGrade()
                                "ENHANCE_AUDIO" -> onEnhanceAudio()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = BackgroundDark),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(sug.actionLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AiFeatureButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SurfaceElevated,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
        modifier = modifier
            .height(72.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = title, tint = CyberGreen, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(title, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
    }
}

@Composable
fun MediaAddPanel(
    onAddSampleVideo: (SampleMediaItem) -> Unit,
    onAddSampleAudio: (SampleAudioItem) -> Unit,
    onPlaySampleAudio: (SampleAudioItem) -> Unit,
    onAddSoundEffect: (name: String, durationMs: Long) -> Unit,
    onPlaySoundEffect: (name: String) -> Unit,
    onPickDeviceMedia: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Clips 4K", "Música Beat", "Efectos SFX", "Importar")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .padding(16.dp)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = SurfaceDark,
            contentColor = CyberGreen
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedTab) {
            0 -> {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(SampleMediaRepository.sampleVideos) { item ->
                        Surface(
                            color = SurfaceElevated,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                            modifier = Modifier
                                .width(130.dp)
                                .height(110.dp)
                                .clickable { onAddSampleVideo(item) }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Icon(Icons.Default.Videocam, contentDescription = "Video", tint = CyberGreen, modifier = Modifier.size(16.dp))
                                    Text("${item.durationMs / 1000}s", color = TextSecondary, fontSize = 10.sp)
                                }
                                Text(item.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                                Text(item.category, color = CyberGreen, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
            1 -> {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(SampleMediaRepository.sampleAudios) { audio ->
                        Surface(
                            color = SurfaceElevated,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                            modifier = Modifier
                                .width(145.dp)
                                .height(115.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { onPlaySampleAudio(audio) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Escuchar", tint = CyberGreen)
                                    }
                                    Text("${audio.bpm} BPM", color = TextSecondary, fontSize = 10.sp)
                                }
                                Text(audio.title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                Button(
                                    onClick = { onAddSampleAudio(audio) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = BackgroundDark),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(28.dp)
                                ) {
                                    Text("+ Añadir", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                // Sound Effects SFX with immediate listen & add
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(SampleMediaRepository.soundEffects) { (sfxName, durMs) ->
                        Surface(
                            color = SurfaceElevated,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                            modifier = Modifier
                                .width(135.dp)
                                .height(115.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { onPlaySoundEffect(sfxName) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.VolumeUp, contentDescription = "Probar", tint = NeonGreen)
                                    }
                                    Text("${durMs}ms", color = TextSecondary, fontSize = 10.sp)
                                }
                                Text(sfxName, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                Button(
                                    onClick = { onAddSoundEffect(sfxName, durMs) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F3820), contentColor = CyberGreen),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(28.dp)
                                ) {
                                    Text("+ Insertar", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
            3 -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Selecciona cualquier archivo de vídeo o audio guardado en tu teléfono.", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onPickDeviceMedia,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = BackgroundDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "Explorar")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Abrir Galería / Archivos Locales", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
