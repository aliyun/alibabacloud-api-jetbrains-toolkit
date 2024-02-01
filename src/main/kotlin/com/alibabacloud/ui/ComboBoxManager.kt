package com.alibabacloud.ui

import com.alibabacloud.models.credentials.ConfigureFile
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox

@Service(Service.Level.APP)
class ComboBoxManager {
    val comboBox: ComboBox<String> by lazy { ComboBox() }

    companion object {
        fun getInstance(project: Project): ComboBoxManager = project.service()

        fun updateComboBoxItem(comboBox: ComboBox<String>) {
            val actionListeners = comboBox.actionListeners
            actionListeners.forEach { comboBox.removeActionListener(it) }
            val configRealTime = ConfigureFile.loadConfigureFile()
            comboBox.selectedItem = configRealTime?.current ?: "Add Profile"
            actionListeners.forEach { comboBox.addActionListener(it) }
        }
    }
}