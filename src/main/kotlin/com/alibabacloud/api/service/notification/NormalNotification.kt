package com.alibabacloud.api.service.notification

import com.alibabacloud.i18n.I18nUtils
import com.alibabacloud.models.telemetry.TelemetryData
import com.alibabacloud.telemetry.TelemetryService
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import java.util.*

object NormalNotification {
    fun showMessage(project: Project?, group: String, title: String, content: String, type: NotificationType) {
        ApplicationManager.getApplication().invokeLater {
            NotificationGroupManager.getInstance().getNotificationGroup(group)
                .createNotification(
                    title,
                    content,
                    type
                ).notify(project)
        }
        TelemetryService.getInstance().record(
            TelemetryData(
                "dialog",
                "alibabacloud.dialog",
                if (I18nUtils.getLocale() == Locale.CHINA) "cn" else "en",
                System.currentTimeMillis().toString(),
                title = title,
                content = content,
                dialogType = type.toString().lowercase()
            )
        )
    }

    fun showNotificationWithActions(
        project: Project?,
        group: String,
        title: String,
        content: String,
        type: NotificationType,
        actions: List<Pair<String, () -> Unit>>
    ) {
        ApplicationManager.getApplication().invokeLater {
            val notification = NotificationGroupManager.getInstance().getNotificationGroup(group)
                .createNotification(title, content, type)

            for ((actionText, action) in actions) {
                val notificationAction = object : NotificationAction(actionText) {
                    override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                        action.invoke()
                        notification.expire()
                        TelemetryService.getInstance().record(
                            TelemetryData(
                                "dialog",
                                "alibabacloud.dialog",
                                if (I18nUtils.getLocale() == Locale.CHINA) "cn" else "en",
                                System.currentTimeMillis().toString(),
                                title = title,
                                content = content,
                                dialogType = type.toString().lowercase(),
                                dialogOption = actionText
                            )
                        )
                    }
                }
                notification.addAction(notificationAction)
            }
            notification.notify(project)
        }
    }

    @Deprecated("use showNotificationWithActions instead")
    fun showMessageWithActions(
        project: Project?,
        group: String,
        title: String,
        content: String,
        type: NotificationType,
        yesAction: () -> Unit,
        noAction: () -> Unit
    ) {
        ApplicationManager.getApplication().invokeLater {
            val notification = NotificationGroupManager.getInstance().getNotificationGroup(group)
                .createNotification(title, content, type)

            val yesNotificationAction = object : NotificationAction("Yes") {
                override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                    yesAction.invoke()
                    notification.expire()
                }
            }

            val noNotificationAction = object : NotificationAction("No") {
                override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                    noAction.invoke()
                    notification.expire()
                }
            }

            notification.addAction(yesNotificationAction)
            notification.addAction(noNotificationAction)
            notification.notify(project)
        }
    }

    @Deprecated("use showNotificationWithActions instead")
    fun showExperienceQuestionnaire(
        project: Project?,
        group: String,
        title: String,
        content: String,
        type: NotificationType,
        feedbackAction: () -> Unit,
        closeAction: () -> Unit,
        noRemindAction: () -> Unit
    ) {
        ApplicationManager.getApplication().invokeLater {
            val notification = NotificationGroupManager.getInstance().getNotificationGroup(group)
                .createNotification(title, content, type)

            val feedbackNotificationAction = object : NotificationAction(I18nUtils.getMsg("feedback.go")) {
                override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                    feedbackAction.invoke()
                    notification.expire()
                }
            }

            val closeNotificationAction = object : NotificationAction(I18nUtils.getMsg("action.close")) {
                override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                    closeAction.invoke()
                    notification.expire()
                }
            }

            val noRemindNotificationAction = object : NotificationAction(I18nUtils.getMsg("questionnaire.no.pop")) {
                override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                    noRemindAction.invoke()
                    notification.expire()
                }
            }

            notification.addAction(feedbackNotificationAction)
            notification.addAction(closeNotificationAction)
            notification.addAction(noRemindNotificationAction)
            notification.notify(project)
        }
    }

    fun showPluginUpdateInstalled(
        project: Project?,
        group: String,
        title: String,
        content: String,
        type: NotificationType,
        restartAction: () -> Unit,
        restartLaterAction: () -> Unit,
        updateSettingsAction: () -> Unit
    ) {
        ApplicationManager.getApplication().invokeLater {
            val notification = NotificationGroupManager.getInstance().getNotificationGroup(group)
                .createNotification(title, content, type)

            val restartNotificationAction = object : NotificationAction(I18nUtils.getMsg("plugin.update.restart.now")) {
                override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                    restartAction.invoke()
                    notification.expire()
                    TelemetryService.getInstance().record(
                        TelemetryData(
                            "dialog",
                            "alibabacloud.dialog",
                            if (I18nUtils.getLocale() == Locale.CHINA) "cn" else "en",
                            System.currentTimeMillis().toString(),
                            title = title,
                            content = content,
                            dialogType = type.toString().lowercase(),
                            dialogOption = "restart now"
                        )
                    )
                }
            }

            val restartLaterNotificationAction = object : NotificationAction(I18nUtils.getMsg("plugin.update.restart.later")) {
                override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                    restartLaterAction.invoke()
                    notification.expire()
                    TelemetryService.getInstance().record(
                        TelemetryData(
                            "dialog",
                            "alibabacloud.dialog",
                            if (I18nUtils.getLocale() == Locale.CHINA) "cn" else "en",
                            System.currentTimeMillis().toString(),
                            title = title,
                            content = content,
                            dialogType = type.toString().lowercase(),
                            dialogOption = "restart later"
                        )
                    )
                }
            }

            val updateSettingsNotificationAction = object : NotificationAction(I18nUtils.getMsg("plugin.auto.update.settings")) {
                override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                    updateSettingsAction.invoke()
                    notification.expire()
                    TelemetryService.getInstance().record(
                        TelemetryData(
                            "dialog",
                            "alibabacloud.dialog",
                            if (I18nUtils.getLocale() == Locale.CHINA) "cn" else "en",
                            System.currentTimeMillis().toString(),
                            title = title,
                            content = content,
                            dialogType = type.toString().lowercase(),
                            dialogOption = "manage settings"
                        )
                    )
                }
            }

            notification.addAction(restartNotificationAction)
            notification.addAction(restartLaterNotificationAction)
            notification.addAction(updateSettingsNotificationAction)
            notification.notify(project)
        }
    }
}