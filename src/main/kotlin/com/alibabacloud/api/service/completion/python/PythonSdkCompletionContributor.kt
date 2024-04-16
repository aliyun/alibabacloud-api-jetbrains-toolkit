package com.alibabacloud.api.service.completion.python

import com.alibabacloud.api.service.completion.SdkCompletionContributor
import com.alibabacloud.api.service.completion.util.ProjectStructureUtil
import com.alibabacloud.api.service.completion.util.PythonPkgInstallUtil
import com.alibabacloud.api.service.constants.CompletionConstants
import com.alibabacloud.api.service.constants.NotificationGroups
import com.alibabacloud.icons.ToolkitIcons
import com.google.gson.JsonArray
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.psi.PyParenthesizedExpression
import com.jetbrains.python.psi.PyStringLiteralExpression
import okhttp3.Request

class PythonSdkCompletionContributor : SdkCompletionContributor() {
    override fun addElements(
        result: CompletionResultSet,
        key: String,
        value: String,
        document: Document,
        request: Request
    ) {
        val productName = key.split("::")[1]
        val defaultVersion = key.split("::")[2]
        result.addElement(
            LookupElementBuilder.create(key)
                .withPresentableText(key)
                .withTailText("  $value")
                .withIcon(ToolkitIcons.LOGO_ICON)
                .withInsertHandler { insertionContext, _ ->
                    insertHandler(insertionContext, document, request, "python") { sdkInfo ->
                        if (!sdkInfo[0].asString.contains(CompletionConstants.NO_SDK)) {
                            checkAndNotifyDependency(insertionContext, productName, defaultVersion, sdkInfo, "python")
                        }
                    }
                }
        )
    }

    override fun isInvalidInsertionLocation(editor: Editor, offset: Int): Boolean {
        val document = editor.document
        val text = document.text

        val project = editor.project ?: return true
        if (offset < 0 || offset > text.length) return true
        if (offset == text.length) return false
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document) as? PyFile ?: return true
        val element = psiFile.findElementAt(offset) ?: return true

        // 字符串、注释或字面量的一部分
        if (element.parent is PyStringLiteralExpression ||
            element.parent is PyParenthesizedExpression ||
            PsiTreeUtil.getParentOfType(element, PsiComment::class.java) != null
        ) {
            return true
        }

        // 顶层作用域
        if (element.parent is PyFile) return false

        // 函数内部，且不在函数声明中
        val function = PsiTreeUtil.getParentOfType(element, PyFunction::class.java)
        if (function != null && function.statementList.textRange.contains(element.textRange)) return false

        val charBefore = text[offset - 1]
        val charAfter = text.getOrNull(offset) ?: return true

        val isCharBeforeIdentifierPart = charBefore.isPythonIdentifierPart()
        val isCharAfterIdentifierPart = charAfter.isPythonIdentifierPart()
        return isCharBeforeIdentifierPart && isCharAfterIdentifierPart
    }

    override fun checkAndNotifyDependency(
        context: InsertionContext,
        productName: String,
        defaultVersion: String,
        sdkInfo: JsonArray,
        lang: String
    ) {
        ApplicationManager.getApplication().executeOnPooledThread {
            val project = context.project
            val sdk = ProjectStructureUtil.getEditingSdk(context)
            val pkgName = "alibabacloud-${productName.lowercase()}${defaultVersion.replace("-", "")}"
            val isPyPkgExists = PythonPkgInstallUtil.isPyPackageExist(project, sdk, pkgName)
            if (!isPyPkgExists && sdk != null) {
                val content = "${CompletionConstants.IF_AUTO_IMPORT} $pkgName?"
                notificationService.showMessageWithActions(
                    project,
                    NotificationGroups.DEPS_NOTIFICATION_GROUP,
                    CompletionConstants.IMPORT,
                    content,
                    NotificationType.INFORMATION,
                    yesAction = {
                        ProgressManager.getInstance()
                            .run(object : Task.Backgroundable(project, "installing package $pkgName", true) {
                                override fun run(indicator: ProgressIndicator) {
                                    PythonPkgInstallUtil.pyPackageInstall(project, sdk, pkgName, sdkInfo[2].asString)

                                }
                            })
                    },
                    noAction = {}
                )
            }
        }
    }

    private fun Char.isPythonIdentifierPart(): Boolean {
        return this.isLetterOrDigit() || this == '_'
    }
}
