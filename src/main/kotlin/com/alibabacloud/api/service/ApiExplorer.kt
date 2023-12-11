package com.alibabacloud.api.service

import com.alibabacloud.api.service.constants.ApiConstants
import com.alibabacloud.ui.CustomTreeCellRenderer
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.intellij.ui.treeStructure.Tree
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.swing.JPanel
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

class ApiExplorer {
    companion object {
        fun explorerTree(data: JsonArray, panel: JPanel): Tree {
            val root = DefaultMutableTreeNode("API LIST")
            val treeModel = DefaultTreeModel(root)
            val tree = Tree(treeModel)
            val treeRenderer = CustomTreeCellRenderer()

            for (element in data) {
                if (element is JsonObject && element.has(ApiConstants.API_DIR_RESPONSE_NAME)) {
                    val name = element.asJsonObject.get(ApiConstants.API_DIR_RESPONSE_NAME).toString().replace("\"", "")
                    val nodeTitle =
                        element.asJsonObject.get(ApiConstants.API_DIR_RESPONSE_NODE_TITLE).toString().replace("\"", "")
                    val children = element.get(ApiConstants.API_DIR_RESPONSE_CHILDREN) as JsonArray

                    if (name == "null" && children.size() == 0) {
                        continue
                    } else if (name == "null") {
                        val parentNode = DefaultMutableTreeNode(nodeTitle)
                        root.add(parentNode)
                        addChildrenToNode(children, parentNode)
                    } else {
                        val apiNode = DefaultMutableTreeNode(name)
                        root.add(apiNode)
                    }
                }
            }

            tree.cellRenderer = treeRenderer
            tree.isRootVisible = true
            tree.expandRow(0)
            tree.isRootVisible = false
            panel.add(tree)
            return tree
        }

        private fun addChildrenToNode(children: JsonArray, parent: DefaultMutableTreeNode) {
            for (element in children) {
                if (element is JsonObject && element.has(ApiConstants.API_DIR_RESPONSE_NAME)) {
                    val name = element.asJsonObject.get(ApiConstants.API_DIR_RESPONSE_NAME).toString().replace("\"", "")
                    val nodeTitle =
                        element.asJsonObject.get(ApiConstants.API_DIR_RESPONSE_NODE_TITLE).toString().replace("\"", "")

                    if (name == "null") {
                        val childNode = DefaultMutableTreeNode(nodeTitle)
                        parent.add(childNode)
                        val grandchildren = element.get(ApiConstants.API_DIR_RESPONSE_CHILDREN) as JsonArray
                        addChildrenToNode(grandchildren, childNode)
                    } else {
                        val apiNode = DefaultMutableTreeNode(name)
                        parent.add(apiNode)
                    }
                }
            }
        }

        fun apiDocContentTree(): Pair<MutableMap<String, Pair<String, String>>, Tree> {
            val root = DefaultMutableTreeNode(ApiConstants.TOOLWINDOW_PRODUCT_TREE)
            val nameAndVersionMap = mutableMapOf<String, Pair<String, String>>()
            val url = URL(ApiConstants.PRODUCT_LIST_URL)
            try {
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = ApiConstants.METHOD_GET
                connection.connect()

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonResponse = Gson().fromJson(response, JsonObject::class.java)
                    val data = jsonResponse.getAsJsonArray(ApiConstants.PRODUCT_RESP_DATA)
                    val resultMap = mutableMapOf<String, MutableMap<String, MutableList<String>>>()
                    connection.disconnect()

                    for (element in data) {
                        val category2Name = element.asJsonObject.get(ApiConstants.PRODUCT_RESP_CATEGORY_2_NAME)
                        val categoryName = element.asJsonObject.get(ApiConstants.PRODUCT_RESP_CATEGORY_NAME)
                        val showNameCn =
                            element.asJsonObject.get(ApiConstants.PRODUCT_RESP_SHOW_NAME_CN).toString()
                                .replace("\"", "")
                        val name =
                            element.asJsonObject.get(ApiConstants.PRODUCT_RESP_PRODUCT_NAME).toString()
                                .replace("\"", "")
                        val defaultVersion =
                            element.asJsonObject.get(ApiConstants.PRODUCT_RESP_DEFAULT_VERSION).toString()
                                .replace("\"", "")

                        if (category2Name == null) {
                            val innerMap =
                                resultMap.getOrPut(ApiConstants.PRODUCT_RESP_NOT_CLASSIFIED) { mutableMapOf() }
                            val showNameList =
                                innerMap.getOrPut(ApiConstants.PRODUCT_RESP_NOT_CLASSIFIED) { mutableListOf() }
                            showNameList.add(showNameCn)
                            nameAndVersionMap[showNameCn] = Pair(name, defaultVersion)
                        } else {
                            val innerMap =
                                resultMap.getOrPut(category2Name.toString().replace("\"", "")) { mutableMapOf() }
                            val showNameList =
                                innerMap.getOrPut(categoryName.toString().replace("\"", "")) { mutableListOf() }
                            showNameList.add(showNameCn)
                            nameAndVersionMap[showNameCn] = Pair(name, defaultVersion)
                        }
                    }

                    for ((category2Name, innerMap) in resultMap) {
                        val category2NameNode = DefaultMutableTreeNode(category2Name)
                        root.add(category2NameNode)

                        for ((categoryName, showNameList) in innerMap) {
                            val categoryNode = DefaultMutableTreeNode(categoryName)
                            category2NameNode.add(categoryNode)

                            for (showNameCn in showNameList) {
                                val showNameNode = DefaultMutableTreeNode(showNameCn)
                                categoryNode.add(showNameNode)
                            }
                        }
                    }
                }
            } catch (_: IOException) {
            }

            val treeModel = DefaultTreeModel(root)
            return Pair(nameAndVersionMap, Tree(treeModel))
        }
    }
}
