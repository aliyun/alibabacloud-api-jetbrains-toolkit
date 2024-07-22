package com.alibabacloud.credentials.util

import com.alibabacloud.i18n.I18nUtils
import com.alibabacloud.models.credentials.ConfigureFile
import javax.swing.DefaultComboBoxModel
import javax.swing.JComboBox

class ConfigFileUtil {
    companion object {
        fun readProfilesFromConfigFile(comboBox: JComboBox<String>) {
            val config = ConfigureFile.loadConfigureFile()
            val profiles = mutableListOf<String>()
            profiles.clear()
            config?.profiles?.map { it.name }?.let { profiles.addAll(it) }
            profiles.add(I18nUtils.getMsg("credentials.new.profile"))
            comboBox.model = DefaultComboBoxModel(profiles.toTypedArray())
            comboBox.isEnabled = true
        }
    }
}
