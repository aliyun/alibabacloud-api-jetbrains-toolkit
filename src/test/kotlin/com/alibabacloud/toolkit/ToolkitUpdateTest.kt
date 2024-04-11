package com.alibabacloud.toolkit

import com.alibabacloud.api.service.notification.NormalNotification
import com.alibabacloud.states.ToolkitSettingsState
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.updateSettings.impl.PluginDownloader
import com.intellij.testFramework.ApplicationRule
import com.intellij.testFramework.DisposableRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.*

class ToolkitUpdateTest {
    @Rule
    @JvmField
    val applicationRule = ApplicationRule()

    @Rule
    @JvmField
    val disposableRule = DisposableRule()

    private lateinit var update: ToolkitUpdate
    private val mockDescriptor = getMockDescriptor(ToolkitInfo.PLUGIN_ID, "0.0.5")
    private var isAutoUpdateEnabled = false
    private lateinit var mockProject: Project
    private lateinit var normalNotification: NormalNotification
    private lateinit var mockPd: PluginDownloader

    @Before
    fun setup() {
        update = spy(ToolkitUpdate())
        update.stub {
            on {
                getPdList()
            } doAnswer {
                val pdSpy = mock<PluginDownloader>()
                pdSpy.stub {
                    onGeneric {
                        id
                    } doAnswer { mockDescriptor.pluginId }
                    onGeneric {
                        pluginVersion
                    } doAnswer { mockDescriptor.version }
                    onGeneric {
                        install()
                    } doAnswer {}
                }
                listOf(pdSpy)
            }
        }
        isAutoUpdateEnabled = ToolkitSettingsState.getInstance().isAutoUpdateEnabled
    }

    @After
    fun teardown() {
        ToolkitSettingsState.getInstance().isAutoUpdateEnabled = isAutoUpdateEnabled
    }

    @Test
    fun `test getLatestPd() should return null if toolkit download is not found`() {
        val mockDescriptor = getMockDescriptor("test", "0.1")
        assertThat(update.getLatestPd(mockDescriptor)).isNull()
    }

    @Test
    fun `test getLatestPd() should return null if version is same or later`() {
        // same
        var mockDescriptor = getMockDescriptor(ToolkitInfo.PLUGIN_ID, "0.0.5")
        assertThat(update.getLatestPd(mockDescriptor)).isNull()
        // later
        mockDescriptor = getMockDescriptor(ToolkitInfo.PLUGIN_ID, "0.0.6")
        assertThat(update.getLatestPd(mockDescriptor)).isNull()
    }

    @Test
    fun `test getLatestPd() should return toolkit if version is not latest`() {
        val mockDescriptor = getMockDescriptor(ToolkitInfo.PLUGIN_ID, "0.0.4")
        val pd = update.getLatestPd(mockDescriptor)
        assertThat(pd).isNotNull
        assertThat(pd?.pluginVersion).isEqualTo("0.0.5")
        assertThat(pd?.id.toString()).isEqualTo(ToolkitInfo.PLUGIN_ID)
    }

    @Test
    fun `test toolkit auto update consistent with user setting`() {
        mockProject = Mockito.mock(Project::class.java)
        ToolkitSettingsState.getInstance().isAutoUpdateEnabled = false
        update.runActivity(mockProject)
        verify(update, never()).checkAndUpdate(any(), any())

        ToolkitSettingsState.getInstance().isAutoUpdateEnabled = true
        update.runActivity(mockProject)
        verify(update).checkAndUpdate(any(), any())
    }

    private fun getMockDescriptor(id: String, version: String): IdeaPluginDescriptor {
        val mockDescriptor = mock<IdeaPluginDescriptor>()
        whenever(mockDescriptor.version).thenReturn(version)
        whenever(mockDescriptor.pluginId).thenReturn(PluginId.getId(id))
        return mockDescriptor
    }
}