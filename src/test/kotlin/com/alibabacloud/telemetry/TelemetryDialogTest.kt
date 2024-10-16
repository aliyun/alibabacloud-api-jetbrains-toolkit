package com.alibabacloud.telemetry

import com.alibabacloud.api.service.notification.NormalNotification
import com.alibabacloud.constants.PropertiesConstants
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class TelemetryDialogTest {

    private lateinit var telemetryDialog: TelemetryDialog
    private lateinit var mockProperties: PropertiesComponent
    private lateinit var mockNotificationService: NormalNotification
    private lateinit var mockProject: Project
    private lateinit var mockApplication: Application

    @BeforeEach
    fun setUp() {
        mockNotificationService = mockk()
        TelemetryDialog.notificationService = mockNotificationService
        mockProperties = mockk()
        mockProject = mockk()

        mockkStatic(PropertiesComponent::class)
        every { PropertiesComponent.getInstance() } returns mockProperties
        every { mockProperties.getValue(PropertiesConstants.TELEMETRY_DIALOG_LAST_PROMPT_KEY) }.returns(null)
        every { mockProperties.getValue(PropertiesConstants.TELEMETRY_DIALOG_KEY) }.returns(null)
        every { mockProperties.getValue(PropertiesConstants.PREFERENCE_LANGUAGE) }.returns("en_US")
        every { mockProperties.getInt(PropertiesConstants.TELEMETRY_DIALOG_KEY, 15 * 24) }.returns(15 * 24)

        mockApplication = mockk(relaxed = true)
        mockkStatic(ApplicationManager::class)
        every { ApplicationManager.getApplication() } returns mockApplication

        every {
            mockNotificationService.showNotificationWithActions(
                eq(mockProject),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns Unit

        telemetryDialog = TelemetryDialog(mockProject)
    }

    @AfterEach
    fun tearDown() {
        TelemetryDialog.notificationService = NormalNotification
    }

    @Test
    fun `should show notification if lastPromptTime is null`() {
        telemetryDialog.checkAndShowNotification()

        verify(exactly = 1) {
            mockNotificationService.showNotificationWithActions(
                mockProject,
                "Alibaba Cloud Developer Toolkit: Telemetry",
                "Help us improve Alibaba Cloud Developer Toolkit",
                "Would you please consider sharing your plugin usage data with Alibaba Cloud to help us improve our products and services?",
                NotificationType.INFORMATION,
                any()
            )
        }
    }

    @Test
    fun `should show notification if time since last prompt exceeds expiration hours`() {
        val lastDateTime = LocalDateTime.now().minusDays(16)
        every { mockProperties.getValue(PropertiesConstants.TELEMETRY_DIALOG_LAST_PROMPT_KEY) } returns lastDateTime.toString()
        every { mockProperties.getInt(PropertiesConstants.TELEMETRY_DIALOG_KEY, 15 * 24) }.returns(15 * 24)

        telemetryDialog.checkAndShowNotification()

        verify(exactly = 1) {
            mockNotificationService.showNotificationWithActions(
                mockProject,
                "Alibaba Cloud Developer Toolkit: Telemetry",
                "Help us improve Alibaba Cloud Developer Toolkit",
                "Would you please consider sharing your plugin usage data with Alibaba Cloud to help us improve our products and services?",
                NotificationType.INFORMATION,
                any()
            )
        }
    }

    @Test
    fun `should not show notification if time since last prompt does not exceed expiration hours`() {
        val lastDateTime = LocalDateTime.now().minusDays(5)
        every { mockProperties.getValue(PropertiesConstants.TELEMETRY_DIALOG_LAST_PROMPT_KEY) } returns lastDateTime.toString()
        every { mockProperties.getInt(PropertiesConstants.TELEMETRY_DIALOG_KEY, 15 * 24) }.returns(15 * 24)


        telemetryDialog.checkAndShowNotification()
        verify(exactly = 0){
            mockNotificationService.showNotificationWithActions(
                mockProject,
                "Alibaba Cloud Developer Toolkit: Telemetry",
                "Help us improve Alibaba Cloud Developer Toolkit",
                "Would you please consider sharing your plugin usage data with Alibaba Cloud to help us improve our products and services?",
                NotificationType.INFORMATION,
                any()
            )
        }
    }
}