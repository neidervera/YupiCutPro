package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ProjectStorage
import com.example.data.SampleMediaRepository
import com.example.engine.AiVideoAssistant
import com.example.model.AspectRatio
import com.example.model.VideoClip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("YupiCut", appName)
    }

    @Test
    fun `project duration and clip management`() {
        val project = SampleMediaRepository.createInitialDemoProject()
        assertTrue(project.videoClips.isNotEmpty())
        assertTrue(project.totalDurationMs > 0L)
    }

    @Test
    fun `project storage persistence`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = ProjectStorage(context)
        val initialProjects = storage.getAllProjects()
        assertTrue(initialProjects.isNotEmpty())

        val testProject = initialProjects.first().copy(
            id = "test_proj_123",
            name = "Test Unit Project",
            aspectRatio = AspectRatio.RATIO_9_16
        )
        storage.saveProject(testProject)

        val retrieved = storage.getProject("test_proj_123")
        assertEquals("Test Unit Project", retrieved?.name)
        assertEquals(AspectRatio.RATIO_9_16, retrieved?.aspectRatio)

        val deleted = storage.deleteProject("test_proj_123")
        assertTrue(deleted)
    }

    @Test
    fun `ai auto subtitle generation`() {
        val project = SampleMediaRepository.createInitialDemoProject()
        val subtitles = AiVideoAssistant.generateAutoSubtitles(project)
        assertTrue(subtitles.isNotEmpty())
    }
}
