package com.alibabacloud.ui

import com.alibabacloud.credentials.util.ConfigFileUtil
import com.alibabacloud.icons.ToolkitIcons
import com.alibabacloud.models.credentials.ConfigureFile
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.impl.status.TextPanel.WithIconAndArrows
import com.intellij.ui.ClickListener
import com.intellij.vcs.commit.NonModalCommitPanel.Companion.showAbove
import java.awt.event.MouseEvent
import javax.swing.Icon
import javax.swing.JComponent

class MyStatusBarWidgetFactory : StatusBarWidgetFactory {
    override fun getId(): String = "AlibabaCloud"

    override fun getDisplayName(): String = "Alibaba Cloud"

    override fun isAvailable(project: Project): Boolean = true

    override fun createWidget(project: Project): StatusBarWidget = MyStatusBarWidget(project)

    override fun disposeWidget(widget: StatusBarWidget) {
    }

    override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true

    class MyStatusBarWidget(project: Project) : WithIconAndArrows(), CustomStatusBarWidget {
        private var statusBar: StatusBar? = null

        init {
            object : ClickListener() {
                override fun onClick(event: MouseEvent, clickCount: Int): Boolean {
                    showProfileListPopup(project)
                    return true
                }
            }.installOn(this, true)
        }

        override fun ID(): String = "Alibaba Cloud Widget"

        @JvmField
        val myIcon = ToolkitIcons.STATUSBAR_ICON

        override fun install(statusBar: StatusBar) {
            this.statusBar = statusBar
            val config = ConfigureFile.loadConfigureFile()
            text = "Alibaba Cloud: " + (config?.current ?: "")
            icon = myIcon
        }

        override fun getComponent(): JComponent = this
        override fun dispose() {}

        fun updateStatusBar(config: ConfigureFile?) {
            if (config != null) {
                text = "Alibaba Cloud: ${config.current}"
                statusBar?.updateWidget(ID())
            } else {
                text = "Alibaba Cloud:"
                statusBar?.updateWidget(ID())
            }
        }

        private fun showProfileListPopup(project: Project) {
            val config = ConfigureFile.loadConfigureFile()
            val profiles = mutableListOf<String>()
            profiles.clear()
            config?.profiles?.map { it.name }?.let { profiles.addAll(it) }

            val listPopup = JBPopupFactory.getInstance().createPopupChooserBuilder(profiles)
                .setTitle("Switch Profile")
                .setItemChosenCallback { selectedProfile ->
                    if (config != null) {
                        config.current = selectedProfile
                        ConfigureFile.saveConfigureFile(config)
                        updateStatusBar(config)
                        val comboBoxManager = ComboBoxManager.getInstance(project)
                        val comboBox = comboBoxManager.comboBox
                        ConfigFileUtil.readProfilesFromConfigFile(comboBox)
                        ComboBoxManager.updateComboBoxItem(comboBox)
                    }
                }
                .setRenderer(CustomListCellRenderer())
                .createPopup()
            listPopup.showAbove(this)

        }
    }
}
