package com.alibabacloud.api.service.notification

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project

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
    }

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

            val feedbackNotificationAction = object : NotificationAction("去反馈") {
                override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                    feedbackAction.invoke()
                    notification.expire()
                }
            }

            val closeNotificationAction = object : NotificationAction("关闭") {
                override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                    closeAction.invoke()
                    notification.expire()
                }
            }

            val noRemindNotificationAction = object : NotificationAction("30天内不再弹出") {
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
}