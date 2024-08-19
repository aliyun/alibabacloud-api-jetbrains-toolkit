package com.alibabacloud.ui

import java.awt.Component
import javax.swing.DefaultListCellRenderer
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.SwingConstants

class SearchListCellRenderer : DefaultListCellRenderer() {
    override fun getListCellRendererComponent(
        list: JList<*>,
        value: Any?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        val c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JLabel
        if (value is String) {
            val origin = c.text.split("::")
            text =
                "<html>&nbsp;&nbsp;${origin[0]}&nbsp;<span style='font-size:smaller; color:gray;'>${origin.getOrElse(1) { "" }}</span><br>" +
                        "<span style='font-size:smaller; color:gray;'>&nbsp;&nbsp;${origin.getOrElse(2) { "" }}&nbsp;${origin.getOrElse(3) { "" }}</span></html>"
        }
        c.horizontalAlignment = SwingConstants.LEFT
        return c
    }
}