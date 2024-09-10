package com.alibabacloud.api.service

import com.alibabacloud.api.service.ApiPage.Companion.notificationService
import com.alibabacloud.api.service.completion.DataService
import com.alibabacloud.api.service.completion.UnLoadNotificationState
import com.alibabacloud.api.service.constants.NotificationGroups
import com.alibabacloud.api.service.util.FormatUtil
import com.alibabacloud.i18n.I18nUtils
import com.alibabacloud.ui.BaseToolWindow
import com.alibabacloud.ui.SearchListCellRenderer
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.ComponentPopupBuilder
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
import java.util.*
import javax.swing.BoxLayout
import javax.swing.DefaultListModel
import javax.swing.JPanel
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
                cellRenderer = SearchListCellRenderer()
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
                    .setResizable(true)

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

                        processPopup(searchResultsModel, popupBuilder, searchField)
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

        private fun processPopup(
            searchResultsModel: DefaultListModel<String>,
            popupBuilder: ComponentPopupBuilder,
            searchField: SearchTextField
        ) {
            if (searchResultsModel.size > 0) {
                if (popup == null || popup?.isDisposed == true) {
                    popup = popupBuilder.createPopup().apply {
                        size = Dimension(searchField.width, 300)
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
        }

        fun globalSearchApi(project: Project, searchField: SearchTextField) {
            val searchResultsModel = DefaultListModel<String>()
            val searchResultsList = JBList(searchResultsModel).apply {
                selectionMode = ListSelectionModel.SINGLE_SELECTION
                cellRenderer = SearchListCellRenderer()
                addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent?) {
                        e?.let {
                            if (it.clickCount == 1) {
                                val selectedValue = (selectedValue as String).split("::")
                                navigateToApiInfo(project, selectedValue[2], selectedValue[3], selectedValue[0])
                                popup?.cancel()
                            }
                        }
                    }
                })
            }

            val popupBuilder =
                JBPopupFactory.getInstance().createComponentPopupBuilder(JBScrollPane(searchResultsList), null)
                    .setRequestFocus(false)
                    .setResizable(true)

            fun updateSearchResults() {
                val searchText = searchField.text.trim()
                searchResultsModel.clear()
                if (searchText.length > 3) {
                    ApplicationManager.getApplication().executeOnPooledThread {
                        val resultList = mutableListOf<String>()
                        val regex = Regex(".*${Regex.escape(searchText)}.*", RegexOption.IGNORE_CASE)
                        if (DataService.isDataLoaded()) {
                            val javaIndex = DataService.javaIndex
                            if (javaIndex.isNotEmpty()) {
                                javaIndex.forEach { (key, value) ->
                                    if (resultList.size >= 300) {
                                        return@forEach
                                    }
                                    val apiInfo = key.split("::")
                                    if (regex.containsMatchIn(apiInfo[0])) {
                                        if (I18nUtils.getLocale() == Locale.CHINA) {
                                            resultList.add("${apiInfo[0]}::$value::${apiInfo[1]}::${apiInfo[2]}")
                                        } else {
                                            resultList.add("${apiInfo[0]}::::${apiInfo[1]}::${apiInfo[2]}")
                                        }
                                    }
                                }
                            }
                        } else {
                            showUnloadedMessage(project)
                        }

                        ApplicationManager.getApplication().invokeLater {
                            searchResultsModel.clear()
                            resultList.forEach { res -> searchResultsModel.addElement(res) }
                            processPopup(searchResultsModel, popupBuilder, searchField)
                        }
                    }
                } else if (popup?.isDisposed == false) {
                    popup?.cancel()
                    return
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

        private fun showUnloadedMessage(project: Project) {
            if (!UnLoadNotificationState.hasShown) {
                notificationService.showMessage(
                    project,
                    NotificationGroups.COMPLETION_NOTIFICATION_GROUP,
                    I18nUtils.getMsg("action.wait"),
                    I18nUtils.getMsg("global.api.search.wait"),
                    NotificationType.INFORMATION
                )
            }
            UnLoadNotificationState.hasShown = true
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
                    val result = "$productNameEn::$productName::$defaultVersion::$group"
                    searchResultsModel.addElement(result)
                }
            }
        }

        fun navigateToApi(tree: Tree, selectedResult: String) {
            val pattern = """(.*)::(.*)::(.*)""".toRegex()
            val matchResult = pattern.matchEntire(selectedResult)

            if (matchResult != null) {
                val (apiName, apiDescription, ancestors) = matchResult.destructured
                val ancestorNames = ancestors.split(" / ")
                val root = tree.model.root as DefaultMutableTreeNode
                var currentNode: DefaultMutableTreeNode? = root

                for (ancestorName in ancestorNames) {
                    currentNode = currentNode?.let { FormatUtil.findNode(it, ancestorName) }
                    if (currentNode == null) break
                }
                expandPath(currentNode, apiName, apiDescription, tree)
            }
        }

        private fun collectLeafNodes(root: TreeNode, leafNodes: MutableList<String>) {
            if (root.isLeaf) {
                val node = root as DefaultMutableTreeNode
                val ancestors = generateSequence(node.parent) { it.parent }.toList().asReversed()
                    .filterIsInstance<DefaultMutableTreeNode>().filter { it.userObject.toString() != "API LIST" }
                    .joinToString(" / ") { it.userObject.toString() }

                val nameAndDescription = node.userObject.toString().split("  ")
                val nodeText =
                    if (ancestors.isNotEmpty()) "${nameAndDescription[0]}::${nameAndDescription[1]}::$ancestors" else node.userObject.toString()
                leafNodes.add(nodeText)
            } else {
                for (i in 0 until root.childCount) {
                    collectLeafNodes(root.getChildAt(i), leafNodes)
                }
            }
        }

        private fun navigateToProduct(tree: Tree, selectedResult: String) {
            val pattern = """(.*)::(.*)::(.*)::(.*)""".toRegex()
            val matchResult = pattern.find(selectedResult)

            if (matchResult != null) {
                val (code, name, _, group) = matchResult.destructured
                val root = tree.model.root as DefaultMutableTreeNode
                val categoryNode = root.let { FormatUtil.findNode(it, group) }
                expandPath(categoryNode, name, code, tree)
            }
        }

        private fun expandPath(node: DefaultMutableTreeNode?, name: String, code: String, tree: Tree) {
            val productNode = node?.let { FormatUtil.findNode(it, "$name  $code") }
            productNode?.let {
                val path = TreePath(it.path)
                tree.selectionPath = path
                tree.expandPath(path)
                tree.scrollPathToVisible(path)
            }
        }

        internal fun navigateToApiInfo(project: Project, productName: String, version: String, apiName: String) {
            val toolWindow = BaseToolWindow().registerToolWindow(project)
            val contentManager = toolWindow.contentManager
            BaseToolWindow().setToolWindowActions(contentManager, toolWindow)

            val apiPanel = JPanel()
            apiPanel.layout = BoxLayout(apiPanel, BoxLayout.Y_AXIS)
            val apiDocContent = toolWindow.contentManager.factory.createContent(
                apiPanel,
                "$productName-$apiName",
                false,
            )
            apiDocContent.tabName = "$productName::$apiName::$version"
            contentManager.addContent(apiDocContent)
            toolWindow.show()
            ApiPage.showApiDetail(
                apiDocContent,
                contentManager,
                apiPanel,
                productName,
                apiName,
                version,
                project,
                true,
            )
        }
    }
}