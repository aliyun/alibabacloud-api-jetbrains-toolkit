package com.alibabacloud.api.service

import com.alibabacloud.api.service.constants.ApiConstants
import com.alibabacloud.api.service.constants.NotificationGroups
import com.alibabacloud.api.service.notification.NormalNotification
import com.alibabacloud.api.service.util.CacheUtil
import com.alibabacloud.api.service.util.RequestUtil
import com.alibabacloud.i18n.I18nUtils
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
import java.util.*
import javax.swing.JPanel
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel


class ApiExplorer {
    companion object {
        fun explorerTree(data: JsonArray, panel: JPanel): Tree {
            val searchField = SearchTextField()
            searchField.textEditor.emptyText.text = I18nUtils.getMsg("toolwindow.search.api")
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
                                "$name  ${I18nUtils.getMsg("description.not.exist")}$isDeprecated"
                            }
                        val apiNode = DefaultMutableTreeNode(nodeText)
                        parent.add(apiNode)
                    }
                } else if (element is JsonObject && !element.has(ApiConstants.API_DIR_RESPONSE_NAME) && element.get(
                        ApiConstants.API_DIR_RESPONSE_DIR_ID
                    ).asInt == 0 && (element.get(ApiConstants.API_DIR_RESPONSE_TITLE).asString == "其它" || element.get(
                        ApiConstants.API_DIR_RESPONSE_TITLE
                    ).asString == "Others") && element.get(
                        ApiConstants.API_DIR_RESPONSE_CHILDREN
                    ).asJsonArray.size() > 0
                ) {
                    val titleNode = DefaultMutableTreeNode(I18nUtils.getMsg("node.others"))
                    parent.add(titleNode)
                    val children = element.get(ApiConstants.API_DIR_RESPONSE_CHILDREN) as JsonArray
                    addNode(children, titleNode)
                }
            }
        }

        fun apiDocContentTree(project: Project): Pair<MutableMap<String, List<String>>, Tree> {
            var root = DefaultMutableTreeNode(I18nUtils.getMsg("toolwindow.product.tree"))
            val treeModel = DefaultTreeModel(root)
            var nameAndVersionMap = mutableMapOf<String, List<String>>()
            try {
                val lang = if (I18nUtils.getLocale() == Locale.CHINA) "" else "?language=en_US"
                val preferLocale = if (lang == "") "" else "-en"
                val request = RequestUtil.createRequest("https://api.aliyun.com/meta/v1/products.json$lang")
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
                            val cacheTreeFile = File(ApiConstants.CACHE_PATH, "tree1$preferLocale")
                            CacheUtil.cleanExceedCache()
                            val nameAndVersionFile = if (I18nUtils.getLocale() == Locale.CHINA) {
                                File(ApiConstants.CACHE_PATH, "nameAndVersion1")
                            } else {
                                File(ApiConstants.CACHE_PATH, "nameAndVersion1-en")
                            }
                            try {
                                CacheUtil.writeMapCache(nameAndVersionFile, nameAndVersionMap)
                                CacheUtil.writeTreeCache(cacheTreeFile, Tree(treeModel))
                            } catch (e: IOException) {
                                nameAndVersionFile.delete()
                                cacheTreeFile.delete()
                                NormalNotification.showMessage(
                                    project,
                                    NotificationGroups.CACHE_NOTIFICATION_GROUP,
                                    I18nUtils.getMsg("cache.write.fail"),
                                    "",
                                    NotificationType.ERROR
                                )
                            }
                        }
                    } else {
                        NormalNotification.showMessage(
                            project,
                            NotificationGroups.NETWORK_NOTIFICATION_GROUP,
                            I18nUtils.getMsg("product.list.fetch.fail"),
                            "${I18nUtils.getMsg("request.fail.error.code")} ${response.code}, ${I18nUtils.getMsg("request.fail.error.message")} ${response.message}",
                            NotificationType.ERROR
                        )
                    }
                }
            } catch (_: IOException) {
                NormalNotification.showMessage(
                    project,
                    NotificationGroups.NETWORK_NOTIFICATION_GROUP,
                    I18nUtils.getMsg("product.list.fetch.fail"),
                    I18nUtils.getMsg("network.check"),
                    NotificationType.ERROR
                )
            }

            return Pair(nameAndVersionMap, Tree(treeModel))
        }

        private fun sortTreeNodes(node: DefaultMutableTreeNode) {
            val children = Collections.list(node.children())
            children.sortWith { node1, node2 ->
                val node1Mutable = node1 as DefaultMutableTreeNode
                val node2Mutable = node2 as DefaultMutableTreeNode
                val node1Name = node1Mutable.userObject.toString()
                val node2Name = node2Mutable.userObject.toString()

                val isNode1Other = node1Name == I18nUtils.getMsg("node.others")
                val isNode2Other = node2Name == I18nUtils.getMsg("node.others")

                return@sortWith when {
                    isNode1Other && isNode2Other -> 0
                    isNode1Other -> 1
                    isNode2Other -> -1
                    else -> node1Name.compareTo(node2Name)
                }
            }
            node.removeAllChildren()
            for (child in children) {
                node.add(child as DefaultMutableTreeNode)
            }
            for (child in children) {
                sortTreeNodes(child as DefaultMutableTreeNode)
            }
        }

        private fun buildProductTree(
            data: JsonArray,
            root: DefaultMutableTreeNode,
            nameAndVersionMap: MutableMap<String, List<String>>
        ): Pair<DefaultMutableTreeNode, MutableMap<String, List<String>>> {
            val groupMap = mutableMapOf<String, DefaultMutableTreeNode>()
            if (data.size() > 0) {
                for (product in data) {
                    val groupElement = product.asJsonObject.get("category2Name")
                    val group =
                        if (groupElement == null || groupElement.isJsonNull) I18nUtils.getMsg("node.others") else groupElement.asString
                    val code = product.asJsonObject.get("code").asString
                    val nameElement = product.asJsonObject.get("name")
                    val name = if (nameElement == null || nameElement.isJsonNull) "--" else nameElement.asString.trim()
                    val defaultVersion = product.asJsonObject.get("defaultVersion").asString.trim()
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
            sortTreeNodes(root)
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
                            I18nUtils.getMsg("api.list.fetch.fail"),
                            "${I18nUtils.getMsg("request.fail.error.code")} ${response.code}, ${I18nUtils.getMsg("request.fail.error.message")} ${response.message}",
                            NotificationType.ERROR
                        )
                    }
                }
            } catch (_: IOException) {
                NormalNotification.showMessage(
                    project,
                    NotificationGroups.NETWORK_NOTIFICATION_GROUP,
                    I18nUtils.getMsg("api.list.fetch.fail"),
                    I18nUtils.getMsg("network.check"),
                    NotificationType.ERROR
                )
            }
            return data
        }
    }
}