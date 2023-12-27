package com.alibabacloud.ui

import com.alibabacloud.api.service.ApiExplorer
import com.alibabacloud.api.service.ProcessMeta
import com.alibabacloud.api.service.constants.ApiConstants
import com.alibabacloud.api.service.util.FormatUtil
import com.alibabacloud.credentials.util.ConfigFileUtil
import com.alibabacloud.models.credentials.ConfigureFile
import com.google.gson.JsonArray
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.wm.*
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentManager
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import com.intellij.ui.treeStructure.Tree
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Point
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.net.URL
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

class BaseToolWindow : ToolWindowFactory, DumbAware {
    private lateinit var comboBox: JComboBox<String>

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val refreshLeftToolWindowAction = RefreshLeftToolWindowAction()
        val refreshAction = listOf(refreshLeftToolWindowAction)
        refreshLeftToolWindowAction.templatePresentation.icon = AllIcons.Actions.Refresh
        refreshLeftToolWindowAction.templatePresentation.text = "Refresh Product List"
        toolWindow.setTitleActions(refreshAction)

        val contentPanel = JPanel()
        contentPanel.layout = BoxLayout(contentPanel, BoxLayout.Y_AXIS)
        val listRenderer = CustomListCellRenderer()

        comboBox = ComboBox()
        comboBox.renderer = listRenderer
        comboBox.maximumSize = Dimension(Integer.MAX_VALUE, 50)
        credentialsContentListener(project)
        contentPanel.add(comboBox)
        val searchField = JTextField("搜索产品：")
        searchField.maximumSize = Dimension(Integer.MAX_VALUE, 50)
        contentPanel.add(searchField)

        val (nameAndVersionMap, apiDocContentTree) = ApiExplorer.apiDocContentTree()
        val scrollPane = FormatUtil.getScrollPane(apiDocContentTree)
        contentPanel.add(scrollPane)
        productClickListener(project, apiDocContentTree, nameAndVersionMap)
        searchProduct(nameAndVersionMap, apiDocContentTree, searchField, contentPanel)

