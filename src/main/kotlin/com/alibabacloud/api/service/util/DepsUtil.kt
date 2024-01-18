package com.alibabacloud.api.service.util

import com.alibabacloud.api.service.OkHttpClientProvider
import com.alibabacloud.api.service.constants.ApiConstants
import com.alibabacloud.api.service.constants.NotificationGroups
import com.alibabacloud.api.service.notification.NormalNotification
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import okhttp3.Request

class DepsUtil {
    companion object {
        fun importMavenDeps(project: Project, commandUrl: String) {
            val basePath = project.basePath
            val projectBaseDir = basePath?.let { path ->
                LocalFileSystem.getInstance().findFileByPath(path)
            }
            val pomVirtualFile = projectBaseDir?.findChild("pom.xml")

            insertDependencyInPom(project, pomVirtualFile, commandUrl, onSuccess = {
                NormalNotification.showMessage(
                    project,
                    NotificationGroups.DEPS_NOTIFICATION_GROUP,
                    "依赖导入成功",
                    "",
                    NotificationType.INFORMATION
                )
            }, onFailure = {
                NormalNotification.showMessage(
                    project,
                    NotificationGroups.DEPS_NOTIFICATION_GROUP,
                    "依赖导入失败",
                    "请选择正确的语言",
                    NotificationType.ERROR
                )
            })
        }

        private fun insertDependencyInPom(
            project: Project,
            pomVirtualFile: VirtualFile?,
            commandUrl: String,
            onSuccess: () -> Unit,
            onFailure: () -> Unit
        ) {
            val (isDependencyExists, isPomExists) = isMavenDependencyExist(project, commandUrl)
            if (isPomExists) {
                WriteCommandAction.runWriteCommandAction(project) {
                    val documentManager = FileDocumentManager.getInstance()
                    val document = documentManager.getDocument(pomVirtualFile!!) ?: return@runWriteCommandAction
                    val pomBackupContent = document.text
                    ApplicationManager.getApplication().runReadAction {
                        try {
                            val mavenCommand = fetchMavenDependencyCommand(commandUrl)
                            if (mavenCommand != null) {
                                var pomContent = pomBackupContent
                                if (!isDependencyExists) {
                                    val indentedMavenCommand =
                                        mavenCommand.trim().replace("\n  ", "\n    ").replace("\n", "\n    ")
                                            .prependIndent("    ")

                                    pomContent = if (pomContent.contains("<dependencies>")) {
                                        pomContent.replaceFirst(
                                            "</dependencies>", " $indentedMavenCommand\n   </dependencies>"
                                        )
                                    } else {
                                        pomContent.replaceFirst(
                                            "</project>",
                                            "    <dependencies>\n    $indentedMavenCommand\n    </dependencies>\n</project>"
                                        )
                                    }
                                    ApplicationManager.getApplication().invokeLater {
                                        WriteCommandAction.runWriteCommandAction(project) {
                                            document.setText(pomContent)
                                            documentManager.saveDocument(document)
                                        }
                                    }
                                    refreshMavenProject(project)
                                    onSuccess()
                                } else {
                                    NormalNotification.showMessage(
                                        project,
                                        NotificationGroups.DEPS_NOTIFICATION_GROUP,
                                        "已存在",
                                        "该依赖已存在",
                                        NotificationType.INFORMATION
                                    )
                                }
                            } else {
                                onFailure()
                            }
                        } catch (e: Exception) {
                            ApplicationManager.getApplication().invokeLater {
                                WriteCommandAction.runWriteCommandAction(project) {
                                    document.setText(pomBackupContent)
                                    documentManager.saveDocument(document)
                                }
                                onFailure()
                            }
                        }
                    }
                }
            } else {
                NormalNotification.showMessage(
                    project,
                    NotificationGroups.DEPS_NOTIFICATION_GROUP,
                    "依赖导入失败",
                    "未识别到 pom.xml（暂时只支持 Java）",
                    NotificationType.WARNING
                )
            }
        }

        private fun refreshMavenProject(project: Project) {
            val mavenReimportAction = ActionManager.getInstance().getAction("Maven.Reimport")
            if (mavenReimportAction != null) {
                val dataContext = { dataId: String -> if (dataId == CommonDataKeys.PROJECT.name) project else null }
                val anActionEvent = AnActionEvent.createFromDataContext(
                    "Maven.Reimport", null, dataContext
                )
                mavenReimportAction.actionPerformed(anActionEvent)
            }
        }

        private fun fetchMavenDependencyCommand(commandUrl: String): String? {
            var mavenCommand: String? = null

            val request = Request.Builder().url(commandUrl).build()
            OkHttpClientProvider.instance.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val resp = Gson().fromJson(response.body?.string(), JsonObject::class.java)
                    val install =
                        resp?.get(ApiConstants.DEP_INSTALL_DATA)?.asJsonObject?.get(ApiConstants.DEP_INSTALL)?.asJsonArray
                    if (install != null && install.size() > 0) {
                        mavenCommand = install.asSequence().map { it.asJsonObject }
                            .firstOrNull { it.get(ApiConstants.DEP_INSTALL_METHOD).asString == "Apache Maven" }
                            ?.get(ApiConstants.DEP_INSTALL_COMMAND)?.asString?.removePrefix("```xml\n")
                            ?.removeSuffix("\n```")
                    }
                    return mavenCommand
                }
            }
            return null
        }

        private fun isMavenDependencyExist(
            project: Project,
            commandUrl: String
        ): Pair<Boolean, Boolean> {
            var isDependencyExists = false
            var isPomExists = false
            val basePath = project.basePath
            val projectBaseDir = basePath?.let { path ->
                LocalFileSystem.getInstance().findFileByPath(path)
            }
            val pomVirtualFile = projectBaseDir?.findChild("pom.xml")
            if (pomVirtualFile != null && pomVirtualFile.exists()) {
                isPomExists = true
                ApplicationManager.getApplication().runReadAction {
                    val documentManager = FileDocumentManager.getInstance()
                    val document = documentManager.getDocument(pomVirtualFile)
                    if (document != null) {
                        val pomBackupContent = document.text
                        val mavenCommand = fetchMavenDependencyCommand(commandUrl)
                        if (mavenCommand != null) {
                            val groupId =
                                Regex("<groupId>(.*?)</groupId>").find(mavenCommand)?.groups?.get(1)?.value?.trim()
                                    ?: ""
                            val artifactId =
                                Regex("<artifactId>(.*?)</artifactId>").find(mavenCommand)?.groups?.get(1)?.value?.trim()
                                    ?: ""
                            val version =
                                Regex("<version>(.*?)</version>").find(mavenCommand)?.groups?.get(1)?.value?.trim()
                                    ?: ""

                            val dependencyPattern =
                                "<dependency>[\\s\\S]*?<groupId>\\s*$groupId\\s*</groupId>[\\s\\S]*?<artifactId>\\s*$artifactId\\s*</artifactId>[\\s\\S]*?<version>\\s*$version\\s*</version>[\\s\\S]*?</dependency>"
                            isDependencyExists = pomBackupContent.contains(
                                dependencyPattern.toRegex(
                                    setOf(
                                        RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE
                                    )
                                )
                            )
                        }
                    }
                }
            }
            return Pair(isDependencyExists, isPomExists)
        }
    }
}