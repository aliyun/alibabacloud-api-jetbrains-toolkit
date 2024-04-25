package com.alibabacloud.api.service.completion.go

import com.alibabacloud.api.service.completion.SdkCompletionContributor
import com.alibabacloud.api.service.completion.util.ProjectStructureUtil
import com.alibabacloud.api.service.completion.util.PythonPkgInstallUtil
import com.alibabacloud.api.service.constants.CompletionConstants
import com.alibabacloud.api.service.constants.NotificationGroups
import com.alibabacloud.icons.ToolkitIcons
import com.goide.psi.GoFile
import com.goide.psi.GoStringLiteral
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
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.PsiTreeUtil
import okhttp3.Request

class GoSdkCompletionContributor : SdkCompletionContributor() {
    override fun addElements(
        result: CompletionResultSet,
        key: String,
        value: String,
        document: Document,
        request: Request
    ) {
        result.addElement(
            LookupElementBuilder.create(key)
                .withPresentableText(key)
                .withTailText("  $value")
                .withIcon(ToolkitIcons.LOGO_ICON)
                .withInsertHandler { insertionContext, _ ->
                    insertHandler(insertionContext, document, request, "go") {}
                }
        )
    }

    override fun isInvalidInsertionLocation(editor: Editor, offset: Int): Boolean {
        val document = editor.document
        val text = document.text
        if (offset <= 0 || offset >= text.length) return true
        val project = editor.project ?: return true
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document) as? GoFile ?: return true
        val element = psiFile.findElementAt(offset) ?: return true
        val elementType = element.node.elementType.toString()

        if (PsiTreeUtil.getParentOfType(element, GoStringLiteral::class.java) != null ||
            elementType == "GO_LINE_COMMENT" ||
            elementType == "string" ||
            elementType == "raw_string" ||
            psiFile.findElementAt(offset - 1)?.node?.elementType.toString() == "GO_LINE_COMMENT"
        ) {
            return true
        }

        val charBefore = text[offset - 1]
        val charAfter = text.getOrNull(offset) ?: return true
        val isCharBeforeIdentifierPart = Character.isJavaIdentifierPart(charBefore)
        val isCharAfterIdentifierPart = Character.isJavaIdentifierPart(charAfter)

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
}
