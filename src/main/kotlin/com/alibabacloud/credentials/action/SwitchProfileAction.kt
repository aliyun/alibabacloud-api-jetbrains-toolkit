package com.alibabacloud.credentials.action

import com.alibabacloud.models.credentials.ConfigureFile
import com.alibabacloud.ui.ProcessConfig
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

class SwitchProfileAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project: Project? = e.project
        if (project != null) {
            showProfileDialog(project)
        }
    }

    private fun showProfileDialog(project: Project) {
        val config = ConfigureFile.loadConfigureFile()
        val profiles = mutableListOf<ConfigureFile.Profile>()
        profiles.addAll(
            config.profiles.map {
                ConfigureFile.Profile(
                    it.name,
                    it.access_key_id,
                    it.access_key_secret,
                )
            },
        )

        val profileNames = profiles.map { it.name }
        val selectedProfile = Messages.showEditableChooseDialog(
            "Select a profile:",
            "Profiles",
            Messages.getInformationIcon(),
            profileNames.plus("Add Profile").toTypedArray(),
            config.current,
            null,
        )

        if (selectedProfile != null) {
            if (selectedProfile == "Add Profile") {
                val defaultProfile = ConfigureFile.Profile()
                val newProfile = ProcessConfig.createNewProfile(project, defaultProfile)
                if (newProfile != null) {
                    profiles.add(newProfile)
                    config.current = newProfile.name
                    ConfigureFile.saveConfigureFile(config)
                }
            } else {
                config.current = selectedProfile
                ConfigureFile.saveConfigureFile(config)
            }
        }
    }
}
