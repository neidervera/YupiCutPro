package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.data.ProjectStorage
import com.example.data.SampleMediaRepository
import com.example.model.Project
import com.example.ui.editor.ActivePanel
import com.example.ui.editor.EditorScreen
import com.example.ui.editor.EditorViewModel
import com.example.ui.home.HomeScreen
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.MyApplicationTheme

enum class AppScreen {
    HOME,
    EDITOR
}

class MainActivity : ComponentActivity() {

    private val editorViewModel: EditorViewModel by viewModels()
    private val projectStorage by lazy { ProjectStorage(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                var currentScreen by remember { mutableStateOf(AppScreen.HOME) }
                var projectList by remember { mutableStateOf(projectStorage.getAllProjects()) }

                fun refreshProjects() {
                    projectList = projectStorage.getAllProjects()
                }

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                    color = BackgroundDark
                ) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            if (targetState == AppScreen.EDITOR) {
                                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                    slideOutHorizontally { width -> -width } + fadeOut()
                                )
                            } else {
                                (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                    slideOutHorizontally { width -> width } + fadeOut()
                                )
                            }
                        },
                        label = "screen_transition"
                    ) { screen ->
                        when (screen) {
                            AppScreen.HOME -> {
                                HomeScreen(
                                    projects = projectList,
                                    onCreateNewProject = {
                                        val newProject = Project(
                                            name = "Proyecto #${projectList.size + 1}",
                                            videoClips = listOf(
                                                com.example.model.VideoClip(
                                                    title = "Clip Inicial",
                                                    durationMs = 5000L,
                                                    startTrimMs = 0L,
                                                    endTrimMs = 5000L,
                                                    timelineStartMs = 0L,
                                                    sampleType = "cyber_city"
                                                )
                                            ),
                                            audioClips = listOf(
                                                com.example.model.AudioClip(
                                                    title = "Cyber Synth Beat",
                                                    durationMs = 8000L,
                                                    startTrimMs = 0L,
                                                    endTrimMs = 8000L,
                                                    timelineStartMs = 0L,
                                                    waveform = SampleMediaRepository.generateWaveform(40)
                                                )
                                            )
                                        )
                                        projectStorage.saveProject(newProject)
                                        editorViewModel.loadProject(newProject)
                                        currentScreen = AppScreen.EDITOR
                                    },
                                    onOpenProject = { proj ->
                                        editorViewModel.loadProject(proj)
                                        currentScreen = AppScreen.EDITOR
                                    },
                                    onDuplicateProject = { proj ->
                                        projectStorage.duplicateProject(proj)
                                        refreshProjects()
                                    },
                                    onDeleteProject = { id ->
                                        projectStorage.deleteProject(id)
                                        refreshProjects()
                                    },
                                    onQuickAiTool = { tool ->
                                        val proj = projectList.firstOrNull() ?: SampleMediaRepository.createInitialDemoProject()
                                        editorViewModel.loadProject(proj)
                                        when (tool) {
                                            "SUBTITLES" -> editorViewModel.setActivePanel(ActivePanel.AI_TOOLS)
                                            "SILENCE" -> editorViewModel.setActivePanel(ActivePanel.AI_TOOLS)
                                            "SLOWMO" -> editorViewModel.setActivePanel(ActivePanel.ADJUSTMENTS)
                                            "CHROMA" -> editorViewModel.setActivePanel(ActivePanel.AI_TOOLS)
                                        }
                                        currentScreen = AppScreen.EDITOR
                                    }
                                )
                            }
                            AppScreen.EDITOR -> {
                                EditorScreen(
                                    viewModel = editorViewModel,
                                    onNavigateBack = {
                                        refreshProjects()
                                        currentScreen = AppScreen.HOME
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
