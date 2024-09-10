package com.alibabacloud.api.service.completion.util

import com.alibabacloud.api.service.constants.NotificationGroups
import com.alibabacloud.api.service.notification.NormalNotification
import com.alibabacloud.i18n.I18nUtils
import com.intellij.execution.ExecutionException
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.jetbrains.python.packaging.PyPackage
import com.jetbrains.python.packaging.PyPackageManager
import com.jetbrains.python.packaging.PyPackageManagerUI
import com.jetbrains.python.packaging.PyRequirementParser

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

        fun isPyPackageExist(project: Project, sdk: Sdk?, pkgName: String): Boolean {
            if (sdk == null) {
                NormalNotification.showMessage(
                    project,
                    NotificationGroups.DEPS_NOTIFICATION_GROUP,
                    I18nUtils.getMsg("auto.install.package.fail"),
                    I18nUtils.getMsg("python.no.interpreter"),
                    NotificationType.WARNING
                )
                return true
            }
            val packageList = getPackageList(sdk)
            return packageList.any { it.name.contains(pkgName) }
        }

        private fun getPackageList(pythonSdk: Sdk): List<PyPackage> {
            return PyPackageManager.getInstance(pythonSdk).packages ?: return emptyList()
        }
    }
}