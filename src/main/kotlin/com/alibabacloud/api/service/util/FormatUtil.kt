package com.alibabacloud.api.service.util

import com.aliyun.teautil.MapTypeAdapter
import com.google.common.reflect.TypeToken
import com.google.gson.GsonBuilder
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
            val darker = backgroundColor.darker()
            val brighter = backgroundColor.brighter()
            val textColor: Color = defaults.getColor("Label.foreground")
            val backgroundCss = String.format("#%06x", backgroundColor.rgb and 0xFFFFFF)
            val textCss = String.format("#%06x", textColor.rgb and 0xFFFFFF)
            val darkerCss = String.format("#%06x", darker.rgb and 0xFFFFFF)
            val brighterCss = String.format("#%06x", brighter.rgb and 0xFFFFFF)

            val colorList = mutableListOf<String>()
            colorList.add(backgroundCss)
            colorList.add(textCss)
            colorList.add(darkerCss)
            colorList.add(brighterCss)
            return colorList
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

        fun getArg(arg: String?): Pair<Map<String, Any>?, String> {
            val argsType = object : TypeToken<Map<String, Any>>() {}.type
            val gson = GsonBuilder()
                .registerTypeAdapter(object : TypeToken<Map<String?, Any?>?>() {}.type, MapTypeAdapter())
                .create()
            val params: Map<String, Any> = gson.fromJson(arg, argsType)
            val paramsValue = params["paramsValue"] as Map<String, Any>
            val regionId = params["regionId"].toString()
            return Pair(paramsValue, regionId)
        }
    }
}
