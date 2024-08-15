package com.alibabacloud.api.service.sdksample

import com.alibabacloud.api.service.OkHttpClientProvider
import com.alibabacloud.api.service.constants.ApiConstants
import com.alibabacloud.api.service.constants.NotificationGroups
import com.alibabacloud.api.service.notification.NormalNotification
import com.alibabacloud.api.service.sdksample.util.AutoInstallPkgUtil
import com.alibabacloud.api.service.util.FormatUtil
import com.alibabacloud.api.service.util.RequestUtil
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import com.intellij.ide.BrowserUtil
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.EditorSettings
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFileFactory
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandler
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.network.CefRequest
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.IOException
import java.net.URI
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants
import javax.swing.border.Border

class SdkSample {
    companion object {
        fun sdkSamplePanel(
            apiName: String,
            defaultVersion: String,
            productName: String,
            project: Project,
            sdkPanel: JPanel,
            demoSdkObject: JsonObject
        ) {
            sdkPanel.removeAll()
            sdkPanel.revalidate()
            sdkPanel.repaint()
            val headPanel = JPanel(FlowLayout(FlowLayout.LEFT))
            val headScrollPane = JBScrollPane(headPanel).apply {
                horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
                verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
            }
            headScrollPane.minimumSize = Dimension(0, 0)
            headPanel.minimumSize = Dimension(0, 0)
            val languages = arrayOf("Java-async", "Java", "Python", "TypeScript", "Go", "PHP")
            val langComboBox = ComboBox(languages)

            val applicationNamesInfo = ApplicationNamesInfo.getInstance()
            val ideName = applicationNamesInfo.fullProductName

            var editor: EditorEx?
            val scrollPane = JBScrollPane()
            scrollPane.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
            scrollPane.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS

            var installUrl: String
            var codeUrl = "https://github.com/aliyun/alibabacloud-java-sdk/tree/master/${productName.lowercase()}-${defaultVersion.replace("-", "")}"

            when (ideName) {
                "PyCharm" -> {
                    val demoSdkPy =
                        if (demoSdkObject.size() == 0) ApiConstants.CODE_GENERATE_ERROR else demoSdkObject.get("python")?.asString
                            ?: ApiConstants.CODE_LANG_NOT_SUPPORT
                    editor = createEditorWithPsiFile(project, apiName, demoSdkPy, "python")
                    langComboBox.selectedItem = "Python"
                    installUrl =
                        "https://api.aliyun.com/api-tools/sdk/$productName?version=$defaultVersion&language=python-tea"
                }

                "GoLand" -> {
                    val demoSdkGo =
                        if (demoSdkObject.size() == 0) ApiConstants.CODE_GENERATE_ERROR else demoSdkObject.get("go")?.asString
                            ?: ApiConstants.CODE_LANG_NOT_SUPPORT
                    editor = createEditorWithPsiFile(project, apiName, demoSdkGo, "go")
                    langComboBox.selectedItem = "Go"
                    installUrl =
                        "https://api.aliyun.com/api-tools/sdk/$productName?version=$defaultVersion&language=go-tea"
                }

                "WebStorm" -> {
                    val demoSdkTs =
                        if (demoSdkObject.size() == 0) ApiConstants.CODE_GENERATE_ERROR else demoSdkObject.get("typescript")?.asString
                            ?: ApiConstants.CODE_LANG_NOT_SUPPORT
                    editor = createEditorWithPsiFile(project, apiName, demoSdkTs, "typescript")
                    langComboBox.selectedItem = "TypeScript"
                    installUrl =
                        "https://api.aliyun.com/api-tools/sdk/$productName?version=$defaultVersion&language=nodejs-tea"
                }

                else -> {
                    val demoSdkJava =
                        if (demoSdkObject.size() == 0) ApiConstants.CODE_GENERATE_ERROR else demoSdkObject.get("java")?.asString
                            ?: ApiConstants.CODE_LANG_NOT_SUPPORT
                    editor = createEditorWithPsiFile(project, apiName, demoSdkJava, "java")
                    langComboBox.selectedItem = "Java"
                    installUrl =
                        "https://api.aliyun.com/api-tools/sdk/$productName?version=$defaultVersion&language=java-tea"
                }
            }

            val installButton = sdkSampleButton("安装方式") { installUrl }
            val codeButton = sdkSampleButton("查看源码") { codeUrl }

            val openFileButton = JButton("在IDE中打开").apply {
                addActionListener {
                    val fileDocumentManager = FileDocumentManager.getInstance()
                    val document = editor!!.document
                    val virtualFile = fileDocumentManager.getFile(document)
                    val fileEditorManager = FileEditorManager.getInstance(project)
                    if (virtualFile != null && !fileEditorManager.isFileOpen(virtualFile)) {
                        val descriptor = OpenFileDescriptor(project, virtualFile)
                        fileEditorManager.openTextEditor(descriptor, true)
                    }
                }
            }

            langComboBox.addActionListener {
                ApplicationManager.getApplication().invokeLater {
                    val selectedLang = langComboBox.selectedItem as String
                    val lang = selectedLang.lowercase()
                    installUrl =
                        "https://api.aliyun.com/api-tools/sdk/$productName?version=$defaultVersion&language=$lang-tea"
                    codeUrl =
                        "https://github.com/aliyun/alibabacloud-$lang-sdk/tree/master/${productName.lowercase()}-${
                            defaultVersion.replace(
                                "-", ""
                            )
                        }"

                    val demoSdkLang =
                        if (demoSdkObject.size() == 0) "获取示例代码失败，请重试" else demoSdkObject.get(selectedLang.lowercase())?.asString
                            ?: "暂不支持该语言"

                    if (editor?.isDisposed == false) {
                        EditorFactory.getInstance().releaseEditor(editor!!)
                    }
                    editor = createEditorWithPsiFile(project, apiName, demoSdkLang, lang)
                    scrollPane.setViewportView(editor?.component)
                    sdkPanel.revalidate()
                    sdkPanel.repaint()
                }
            }

            val importDependencyButton = JButton("自动导入依赖").apply {
                addActionListener {
                    val selectedLang = langComboBox.selectedItem as String
                    val lang = selectedLang.lowercase()
                    val lastSdkInfo = AutoInstallPkgUtil.getLastSdkInfo(project, productName, defaultVersion)
                    // TODO 暂时只支持 Maven 和 Python 依赖导入
                    AutoInstallPkgUtil.autoImport(project, lang, productName, defaultVersion, lastSdkInfo)
                }
            }

            scrollPane.setViewportView(editor!!.component)
            headPanel.add(langComboBox)
            headPanel.add(installButton)
            headPanel.add(codeButton)
            headPanel.add(openFileButton)
            headPanel.add(importDependencyButton)

            sdkPanel.add(headScrollPane, BorderLayout.NORTH)
            sdkPanel.add(scrollPane, BorderLayout.CENTER)
            sdkPanel.minimumSize = Dimension(0, 0)
        }

