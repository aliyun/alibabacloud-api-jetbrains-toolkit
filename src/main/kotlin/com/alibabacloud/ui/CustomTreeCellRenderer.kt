package com.alibabacloud.ui

import com.alibabacloud.icons.ToolkitIcons
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.JBColor
import java.awt.Color
import java.awt.Component
import javax.swing.Icon
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer

class CustomTreeCellRenderer : DefaultTreeCellRenderer() {
    override fun getTreeCellRendererComponent(
        tree: JTree?,
        value: Any?,
        selected: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean,
    ): Component {
        super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus)

        val node = value as? DefaultMutableTreeNode
        val regularIcon = ToolkitIcons.API_ICON
        val selectedIcon = ToolkitIcons.API_SELECTED_ICON

        node?.let {
            val nodeData = it.userObject as? String
            if (leaf) {
                icon = if (selected) {
                    selectedIcon
                } else {
                    regularIcon
                }
                val parts = nodeData?.split("  ", limit = 2)
                val name = parts?.getOrNull(0) ?: ""
                val title = parts?.getOrNull(1) ?: ""
                text = "<html>$name <span style='font-size:smaller; color:gray;'>$title</span></html>"
            }
            backgroundNonSelectionColor = JBColor(Color(0, 0, 0, 0), Color(0, 0, 0, 0))
        }
        return this
    }
}
