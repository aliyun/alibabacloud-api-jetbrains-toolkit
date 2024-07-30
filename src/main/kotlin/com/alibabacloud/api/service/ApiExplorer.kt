package com.alibabacloud.api.service

import com.alibabacloud.api.service.completion.cacheNameAndVersionFile
import com.alibabacloud.api.service.constants.ApiConstants
import com.alibabacloud.api.service.constants.NotificationGroups
import com.alibabacloud.api.service.notification.NormalNotification
import com.alibabacloud.api.service.util.CacheUtil
import com.alibabacloud.api.service.util.RequestUtil
import com.alibabacloud.ui.CustomTreeCellRenderer
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.ui.SearchTextField
import com.intellij.ui.treeStructure.Tree
import java.awt.BorderLayout
import java.awt.Dimension
import java.io.File
import java.io.IOException
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

            addNode(data, root)

            tree.cellRenderer = treeRenderer
            tree.isRootVisible = true
            tree.expandRow(0)
            tree.isRootVisible = false
            panel.add(tree, BorderLayout.CENTER)
            SearchHelper.search(null, tree, searchField)
            return tree
        }

        private fun addNode(data: JsonArray, parent: DefaultMutableTreeNode) {
            for (element in data) {
                if (element is JsonObject && element.has(ApiConstants.API_DIR_RESPONSE_NAME)) {
                    val name = element.get(ApiConstants.API_DIR_RESPONSE_NAME).toString().replace("\"", "")
                    val nodeTitle = element.get(ApiConstants.API_DIR_RESPONSE_NODE_TITLE)?.toString()?.replace("\"", "")
                    var children = element.get(ApiConstants.API_DIR_RESPONSE_CHILDREN)?.asJsonArray
                    if (name == "null" && children != null && children.size() == 0) {
                        continue
                    } else if (name == "null" && nodeTitle !== null) {
                        if (nodeTitle != "") {
                            val titleNode = DefaultMutableTreeNode(nodeTitle)
                            parent.add(titleNode)
                            children = element.get(ApiConstants.API_DIR_RESPONSE_CHILDREN) as JsonArray
                            addNode(children, titleNode)
                        } else {
                            children = element.get(ApiConstants.API_DIR_RESPONSE_CHILDREN) as JsonArray
                            addNode(children, parent)
                        }
                    } else {
                        val isDeprecated = if ((element.get("deprecated")?.asInt ?: 0) == 1) " [Deprecated]" else ""
                        val nodeText =
                            if (element.has(ApiConstants.API_DIR_RESPONSE_TITLE) && !element.get(ApiConstants.API_DIR_RESPONSE_TITLE).isJsonNull) {
                                "$name  ${element.get(ApiConstants.API_DIR_RESPONSE_TITLE).asString}$isDeprecated"
                            } else {
                                "$name  暂无描述$isDeprecated"
                            }
                        val apiNode = DefaultMutableTreeNode(nodeText)
                        parent.add(apiNode)
                    }
                } else if (element is JsonObject && !element.has(ApiConstants.API_DIR_RESPONSE_NAME) && element.get(
                        ApiConstants.API_DIR_RESPONSE_DIR_ID
                    ).asInt == 0 && (element.get(ApiConstants.API_DIR_RESPONSE_TITLE).asString == "其它" || element.get(ApiConstants.API_DIR_RESPONSE_TITLE).asString == "Others") && element.get(
                        ApiConstants.API_DIR_RESPONSE_CHILDREN
                    ).asJsonArray.size() > 0
                ) {
                    val titleNode = DefaultMutableTreeNode("其他")
                    parent.add(titleNode)
                    val children = element.get(ApiConstants.API_DIR_RESPONSE_CHILDREN) as JsonArray
                    addNode(children, titleNode)
                }
            }
        }

        fun apiDocContentTree(project: Project): Pair<MutableMap<String, List<String>>, Tree> {
            var root = DefaultMutableTreeNode(ApiConstants.TOOLWINDOW_PRODUCT_TREE)
            val treeModel = DefaultTreeModel(root)
            var nameAndVersionMap = mutableMapOf<String, List<String>>()
            try {
                val request = RequestUtil.createRequest("https://api.aliyun.com/meta/v1/products.json")
                var data: JsonArray

                OkHttpClientProvider.instance.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        if (responseBody != null) {
                            data = Gson().fromJson(responseBody, JsonArray::class.java)
                            val buildResult = buildProductTree(data, root, nameAndVersionMap)
                            root = buildResult.first
                            nameAndVersionMap = buildResult.second

                            val cacheDir = File(ApiConstants.CACHE_PATH)
                            if (!cacheDir.exists()) {
                                cacheDir.mkdir()
                            }
                            val cacheTreeFile = File(ApiConstants.CACHE_PATH, "tree1")
                            CacheUtil.cleanExceedCache()
                            try {
                                CacheUtil.writeMapCache(cacheNameAndVersionFile, nameAndVersionMap)
                                CacheUtil.writeTreeCache(cacheTreeFile, Tree(treeModel))
                            } catch (e: IOException) {
                                cacheNameAndVersionFile.delete()
                                cacheTreeFile.delete()
                                NormalNotification.showMessage(
                                    project,
                                    NotificationGroups.CACHE_NOTIFICATION_GROUP,
                                    "缓存写入失败",
                                    "",
                                    NotificationType.ERROR
                                )
                            }
                        }
                    } else {
                        NormalNotification.showMessage(
                            project,
                            NotificationGroups.NETWORK_NOTIFICATION_GROUP,
                            "拉取产品列表失败",
                            "网络请求失败，错误码 ${response.code}, 错误信息 ${response.message}",
                            NotificationType.ERROR
                        )
                    }
                }
            } catch (_: IOException) {
                NormalNotification.showMessage(
                    project,
                    NotificationGroups.NETWORK_NOTIFICATION_GROUP,
                    "拉取产品列表失败",
                    "请检查网络",
                    NotificationType.ERROR
                )
            }

            return Pair(nameAndVersionMap, Tree(treeModel))
        }

        private fun buildProductTree(
            data: JsonArray,
            root: DefaultMutableTreeNode,
            nameAndVersionMap: MutableMap<String, List<String>>
        ): Pair<DefaultMutableTreeNode, MutableMap<String, List<String>>> {
            val groupMap = mutableMapOf<String, DefaultMutableTreeNode>()
            if (data.size() > 0) {
                for (product in data) {
                    val group = product.asJsonObject.get("group").asString
                    val code = product.asJsonObject.get("code").asString
                    val name = product.asJsonObject.get("name").asString
                    val defaultVersion = product.asJsonObject.get("defaultVersion").asString
                    val groupNode = groupMap.getOrPut(group) {
                        val node = DefaultMutableTreeNode(group)
                        root.add(node)
                        node
                    }
                    val productNode = DefaultMutableTreeNode("$name  $code")
                    groupNode.add(productNode)
                    val list = mutableListOf<String>()
                    list.add(code)
                    list.add(group)
                    list.add(defaultVersion)
                    nameAndVersionMap[name] = list
                }
            }
            return Pair(root, nameAndVersionMap)
        }

        fun getApiListRequest(project: Project, url: String): JsonArray {
            var data = JsonArray()
            try {
                val request = RequestUtil.createRequest(url)
                OkHttpClientProvider.instance.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        if (responseBody != null) {
                            val jsonResponse = Gson().fromJson(responseBody, JsonObject::class.java)
                            data = jsonResponse.getAsJsonArray(ApiConstants.API_DIR_DATA)
                        }
                    } else {
                        NormalNotification.showMessage(
                            project,
                            NotificationGroups.NETWORK_NOTIFICATION_GROUP,
                            "拉取 API 列表失败",
                            "网络请求失败，错误码 ${response.code}, 错误信息 ${response.message}",
                            NotificationType.ERROR
                        )
                    }
                }
            } catch (_: IOException) {
                NormalNotification.showMessage(
                    project,
                    NotificationGroups.NETWORK_NOTIFICATION_GROUP,
                    "拉取 API 列表失败",
                    "请检查网络",
                    NotificationType.ERROR
                )
            }
            return data
        }
    }
}