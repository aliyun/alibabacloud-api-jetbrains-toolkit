package com.alibabacloud.ui

import com.alibabacloud.api.service.constants.NotificationGroups
import com.alibabacloud.api.service.notification.NormalNotification
import com.alibabacloud.credentials.constants.CredentialsConstants
import com.alibabacloud.models.credentials.ConfigureFile
import com.google.gson.Gson
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.WrapLayout
import java.awt.BorderLayout
import java.awt.Desktop
import java.awt.GridLayout
import java.io.File
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class CollapsibleInputPanel(private val project: Project) : JPanel() {
    private val profileNameField = JBTextField()
    private val akField = JBTextField()
    private val skField = JPasswordField()
    private val showPasswordCheckBox = JCheckBox("显示").apply {
        addActionListener {
            skField.echoChar = if (isSelected) Char(0) else '•'
        }
    }
    private val passwordPanel = JPanel(BorderLayout()).apply {
        add(skField, BorderLayout.CENTER)
        add(showPasswordCheckBox, BorderLayout.EAST)
    }
    private val regionField = JBTextField()
    private val confirmButton = JButton()
    private val cancelButton = JButton("取消").apply {
        addActionListener {
            collapsePanel()
        }
    }
    private val buttonPanel = JPanel(WrapLayout())
    private var isExpanded: Boolean = false
    private var hasValidationError = false
    private var isUserInteraction = true


    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        this.border = JBUI.Borders.empty(30)
        val inputPanel = JPanel(GridLayout(0, 1, 0, 5))
        profileNameField.emptyText.text = "Profile Name:"
        akField.emptyText.text = "Access Key Id:"
        skField.toolTipText = "Access Key Secret:"
        regionField.emptyText.text = "RegionId:"
        skField.echoChar = '•'

        inputPanel.add(profileNameField)
        inputPanel.add(regionField)
        inputPanel.add(akField)
        inputPanel.add(passwordPanel)
        add(inputPanel)

        profileNameField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) {
                if (isUserInteraction) {
                    validateProfileNameUnique()
                }
            }

            override fun removeUpdate(e: DocumentEvent?) {
                if (isUserInteraction) {
                    validateProfileNameUnique()
                }
            }

            override fun changedUpdate(e: DocumentEvent?) {
                if (isUserInteraction) {
                    validateProfileNameUnique()
                }
            }
        })


        confirmButton.addActionListener {
            if (confirmButton.text == "添加配置") {
                saveProfile()
            }
        }

        buttonPanel.add(confirmButton)
        buttonPanel.add(cancelButton)
        add(buttonPanel)

        isVisible = isExpanded
    }

    private fun validateProfileNameUnique() {
        val profileName = profileNameField.text
        val config = ConfigureFile.loadConfigureFile()
        hasValidationError = if (config?.profiles?.any { it.name == profileName } == true) {
            NormalNotification.showMessage(
                project,
                NotificationGroups.CONFIG_NOTIFICATION_GROUP,
                "该 profile name 已存在",
                "",
                NotificationType.ERROR
            )
            true
        } else {
            false
        }
    }

    private fun saveProfile() {
        confirmButton.text = "添加配置"
        if (confirmButton.text == "添加配置") {
            hasValidationError = false
            val profileName = profileNameField.text
            val accessKeyId = akField.text
            val accessKeySecret = String(skField.password)
            val regionId = regionField.text

            if (profileName.isEmpty() || accessKeyId.isEmpty() || accessKeySecret.isEmpty() || regionId.isEmpty()) {
                NormalNotification.showMessage(
                    project,
                    NotificationGroups.CONFIG_NOTIFICATION_GROUP,
                    "添加 Profile 失败",
                    "请填入必填参数",
                    NotificationType.ERROR
                )
                hasValidationError = true
            }

            val config = ConfigureFile.loadConfigureFile()
            if (config?.profiles?.any { it.name == profileName } == true) {
                NormalNotification.showMessage(
                    project,
                    NotificationGroups.CONFIG_NOTIFICATION_GROUP,
                    "添加 Profile 失败",
                    "该 profile name 已存在",
                    NotificationType.ERROR
                )
                hasValidationError = true
            }

            if (hasValidationError) {
                return
            }
            val newProfile = ConfigureFile.Profile(
                name = profileName,
                access_key_id = accessKeyId,
                access_key_secret = accessKeySecret,
                region_id = regionId
            )

            if (config != null) {
                config.profiles.add(newProfile)
                ConfigureFile.saveConfigureFile(config)
            } else {
                val configDir = File(CredentialsConstants.CONFIG_DIR)
                if (!configDir.exists()) {
                    configDir.mkdir()
                }
                val profiles = mutableListOf<ConfigureFile.Profile>()
                profiles.add(newProfile)
                val configJson = Gson().toJson(ConfigureFile(profileName, profiles, ""))
                val path = ConfigureFile.getDefaultPath()
                val configFile = File(path)
                configFile.writeText(configJson)
            }

            NormalNotification.showMessage(
                project,
                NotificationGroups.CONFIG_NOTIFICATION_GROUP,
                "添加 Profile 成功",
                "",
                NotificationType.INFORMATION
            )
            collapsePanel()
        }
    }

    private fun expandPanel() {
        if (!isExpanded) {
            isExpanded = true
            isVisible = true
        }
    }

    fun expandForAddProfile() {
        confirmButton.text = "添加配置"
        profileNameField.isEditable = true
        akField.isEditable = true
        skField.isEditable = true
        regionField.isEditable = true
        if (!isExpanded) {
            isExpanded = true
            isVisible = true
        }
    }

    private fun collapsePanel() {
        if (isExpanded) {
            isExpanded = false
            isVisible = false
        }
    }

    fun clearFields() {
        profileNameField.text = ""
        akField.text = ""
        skField.text = ""
        regionField.text = ""
    }

    fun showProfiles(profile: ConfigureFile.Profile?) {
        isUserInteraction = false
        confirmButton.text = "From file: ${CredentialsConstants.CONFIG_DIR}/config.json [只读]"
        confirmButton.addActionListener {
            if (confirmButton.text != "添加配置") {
                val filePath = "${System.getProperty("user.home")}/.aliyun/config.json"
                val configFile = File(filePath)
                if (!Desktop.isDesktopSupported() || !Desktop.getDesktop()
                        .isSupported(Desktop.Action.BROWSE_FILE_DIR)
                ) {
                    NormalNotification.showMessage(
                        project,
                        NotificationGroups.CONFIG_NOTIFICATION_GROUP,
                        "打开文件夹失败",
                        "不支持在文件浏览器中打开文件",
                        NotificationType.ERROR
                    )
                    return@addActionListener
                }
                if (configFile.exists()) {
                    Desktop.getDesktop().browseFileDirectory(configFile)
                } else {
                    NormalNotification.showMessage(
                        project,
                        NotificationGroups.CONFIG_NOTIFICATION_GROUP,
                        "打开文件夹失败",
                        "配置文件未找到",
                        NotificationType.ERROR
                    )
                }
            }
        }

        profileNameField.isEditable = false
        akField.isEditable = false
        skField.isEditable = false
        regionField.isEditable = false

        profileNameField.text = profile?.name ?: ""
        akField.text = profile?.access_key_id ?: ""
        skField.text = profile?.access_key_secret ?: ""
        skField.echoChar = if (showPasswordCheckBox.isSelected) Char(0) else '•'
        regionField.text = profile?.region_id ?: ""
        expandPanel()
        isUserInteraction = true
    }
}
