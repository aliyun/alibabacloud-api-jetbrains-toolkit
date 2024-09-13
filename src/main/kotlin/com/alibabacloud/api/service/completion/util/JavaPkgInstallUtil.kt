package com.alibabacloud.api.service.completion.util

import com.alibabacloud.api.service.constants.NotificationGroups
import com.alibabacloud.api.service.notification.NormalNotification
import com.alibabacloud.i18n.I18nUtils
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
import java.io.IOException

class JavaPkgInstallUtil {
    companion object {
        fun importMavenDeps(
            project: Project,
            mavenCommand: String,
        ) {
            val basePath = project.basePath
            val projectBaseDir = basePath?.let { path ->
                LocalFileSystem.getInstance().findFileByPath(path)
            }
            val pomVirtualFile = projectBaseDir?.findChild("pom.xml")

            insertDependencyInPom(
                project,
                pomVirtualFile,
                mavenCommand,
                onSuccess = {
                    NormalNotification.showMessage(
                        project,
                        NotificationGroups.DEPS_NOTIFICATION_GROUP,
                        I18nUtils.getMsg("auto.install.package.begin"),
                        "",
                        NotificationType.INFORMATION
                    )
                },
                onFailure = {
                    NormalNotification.showMessage(
                        project,
                        NotificationGroups.DEPS_NOTIFICATION_GROUP,
                        I18nUtils.getMsg("auto.install.package.fail"),
                        I18nUtils.getMsg("network.check"),
                        NotificationType.ERROR
                    )
                })
        }

        fun updateMavenDeps(
            project: Project,
            mavenCommand: String,
        ) {
            val basePath = project.basePath
            val projectBaseDir = basePath?.let { path ->
                LocalFileSystem.getInstance().findFileByPath(path)
            }
            val pomVirtualFile = projectBaseDir?.findChild("pom.xml")

            updateDependencyInPom(
                project,
                pomVirtualFile,
                mavenCommand,
                onSuccess = {
                    NormalNotification.showMessage(
                        project,
                        NotificationGroups.DEPS_NOTIFICATION_GROUP,
                        I18nUtils.getMsg("auto.install.package.update.begin"),
                        "",
                        NotificationType.INFORMATION
                    )
                },
                onFailure = {
                    NormalNotification.showMessage(
                        project,
                        NotificationGroups.DEPS_NOTIFICATION_GROUP,
                        I18nUtils.getMsg("auto.install.package.fail"),
                        I18nUtils.getMsg("network.check"),
                        NotificationType.ERROR
                    )
                })
        }


        private fun insertDependencyInPom(
            project: Project,
            pomVirtualFile: VirtualFile?,
            mavenCommand: String,
            onSuccess: () -> Unit,
            onFailure: () -> Unit
        ) {
            WriteCommandAction.runWriteCommandAction(project) {
                val documentManager = FileDocumentManager.getInstance()
                val document = documentManager.getDocument(pomVirtualFile!!) ?: return@runWriteCommandAction
                val pomBackupContent = document.text
                ApplicationManager.getApplication().runReadAction {
                    try {
                        var pomContent = pomBackupContent
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
                    } catch (e: IOException) {
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
        }

        private fun updateDependencyInPom(
            project: Project,
            pomVirtualFile: VirtualFile?,
            mavenCommand: String,
            onSuccess: () -> Unit,
            onFailure: () -> Unit
        ) {
            WriteCommandAction.runWriteCommandAction(project) {
                val documentManager = FileDocumentManager.getInstance()
                val document = documentManager.getDocument(pomVirtualFile!!) ?: return@runWriteCommandAction
                val pomBackupContent = document.text
                ApplicationManager.getApplication().runReadAction {
                    try {
                        val commandInfo = parseMavenCommand(mavenCommand)
                        val newVersion = commandInfo[0]
                        val dependencyPattern = commandInfo[1]

                        val updatedContent =
                            pomBackupContent.replace(dependencyPattern.toRegex(setOf(RegexOption.DOT_MATCHES_ALL))) { matchResult ->
                                val matchedDependency = matchResult.value
                                matchedDependency.replace(
                                    Regex("<version>[^<]+</version>"),
                                    "<version>$newVersion</version>"
                                )
                            }
                        ApplicationManager.getApplication().invokeLater {
                            WriteCommandAction.runWriteCommandAction(project) {
                                document.setText(updatedContent)
                                documentManager.saveDocument(document)
                            }
                        }
                        refreshMavenProject(project)
                        onSuccess()
                    } catch (e: IOException) {
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

        fun isMavenDependencyExist(
            project: Project,
            mavenCommand: String?,
        ): MutableList<Boolean> {
            var isDependencyExists = false
            var isPomExists = false
            var needUpdate = false
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
                        if (mavenCommand != null) {
                            val commandInfo = parseMavenCommand(mavenCommand)
                            val version = commandInfo[0]
                            val dependencyPattern = commandInfo[1]
                            val matches = Regex(
                                dependencyPattern,
                                setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
                            ).findAll(pomBackupContent)
                            for (match in matches) {
                                val foundVersion = match.groups[1]?.value?.trim()
                                if (foundVersion != null) {
                                    if (foundVersion == version) {
                                        isDependencyExists = true
                                        break
                                    } else {
                                        needUpdate = true
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return mutableListOf(isDependencyExists, isPomExists, needUpdate)
        }

        fun parseMavenCommand(mavenCommand: String): MutableList<String> {
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
                "<dependency>\\s*?<groupId>\\s*$groupId\\s*</groupId>\\s*?<artifactId>\\s*$artifactId\\s*</artifactId>\\s*?<version>(.*?)</version>\\s*?</dependency>"
            return mutableListOf(version, dependencyPattern, artifactId)
        }
    }
}