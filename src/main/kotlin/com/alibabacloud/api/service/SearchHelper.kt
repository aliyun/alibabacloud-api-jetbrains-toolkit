package com.alibabacloud.api.service

import com.alibabacloud.api.service.util.FormatUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBList
import com.intellij.ui.treeStructure.Tree
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath

class SearchHelper {
    companion object {
        fun search(
            nameAndVersionMap: MutableMap<String, List<String>>?,
            tree: Tree,
            searchField: SearchTextField,
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
                                if (nameAndVersionMap == null) {
                                    navigateToApi(tree, selectedValue)
                                } else {
                                    navigateToProduct(tree, selectedValue)
                                }
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

            searchField.addDocumentListener(object : DocumentListener {
                override fun insertUpdate(e: DocumentEvent?) {
                    updateSearchResults()
                }

                override fun removeUpdate(e: DocumentEvent?) {
                    updateSearchResults()
                }

                override fun changedUpdate(e: DocumentEvent?) {
                    updateSearchResults()
                }

                private fun updateSearchResults() {
                    ApplicationManager.getApplication().invokeLater {
                        val searchText = searchField.text.trim()
                        searchResultsModel.clear()

                        if (searchText.isNotEmpty()) {
                            if (nameAndVersionMap == null) {
                                getSearchApiElement(tree, searchText, searchResultsModel)
                            } else {
                                getSearchProductElement(searchText, nameAndVersionMap, searchResultsModel)
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
            })
        }

        private fun getSearchApiElement(tree: Tree, searchText: String, searchResultsModel: DefaultListModel<String>) {
            val leafNodes = mutableListOf<String>()
            val root = tree.model.root as DefaultMutableTreeNode
            collectLeafNodes(root, leafNodes)

            val filteredNodes = leafNodes.filter { it.contains(searchText, ignoreCase = true) }
            for (nodeName in filteredNodes) {
                searchResultsModel.addElement(nodeName)
            }
        }

        private fun getSearchProductElement(
            searchText: String,
            nameAndVersionMap: MutableMap<String, List<String>>,
            searchResultsModel: DefaultListModel<String>
        ) {
            nameAndVersionMap.forEach { (productName, nodePaths) ->
                val productNameEn = nameAndVersionMap[productName]?.get(0) ?: ""
                if ((productName.contains(searchText, ignoreCase = true) || productNameEn.contains(
                        searchText,
                        ignoreCase = true
                    )) && nodePaths.size >= 4
                ) {
                    val secondLevel = nodePaths[1]
                    val thirdLevel = nodePaths[2]
                    val result = "[$productNameEn] $productName（$secondLevel / $thirdLevel）"
                    searchResultsModel.addElement(result)
                }
            }
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

                val apiNode = currentNode?.let { FormatUtil.findNode(it, nodeName) }
                apiNode?.let {
                    val path = TreePath(it.path)
                    tree.selectionPath = path
                    tree.expandPath(path)
                    tree.scrollPathToVisible(path)
                }
            }
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

        private fun navigateToProduct(tree: Tree, selectedResult: String) {

            val pattern = """\[(.*)] (.*)（(.*) / (.*)）""".toRegex()
            val matchResult = pattern.find(selectedResult)

            if (matchResult != null) {
                val (_, productName, secondLevel, thirdLevel) = matchResult.destructured
                val root = tree.model.root as DefaultMutableTreeNode
                val category2NameNode = FormatUtil.findNode(root, secondLevel)
                val categoryNameNode = category2NameNode?.let { FormatUtil.findNode(it, thirdLevel) }
                val productNode = categoryNameNode?.let { FormatUtil.findNode(it, productName) }

                productNode?.let {
                    val path = TreePath(it.path)
                    tree.selectionPath = path
                    tree.expandPath(path)
                    tree.scrollPathToVisible(path)
                }
            }
        }
    }
}