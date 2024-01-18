package com.alibabacloud.api.service

import com.alibabacloud.api.service.constants.ApiConstants
import com.alibabacloud.api.service.util.CacheUtil
import com.alibabacloud.ui.CustomTreeCellRenderer
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.intellij.ui.SearchTextField
import com.intellij.ui.treeStructure.Tree
import okhttp3.Request
import java.awt.BorderLayout
import java.awt.Dimension
import java.io.File
import java.io.IOException
import java.net.URL
import javax.swing.JPanel
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

class ApiExplorer {
    companion object {
        fun explorerTree(data: JsonArray, panel: JPanel): Tree {
            val searchField = SearchTextField()
            searchField.textEditor.emptyText.text = "搜索 API："
            searchField.maximumSize = Dimension(Integer.MAX_VALUE, 50)
            panel.add(searchField, BorderLayout.NORTH)

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
            panel.add(tree, BorderLayout.CENTER)
            SearchHelper.search(null, tree, searchField, panel)
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

        fun apiDocContentTree(): Pair<MutableMap<String, List<String>>, Tree> {
            val root = DefaultMutableTreeNode(ApiConstants.TOOLWINDOW_PRODUCT_TREE)
            val nameAndVersionMap = mutableMapOf<String, List<String>>()
            val url = URL(ApiConstants.PRODUCT_LIST_URL)
            try {
                val request = Request.Builder().url(url).build()

                val resultMap = mutableMapOf<String, MutableMap<String, MutableList<String>>>()
                var data = JsonArray()
                OkHttpClientProvider.instance.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        if (responseBody != null) {
                            val jsonResponse = Gson().fromJson(responseBody, JsonObject::class.java)
                            data = jsonResponse.getAsJsonArray(ApiConstants.PRODUCT_RESP_DATA)
                        }
                    }
                }

                if (data.size() > 0) {
                    for (element in data) {
                        val category2Name =
                            element.asJsonObject.get(ApiConstants.PRODUCT_RESP_CATEGORY_2_NAME)?.asString ?: ""
                        val categoryName =
                            element.asJsonObject.get(ApiConstants.PRODUCT_RESP_CATEGORY_NAME)?.asString ?: ""
                        val showNameCn =
                            element.asJsonObject.get(ApiConstants.PRODUCT_RESP_SHOW_NAME_CN)?.asString ?: ""
                        val name = element.asJsonObject.get(ApiConstants.PRODUCT_RESP_PRODUCT_NAME)?.asString ?: ""
                        val defaultVersion =
                            element.asJsonObject.get(ApiConstants.PRODUCT_RESP_DEFAULT_VERSION)?.asString ?: ""

                        if (category2Name == "") {
                            val innerMap = resultMap.getOrPut("其他") { mutableMapOf() }
                            val showNameList =
                                innerMap.getOrPut(ApiConstants.PRODUCT_RESP_NOT_CLASSIFIED) { mutableListOf() }
                            showNameList.add(showNameCn)
                            val list = mutableListOf<String>()
                            list.add(name)
                            list.add("其他")
                            list.add("未分类")
                            list.add(defaultVersion)
                            nameAndVersionMap[showNameCn] = list
                        } else {
                            val innerMap = resultMap.getOrPut(category2Name) { mutableMapOf() }
                            val showNameList = innerMap.getOrPut(categoryName) { mutableListOf() }
                            showNameList.add(showNameCn)
                            val list = mutableListOf<String>()
                            list.add(name)
                            list.add(category2Name)
                            list.add(categoryName)
                            list.add(defaultVersion)
                            nameAndVersionMap[showNameCn] = list
                        }
                    }

                    val sortedEntries = resultMap.entries.sortedBy { (category2Name, _) ->
                        if (category2Name == "其他") Int.MAX_VALUE else 0
                    }

                    for ((category2Name, innerMap) in sortedEntries) {
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

            val cacheDir = File(ApiConstants.CACHE_PATH)
            if (!cacheDir.exists()) {
                cacheDir.mkdir()
            }
            val cacheNameAndVersionFile = File(ApiConstants.CACHE_PATH, "nameAndVersion")
            val cacheTreeFile = File(ApiConstants.CACHE_PATH, "tree")
            CacheUtil.cleanExceedCache()
            try {
                CacheUtil.writeMapCache(cacheNameAndVersionFile, nameAndVersionMap)
                CacheUtil.writeTreeCache(cacheTreeFile, Tree(treeModel))
            } catch (e: IOException) {
                cacheNameAndVersionFile.delete()
                cacheTreeFile.delete()
            }

            return Pair(nameAndVersionMap, Tree(treeModel))
        }

        fun getApiListRequest(url: URL): JsonArray {
            var data = JsonArray()
            try {
                val request = Request.Builder().url(url).build()
                OkHttpClientProvider.instance.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        if (responseBody != null) {
                            val jsonResponse = Gson().fromJson(responseBody, JsonObject::class.java)
                            data = jsonResponse.getAsJsonArray(ApiConstants.API_DIR_DATA)
                        }
                    }
                }
            } catch (_: IOException) {
            }
            return data
        }
    }
}