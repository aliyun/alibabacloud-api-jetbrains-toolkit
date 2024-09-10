package com.alibabacloud.toolkit

import com.alibabacloud.api.service.constants.NotificationGroups
import com.alibabacloud.api.service.notification.NormalNotification
import com.alibabacloud.i18n.I18nUtils
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
                .run(object : Task.Backgroundable(null, I18nUtils.getMsg("plugin.update")) {
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
                "${latestPd.pluginVersion} ${I18nUtils.getMsg("plugin.update.restart")}",
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
                I18nUtils.getMsg("plugin.update.fail"),
                NotificationType.INFORMATION,
            )
            return
        } catch (e: Error) {
            NormalNotification.showMessage(
                project,
                NotificationGroups.PLUGIN_INSTALLED_NOTIFICATION_GROUP,
                "Alibaba Cloud Developer Toolkit",
                I18nUtils.getMsg("plugin.update.fail"),
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
