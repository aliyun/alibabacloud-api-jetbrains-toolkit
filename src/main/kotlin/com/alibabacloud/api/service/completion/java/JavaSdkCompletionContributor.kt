package com.alibabacloud.api.service.completion.java

import com.alibabacloud.api.service.completion.SdkCompletionContributor
import com.alibabacloud.api.service.completion.util.JavaPkgInstallUtil
import com.alibabacloud.api.service.completion.util.LookupElementUtil
import com.alibabacloud.api.service.constants.NotificationGroups
import com.alibabacloud.i18n.I18nUtils
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
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import okhttp3.Request
import java.util.*

class JavaSdkCompletionContributor : SdkCompletionContributor() {
    override fun addElements(
        result: CompletionResultSet,
        key: String,
        value: String,
        document: Document,
        request: Request
    ) {
        val apiInfo = LookupElementUtil.getFormat(key)
        val description = if (I18nUtils.getLocale() == Locale.CHINA) "::${value}" else ""
        result.addElement(
            LookupElementBuilder.create(key)
                .withPresentableText(apiInfo.apiName)
                .withTypeText("Java")
                .withTailText("  ${apiInfo.productName}::${apiInfo.defaultVersion}$description")
                .withIcon(ToolkitIcons.LOGO_ICON)
                .withInsertHandler { insertionContext, _ ->
                    insertHandler(insertionContext, document, request, "java") { sdkInfo ->
                        if (!sdkInfo[0].asString.contains(I18nUtils.getMsg("sdk.not.exist.prefix"))) {
                            checkAndNotifyDependency(
                                insertionContext,
                                apiInfo.productName,
                                apiInfo.defaultVersion,
                                sdkInfo,
                                "java"
                            )
                        }
                    }
                }
        )

        result.addElement(
            LookupElementBuilder.create(key)
                .withPresentableText(apiInfo.apiName)
                .withTypeText("JavaAsync")
                .withTailText("  ${apiInfo.productName}::${apiInfo.defaultVersion}$description")
                .withIcon(ToolkitIcons.LOGO_ICON)
                .withInsertHandler { insertionContext, _ ->
                    insertHandler(insertionContext, document, request, "java-async") { sdkInfo ->
                        if (!sdkInfo[0].asString.contains(I18nUtils.getMsg("sdk.not.exist.prefix"))) {
                            checkAndNotifyDependency(
                                insertionContext,
                                apiInfo.productName,
                                apiInfo.defaultVersion,
                                sdkInfo,
                                "java-async"
                            )
                        }
                    }
                }
        )
    }

    override fun checkAndNotifyDependency(
        context: InsertionContext,
        productName: String,
        defaultVersion: String,
        sdkInfo: JsonArray,
        lang: String,
    ) {
        ApplicationManager.getApplication().executeOnPooledThread {
            val project = context.project
            val artifactId = if (lang == "java") {
                "${productName.lowercase().replace("-", "_")}${defaultVersion.replace("-", "")}"
            } else {
                "alibabacloud-${productName.lowercase().replace("-", "_")}${defaultVersion.replace("-", "")}"
            }
            val mavenCommand = "<dependency>\n" +
                    "  <groupId>com.aliyun</groupId>\n" +
                    "  <artifactId>$artifactId</artifactId>\n" +
                    "  <version>${sdkInfo[2].asString}</version>\n" +
                    "</dependency>"

            val resList = JavaPkgInstallUtil.isMavenDependencyExist(project, mavenCommand)

            val isDependencyExists = resList[0]
            val isPomExists = resList[1]
            val needUpdate = resList[2]
            if (isPomExists && needUpdate) {
                val content = I18nUtils.getMsg("auto.install.package.update.ask.prefix") + (if (lang == "java-async") " alibabacloud-" else " ") +
                        "${productName.lowercase().replace("-", "_")}${defaultVersion.replace("-", "")} " +
                        I18nUtils.getMsg("auto.install.package.update.ask.suffix") + " ${sdkInfo[2].asString}?"
                notificationService.showNotificationWithActions(
                    project,
                    NotificationGroups.DEPS_NOTIFICATION_GROUP,
                    I18nUtils.getMsg("auto.install.package.update"),
                    content,
                    NotificationType.INFORMATION,
                    listOf(
                        I18nUtils.getMsg("dialog.yes") to {
                            ProgressManager.getInstance()
                                .run(object : Task.Backgroundable(project, "Importing maven dependencies", true) {
                                    override fun run(indicator: ProgressIndicator) {
                                        JavaPkgInstallUtil.updateMavenDeps(
                                            project,
                                            mavenCommand,
                                        )
                                    }
                                })
                        },
                        I18nUtils.getMsg("dialog.no") to {}
                    )
                )
            } else if (isPomExists && !isDependencyExists) {
                val content = I18nUtils.getMsg("auto.install.package.ask") + (if (lang == "java-async") " alibabacloud-" else " ") +
                        "${productName.lowercase().replace("-", "_")}${defaultVersion.replace("-", "")}?"
                notificationService.showNotificationWithActions(
                    project,
                    NotificationGroups.DEPS_NOTIFICATION_GROUP,
                    I18nUtils.getMsg("auto.install.package"),
                    content,
                    NotificationType.INFORMATION,
                    listOf(
                        I18nUtils.getMsg("dialog.yes") to {
                            ProgressManager.getInstance()
                                .run(object : Task.Backgroundable(project, "Importing maven dependencies", true) {
                                    override fun run(indicator: ProgressIndicator) {
                                        JavaPkgInstallUtil.importMavenDeps(
                                            project,
                                            mavenCommand,
                                        )
                                    }
                                })
                        },
                        I18nUtils.getMsg("dialog.no") to {}
                    )
                )
            }
        }
    }


    override fun isInvalidInsertionLocation(editor: Editor, offset: Int): Boolean {
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