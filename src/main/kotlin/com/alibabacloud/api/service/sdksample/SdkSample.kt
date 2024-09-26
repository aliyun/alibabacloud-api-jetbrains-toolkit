package com.alibabacloud.api.service.sdksample

import com.alibabacloud.api.service.OkHttpClientProvider
import com.alibabacloud.api.service.SdkDetail
import com.alibabacloud.api.service.constants.ApiConstants
import com.alibabacloud.api.service.constants.NotificationGroups
import com.alibabacloud.api.service.notification.NormalNotification
import com.alibabacloud.api.service.sdksample.util.AutoInstallPkgUtil
import com.alibabacloud.api.service.util.FormatUtil
import com.alibabacloud.api.service.util.RequestUtil
import com.alibabacloud.i18n.I18nUtils
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
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFileFactory
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
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
import java.awt.*
import java.awt.datatransfer.StringSelection
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.io.IOException
import java.net.URI
import java.util.*
import javax.swing.*
import javax.swing.border.Border


class SdkSample {
    companion object {
        fun sdkSamplePanel(
            apiName: String,
            defaultVersion: String,
            productName: String,
            project: Project,
            sdkPanel: JPanel,
            demoSdkObject: JsonObject,
            sdkInfoData: MutableMap<String, SdkDetail>
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
            var codeUrl = "https://github.com/aliyun/alibabacloud-java-sdk/tree/master/${productName.lowercase()}-${
                defaultVersion.replace(
                    "-",
                    ""
                )
            }"

            when (ideName) {
                "PyCharm" -> {
                    val demoSdkPy =
                        if (demoSdkObject.size() == 0) I18nUtils.getMsg("sdk.code.sample.generate.error") else demoSdkObject.get("python")?.asString
                            ?: I18nUtils.getMsg("sdk.code.sample.lang.not.support")
                    editor = createEditorWithPsiFile(project, apiName, demoSdkPy, "python")
                    langComboBox.selectedItem = "Python"
                    installUrl =
                        "https://api.aliyun.com/api-tools/sdk/$productName?version=$defaultVersion&language=python-tea"
                }

                "GoLand" -> {
                    val demoSdkGo =
                        if (demoSdkObject.size() == 0) I18nUtils.getMsg("sdk.code.sample.generate.error") else demoSdkObject.get("go")?.asString
                            ?: I18nUtils.getMsg("sdk.code.sample.lang.not.support")
                    editor = createEditorWithPsiFile(project, apiName, demoSdkGo, "go")
                    langComboBox.selectedItem = "Go"
                    installUrl =
                        "https://api.aliyun.com/api-tools/sdk/$productName?version=$defaultVersion&language=go-tea"
                }

                "WebStorm" -> {
                    val demoSdkTs =
                        if (demoSdkObject.size() == 0) I18nUtils.getMsg("sdk.code.sample.generate.error") else demoSdkObject.get("typescript")?.asString
                            ?: I18nUtils.getMsg("sdk.code.sample.lang.not.support")
                    editor = createEditorWithPsiFile(project, apiName, demoSdkTs, "typescript")
                    langComboBox.selectedItem = "TypeScript"
                    installUrl =
                        "https://api.aliyun.com/api-tools/sdk/$productName?version=$defaultVersion&language=nodejs-tea"
                }

                else -> {
                    val demoSdkJava =
                        if (demoSdkObject.size() == 0) I18nUtils.getMsg("sdk.code.sample.generate.error") else demoSdkObject.get("java")?.asString
                            ?: I18nUtils.getMsg("sdk.code.sample.lang.not.support")
                    editor = createEditorWithPsiFile(project, apiName, demoSdkJava, "java")
                    langComboBox.selectedItem = "Java"
                    installUrl =
                        "https://api.aliyun.com/api-tools/sdk/$productName?version=$defaultVersion&language=java-tea"
                }
            }

            val sdkInfoButton = JButton(I18nUtils.getMsg("code.sample.sdk.info.button"))
            val language = langComboBox.selectedItem?.toString()?.lowercase() ?: "java"
            var sdkDetail = sdkInfoData[language] ?: SdkDetail("null", "null", "null", "null")

            sdkInfoButton.addActionListener {
                sdkInfoPanel(sdkInfoButton, headPanel, sdkDetail)
            }

            val installButton = sdkSampleButton(I18nUtils.getMsg("code.sample.install.method.button")) { installUrl }
            val codeButton = sdkSampleButton(I18nUtils.getMsg("code.sample.view.source.button")) { codeUrl }

            val openFileButton = JButton(I18nUtils.getMsg("open.in.ide")).apply {
                addActionListener {
                    val document = editor!!.document
                    val content = document.getText(TextRange(0, document.textLength))
                    val fileName = if (content.contains("package demo;")) {
                        "$apiName.java"
                    } else if (content.contains("package com.aliyun.sample;")) {
                        "Sample.java"
                    } else if (content.contains("'use strict';")) {
                        "client.js"
                    } else if (content.contains("export default class Client {")) {
                        "client.ts"
                    } else if (content.contains("package main")) {
                        "client.go"
                    } else if (content.contains("namespace AlibabaCloud\\SDK\\Sample;")) {
                        "Sample.php"
                    } else if (content.contains("def __init__")) {
                        "Sample.py"
                    } else if (content.contains("using System;")) {
                        "Sample.cs"
                    } else {
                        "Sample.txt"
                    }
                    createAndOpenRealFile(project, content, fileName)
                }
            }

            langComboBox.addActionListener {
                ApplicationManager.getApplication().invokeLater {
                    val selectedLang = langComboBox.selectedItem as String
                    val lang = selectedLang.lowercase()
                    sdkDetail = sdkInfoData[lang] ?: SdkDetail("null", "null", "null", "null")
                    sdkInfoButton.addActionListener {
                        sdkInfoPanel(sdkInfoButton, headPanel, sdkDetail)
                    }
                    installUrl =
                        "https://api.aliyun.com/api-tools/sdk/$productName?version=$defaultVersion&language=$lang-tea"
                    codeUrl =
                        "https://github.com/aliyun/alibabacloud-$lang-sdk/tree/master/${productName.lowercase()}-${
                            defaultVersion.replace(
                                "-", ""
                            )
                        }"

                    val demoSdkLang =
                        if (demoSdkObject.size() == 0) I18nUtils.getMsg("code.sample.obtain.fail") else demoSdkObject.get(
                            selectedLang.lowercase()
                        )?.asString
                            ?: I18nUtils.getMsg("sdk.code.sample.lang.not.support")

                    if (editor?.isDisposed == false) {
                        EditorFactory.getInstance().releaseEditor(editor!!)
                    }
                    editor = createEditorWithPsiFile(project, apiName, demoSdkLang, lang)
                    scrollPane.setViewportView(editor?.component)
                    sdkPanel.revalidate()
                    sdkPanel.repaint()
                }
            }

