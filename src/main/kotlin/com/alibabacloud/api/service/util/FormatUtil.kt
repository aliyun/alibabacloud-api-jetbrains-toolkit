package com.alibabacloud.api.service.util

import com.alibabacloud.api.service.constants.ApiConstants
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.intellij.ui.components.JBScrollPane
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import java.awt.Color
import java.awt.Component
import javax.swing.ScrollPaneConstants
import javax.swing.UIManager
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeNode

class FormatUtil {
    companion object {
        fun parseMdToHtml(mdStr: String): String {
            val parser = Parser.builder().build()
            val renderer = HtmlRenderer.builder().build()
            val mdDocument = parser.parse(mdStr)
            return renderer.render(mdDocument)
        }

        fun editDescription(description: String): String {
            var editDescription = description.trim()
            if (!editDescription.trim().startsWith(ApiConstants.COMPLETE_API_DESC)) {
                editDescription = "${ApiConstants.COMPLETE_API_DESC}\n$editDescription"
            }
            return editProps(editDescription, "desc")
        }

        fun editProps(str: String?, descOrTable: String): String {
            if (str != null) {
                val editStr = str.trim()
                val regex1 = Regex("\n<props=\"intl\">\n(([\\s\\S]*?))\n</props>\n")
                val regex2 = Regex("\n<props=\"partner\">\n(([\\s\\S]*?))\n</props>\n")
                return if (descOrTable == "desc") {
                    editStr.replace(regex1, "").replace(regex2, "")
                        .replace("\n<props=\"china\">", "")
                        .replace("</props>\n", "")
                } else {
                    editStr.replace(regex1, "").replace(regex2, "").replace("\n\n", "\n")
                        .replace("\n<props=\"china\">", "")
                        .replace("</props>\n", "")
                }
            }
            return ""
        }

        fun editHtml(htmlText: String): String {
            val editHtml = htmlText.trim()
            val regex = Regex("\\[([\\s\\S]*?)\\]\\(([\\s\\S]*?)\\)")

            val result = regex.replace(editHtml) {
                val linkText = it.groupValues[1]
                val link = it.groupValues[2]
                "<a href=\"$link\">$linkText</a>"
            }
            return result
        }

        fun getMethod(methods: String): String {
            val methodArray = if (methods.contains("|")) {
                methods.split("|")
            } else if (methods.contains(",")) {
                methods.split("|")
            } else {
                return methods
            }
            return if (methodArray.contains("POST")) "POST" else methodArray[0]
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

        fun regexHref(htmlText: String): String {
            return htmlText.replace("""~~(\d+)~~""".toRegex()) { match -> """${ApiConstants.DOC_DETAIL_URL}/${match.groupValues[1]}.html" onclick="openInExternalBrowser(event)""" }
                .replace("""href="([^"]*)"""".toRegex()) { """href="${it.groups[1]?.value}" onclick="openInExternalBrowser(event)"""" }
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

        fun getRefTypeAndEnum(
            type: String,
            ref: String,
            refSchema: JsonObject,
            enumArray: JsonArray?,
        ): Pair<String, String> {
            var refType = String()
            var enum = getEnum(enumArray)
            if (type == "" && ref !== "") {
                val refStr = ref.split('/').last()
                if (refSchema.size() == 0) {
                    refType = "any"
                } else {
                    val refContent = refSchema.get(refStr).asJsonObject
                    refType = refContent.get(ApiConstants.API_DOCS_REF_TYPE).asString
                    enum = getEnum(refContent.get(ApiConstants.API_DOCS_REF_ENUM)?.asJsonArray)
                }
            }
            return Pair(refType, enum)
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

    }
}
