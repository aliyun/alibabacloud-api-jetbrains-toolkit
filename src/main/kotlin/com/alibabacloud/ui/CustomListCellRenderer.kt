package com.alibabacloud.ui

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
        val c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
        (c as? JLabel)?.horizontalAlignment = SwingConstants.CENTER
        return c
    }
}
