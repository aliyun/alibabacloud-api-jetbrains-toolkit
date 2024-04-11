package com.alibabacloud.toolkit

import com.alibabacloud.api.service.constants.NotificationGroups
import com.alibabacloud.api.service.notification.NormalNotification
import com.alibabacloud.settings.ToolkitSettingsConfigurable
import com.alibabacloud.states.ToolkitSettingsState
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.InstalledPluginsState
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.updateSettings.impl.PluginDownloader
import com.intellij.openapi.updateSettings.impl.UpdateChecker
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import org.jetbrains.annotations.VisibleForTesting

class ToolkitUpdate : StartupActivity {
    @VisibleForTesting
    override fun runActivity(project: Project) {
        val enabled = ToolkitSettingsState.getInstance().isAutoUpdateEnabled
        if (enabled) {
            ProgressManager.getInstance()
                .run(object : Task.Backgroundable(null, ("Updating Alibaba Cloud Developer Toolkit")) {
                    override fun run(indicator: ProgressIndicator) {
                        checkAndUpdate(project, indicator)
                    }
                })
        }
    }

    @RequiresBackgroundThread
    fun checkAndUpdate(project: Project, progressIndicator: ProgressIndicator) {
        val latestPd: PluginDownloader
        try {
            if (InstalledPluginsState.getInstance().wasUpdatedWithRestart(PluginId.getId(ToolkitInfo.PLUGIN_ID))) {
                return
            }
            val toolkit = ToolkitInfo.DESCRIPTOR as IdeaPluginDescriptor? ?: return
            if (!toolkit.isEnabled) {
                return
            }
            latestPd = getLatestPd(toolkit) ?: return

            if (!latestPd.prepareToInstall(progressIndicator)) return
            latestPd.install()

            NormalNotification.showPluginUpdateInstalled(
                project,
                NotificationGroups.PLUGIN_INSTALLED_NOTIFICATION_GROUP,
                "Alibaba Cloud Developer Toolkit",
                "${latestPd.pluginVersion} 版本安装完成，请重启 IDE",
                NotificationType.INFORMATION,
                restartAction = {
                    ApplicationManager.getApplication().restart()
                },
                restartLaterAction = {

                },
                updateSettingsAction = {
                    ShowSettingsUtil.getInstance().showSettingsDialog(project, ToolkitSettingsConfigurable::class.java)
                }
            )
        } catch (e: Exception) {
            NormalNotification.showMessage(
                project,
                NotificationGroups.PLUGIN_INSTALLED_NOTIFICATION_GROUP,
                "Alibaba Cloud Developer Toolkit",
                "插件自动更新失败，请稍后重试",
                NotificationType.INFORMATION,
            )
            return
        } catch (e: Error) {
            NormalNotification.showMessage(
                project,
                NotificationGroups.PLUGIN_INSTALLED_NOTIFICATION_GROUP,
                "Alibaba Cloud Developer Toolkit",
                "插件自动更新失败，请稍后重试",
                NotificationType.INFORMATION,
            )
            return
        }
    }

    @VisibleForTesting
    internal fun getLatestPd(pluginDescriptor: IdeaPluginDescriptor): PluginDownloader? =
        getPdList().firstOrNull {
            it.id == pluginDescriptor.pluginId && PluginDownloader.compareVersionsSkipBrokenAndIncompatible(
                it.pluginVersion,
                pluginDescriptor
            ) > 0
        }

    @VisibleForTesting
    internal fun getPdList(): Collection<PluginDownloader> = UpdateChecker.getPluginUpdates() ?: emptyList()
}
