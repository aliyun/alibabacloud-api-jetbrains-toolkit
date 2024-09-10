package com.alibabacloud.ui

import com.alibabacloud.i18n.I18nUtils
import com.intellij.icons.AllIcons
import java.awt.Component
import javax.swing.DefaultListCellRenderer
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.SwingConstants

class CustomListCellRenderer : DefaultListCellRenderer() {
    override fun getListCellRendererComponent(
        list: JList<*>,
        value: Any?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean,
    ): Component {
        val c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JLabel
        if (c.text == I18nUtils.getMsg("credentials.new.profile")) {
            c.icon = AllIcons.General.Add
        }
        c.horizontalAlignment = SwingConstants.CENTER
        return c
    }
}
