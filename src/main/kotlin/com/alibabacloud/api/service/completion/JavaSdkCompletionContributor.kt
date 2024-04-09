package com.alibabacloud.api.service.completion

import com.alibabacloud.api.service.OkHttpClientProvider
import com.alibabacloud.api.service.constants.NotificationGroups
import com.alibabacloud.api.service.notification.NormalNotification
import com.alibabacloud.api.service.util.DepsUtil
import com.alibabacloud.states.ToolkitSettingsState
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.castSafelyTo
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class JavaSdkCompletionContributor : CompletionContributor() {
    private var notificationService: NormalNotification = NormalNotification
    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        super.fillCompletionVariants(parameters, result)

        val editor = parameters.editor
        val caretOffset = editor.caretModel.offset
        if (!ToolkitSettingsState.getInstance().state.isCompletionEnabled || isInvalidInsertionLocation(editor, caretOffset)) {
            return
        }
        val document = editor.document
        val prefix = result.prefixMatcher.prefix
        if (prefix.length >= 2) {
            val originalPosition = parameters.originalPosition
            val project = originalPosition?.project
            if (DataService.isDataLoaded()) {
                val javaIndex = DataService.javaIndex
                if (javaIndex.isNotEmpty()) {
                    for ((key, value) in javaIndex) {
                        if (key.contains(prefix, ignoreCase = true)) {
                            val apiName = key.split("::")[0]
                            val productName = key.split("::")[1]
                            val defaultVersion = key.split("::")[2]

                            val bodyParams = JsonObject()
                            bodyParams.addProperty("apiName", apiName)
                            bodyParams.addProperty("apiVersion", defaultVersion)
                            bodyParams.addProperty("product", productName)
                            bodyParams.add("params", JsonObject())
                            bodyParams.addProperty("sdkType", "dara")
                            bodyParams.addProperty("simplify", true)
                            val mediaType = "application/json; charset=utf-8".toMediaType()
                            val bodyStr = Gson().toJson(bodyParams)
                            val body = bodyStr.toRequestBody(mediaType)
                            val request = Request.Builder()
                                .url("https://api.aliyun.com/api/product/makeCode").post(body)
                                .build()

                            result.addElement(
                                LookupElementBuilder.create(key)
                                    .withPresentableText(key)
                                    .withTypeText("Java")
                                    .withTailText("  $value")
                                    .withIcon(IconLoader.getIcon("/icons/logo.svg", javaClass))
                                    .withInsertHandler { insertionContext, _ ->
                                        insertHandler(insertionContext, document, request, "java")
                                        checkAndNotifyDependency(insertionContext, productName, defaultVersion, "java")
                                    }
                            )

                            result.addElement(
                                LookupElementBuilder.create(key)
                                    .withPresentableText(key)
                                    .withTypeText("JavaAsync")
                                    .withTailText("  $value")
                                    .withIcon(IconLoader.getIcon("/icons/logo.svg", javaClass))
                                    .withInsertHandler { insertionContext, _ ->
                                        insertHandler(insertionContext, document, request, "java-async")
                                        checkAndNotifyDependency(
                                            insertionContext,
                                            productName,
                                            defaultVersion,
                                            "java-async"
                                        )
                                    }
                            )
                        }
                    }
                }
            } else {
                if (!UnLoadNotificationState.hasShown) {
                    notificationService.showMessage(
                        project,
                        NotificationGroups.COMPLETION_NOTIFICATION_GROUP,
                        "请稍候",
                        "元数据尚未加载完成",
                        NotificationType.WARNING
                    )
                }
                UnLoadNotificationState.hasShown = true
            }
        }
    }

    private fun insertHandler(insertionContext: InsertionContext, document: Document, request: Request, lang: String) {
        var demoSdk: String
        val project = insertionContext.project
        WriteCommandAction.runWriteCommandAction(project) {
            val startOffset = insertionContext.startOffset
            val tailOffset = insertionContext.tailOffset
            if (startOffset < tailOffset && tailOffset <= document.textLength) {
                document.deleteString(startOffset, tailOffset)
            }
        }
        ProgressManager.getInstance()
            .run(object : Task.Backgroundable(project, "拉取示例代码...", true) {
                override fun run(indicator: ProgressIndicator) {
                    indicator.isIndeterminate = true
                    demoSdk = getDemoSdk(project, OkHttpClientProvider.instance, request, lang)
                    ApplicationManager.getApplication().invokeLater {
                        insertCodeSnippet(document, insertionContext, demoSdk)
                    }
                }
            })
    }
    fun getDemoSdk(project: Project, instance: OkHttpClient, request: Request, lang: String): String {
        var demoSdk = String()
        try {
            instance.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val demoSdkObject = Gson().fromJson(responseBody, JsonObject::class.java).get("data")?.asJsonObject?.get("demoSdk")?.asJsonObject
                        demoSdk = demoSdkObject?.get(lang)?.asJsonObject?.get("codeSample")?.asString ?: "该 API 暂无 $lang SDK 示例"
                    }
                } else {
                    demoSdk = "SDK 示例生成出错，请联系支持群开发同学解决"
                }
            }
        } catch (e: IOException) {
            notificationService.showMessage(
                project,
                NotificationGroups.COMPLETION_NOTIFICATION_GROUP,
                "生成示例代码失败",
                "请检查网络",
                NotificationType.WARNING
            )
        }
        return demoSdk
    }

    private fun insertCodeSnippet(document: Document, context: InsertionContext, snippet: String) {
        WriteCommandAction.runWriteCommandAction(context.project) {
            val startOffset = context.startOffset
            val tailOffset = context.tailOffset
            val lineNumber = document.getLineNumber(startOffset)
            val lineStartOffset = document.getLineStartOffset(lineNumber)
            val lineIndentation =
                document.getText(TextRange(lineStartOffset, startOffset)).takeWhile { it.isWhitespace() }
            val lines = snippet.split("\n")
            val indentedSnippet = lines.first() + "\n" + lines.drop(1).joinToString("\n") { lineIndentation + it }

            document.deleteString(startOffset, tailOffset)
            document.insertString(startOffset, indentedSnippet)
            context.editor.caretModel.moveToOffset(startOffset + indentedSnippet.length)
        }

    }

    private fun checkAndNotifyDependency(
        context: InsertionContext,
        productName: String,
        defaultVersion: String,
        lang: String
    ) {
        ApplicationManager.getApplication().executeOnPooledThread {
            val project = context.project
            val commandUrl =
                "https://api.aliyun.com/api/sdk/product/docNew?product=$productName&version=$defaultVersion&language=$lang-tea&lang=zh_CN"
            val resList = DepsUtil.isMavenDependencyExist(project, commandUrl)
            val isDependencyExists = resList[0].castSafelyTo<Boolean>()!!
            val isPomExists = resList[1].castSafelyTo<Boolean>()!!
            if (isPomExists && !isDependencyExists) {
                val content = "是否自动导入依赖：" + (if (lang == "java-async") "alibabacloud-" else "") + "${
                    productName.lowercase().replace("-", "_")
                }${defaultVersion.replace("-", "")}?"
                notificationService.showMessageWithActions(
                    project,
                    NotificationGroups.DEPS_NOTIFICATION_GROUP,
                    "导入依赖",
                    content,
                    NotificationType.INFORMATION,
                    yesAction = {
                        ProgressManager.getInstance().run(object :
                            Task.Backgroundable(
                                project,
                                "Importing maven dependencies",
                                true
                            ) {
                            override fun run(indicator: ProgressIndicator) {
                                DepsUtil.importMavenDeps(
                                    project,
                                    commandUrl
                                )
                            }
                        })
                    },
                    noAction = {}
                )
            }
        }
    }


    private fun isInvalidInsertionLocation(editor: Editor, offset: Int): Boolean {
        val document = editor.document
        val text = document.text

        if (offset <= 0 || offset >= text.length) {
            return true
        }

        val charBefore = text[offset - 1]
        val charAfter = text.getOrNull(offset) ?: return true
        val isCharBeforeNonWord = Character.isJavaIdentifierPart(charBefore)
        val isCharAfterNonWord = Character.isJavaIdentifierPart(charAfter)

        val project = editor.project ?: return true
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document) ?: return true
        val element = psiFile.findElementAt(offset) ?: return true
        val method = PsiTreeUtil.getParentOfType(element, PsiMethod::class.java)
        if (method == null || !method.textRange.contains(element.textRange)) {
            return true
        }
        return isCharBeforeNonWord && isCharAfterNonWord
    }
}