package com.alibabacloud.ui

import com.intellij.openapi.util.IconLoader
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
        val regularIcon: Icon = IconLoader.getIcon("/icons/api.svg", javaClass)
        val selectedIcon: Icon = IconLoader.getIcon("/icons/api-selected.svg", javaClass)

        node?.let {
            val nodeData = node.userObject
            if (nodeData != null) {
                text = nodeData.toString()
            }

            if (leaf) {
                icon = if (selected) {
                    selectedIcon
                } else {
                    regularIcon
                }
            }
        }
        return this
    }
}
