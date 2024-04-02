package com.alibabacloud.api.service

import com.alibabacloud.api.service.util.FormatUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.SearchTextField
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import java.awt.Dimension
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.DefaultListModel
import javax.swing.ListSelectionModel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath


class SearchHelper {
    companion object {
        private var popup: JBPopup? = null
        fun search(
            nameAndVersionMap: MutableMap<String, List<String>>?,
            tree: Tree,
            searchField: SearchTextField,
        ) {
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
                                popup?.cancel()
                            }
                        }
                    }
                })
            }

            val popupBuilder =
                JBPopupFactory.getInstance().createComponentPopupBuilder(JBScrollPane(searchResultsList), null)
                    .setRequestFocus(false)

            val updateSearchResults = {
                ApplicationManager.getApplication().invokeLater {
                    val searchText = searchField.text.trim()
                    searchResultsModel.clear()

                    if (searchText.isNotEmpty()) {
                        if (nameAndVersionMap == null) {
                            getSearchApiElement(tree, searchText, searchResultsModel)
                        } else {
                            getSearchProductElement(searchText, nameAndVersionMap, searchResultsModel)
                        }

                        if (searchResultsModel.size > 0) {
                            if (popup == null || popup?.isDisposed == true) {
                                popup = popupBuilder.createPopup().apply {
                                    size = Dimension(searchField.width, 100)
                                }
                            }
                            if (popup?.isVisible != true) {
                                val locationOnScreen = searchField.locationOnScreen
                                val pointToShow = Point(locationOnScreen.x, locationOnScreen.y + searchField.height)
                                popup?.show(RelativePoint(pointToShow))
                            }
                        } else if (popup?.isDisposed == false) {
                            popup?.cancel()
                        }
                    } else if (popup?.isDisposed == false) {
                        popup?.cancel()
                    }
                }
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
                val productNameEn = nodePaths[0]
                if ((productName.contains(searchText, ignoreCase = true) || productNameEn.contains(
                        searchText, ignoreCase = true
                    ))
                ) {
                    val group = nodePaths[1]
                    val defaultVersion = nodePaths[2]
                    val result = " [$productNameEn] $productName（推荐版本: $defaultVersion / $group）"
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
                val ancestors = generateSequence(node.parent) { it.parent }.toList().asReversed()
                    .filterIsInstance<DefaultMutableTreeNode>().filter { it.userObject.toString() != "API LIST" }
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
            val pattern = """ \[(.*)] (.*)（推荐版本: (.*) / (.*)）""".toRegex()
            val matchResult = pattern.find(selectedResult)

            if (matchResult != null) {
                val (code, name, _, group) = matchResult.destructured
                val root = tree.model.root as DefaultMutableTreeNode
                val categoryNode = root.let { FormatUtil.findNode(it, group) }
                val productNode = categoryNode?.let { FormatUtil.findNode(it, "$name  $code") }
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