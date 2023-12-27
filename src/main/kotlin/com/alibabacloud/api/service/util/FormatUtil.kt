package com.alibabacloud.api.service.util

import com.alibabacloud.api.service.constants.ApiConstants
import com.google.gson.JsonArray
import com.intellij.ui.components.JBScrollPane
import java.awt.Color
import java.awt.Component
import java.util.*
import javax.swing.ScrollPaneConstants
import javax.swing.UIManager
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeNode

class FormatUtil {
    companion object {
        fun getMethod(methods: JsonArray): String {
            if (methods.size() > 0) {
                for (element in methods) {
                    if (element.asString.uppercase(Locale.getDefault()) == "POST") {
                        return "POST"
                    }
                }
                return methods[0].asString.uppercase(Locale.getDefault())
            } else {
                return "GET"
            }
        }

        fun getProtocol(schemes: JsonArray): String {
            if (schemes.size() > 0) {
                for (element in schemes) {
                    if (element.asString.uppercase(Locale.getDefault()) == "HTTPS") {
                        return "HTTPS"
                    }
                }
                return "HTTP"
            } else {
                return "HTTPS"
            }
        }

        fun adjustColor(): List<String> {
            val defaults = UIManager.getDefaults()
            val backgroundColor: Color = defaults.getColor("Panel.background")
            val textColor: Color = defaults.getColor("Label.foreground")
            val backgroundCss = java.lang.String.format("#%06x", backgroundColor.rgb and 0xFFFFFF)
            val textCss = java.lang.String.format("#%06x", textColor.rgb and 0xFFFFFF)

            val darkerRed = backgroundColor.red - 20 and 0xFF
            val darkerGreen = backgroundColor.green - 20 and 0xFF
            val darkerBlue = backgroundColor.blue - 20 and 0xFF
            val darkerBackgroundCss = String.format("#%02x%02x%02x", darkerRed, darkerGreen, darkerBlue)
            val colorList = mutableListOf<String>()
            colorList.add(backgroundCss)
            colorList.add(textCss)
            colorList.add(darkerBackgroundCss)
            return colorList
        }

        private fun getEnum(enumArray: JsonArray?): String {
            val enum = enumArray?.let {
                if (it.size() > 0) {
                    it.joinToString(separator = "&nbsp", prefix = ApiConstants.PARAM_TABLE_SCHEMA_ENUM)
                        .replace("\"", "")
                } else {
                    ""
                }
            }.orEmpty()
            return enum
        }

        fun getScrollPane(component: Component): JBScrollPane {
            val scrollPane = JBScrollPane(component)
            scrollPane.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
            scrollPane.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
            return scrollPane
        }

        fun findNode(root: TreeNode, nodeName: String): DefaultMutableTreeNode? {
            if (root is DefaultMutableTreeNode && root.toString() == nodeName) {
                return root
            }
            if (root.childCount > 0) {
                for (i in 0 until root.childCount) {
                    val node = root.getChildAt(i)
                    val resultNode = findNode(node, nodeName)
                    if (resultNode != null) return resultNode
                }
            }
            return null
        }

        fun _bodyType(types: JsonArray?): String {
            if (types == null || types.isEmpty) {
                return "none"
            }
            val type = types[0].asString
            if (type == "application/json") {
                return "json"
            }
            if (type == "application/xml") {
                return "xml"
            }
            if (type == "application/x-www-form-urlencoded") {
                return "form"
            }
            if (type == "application/octet-stream") {
                return "binary"
            }
            return "none"
        }

        fun joinValueArray(valuesList: MutableList<Any>, valueArray: List<*>, separator: String): String {
            for (element in valueArray) {
                if (element != null) {
                    valuesList.add(element)
                }
            }
            return valuesList.joinToString(separator)
        }
    }
}
