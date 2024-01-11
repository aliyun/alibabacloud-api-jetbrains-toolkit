package com.alibabacloud.api.service.notification

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
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
}