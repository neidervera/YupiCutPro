package com.example.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AspectRatio
import com.example.model.Project
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    projects: List<Project>,
    onCreateNewProject: () -> Unit,
    onOpenProject: (Project) -> Unit,
    onDuplicateProject: (Project) -> Unit,
    onDeleteProject: (String) -> Unit,
    onQuickAiTool: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var projectToDelete by remember { mutableStateOf<Project?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brush.linearGradient(listOf(CyberGreen, Color(0xFF0A3A20)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MovieFilter, contentDescription = "Logo", tint = BackgroundDark, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "YupiCut",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = CyberGreen.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberGreen.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "PRO",
                                color = CyberGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { /* Settings */ }) {
                        Icon(Icons.Default.Settings, contentDescription = "Ajustes", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Giant "Nuevo Proyecto" Hero Banner (CapCut / Filmora style)
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clickable { onCreateNewProject() }
                        .testTag("create_new_project_button")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        SurfaceDark,
                                        SurfaceElevated,
                                        Color(0xFF0F3820).copy(alpha = 0.4f)
                                    )
                                )
                            )
                            .padding(20.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Nuevo Proyecto",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Comienza a editar clips 4K, audio e IA",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(CyberGreen, Color(0xFF0A3A20)))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Crear", tint = BackgroundDark, modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                }
            }

            // 2. Quick AI Tools Row
            item {
                Text(
                    text = "HERRAMIENTAS RÁPIDAS IA",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickToolItem(
                        title = "Auto Subtítulos",
                        icon = Icons.Default.Subtitles,
                        color = CyberGreen,
                        onClick = { onQuickAiTool("SUBTITLES") },
                        modifier = Modifier.weight(1f)
                    )
                    QuickToolItem(
                        title = "Quitar Silencios",
                        icon = Icons.Default.GraphicEq,
                        color = AudioTrackBorder,
                        onClick = { onQuickAiTool("SILENCE") },
                        modifier = Modifier.weight(1f)
                    )
                    QuickToolItem(
                        title = "Cámara Lenta",
                        icon = Icons.Default.Speed,
                        color = WarningAmber,
                        onClick = { onQuickAiTool("SLOWMO") },
                        modifier = Modifier.weight(1f)
                    )
                    QuickToolItem(
                        title = "Fondo IA",
                        icon = Icons.Default.AutoFixHigh,
                        color = EffectTrackBorder,
                        onClick = { onQuickAiTool("CHROMA") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 3. Projects List Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MIS PROYECTOS (${projects.size})",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            // 4. Projects Cards
            items(projects) { project ->
                ProjectCardItem(
                    project = project,
                    onOpen = { onOpenProject(project) },
                    onDuplicate = { onDuplicateProject(project) },
                    onDelete = { projectToDelete = project }
                )
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // Delete confirmation dialog
    projectToDelete?.let { proj ->
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            title = { Text("¿Eliminar proyecto?", color = Color.White) },
            text = { Text("Se eliminará '${proj.name}' permanentemente.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteProject(proj.id)
                        projectToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToDelete = null }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = SurfaceElevated
        )
    }
}

@Composable
private fun QuickToolItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SurfaceDark,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
        modifier = modifier
            .height(80.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ProjectCardItem(
    project: Project,
    onOpen: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    val dateStr = dateFormat.format(Date(project.lastModified))
    val durationSec = (project.totalDurationMs / 1000f)

    Surface(
        color = SurfaceDark,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail Box
            Box(
                modifier = Modifier
                    .size(width = 80.dp, height = 64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(VideoTrackColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayCircle, contentDescription = "Play", tint = CyberGreen, modifier = Modifier.size(28.dp))
                // Aspect Ratio indicator
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                ) {
                    Text(
                        text = project.aspectRatio.iconName,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Info Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = String.format("%.1fs", durationSec),
                        color = CyberGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(" • ", color = TextTertiary, fontSize = 11.sp)
                    Text(
                        text = "${project.videoClips.size} clips",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Text(" • ", color = TextTertiary, fontSize = 11.sp)
                    Text(
                        text = project.exportResolution,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dateStr,
                    color = TextTertiary,
                    fontSize = 10.sp
                )
            }

            // More Options Dropdown
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Opciones", tint = TextSecondary)
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(SurfaceElevated)
                ) {
                    DropdownMenuItem(
                        text = { Text("Abrir en Editor", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = CyberGreen) },
                        onClick = {
                            showMenu = false
                            onOpen()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Duplicar", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = TextPrimary) },
                        onClick = {
                            showMenu = false
                            onDuplicate()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Eliminar", color = DangerRed) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = DangerRed) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
