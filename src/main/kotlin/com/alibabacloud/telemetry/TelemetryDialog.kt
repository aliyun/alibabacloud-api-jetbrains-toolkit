package com.alibabacloud.telemetry

import com.alibabacloud.api.service.constants.NotificationGroups
import com.alibabacloud.api.service.notification.NormalNotification
import com.alibabacloud.constants.PropertiesConstants
import com.alibabacloud.i18n.I18nUtils
import com.alibabacloud.states.ToolkitSettingsState
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class TelemetryDialog(private val project: Project) {
    companion object {
        var notificationService: NormalNotification = NormalNotification
    }

    fun checkAndShowNotification() {
        val properties = PropertiesComponent.getInstance()
        val lastPromptTime = properties.getValue(PropertiesConstants.TELEMETRY_DIALOG_LAST_PROMPT_KEY)
        val lastDateTime = lastPromptTime?.let { LocalDateTime.parse(it) } ?: LocalDateTime.MIN
        val currentDateTime = LocalDateTime.now()

        val expirationHours = properties.getInt(PropertiesConstants.TELEMETRY_DIALOG_KEY, 15 * 24)
        val timeSinceLastPrompt = ChronoUnit.HOURS.between(lastDateTime, currentDateTime)

        if (lastPromptTime == null || timeSinceLastPrompt >= expirationHours) {
            notificationService.showNotificationWithActions(
                project,
                NotificationGroups.TELEMETRY_DATA_GROUP,
                I18nUtils.getMsg("telemetry.title"),
                I18nUtils.getMsg("telemetry.dialog"),
                NotificationType.INFORMATION,
                listOf(
                    I18nUtils.getMsg("dialog.yes") to {
                        properties.setValue(PropertiesConstants.TELEMETRY_DIALOG_KEY, 15 * 24, 15 * 24)
                        properties.setValue(
                            PropertiesConstants.TELEMETRY_DIALOG_LAST_PROMPT_KEY,
                            currentDateTime.toString()
                        )
                        ToolkitSettingsState.getInstance().isTelemetryEnabled = true
                        TelemetryService.getInstance().setTelemetryEnabled(true)
                    },
                    I18nUtils.getMsg("dialog.no") to {
                        properties.setValue(PropertiesConstants.TELEMETRY_DIALOG_KEY, 1 * 24, 15 * 24)
                        properties.setValue(
                            PropertiesConstants.TELEMETRY_DIALOG_LAST_PROMPT_KEY,
                            currentDateTime.toString()
                        )
                        ToolkitSettingsState.getInstance().isTelemetryEnabled = false
                        TelemetryService.getInstance().setTelemetryEnabled(false)
                    },
                    I18nUtils.getMsg("dialog.no.pop") to {
                        properties.setValue(PropertiesConstants.TELEMETRY_DIALOG_KEY, 15 * 24, 15 * 24)
                        properties.setValue(
                            PropertiesConstants.TELEMETRY_DIALOG_LAST_PROMPT_KEY,
                            currentDateTime.toString()
                        )
                        ToolkitSettingsState.getInstance().isTelemetryEnabled = false
                        TelemetryService.getInstance().setTelemetryEnabled(false)
                    },
                )
            )
        }
    }

}