package com.alibabacloud.ui

import com.alibabacloud.api.service.ApiExplorer
import com.alibabacloud.api.service.ApiPage
import com.alibabacloud.api.service.SearchHelper
import com.alibabacloud.api.service.completion.cacheNameAndVersionFile
import com.alibabacloud.api.service.constants.ApiConstants
import com.alibabacloud.api.service.constants.NotificationGroups
import com.alibabacloud.api.service.notification.NormalNotification
import com.alibabacloud.api.service.util.CacheUtil
import com.alibabacloud.api.service.util.FormatUtil
import com.alibabacloud.credentials.util.ConfigFileUtil
import com.alibabacloud.models.credentials.ConfigureFile
import com.google.gson.JsonArray
import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.wm.*
import com.intellij.ui.SearchTextField
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentManager
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import com.intellij.ui.treeStructure.Tree
import java.awt.BorderLayout
import java.awt.Dimension
import java.io.File
import java.io.IOException
import javax.swing.*
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeSelectionModel

class BaseToolWindow : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentPanel = JPanel()
        contentPanel.layout = BoxLayout(contentPanel, BoxLayout.Y_AXIS)
        val listRenderer = CustomListCellRenderer()

        val comboBoxManager = ComboBoxManager.getInstance(project)
        val comboBox = comboBoxManager.comboBox
        comboBox.renderer = listRenderer
        comboBox.maximumSize = Dimension(Integer.MAX_VALUE, 50)
        contentPanel.add(comboBox)

        val collapsibleInputPanel = CollapsibleInputPanel(project)
        contentPanel.add(collapsibleInputPanel, BorderLayout.NORTH)

        val addProfileToolWindowAction = AddProfileToolWindowAction(collapsibleInputPanel)
        credentialsContentListener(project, collapsibleInputPanel, comboBox)

        val searchField = SearchTextField()
        searchField.name = "searchProduct"
        searchField.textEditor.emptyText.text = "搜索产品："
        searchField.maximumSize = Dimension(Integer.MAX_VALUE, 50)
        contentPanel.add(searchField)

        val refreshLeftToolWindowAction = RefreshLeftToolWindowAction(project, contentPanel, searchField)
        refreshLeftToolWindowAction.templatePresentation.icon = AllIcons.Actions.Refresh
        refreshLeftToolWindowAction.templatePresentation.text = "Refresh Product List"
        val addProfileAction = listOf(addProfileToolWindowAction, refreshLeftToolWindowAction)
        toolWindow.setTitleActions(addProfileAction)

        val cacheTreeFile = File(ApiConstants.CACHE_PATH, "tree1")
        var cacheNameAndVersionMap: MutableMap<String, List<String>>? = null
        var cacheTree: Tree? = null
        try {
            if (cacheNameAndVersionFile.exists() && cacheNameAndVersionFile.lastModified() + ApiConstants.ONE_DAY.toMillis() > System.currentTimeMillis()) {
                cacheNameAndVersionMap = CacheUtil.readMapCache(cacheNameAndVersionFile)
            }
            if (cacheTreeFile.exists() && cacheTreeFile.lastModified() + ApiConstants.ONE_DAY.toMillis() > System.currentTimeMillis()) {
                cacheTree = CacheUtil.readTreeCache(cacheTreeFile)
            }
        } catch (_: IOException) {
        }

        if (cacheNameAndVersionMap != null && cacheTree != null) {
            val treeRenderer = ProductTreeCellRenderer()
            cacheTree.cellRenderer = treeRenderer
            val scrollPane = FormatUtil.getScrollPane(cacheTree)
            scrollPane.name = "productTree"
            contentPanel.add(scrollPane)
            productClickListener(project, cacheTree, cacheNameAndVersionMap)
            SearchHelper.search(cacheNameAndVersionMap, cacheTree, searchField)
        } else {
            var nameAndVersionMap = mutableMapOf<String, List<String>>()
            var apiDocContentTree = Tree()
            ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Loading API List", true) {
                override fun run(indicator: ProgressIndicator) {
                    val explorer = ApiExplorer.apiDocContentTree(project)
                    nameAndVersionMap = explorer.first
                    apiDocContentTree = explorer.second
                    val treeRenderer = ProductTreeCellRenderer()
                    apiDocContentTree.cellRenderer = treeRenderer
                }

                override fun onSuccess() {
                    val scrollPane = FormatUtil.getScrollPane(apiDocContentTree)
                    contentPanel.add(scrollPane)
                    productClickListener(project, apiDocContentTree, nameAndVersionMap)
                    SearchHelper.search(nameAndVersionMap, apiDocContentTree, searchField)
                }
            })
        }

        toolWindow.contentManager.apply {
            val content = factory.createContent(contentPanel, null, false)
            toolWindow.contentManager.addContent(content)
        }
    }

    private fun credentialsContentListener(
        project: Project,
        collapsibleInputPanel: CollapsibleInputPanel,
        comboBox: ComboBox<String>
    ) {
        ConfigFileUtil.readProfilesFromConfigFile(comboBox)
        var config = ConfigureFile.loadConfigureFile()
        if (config != null) {
            comboBox.selectedItem = config.current
        } else {
            comboBox.selectedItem = "New Profile"
        }
        comboBox.addActionListener {
            val selectedProfile = comboBox.selectedItem as String
            if (selectedProfile == "New Profile") {
                collapsibleInputPanel.clearFields()
                collapsibleInputPanel.expandForAddProfile()
            } else {
                config = ConfigureFile.loadConfigureFile()
                val selected = config!!.profiles.firstOrNull { it.name == selectedProfile }
                collapsibleInputPanel.showProfiles(selected)
                config!!.current = selectedProfile
                ConfigureFile.saveConfigureFile(config!!)
                val statusBar = WindowManager.getInstance().getStatusBar(project)
                val statusBarWidget = statusBar?.getWidget("Alibaba Cloud Widget")
                if (statusBarWidget is MyStatusBarWidgetFactory.MyStatusBarWidget) {
                    statusBarWidget.updateStatusBar(config)
                }
            }
        }

        comboBox.addPopupMenuListener(object : PopupMenuListener {
            override fun popupMenuWillBecomeVisible(e: PopupMenuEvent?) {
                ConfigFileUtil.readProfilesFromConfigFile(comboBox)
                ComboBoxManager.updateComboBoxItem(comboBox)
            }

            override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent?) {
                ComboBoxManager.updateComboBoxItem(comboBox)
            }

            override fun popupMenuCanceled(e: PopupMenuEvent?) {
                ComboBoxManager.updateComboBoxItem(comboBox)
            }
        })
    }

    private fun productClickListener(
        project: Project,
        tree: JTree,
        nameAndVersionMap: MutableMap<String, List<String>>,
    ) {
        val selectionModel = tree.selectionModel
        val apiPanel = JPanel()
        apiPanel.layout = BoxLayout(apiPanel, BoxLayout.Y_AXIS)

        var apiDocContent: Content? = null
        var apiName: String

        selectionModel.addTreeSelectionListener {
            val selectedNode = tree.lastSelectedPathComponent as? DefaultMutableTreeNode
            if (selectedNode != null) {
                if (selectedNode.isLeaf) {
                    val match = (selectedNode.userObject as String)
                    val pattern = """(.+?) {2}(.+)""".toRegex()
                    val matchProduct = pattern.matchEntire(match)
                    var productNameCN = ""
                    var productName = ""
                    matchProduct?.let {
                        productNameCN = it.destructured.component1()
                        productName = it.destructured.component2()
                    }
                    val defaultVersion = nameAndVersionMap[productNameCN]?.get(2) ?: ""
                    val apiUrl = "https://api.aliyun.com/api/product/apiDir?product=$productName&version=$defaultVersion"
                    val cacheApiDataFile = File(ApiConstants.CACHE_PATH, "$productName-api-list")
                    val cacheApiData: JsonArray?
                    var apiData: JsonArray? = null

                    if (cacheApiDataFile.exists() && cacheApiDataFile.lastModified() + ApiConstants.ONE_DAY.toMillis() > System.currentTimeMillis()) {
                        try {
                            cacheApiData = CacheUtil.readApiListCache(cacheApiDataFile)
                            apiData = cacheApiData
                        } catch (_: IOException) {
                        }
                    }
                    if (apiData == null) {
                        apiData = ApiExplorer.getApiListRequest(project, apiUrl)
                        val cacheApiListFile = File(ApiConstants.CACHE_PATH, "$productName-api-list")
                        if (apiData.size() > 0) {
                            try {
                                CacheUtil.writeApiListCache(cacheApiListFile, apiData)
                            } catch (e: IOException) {
                                cacheApiListFile.delete()
                                NormalNotification.showMessage(
                                    project,
                                    NotificationGroups.CACHE_NOTIFICATION_GROUP,
                                    "缓存写入失败",
                                    "",
                                    NotificationType.ERROR
                                )
                            }
                        }
                    }

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
                            apiName = (selectedApi.userObject as String).split("  ", limit = 2)[0]
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
                                ApiPage.showApiDetail(
                                    apiDocContent!!,
                                    contentManager,
                                    apiPanel,
                                    productName,
                                    apiName,
                                    defaultVersion,
                                    project,
                                    true,
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

    private class AddProfileToolWindowAction(private val collapsibleInputPanel: CollapsibleInputPanel) :
        AnAction("New Profile", "New profile", AllIcons.General.User) {
        override fun actionPerformed(e: AnActionEvent) {
            collapsibleInputPanel.clearFields()
            collapsibleInputPanel.expandForAddProfile()
        }
    }

    private class RefreshLeftToolWindowAction(
        val project: Project,
        val contentPanel: JPanel,
        val searchField: SearchTextField
    ) : AnAction() {
        override fun actionPerformed(e: AnActionEvent) {
            contentPanel.components.filter { it.name == "productTree" }.forEach {
                contentPanel.remove(it)
            }
            contentPanel.revalidate()
            contentPanel.repaint()

            var nameAndVersionMap = mutableMapOf<String, List<String>>()
            var apiDocContentTree = Tree()
            ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Loading API List", true) {
                override fun run(indicator: ProgressIndicator) {
                    val explorer = ApiExplorer.apiDocContentTree(project)
                    nameAndVersionMap = explorer.first
                    apiDocContentTree = explorer.second
                }

                override fun onSuccess() {
                    val treeRenderer = ProductTreeCellRenderer()
                    apiDocContentTree.cellRenderer = treeRenderer
                    val scrollPane = FormatUtil.getScrollPane(apiDocContentTree)
                    scrollPane.name = "productTree"
                    contentPanel.add(scrollPane)
                    BaseToolWindow().productClickListener(project, apiDocContentTree, nameAndVersionMap)
                    SearchHelper.search(nameAndVersionMap, apiDocContentTree, searchField)
                    contentPanel.revalidate()
                    contentPanel.repaint()
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
            ApiPage.showApiDetail(
                apiDocContent,
                contentManager,
                apiPanel,
                name,
                apiName,
                defaultVersion,
                project,
                false,
            )
        }
    }
}