        private fun sdkSampleButton(buttonText: String, getButtonUrl: () -> String): JButton {
            val button = JButton(buttonText).apply {
                foreground = JBColor.BLUE
                addMouseListener(object : MouseAdapter() {
                    val originalBorder = border
                    val underlineBorder: Border = BorderFactory.createCompoundBorder(
                        originalBorder, BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor.BLUE)
                    )

                    override fun mouseEntered(e: MouseEvent) {
                        border = underlineBorder
                    }

                    override fun mouseExited(e: MouseEvent) {
                        border = originalBorder
                    }
                })

                addActionListener {
                    val buttonUrl = getButtonUrl()
                    BrowserUtil.browse(URI(buttonUrl))
                }
            }
            return button
        }

        fun getDemoSdk(project: Project, bodyParams: JsonObject): JsonObject {
            val url = "https://api.aliyun.com/api/product/makeCode"
            var demoSdkObject = JsonObject()
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val bodyStr = Gson().toJson(bodyParams)
            val body = bodyStr.toRequestBody(mediaType)

            try {
                val request = RequestUtil.createRequest(url, "POST", body)
                OkHttpClientProvider.instance.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        if (responseBody != null) {
                            demoSdkObject = Gson().fromJson(responseBody, JsonObject::class.java)
                                .get(ApiConstants.SDK_MAKE_CODE_DATA)?.asJsonObject?.get(ApiConstants.SDK_MAKE_CODE_DEMO)?.asJsonObject
                                ?: JsonObject()
                        }
                    }
                }
            } catch (e: IOException) {
                NormalNotification.showMessage(
                    project,
                    NotificationGroups.SDK_NOTIFICATION_GROUP,
                    "请求超时",
                    "获取SDK示例代码超时，请重新生成示例",
                    NotificationType.WARNING
                )
            }
            return demoSdkObject
        }

        private fun createEditorWithPsiFile(project: Project, apiName: String, code: String, lang: String): EditorEx {
            val language = ApiConstants.SUFFIX_MAP[lang]!!
            val lightVirtualFile: LightVirtualFile
            var fileType = FileTypeManager.getInstance().getFileTypeByExtension(language)

            if (fileType is LanguageFileType) {
                lightVirtualFile = if (lang == "java-async") {
                    LightVirtualFile("$apiName.java", fileType, code)
                } else {
                    LightVirtualFile(ApiConstants.FILE_MAP[lang]!!, fileType, code)
                }

            } else {
                fileType = PlainTextFileType.INSTANCE as LanguageFileType
                lightVirtualFile = LightVirtualFile(ApiConstants.FILE_MAP[lang]!!, fileType, code)
            }

            val psiFileFactory = PsiFileFactory.getInstance(project)
            val psiFile = psiFileFactory.createFileFromText(
                lightVirtualFile.name,
                fileType.language,
                lightVirtualFile.content,
            )

            val documentManager = PsiDocumentManager.getInstance(project)
            val document = documentManager.getDocument(psiFile)

            val editorFactory = EditorFactory.getInstance()
            val editor = editorFactory.createEditor(document!!, project) as EditorEx

            val editorSettings: EditorSettings = editor.settings
            editorSettings.isLineNumbersShown = true
            editorSettings.isLineMarkerAreaShown = false
            editorSettings.isFoldingOutlineShown = false
            editorSettings.isVirtualSpace = false
            editorSettings.isAdditionalPageAtBottom = false
            editorSettings.isCaretRowShown = false
            editor.isViewer = true

            val editorHighlighter =
                EditorHighlighterFactory.getInstance().createEditorHighlighter(project, lightVirtualFile)
            editor.highlighter = editorHighlighter
            return editor
        }

        fun executeSdk(
            browser: JBCefBrowser, callback: (Map<String, Any>?, String) -> Unit
        ) {
            val query = JBCefJSQuery.create(browser as JBCefBrowserBase)

            query.addHandler { arg: String? ->
                try {
                    val paramsValue = FormatUtil.getArg(arg).first
                    val regionId = FormatUtil.getArg(arg).second
                    callback(paramsValue, regionId)
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
                        cefBrowser.executeJavaScript(
                            """
                        window.callSdkSample = function(arg) {
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
}