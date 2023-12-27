package com.alibabacloud.api.service

import com.alibabacloud.api.service.constants.ApiConstants
import com.alibabacloud.api.service.util.CacheUtil
import com.alibabacloud.api.service.util.FormatUtil
import com.alibabacloud.ui.CustomTreeCellRenderer
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.intellij.openapi.application.ApplicationManager
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBList
import com.intellij.ui.treeStructure.Tree
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Point
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath

class ApiExplorer {
    companion object {
        fun explorerTree(data: JsonArray, panel: JPanel): Tree {
            val searchField = JTextField("搜索 API：")
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
            searchApi(tree, searchField, panel)
            return tree
        }

        private fun collectLeafNodes(root: TreeNode, leafNodes: MutableList<String>) {
            if (root.isLeaf) {
                val node = root as DefaultMutableTreeNode
                val ancestors = generateSequence(node.parent) { it.parent }
                    .toList()
                    .asReversed()
                    .filterIsInstance<DefaultMutableTreeNode>()
                    .filter { it.userObject.toString() != "API LIST" }
                    .joinToString(" / ") { it.userObject.toString() }

                val nodeText =
                    if (ancestors.isNotEmpty()) "${node.userObject}（$ancestors）" else node.userObject.toString()
                leafNodes.add(nodeText)
            } else {
                for (i in 0 until root.childCount) {
                    collectLeafNodes(root.getChildAt(i), leafNodes)
                }
            }
        }

        private fun searchApi(
            tree: Tree,
            searchField: JTextField,
            contentPanel: JPanel,
        ) {
            val searchResultsWindow = JWindow(SwingUtilities.getWindowAncestor(contentPanel))
            val searchResultsModel = DefaultListModel<String>()
            val searchResultsList = JBList(searchResultsModel).apply {
                selectionMode = ListSelectionModel.SINGLE_SELECTION
                addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent?) {
                        e?.let {
                            if (it.clickCount == 1) {
                                val selectedValue = selectedValue as String
                                navigateToApi(tree, selectedValue)
                                searchResultsWindow.isVisible = false
                            }
                        }
                    }
                })
            }

            searchResultsWindow.apply {
                contentPane.add(JScrollPane(searchResultsList), BorderLayout.CENTER)
                pack()
            }

            val textColor = searchField.foreground
            searchField.addFocusListener(object : FocusAdapter() {
                override fun focusGained(e: FocusEvent?) {
                    if (searchField.text == "搜索 API：") {
                        searchField.text = ""
                        searchField.foreground = textColor
                    }
                }

                override fun focusLost(e: FocusEvent?) {
                    if (searchField.text.isEmpty()) {
                        searchField.text = "搜索 API："
                        searchField.foreground = JBColor.GRAY
                    }
                }
            })

            searchField.document.addDocumentListener(object : DocumentListener {
                override fun insertUpdate(e: DocumentEvent?) {
                    ensurePlaceholder()
                    updateSearchResults()
                }

                override fun removeUpdate(e: DocumentEvent?) {
                    ensurePlaceholder()
                    updateSearchResults()
                }

                override fun changedUpdate(e: DocumentEvent?) {
                    ensurePlaceholder()
                    updateSearchResults()
                }

                private fun updateSearchResults() {
                    ApplicationManager.getApplication().invokeLater {
                        val searchText = searchField.text.trim()
                        searchResultsModel.clear()

                        if (searchText.isNotEmpty()) {
                            val leafNodes = mutableListOf<String>()
                            val root = tree.model.root as DefaultMutableTreeNode
                            collectLeafNodes(root, leafNodes)

                            val filteredNodes = leafNodes.filter { it.contains(searchText, ignoreCase = true) }
                            for (nodeName in filteredNodes) {
                                searchResultsModel.addElement(nodeName)
                            }

                            if (!searchResultsModel.isEmpty) {
                                searchResultsWindow.size = Dimension(searchField.width, 100)
                                val locationOnScreen = searchField.locationOnScreen
                                searchResultsWindow.location =
                                    Point(locationOnScreen.x, locationOnScreen.y + searchField.height)
                                searchResultsWindow.isVisible = true
                            } else {
                                searchResultsWindow.isVisible = false
                            }
                        } else {
                            searchResultsWindow.isVisible = false
                        }
                    }
                }

                private fun ensurePlaceholder() {
                    ApplicationManager.getApplication().invokeLater {
                        if (searchField.text.isEmpty() && !searchField.isFocusOwner) {
                            searchField.text = "搜索产品："
                            searchField.foreground = JBColor.GRAY
                        }
                    }
                }
            })
        }

        fun navigateToApi(tree: Tree, selectedResult: String) {
            val pattern = "([^（]+)（([^）]+)）".toRegex()
            val matchResult = pattern.matchEntire(selectedResult)

            if (matchResult != null) {
                val (nodeName, ancestors) = matchResult.destructured
                val ancestorNames = ancestors.split(" / ")
                val root = tree.model.root as DefaultMutableTreeNode
                var currentNode: DefaultMutableTreeNode? = root

                for (ancestorName in ancestorNames) {
                    currentNode = currentNode?.let { FormatUtil.findNode(it, ancestorName) }
                    if (currentNode == null) break
                }

                val targetNode = currentNode?.let { FormatUtil.findNode(it, nodeName) }
                targetNode?.let {
                    val path = TreePath(it.path)
                    tree.selectionPath = path
                    tree.expandPath(path)
                    tree.scrollPathToVisible(path)
                }
            }
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
                        val category2Name =
                            element.asJsonObject.get(ApiConstants.PRODUCT_RESP_CATEGORY_2_NAME)?.asString ?: ""
                        val categoryName =
                            element.asJsonObject.get(ApiConstants.PRODUCT_RESP_CATEGORY_NAME)?.asString ?: ""
                        val showNameCn =
                            element.asJsonObject.get(ApiConstants.PRODUCT_RESP_SHOW_NAME_CN)?.asString ?: ""
                        val name =
                            element.asJsonObject.get(ApiConstants.PRODUCT_RESP_PRODUCT_NAME)?.asString ?: ""
                        val defaultVersion =
                            element.asJsonObject.get(ApiConstants.PRODUCT_RESP_DEFAULT_VERSION)?.asString ?: ""

                        if (category2Name == "") {
                            val innerMap = resultMap.getOrPut("其他") { mutableMapOf() }
                            val showNameList =
                                innerMap.getOrPut(ApiConstants.PRODUCT_RESP_NOT_CLASSIFIED) { mutableListOf() }
                            showNameList.add(showNameCn)
                            val list = mutableListOf<String>()
                            list.add(name)
                            list.add("未分类")
                            list.add("其他")
                            list.add(defaultVersion)
                            nameAndVersionMap[showNameCn] = list
                        } else {
                            val innerMap =
                                resultMap.getOrPut(category2Name) { mutableMapOf() }
                            val showNameList =
                                innerMap.getOrPut(categoryName) { mutableListOf() }
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
    }
}
