package com.alibabacloud.credentials.settings

import com.intellij.ide.BrowserUtil
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import org.apache.commons.lang3.ObjectUtils
import javax.swing.JComponent
import javax.swing.JEditorPane
import javax.swing.JPanel
import javax.swing.event.HyperlinkEvent

class ToolkitSettingsComponent {
    val panel: JPanel
    private val myProfileName = JBTextField()
    private val myAccessKeyId = JBTextField()
    private val myAccessKeySecret = JBPasswordField()
    private val myMemoryStatus = JBCheckBox("Remember me")

    init {
        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Profile: "), myProfileName, 1, false)
            .addLabeledComponent(JBLabel("AccessKeyId: "), myAccessKeyId, 1, false)
            .addLabeledComponent(JBLabel("AccessKeySecret: "), myAccessKeySecret, 1, false)
            .addLabeledComponent("", myMemoryStatus)
            .addComponentFillVertically(JPanel(), 0)
            .addComponent(getLinkPane("Login Tips"))
            .panel
    }

    val preferredFocusedComponent: JComponent
        get() = myAccessKeyId

    var userProfileName: String?
        get() = myProfileName.getText()
        set(newProfileName) {
            if (newProfileName == null) {
                // TODO
            }
            myProfileName.setText(newProfileName)
        }

    var userAccessKeyId: String?
        get() = myAccessKeyId.getText()
        set(newAccessKeyId) {
            if (newAccessKeyId == null) {
                // TODO
            }
            myAccessKeyId.setText(newAccessKeyId)
        }

    var userAccessKeySecret: String?
        get() = String(myAccessKeySecret.getPassword())
        set(newAccessSecret) {
            if (newAccessSecret == null) {
                // TODO
            }
            myAccessKeySecret.setText(newAccessSecret)
        }

    var memoryStatus: Boolean
        get() = myMemoryStatus.isSelected
        set(memoryStatus) {
            myMemoryStatus.setSelected(memoryStatus)
        }

    private fun getLinkPane(tip: String): JEditorPane {
        val loginTip = JEditorPane("text/html", tip)
        loginTip.isEditable = false
        loginTip.setOpaque(false)
        loginTip.addHyperlinkListener { clickEvent: HyperlinkEvent ->
            if (HyperlinkEvent.EventType.ACTIVATED == clickEvent.eventType && ObjectUtils.isNotEmpty(
                    clickEvent.url,
                )
            ) {
                BrowserUtil.open(clickEvent.url.toString())
            }
        }
        return loginTip
    }
}
