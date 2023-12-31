package com.alibabacloud.ui

import com.alibabacloud.api.service.ApiExplorer
import com.alibabacloud.api.service.ProcessMeta
import com.alibabacloud.api.service.SearchHelper
import com.alibabacloud.api.service.constants.ApiConstants
import com.alibabacloud.api.service.util.CacheUtil
import com.alibabacloud.api.service.util.FormatUtil
import com.alibabacloud.credentials.util.ConfigFileUtil
import com.alibabacloud.models.credentials.ConfigureFile
import com.google.gson.JsonArray
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
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
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentManager
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import com.intellij.ui.treeStructure.Tree
import java.awt.BorderLayout
import java.awt.Dimension
import java.io.File
import java.io.IOException
import java.net.URL
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
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
        val searchField = SearchTextField()
        searchField.textEditor.emptyText.text = "搜索产品："
        searchField.maximumSize = Dimension(Integer.MAX_VALUE, 50)
        contentPanel.add(searchField)

        val cacheNameAndVersionFile = File(ApiConstants.CACHE_PATH, "nameAndVersion")
        val cacheTreeFile = File(ApiConstants.CACHE_PATH, "tree")
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
            val scrollPane = FormatUtil.getScrollPane(cacheTree)
            contentPanel.add(scrollPane)
            productClickListener(project, cacheTree, cacheNameAndVersionMap)
            SearchHelper.search(cacheNameAndVersionMap, cacheTree, searchField, contentPanel)
        } else {
            var nameAndVersionMap = mutableMapOf<String, List<String>>()
            var apiDocContentTree = Tree()
            ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Loading API List", true) {
                override fun run(indicator: ProgressIndicator) {
                    nameAndVersionMap = ApiExplorer.apiDocContentTree().first
                    apiDocContentTree = ApiExplorer.apiDocContentTree().second
                }

                override fun onSuccess() {
                    val scrollPane = FormatUtil.getScrollPane(apiDocContentTree)
                    contentPanel.add(scrollPane)
                    productClickListener(project, apiDocContentTree, nameAndVersionMap)
                    SearchHelper.search(nameAndVersionMap, apiDocContentTree, searchField, contentPanel)
                }
            })
        }

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
                        apiData = ProcessMeta.getApiListRequest(apiUrl)
                        val cacheApiListFile = File(ApiConstants.CACHE_PATH, "$productName-api-list")

                        try {
                            CacheUtil.writeApiListCache(cacheApiListFile, apiData)
                        } catch (e: IOException) {
                            cacheApiListFile.delete()
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
                project,
                false,
            )
        }
    }
}
