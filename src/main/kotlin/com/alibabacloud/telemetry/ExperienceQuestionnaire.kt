package com.alibabacloud.telemetry

import com.alibabacloud.api.service.constants.NotificationGroups
import com.alibabacloud.api.service.notification.NormalNotification
import com.google.gson.JsonSyntaxException
import com.intellij.ide.BrowserUtil
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandler
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.network.CefRequest
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

    fun executeQuestionnaire(browser: JBCefBrowser, callback: (String?) -> Unit) {
        val query = JBCefJSQuery.create(browser as JBCefBrowserBase)
        val properties = PropertiesComponent.getInstance()
        val lastPromptTime = properties.getValue(QUESTIONNAIRE_LAST_PROMPT_KEY)
        val lastDateTime = lastPromptTime?.let { LocalDateTime.parse(it) } ?: LocalDateTime.MIN
        val currentDateTime = LocalDateTime.now()

        val expirationHours = properties.getInt(QUESTIONNAIRE_EXPIRATION_KEY, 30 * 24)
        val timeSinceLastPrompt = ChronoUnit.HOURS.between(lastDateTime, currentDateTime)
        var isNotice = false

        if (lastPromptTime == null || timeSinceLastPrompt >= expirationHours) {
            isNotice = true
        }

        // 如果什么都没点，是不是就没有返回
        query.addHandler { arg: String? ->
            try {
                println("callback arg---$arg")
                callback(arg)
                return@addHandler JBCefJSQuery.Response("ok")
            } catch (e: JsonSyntaxException) {
                return@addHandler JBCefJSQuery.Response(null, 0, "errorMsg")
            }
        }

        browser.jbCefClient.addLoadHandler(
            object : CefLoadHandlerAdapter() {
                override fun onLoadingStateChange(
                    cefBrowser: CefBrowser,
                    isLoading: Boolean,
                    canGoBack: Boolean,
                    canGoForward: Boolean,
                ) {
                }

                override fun onLoadStart(
                    cefBrowser: CefBrowser,
                    frame: CefFrame,
                    transitionType: CefRequest.TransitionType,
                ) {
                }

                override fun onLoadEnd(cefBrowser: CefBrowser, frame: CefFrame, httpStatusCode: Int) {
//                    println("isNotice---$isNotice")
                    isNotice = true
                    cefBrowser.executeJavaScript(
                        """
                        getNoticeFlag($isNotice);
                        window.updateQuestionnaireExpiration = function(arg) {
                        ${
                            query.inject(
                                "arg",
                                "response => console.log('读取参数成功', (response))",
                                "(error_code, error_message) => console.log('读取参数失败', error_code, error_message)",
                            )
                        }
                        };

                            """.trimIndent(),
                        browser.cefBrowser.url,
                        0,
                    )
                }

                override fun onLoadError(
                    cefBrowser: CefBrowser,
                    frame: CefFrame,
                    errorCode: CefLoadHandler.ErrorCode,
                    errorText: String,
                    failedUrl: String,
                ) {
                }
            },
            browser.cefBrowser,
        )
    }
}