            val importDependencyButton = JButton(I18nUtils.getMsg("auto.install.package")).apply {
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
            headPanel.add(sdkInfoButton)
            headPanel.add(installButton)
            headPanel.add(codeButton)
            headPanel.add(openFileButton)
            headPanel.add(importDependencyButton)

            sdkPanel.add(headScrollPane, BorderLayout.NORTH)
            sdkPanel.add(scrollPane, BorderLayout.CENTER)
            sdkPanel.minimumSize = Dimension(0, 0)
        }

        private fun sdkInfoPanel(sdkInfoButton: JButton, headPanel: JPanel, sdkDetail: SdkDetail) {
            val contentPanel = JBPanel<JBPanel<*>>(BorderLayout())
            contentPanel.preferredSize = Dimension(300, 200)

            val nameTitle = I18nUtils.getMsg("code.sample.sdk.package.name")
            val nameValue = "<span style='color: yellow;'>${sdkDetail.sdkName}</span>"

            val versionTitle = I18nUtils.getMsg("code.sample.sdk.package.version")
            val versionValue = "<span style='color: yellow;'>${sdkDetail.sdkPkgVersion}</span>"

            val platformTitle = I18nUtils.getMsg("code.sample.sdk.package.management.platform")
            val platformValue = "<span style='color: yellow;'>${sdkDetail.sdkPkgManagementPlatform}</span>"

            val commandTitle = I18nUtils.getMsg("code.sample.sdk.package.install.command")
            val commandValue = sdkDetail.sdkInstallationCommand

            val supplement = "<span style='color: gray; font-size: smaller;'>${I18nUtils.getMsg("code.sample.sdk.package.supplement")}</span>"

            val contentHtml =
                """
                    <html>
                        <body style='font-family: Arial, sans-serif; font-size: 9px;margin: 10px;'>
                            <table style='width: calc(100% - 20px); white-space: nowrap;'>
                                <tr>
                                    <td style='text-align: left;'>$nameTitle</td>
                                    <td style='text-align: left; padding-left: 10px;'>$nameValue</td>
                                </tr>
                                <tr>
                                    <td style='text-align: left;'>$versionTitle</td>
                                    <td style='text-align: left; padding-left: 10px;'>$versionValue</td>
                                </tr>
                                <tr>
                                    <td style='text-align: left;'>$platformTitle</td>
                                    <td style='text-align: left; padding-left: 10px;'>$platformValue</td>
                                </tr>
                                <tr>
                                    <td style='text-align: left;'>$commandTitle</td>
                                    <td style='text-align: left; padding-left: 10px;'>$commandValue</td>
                                </tr>
                            </table>
                            <br/>
                            $supplement
                        </body>
                    </html>
                """.trimIndent()



            val contentTextPane = JTextPane().apply {
                contentType = "text/html"
                text = contentHtml
                isEditable = false
                preferredSize = Dimension(300, 200)
            }

            val scrollPane1 = JBScrollPane(contentTextPane).apply {
                horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
                verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
            }
            scrollPane1.preferredSize = Dimension(300, 200)
            contentPanel.add(scrollPane1, BorderLayout.CENTER)

            val popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(contentPanel, contentPanel)
                .setRequestFocus(true)
                .setCancelOnClickOutside(true)
                .setMovable(true)
                .createPopup()

            val location = sdkInfoButton.locationOnScreen
            popup.showInScreenCoordinates(headPanel, Point(location.x, location.y + sdkInfoButton.height))
        }

        private fun createAndOpenRealFile(project: Project, content: String, fileName: String) {
            var filePath = File(project.basePath, fileName)
            var index = 1

            while (filePath.exists()) {
                val newFileName = "${fileName.substringBeforeLast('.')}$index.${fileName.substringAfterLast('.')}"
                filePath = File(project.basePath, newFileName)
                index++
            }

            filePath.writeText(content)

            val virtualFile: VirtualFile? = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(filePath)

            if (virtualFile != null) {
                val fileEditorManager = FileEditorManager.getInstance(project)
                val descriptor = OpenFileDescriptor(project, virtualFile)
                fileEditorManager.openTextEditor(descriptor, true)
            } else {
                throw IOException(I18nUtils.getMsg("open.in.ide.fail"))
            }
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
            val locale = if (I18nUtils.getLocale() == Locale.CHINA) "" else "?language=EN_US"
            val url = "https://api.aliyun.com/api/product/makeCode$locale"
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
                    I18nUtils.getMsg("request.timeout"),
                    I18nUtils.getMsg("code.sample.obtain.fail"),
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