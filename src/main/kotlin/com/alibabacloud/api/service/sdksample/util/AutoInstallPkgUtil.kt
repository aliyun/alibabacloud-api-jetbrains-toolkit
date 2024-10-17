package com.alibabacloud.api.service.sdksample.util

import com.alibabacloud.api.service.OkHttpClientProvider
import com.alibabacloud.api.service.completion.util.JavaPkgInstallUtil
import com.alibabacloud.api.service.completion.util.ProjectStructureUtil
import com.alibabacloud.api.service.completion.util.PythonPkgInstallUtil
import com.alibabacloud.api.service.constants.NotificationGroups
import com.alibabacloud.api.service.notification.NormalNotification
import com.alibabacloud.api.service.util.RequestUtil
import com.alibabacloud.i18n.I18nUtils
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.intellij.notification.NotificationType
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import java.io.IOException

class AutoInstallPkgUtil {
    companion object {
        internal fun getLastSdkInfo(project: Project, product: String, defaultVersion: String): JsonObject? {
            val url = "https://api.aliyun.com/api/sdk/product/info?product=$product&version=$defaultVersion"
            val request = RequestUtil.createRequest(url)
            try {
                OkHttpClientProvider.instance.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val resp = Gson().fromJson(response.body?.string(), JsonObject::class.java)
                        return resp.get("data").asJsonObject.get("lastSdkInfo").asJsonObject
                    }
                }
            } catch (e: IOException) {
                NormalNotification.showMessage(
                    project,
                    NotificationGroups.NETWORK_NOTIFICATION_GROUP,
                    I18nUtils.getMsg("auto.install.package.fail"),
                    I18nUtils.getMsg("network.check"),
                    NotificationType.ERROR
                )
            }
            return null
        }

        private fun makeMavenCommand(
            product: String,
            defaultVersion: String,
            sdkVersion: String?
        ): Pair<String?, String?> {
            val artifactIdJava = "${product.lowercase().replace("-", "_")}${defaultVersion.replace("-", "")}"
            val artifactIdJavaAsync = "alibabacloud-${product.lowercase().replace("-", "_")}${
                defaultVersion.replace(
                    "-",
                    ""
                )
            }"

            val commandJava = if (sdkVersion != null) {
                "<dependency>\n" +
                        "  <groupId>com.aliyun</groupId>\n" +
                        "  <artifactId>$artifactIdJava</artifactId>\n" +
                        "  <version>$sdkVersion</version>\n" +
                        "</dependency>"
            } else null

            val commandJavaAsync = if (sdkVersion != null) {
                "<dependency>\n" +
                        "  <groupId>com.aliyun</groupId>\n" +
                        "  <artifactId>$artifactIdJavaAsync</artifactId>\n" +
                        "  <version>$sdkVersion</version>\n" +
                        "</dependency>"
            } else null

            return Pair(commandJava, commandJavaAsync)

        }

        fun installPyPkg(project: Project, productName: String, defaultVersion: String, sdkVersion: String?) {
            val sdk = ProjectStructureUtil.getEditingSdk(project)
            val pkgName = "alibabacloud-${productName.lowercase()}${defaultVersion.replace("-", "")}"

            val isPyPkgExists = PythonPkgInstallUtil.isPyPackageExist(project, sdk, pkgName, sdkVersion!!)
            if (sdk != null && !isPyPkgExists[0]) {
                val content = if (!isPyPkgExists[1]) {
                    "${I18nUtils.getMsg("auto.install.package.ask")} $pkgName?"
                } else {
                    I18nUtils.getMsg("auto.install.package.update.ask.prefix") + " $pkgName " +
                            I18nUtils.getMsg("auto.install.package.update.ask.suffix") + " $sdkVersion?"
                }
                NormalNotification.showNotificationWithActions(
                    project,
                    NotificationGroups.DEPS_NOTIFICATION_GROUP,
                    I18nUtils.getMsg("auto.install.package"),
                    content,
                    NotificationType.INFORMATION,
                    listOf(
                        I18nUtils.getMsg("dialog.yes") to {
                            ProgressManager.getInstance()
                                .run(object : Task.Backgroundable(project, "", true) {
                                    override fun run(indicator: ProgressIndicator) {
                                        PythonPkgInstallUtil.pyPackageInstall(project, sdk, pkgName, sdkVersion)
                                    }
                                })
                        },
                        I18nUtils.getMsg("dialog.no") to {}
                    )
                )
            }
        }

        private fun installMavenPkg(project: Project, mavenCommand: String?, lang: String) {
            val resList = JavaPkgInstallUtil.isMavenDependencyExist(project, mavenCommand)
            val isDependencyExists = resList[0]
            val isPomExists = resList[1]
            val needUpdate = resList[2]
            if (isPomExists && needUpdate && mavenCommand != null) {
                val commandInfo = JavaPkgInstallUtil.parseMavenCommand(mavenCommand)
                val version = commandInfo[0]
                val artifactId = commandInfo[2]
                val content = I18nUtils.getMsg("auto.install.package.update.ask.prefix")  + " $artifactId " +
                        I18nUtils.getMsg("auto.install.package.update.ask.suffix") + " $version?"
                NormalNotification.showNotificationWithActions(
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
            } else if (isPomExists && !isDependencyExists && mavenCommand != null) {
                JavaPkgInstallUtil.importMavenDeps(
                    project,
                    mavenCommand,
                )
            } else if (!isPomExists) {
                NormalNotification.showMessage(
                    project,
                    NotificationGroups.DEPS_NOTIFICATION_GROUP,
                    I18nUtils.getMsg("auto.install.package.fail"),
                    "${I18nUtils.getMsg("java.no.pom.prefix")}$lang ${I18nUtils.getMsg("java.no.pom.suffix")}",
                    NotificationType.WARNING
                )
            } else if (isDependencyExists) {
                NormalNotification.showMessage(
                    project,
                    NotificationGroups.DEPS_NOTIFICATION_GROUP,
                    I18nUtils.getMsg("auto.install.package.exist"),
                    "",
                    NotificationType.INFORMATION
                )
            } else {
                NormalNotification.showMessage(
                    project,
                    NotificationGroups.DEPS_NOTIFICATION_GROUP,
                    I18nUtils.getMsg("auto.install.package.fail"),
                    "${I18nUtils.getMsg("sdk.not.exist.prefix")} $lang ${I18nUtils.getMsg("sdk.not.exist.suffix")}",
                    NotificationType.INFORMATION
                )
            }
        }


        internal fun autoImport(
            project: Project,
            lang: String,
            productName: String,
            defaultVersion: String,
            lastSdkInfo: JsonObject?
        ) {
            when (lang) {
                "java" -> {
                    val sdkVersion = lastSdkInfo?.get("java-tea")?.asJsonObject?.get("last_version")?.asString
                    val mavenCommand = makeMavenCommand(productName, defaultVersion, sdkVersion).first
                    installMavenPkg(project, mavenCommand, lang)
                }

                "java-async" -> {
                    val sdkVersion = lastSdkInfo?.get("java-async-tea")?.asJsonObject?.get("last_version")?.asString
                    val mavenCommand = makeMavenCommand(productName, defaultVersion, sdkVersion).first
                    installMavenPkg(project, mavenCommand, lang)
                }

                "python" -> {
                    val sdkVersion = lastSdkInfo?.get("python-tea")?.asJsonObject?.get("last_version")?.asString
                    installPyPkg(project, productName, defaultVersion, sdkVersion)
                }

                else -> {
                    NormalNotification.showMessage(
                        project,
                        NotificationGroups.DEPS_NOTIFICATION_GROUP,
                        I18nUtils.getMsg("auto.install.package.fail"),
                        "${I18nUtils.getMsg("sdk.code.sample.lang.not.support")}${I18nUtils.getMsg("auto.install.package.click.for.package")}",
                        NotificationType.INFORMATION
                    )
                }
            }
        }
    }
}