package com.alibabacloud.telemetry

import com.alibabacloud.api.service.constants.NotificationGroups
import com.alibabacloud.api.service.notification.NormalNotification
import com.intellij.ide.BrowserUtil
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import java.net.URI
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class ExperienceQuestionnaire(private val project: Project) {
    companion object {
        const val QUESTIONNAIRE_EXPIRATION_KEY = "questionnaireExpiration"
        const val QUESTIONNAIRE_LAST_PROMPT_KEY = "lastPromptTime"
    }

    fun checkAndShowNotification() {
        val properties = PropertiesComponent.getInstance()
        val lastPromptTime = properties.getValue(QUESTIONNAIRE_LAST_PROMPT_KEY)
        val lastDateTime = lastPromptTime?.let { LocalDateTime.parse(it) } ?: LocalDateTime.MIN
        val currentDateTime = LocalDateTime.now()

        val expirationHours = properties.getInt(QUESTIONNAIRE_EXPIRATION_KEY, 30 * 24)
        val timeSinceLastPrompt = ChronoUnit.HOURS.between(lastDateTime, currentDateTime)

        if (lastPromptTime == null || timeSinceLastPrompt >= expirationHours) {
            NormalNotification.showExperienceQuestionnaire(project,
                NotificationGroups.QUESTIONNAIRE_NOTIFICATION_GROUP,
                "Alibaba Cloud Developer Toolkit",
                "您在使用插件期间是否遇到问题？欢迎吐槽或点赞，您的反馈对我们十分重要！",
                NotificationType.INFORMATION,
                feedbackAction = {
                    BrowserUtil.browse(URI("https://g.alicdn.com/aes/tracker-survey-preview/0.0.13/survey.html?pid=fePxMy&id=3494"))
                    properties.setValue(QUESTIONNAIRE_EXPIRATION_KEY, 30 * 24, 30 * 24)
                    properties.setValue(QUESTIONNAIRE_LAST_PROMPT_KEY, currentDateTime.toString())
                },
                closeAction = {
                    properties.setValue(QUESTIONNAIRE_EXPIRATION_KEY, 1 * 24, 30 * 24)
                    properties.setValue(QUESTIONNAIRE_LAST_PROMPT_KEY, currentDateTime.toString())
                },
                noRemindAction = {
                    properties.setValue(QUESTIONNAIRE_EXPIRATION_KEY, 30 * 24, 30 * 24)
                    properties.setValue(QUESTIONNAIRE_LAST_PROMPT_KEY, currentDateTime.toString())
                })
        }
    }
}