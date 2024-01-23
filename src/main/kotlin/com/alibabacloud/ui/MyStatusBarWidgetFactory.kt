package com.alibabacloud.ui

import com.alibabacloud.models.credentials.ConfigureFile
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.impl.status.TextPanel.WithIconAndArrows
import com.intellij.ui.ClickListener
import java.awt.event.MouseEvent
import javax.swing.Icon
import javax.swing.JComponent

class MyStatusBarWidgetFactory : StatusBarWidgetFactory {
    override fun getId(): String = "AlibabaCloud"

    override fun getDisplayName(): String = "Alibaba Cloud"

    override fun isAvailable(project: Project): Boolean = true

    override fun createWidget(project: Project): StatusBarWidget = MyStatusBarWidget()

    override fun disposeWidget(widget: StatusBarWidget) {
    }

    override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true

    class MyStatusBarWidget : WithIconAndArrows(), CustomStatusBarWidget {
        private var statusBar: StatusBar? = null

        init {
            object : ClickListener() {
                override fun onClick(event: MouseEvent, clickCount: Int): Boolean {
                    return true
                }
            }.installOn(this, true)
        }

        override fun ID(): String = "Alibaba Cloud Widget"

        @JvmField
        val myIcon: Icon = IconLoader.getIcon("/icons/statusbar.svg", javaClass)

        override fun install(statusBar: StatusBar) {
            val config = ConfigureFile.loadConfigureFile()
            text = "Alibaba Cloud: " + (config?.current ?: "")
            icon = myIcon
        }

        override fun getComponent(): JComponent = this
        override fun dispose() {}
        fun updateStatusBarText(newText: String) {
            text = "Alibaba Cloud: $newText"
            statusBar?.updateWidget(ID())
        }
    }
}
