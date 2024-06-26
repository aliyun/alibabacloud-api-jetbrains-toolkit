package com.alibabacloud.api.service.sdksample.util

import com.alibabacloud.api.service.OkHttpClientProvider
import com.alibabacloud.api.service.completion.util.JavaPkgInstallUtil
import com.alibabacloud.api.service.completion.util.ProjectStructureUtil
import com.alibabacloud.api.service.completion.util.PythonPkgInstallUtil
import com.alibabacloud.api.service.constants.CompletionConstants
import com.alibabacloud.api.service.constants.NotificationGroups
import com.alibabacloud.api.service.notification.NormalNotification
import com.alibabacloud.api.service.util.RequestUtil
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.intellij.notification.NotificationType
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
                    "获取依赖失败",
                    "请检查网络",
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

        private fun installPyPkg(project: Project, productName: String, defaultVersion: String, sdkVersion: String?) {
            val sdk = ProjectStructureUtil.getEditingSdk(project)
            val pkgName = "alibabacloud-${productName.lowercase()}${defaultVersion.replace("-", "")}"

            val isPyPkgExists = PythonPkgInstallUtil.isPyPackageExist(project, sdk, pkgName)
            if (!isPyPkgExists && sdk != null) {
                if (sdkVersion != null) {
                    PythonPkgInstallUtil.pyPackageInstall(project, sdk, pkgName, sdkVersion)
                } else {
                    NormalNotification.showMessage(
                        project,
                        NotificationGroups.DEPS_NOTIFICATION_GROUP,
                        "依赖导入失败",
                        "${CompletionConstants.NO_SDK} Python SDK",
                        NotificationType.INFORMATION
                    )
                }
            } else {
                NormalNotification.showMessage(
                    project,
                    NotificationGroups.DEPS_NOTIFICATION_GROUP,
                    "依赖已存在",
                    "",
                    NotificationType.INFORMATION
                )
            }
        }

        private fun installMavenPkg(project: Project, mavenCommand: String?, lang: String) {
            val resList = JavaPkgInstallUtil.isMavenDependencyExist(project, mavenCommand)
            val isDependencyExists = resList[0]
            val isPomExists = resList[1]
            if (isPomExists && !isDependencyExists && mavenCommand != null) {
                JavaPkgInstallUtil.importMavenDeps(
                    project,
                    mavenCommand,
                )
            } else if (!isPomExists) {
                NormalNotification.showMessage(
                    project,
                    NotificationGroups.DEPS_NOTIFICATION_GROUP,
                    CompletionConstants.IMPORT_FAILED,
                    "未识别到 pom.xml（$lang 暂时只支持 Maven 依赖自动导入）",
                    NotificationType.WARNING
                )
            } else if (isDependencyExists) {
                NormalNotification.showMessage(
                    project,
                    NotificationGroups.DEPS_NOTIFICATION_GROUP,
                    CompletionConstants.ALREADY_EXIST,
                    "",
                    NotificationType.INFORMATION
                )
            } else {
                NormalNotification.showMessage(
                    project,
                    NotificationGroups.DEPS_NOTIFICATION_GROUP,
                    CompletionConstants.IMPORT_FAILED,
                    "${CompletionConstants.NO_SDK} $lang SDK",
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
                        "暂不支持该语言的依赖自动导入",
                        "请点击安装方式按钮获取",
                        NotificationType.INFORMATION
                    )
                }
            }
        }
    }
}