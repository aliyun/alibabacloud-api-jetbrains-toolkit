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

class ProductTreeCellRenderer : DefaultTreeCellRenderer() {
    override fun getTreeCellRendererComponent(
        tree: JTree?,
        value: Any?,
        selected: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean
    ): Component {
        super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus)

        val node = value as? DefaultMutableTreeNode
        node?.let {
            val nodeData = it.userObject as? String
            if (leaf) {
                val parts = nodeData?.split("  ", limit = 2)
                val name = parts?.getOrNull(0) ?: ""
                val code = parts?.getOrNull(1) ?: ""
                text = "<html>$name <span style='font-size:smaller; color:gray;'>$code</span></html>"
                icon = ToolkitIcons.PRODUCT_ICON
            } else {
                icon = null
            }
            backgroundNonSelectionColor = JBColor(Color(0, 0, 0, 0), Color(0, 0, 0, 0))
        }
        return this
    }
}