        toolWindow.contentManager.apply {
            val content = factory.createContent(contentPanel, null, false)
            toolWindow.contentManager.addContent(content)
        }
    }

    private fun credentialsContentListener(project: Project) {
        ConfigFileUtil.readProfilesFromConfigFile(comboBox)

        comboBox.addActionListener {
            val selectedProfile = comboBox.selectedItem as String
            if (selectedProfile == "Edit Profile") {
                if (!File(ConfigureFile.getDefaultPath()).exists()) {
                    ConfigureFile.saveConfigureFile(ConfigureFile.loadConfigureFile())
                }
                val virtualFile =
                    LocalFileSystem.getInstance().refreshAndFindFileByIoFile(File(ConfigureFile.getDefaultPath()))
                OpenFileDescriptor(project, virtualFile!!).navigate(true)
            } else {
                val config = ConfigureFile.loadConfigureFile()
                config.current = selectedProfile
                ConfigureFile.saveConfigureFile(config)
            }
            val config = ConfigureFile.loadConfigureFile()
            val statusBar = WindowManager.getInstance().getStatusBar(project)
            val statusBarWidget = statusBar?.getWidget("Alibaba Cloud Widget")
            if (statusBarWidget is MyStatusBarWidgetFactory.MyStatusBarWidget) {
                statusBarWidget.updateStatusBarText(config.current)
            }
            ConfigFileUtil.subscribeToFileChangeEvent(project, comboBox)
        }
    }

    private fun productClickListener(
        project: Project,
        tree: JTree,
        nameAndVersionMap: MutableMap<String, List<String>>,
    ) {
        val selectionModel = tree.selectionModel
        val apiPanel = JPanel()
        apiPanel.layout = BoxLayout(apiPanel, BoxLayout.Y_AXIS)
        val scrollPane = JBScrollPane()
        scrollPane.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
        scrollPane.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
        apiPanel.add(scrollPane)

        var apiDocContent: Content? = null
        var apiName: String

        selectionModel.addTreeSelectionListener {
            val selectedNode = tree.lastSelectedPathComponent as? DefaultMutableTreeNode
            if (selectedNode != null) {
                if (selectedNode.isLeaf) {
                    val productNameCN = selectedNode.userObject as String
                    val productName = nameAndVersionMap[productNameCN]?.get(0) ?: ""
                    val defaultVersion = nameAndVersionMap[productNameCN]?.get(3) ?: ""
                    val apiUrl = URL("${ApiConstants.API_DIR_URL}?product=$productName&version=$defaultVersion")
                    val apiData = ProcessMeta.getApiListRequest(apiUrl)

                    val myIcon: Icon = IconLoader.getIcon("/icons/toolwindow.svg", javaClass)
                    val existToolWindow =
                        ToolWindowManager.getInstance(project).getToolWindow(ApiConstants.TOOLWINDOW_APIS)
                    val toolWindow: ToolWindow
                    if (existToolWindow == null) {
                        val builder: RegisterToolWindowTaskBuilder.() -> Unit = {
                            icon = myIcon
                            anchor = ToolWindowAnchor.RIGHT
                            canCloseContent = true
                        }
                        toolWindow = ToolWindowManager.getInstance(project)
                            .registerToolWindow(ApiConstants.TOOLWINDOW_APIS, builder)
                        toolWindow.contentManager.removeAllContents(true)
                    } else {
                        toolWindow = existToolWindow
                        toolWindow.contentManager.removeAllContents(true)
                    }

                    val contentManager = toolWindow.contentManager
                    val (selectionApi, apiTree) = openWebToolWindow(apiData, toolWindow)

                    selectionApi.addTreeSelectionListener {
                        val selectedApi = apiTree.lastSelectedPathComponent as? DefaultMutableTreeNode
                        if (selectedApi != null) {
                            apiName = selectedApi.userObject as String
                            if (selectedApi.isLeaf) {
                                if (apiDocContent == null) {
                                    apiDocContent = toolWindow.contentManager.factory.createContent(
                                        apiPanel,
                                        "API: $apiName",
                                        false,
                                    )
                                    contentManager.addContent(apiDocContent!!)
                                } else {
                                    apiDocContent!!.displayName = "API: $apiName"
                                }
                                ProcessMeta.showApiDetail(
                                    apiDocContent!!,
                                    contentManager,
                                    apiPanel,
                                    productName,
                                    apiName,
                                    defaultVersion,
                                    project
                                )

                                val refreshRightToolWindowAction = RefreshRightToolWindowAction(
                                    apiDocContent!!,
                                    contentManager,
                                    apiPanel,
                                    productName,
                                    apiName,
                                    defaultVersion,
                                )
                                val refreshAction = listOf(refreshRightToolWindowAction)
                                refreshRightToolWindowAction.templatePresentation.icon = AllIcons.Actions.Refresh
                                refreshRightToolWindowAction.templatePresentation.text = "Refresh API Doc"
                                toolWindow.setTitleActions(refreshAction)
                            }
                        }
                    }
                    contentManager.addContentManagerListener(object : ContentManagerListener {
                        override fun contentRemoved(event: ContentManagerEvent) {
                            val content = event.content
                            if (content === apiDocContent) {
                                apiDocContent = null
                            }
                        }
                    })
                }
            }
        }
    }

    private fun openWebToolWindow(
        apiData: JsonArray,
        toolWindow: ToolWindow,
    ): Pair<TreeSelectionModel, Tree> {
        val icon = IconLoader.getIcon("/icons/toolwindow.svg", BaseToolWindow::class.java)
        toolWindow.setIcon(icon)

        val contentManager = toolWindow.contentManager
        var content = contentManager.getContent(0)
        val tree: Tree

        if (content != null && content.component is JScrollPane) {
            val scrollPane = content.component as JScrollPane
            val panel = scrollPane.viewport.view as JPanel

            panel.removeAll()
            tree = ApiExplorer.explorerTree(apiData, panel)
            panel.revalidate()
            panel.repaint()
            contentManager.setSelectedContent(content, true)
        } else {
            val panel = JPanel(BorderLayout())
            tree = ApiExplorer.explorerTree(apiData, panel)

            val scrollPane = FormatUtil.getScrollPane(panel)
            content =
                toolWindow.contentManager.factory.createContent(scrollPane, ApiConstants.TOOLWINDOW_API_TREE, false)
            contentManager.addContent(content)
            contentManager.setSelectedContent(content, true)
        }
        toolWindow.show()
        val selectionModel = tree.selectionModel
        return Pair(selectionModel, tree)
    }

    private fun searchProduct(
        nameAndVersionMap: MutableMap<String, List<String>>,
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
                            navigateToProduct(tree, selectedValue)
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
                if (searchField.text == "搜索产品：") {
                    searchField.text = ""
                    searchField.foreground = textColor
                }
            }

            override fun focusLost(e: FocusEvent?) {
                if (searchField.text.isEmpty()) {
                    searchField.text = "搜索产品："
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
                        nameAndVersionMap.forEach { (productName, nodePaths) ->
                            if (productName.contains(searchText, ignoreCase = true) && nodePaths.size >= 4) {
                                val secondLevel = nodePaths[1]
                                val thirdLevel = nodePaths[2]
                                val result = "$productName（$secondLevel / $thirdLevel）"
                                searchResultsModel.addElement(result)
                            }
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

    private fun navigateToProduct(tree: Tree, selectedResult: String) {
        val pattern = "(.*)（(.*) / (.*)）".toRegex()
        val matchResult = pattern.find(selectedResult)

        if (matchResult != null) {
            val (productName, secondLevel, thirdLevel) = matchResult.destructured
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

    private class RefreshLeftToolWindowAction : AnAction() {
        override fun actionPerformed(e: AnActionEvent) {
            val project = e.project ?: return
            var nameAndVersionMap = mutableMapOf<String, List<String>>()
            var apiDocContentTree = Tree()
            ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Loading API List", true) {
                override fun run(indicator: ProgressIndicator) {
                    nameAndVersionMap = ApiExplorer.apiDocContentTree().first
                    apiDocContentTree = ApiExplorer.apiDocContentTree().second
                }

                override fun onSuccess() {
                    BaseToolWindow().productClickListener(project, apiDocContentTree, nameAndVersionMap)
                }
            })
        }
    }

    private class RefreshRightToolWindowAction(
        val apiDocContent: Content,
        val contentManager: ContentManager,
        val apiPanel: JPanel,
        val name: String,
        val apiName: String,
        val defaultVersion: String,
    ) : AnAction() {
        override fun actionPerformed(e: AnActionEvent) {
            val project = e.project ?: return
            ProcessMeta.showApiDetail(
                apiDocContent,
                contentManager,
                apiPanel,
                name,
                apiName,
                defaultVersion,
                project
            )
        }
    }
}
