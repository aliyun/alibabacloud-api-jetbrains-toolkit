package com.alibabacloud.api.service.completion

import com.alibabacloud.api.service.OkHttpClientProvider
import com.alibabacloud.api.service.constants.ApiConstants
import com.alibabacloud.api.service.constants.CompletionConstants
import com.alibabacloud.api.service.constants.NotificationGroups
import com.alibabacloud.api.service.notification.NormalNotification
import com.alibabacloud.states.ToolkitSettingsState
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.*

abstract class SdkCompletionContributor : CompletionContributor() {
    internal var notificationService: NormalNotification = NormalNotification

    internal abstract fun addElements(
        result: CompletionResultSet,
        key: String,
        value: String,
        document: Document,
        request: Request
    )

    protected abstract fun isInvalidInsertionLocation(editor: Editor, offset: Int): Boolean

    protected abstract fun checkAndNotifyDependency(
        context: InsertionContext,
        productName: String,
        defaultVersion: String,
        sdkInfo: JsonArray,
        lang: String
    )

    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        super.fillCompletionVariants(parameters, result)
        val editor = parameters.editor
        val caretOffset = editor.caretModel.offset
        if (!ToolkitSettingsState.getInstance().state.isCompletionEnabled || isInvalidInsertionLocation(
                editor,
                caretOffset
            )
        ) {
            return
        }
        val document = editor.document
        val prefix = result.prefixMatcher.prefix

        if (prefix.isNotEmpty()) {
            val project = parameters.originalPosition?.project ?: return
            if (DataService.isDataLoaded()) {
                val javaIndex = DataService.javaIndex
                if (javaIndex.isNotEmpty()) {
                    for ((key, value) in javaIndex) {
                        if (key.contains(prefix, ignoreCase = true)) {
                            val request = makeRequest(key)
                            addElements(result, key, value, document, request)
                        }
                    }
                }
            } else {
                showUnloadedMessage(project)
            }
        }
    }

    private fun showUnloadedMessage(project: Project) {
        if (!UnLoadNotificationState.hasShown) {
            notificationService.showMessage(
                project,
                NotificationGroups.COMPLETION_NOTIFICATION_GROUP,
                "请稍候",
                "如需使用 SDK 示例代码自动补全功能，请等待元数据加载完成(约半分钟)",
                NotificationType.INFORMATION
            )
        }
        UnLoadNotificationState.hasShown = true
    }

    private fun makeRequest(key: String): Request {
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
        return Request.Builder()
            .url("https://api.aliyun.com/api/product/makeCode").post(body)
            .build()
    }

    protected fun insertCodeSnippet(
        document: Document,
        context: InsertionContext,
        snippet: String,
        importList: JsonArray,
        lang: String
    ) {
        WriteCommandAction.runWriteCommandAction(context.project) {
            insertImportList(importList, document, lang)

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

    private fun insertImportList(importList: JsonArray, document: Document, lang: String) {
        if (lang == "java" || lang == "java-async" || lang == "python") {
            for (refText in importList) {
                val ref = refText.asString
                val documentText = document.text
                if (!documentText.contains(ref)) {
                    val packageInsertOffset = document.getLineStartOffset(0)
                    document.insertString(packageInsertOffset, "$ref\n")
                }
            }
        } else if (lang == "go") {
            val documentText = document.text
            val existingImportBlockMatch = Regex("import \\([^)]+\\)").find(documentText)
            if (existingImportBlockMatch != null) {
                val importBlockContent = existingImportBlockMatch.groupValues[0]
                val importBlockStartOffset = existingImportBlockMatch.range.first + "import (".length

                val firstNonWhitespaceAfterImport =
                    documentText.substring(importBlockStartOffset).indexOf("\n", importBlockStartOffset)
                val actualInsertOffset = importBlockStartOffset + firstNonWhitespaceAfterImport + 1

                val reversedList = importList.drop(1).dropLast(1).reversed()
                for (refText in reversedList) {
                    val ref = refText.asString
                    if (!importBlockContent.contains(ref)) {
                        document.insertString(actualInsertOffset, "$ref\n")
                    }
                }
            } else {
                val packagePattern = Regex("package\\s+\\w+\\s*\\n")
                val matchResult = packagePattern.find(documentText)
                val packageInsertOffset = matchResult?.range?.last?.plus(1) ?: document.getLineStartOffset(0)
                document.insertString(packageInsertOffset, "\n")

                val reversedList = importList.reversed()
                for (refText in reversedList) {
                    val ref = refText.asString
                    document.insertString(packageInsertOffset, "$ref\n")

                }
            }
        }
    }

    internal fun getDemoSdk(
        project: Project,
        document: Document,
        instance: OkHttpClient,
        request: Request,
        lang: String
    ): JsonArray {
        var demoSdk = ""
        var importList = JsonArray()
        var sdkVersion = ""
        try {
            instance.newCall(request).execute().use { response ->
                val commentPrefix = getCommentPrefix(project, document)
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val demoSdkObject = Gson().fromJson(responseBody, JsonObject::class.java)
                            .get("data")?.asJsonObject?.get("demoSdk")?.asJsonObject
                        val sdkInfo = demoSdkObject?.get(lang)?.asJsonObject
                        demoSdk = sdkInfo?.get("codeSample")?.asString.takeUnless { it.isNullOrBlank() }
                            ?: "$commentPrefix ${CompletionConstants.NO_SDK} $lang SDK 示例"
                        importList = sdkInfo?.get("importList")?.asJsonArray ?: JsonArray()
                        sdkVersion = sdkInfo?.get("sdkVersion")?.asString ?: ""
                    }
                } else {
                    demoSdk = "$commentPrefix ${ApiConstants.CODE_GENERATE_ERROR}"
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
        val sdkInfo = JsonArray()
        sdkInfo.add(demoSdk)
        sdkInfo.add(importList)
        sdkInfo.add(sdkVersion)
        return sdkInfo
    }

    protected fun insertHandler(
        insertionContext: InsertionContext,
        document: Document,
        request: Request,
        lang: String,
        callback: (JsonArray) -> Unit
    ) {
        var demoSdk: String
        var importList: JsonArray
        var sdkInfo: JsonArray
        val project = insertionContext.project
        WriteCommandAction.runWriteCommandAction(project) {
            val startOffset = insertionContext.startOffset
            val tailOffset = insertionContext.tailOffset
            if (startOffset < tailOffset && tailOffset <= document.textLength) {
                document.deleteString(startOffset, tailOffset)
            }
        }
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "拉取示例代码...", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                sdkInfo = getDemoSdk(project, document, OkHttpClientProvider.instance, request, lang)
                demoSdk = sdkInfo[0].asString
                importList = sdkInfo[1].asJsonArray
                ApplicationManager.getApplication().invokeLater {
                    insertCodeSnippet(document, insertionContext, demoSdk, importList, lang)
                    callback(sdkInfo)
                }
            }
        })
    }

    private fun getCommentPrefix(project: Project, document: Document): String {
        val commentPrefix = ApplicationManager.getApplication().runReadAction<String> {
            val psiFile = PsiDocumentManager.getInstance(project).getCachedPsiFile(document)
            val fileType = psiFile?.fileType?.name?.lowercase(Locale.getDefault()) ?: ""
            when (fileType) {
                "python" -> CompletionConstants.HASH
                else -> CompletionConstants.SLASH
            }
        }
        return commentPrefix
    }
}