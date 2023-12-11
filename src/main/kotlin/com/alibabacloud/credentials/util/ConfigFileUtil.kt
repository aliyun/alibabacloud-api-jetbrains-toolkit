package com.alibabacloud.credentials.util

import com.alibabacloud.models.credentials.ConfigureFile
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.util.messages.MessageBusConnection
import java.io.File
import javax.swing.DefaultComboBoxModel
import javax.swing.JComboBox

class ConfigFileUtil {
    companion object {
        fun readProfilesFromConfigFile(comboBox: JComboBox<String>) {
            try {
                val config = ConfigureFile.loadConfigureFile()
                val profiles = mutableListOf<String>()
                profiles.clear()
                profiles.addAll(config.profiles.map { it.name })
                profiles.add("Edit Profile")
                comboBox.model = DefaultComboBoxModel(profiles.toTypedArray())
                comboBox.selectedItem = "Edit Profile"
                comboBox.isEnabled = true
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
        fun subscribeToFileChangeEvent(project: Project, comboBox: JComboBox<String>) {
            val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(File(ConfigureFile.getDefaultPath()))
            val busConnection: MessageBusConnection
            if (virtualFile != null) {
                busConnection = project.messageBus.connect()
                busConnection.subscribe(
                    VirtualFileManager.VFS_CHANGES,
                    object : BulkFileListener {
                        override fun after(events: MutableList<out VFileEvent>) {
                            if (events.any { it.file == virtualFile }) {
                                readProfilesFromConfigFile(comboBox)
                            }
                        }
                    },
                )
            }
        }
    }
}
