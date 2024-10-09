package com.alibabacloud.api.service.completion.util

import com.alibabacloud.api.service.constants.NotificationGroups
import com.alibabacloud.api.service.notification.NormalNotification
import com.alibabacloud.i18n.I18nUtils
import com.intellij.execution.ExecutionException
import com.intellij.notification.NotificationType
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.jetbrains.python.packaging.PyPackageManagerUI
import com.jetbrains.python.packaging.PyRequirementParser
import com.jetbrains.python.packaging.common.PythonPackage
import com.jetbrains.python.packaging.management.PythonPackageManager

class PythonPkgInstallUtil {
    companion object {
        fun pyPackageInstall(project: Project, sdk: Sdk, pkgName: String, sdkVersion: String?) {
            if (sdkVersion == null) {
                NormalNotification.showMessage(
                    project,
                    NotificationGroups.DEPS_NOTIFICATION_GROUP,
                    I18nUtils.getMsg("auto.install.package.fail"),
                    "${I18nUtils.getMsg("sdk.not.exist.prefix")} Python ${I18nUtils.getMsg("sdk.not.exist.suffix")}",
                    NotificationType.INFORMATION
                )
            } else {
                val pyPackageManagerUI = PyPackageManagerUI(project, sdk, PyPackageListener())
                pyPackageManagerUI.install(
                    listOf(PyRequirementParser.fromLine("$pkgName==$sdkVersion")), emptyList<String?>()
                )
            }
        }

        private class PyPackageListener : PyPackageManagerUI.Listener {
            override fun started() {}
            override fun finished(list: List<ExecutionException>) {}
        }

        fun isPyPackageExist(project: Project, sdk: Sdk?, pkgName: String, sdkVersion: String): MutableList<Boolean> {
            var isPackageExists = false
            var needUpdate = false
            if (sdk == null) {
                NormalNotification.showMessage(
                    project,
                    NotificationGroups.DEPS_NOTIFICATION_GROUP,
                    I18nUtils.getMsg("auto.install.package.fail"),
                    I18nUtils.getMsg("python.no.interpreter"),
                    NotificationType.WARNING
                )
            } else {
                val packageList = getPackageList(project, sdk)
                for (pyPky in packageList) {
                    if (pyPky.name == pkgName) {
                        if (pyPky.version == sdkVersion) {
                            isPackageExists = true
                        } else {
                            needUpdate = true
                        }
                        break
                    }
                }
            }
            return mutableListOf(isPackageExists, needUpdate)
        }

        private fun getPackageList(project: Project, pythonSdk: Sdk): List<PythonPackage> {
            return PythonPackageManager.forSdk(project, pythonSdk).installedPackages
        }
    }
}