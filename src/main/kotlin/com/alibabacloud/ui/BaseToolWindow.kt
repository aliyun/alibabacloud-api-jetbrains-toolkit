package com.alibabacloud.ui

import com.alibabacloud.api.service.ApiExplorer
import com.alibabacloud.api.service.ProcessMeta
import com.alibabacloud.api.service.constants.ApiConstants
import com.alibabacloud.api.service.util.FormatUtil
import com.alibabacloud.credentials.util.ConfigFileUtil
import com.alibabacloud.models.credentials.ConfigureFile
import com.google.gson.JsonArray
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.wm.*
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import com.intellij.ui.treeStructure.Tree
import java.awt.BorderLayout
import java.io.File
import java.net.URL
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeSelectionModel

class BaseToolWindow : ToolWindowFactory, DumbAware {
    private lateinit var comboBox: JComboBox<String>

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentPanel = JPanel(BorderLayout())
        val listRenderer = CustomListCellRenderer()

        comboBox = ComboBox()
        comboBox.renderer = listRenderer
        credentialsContentListener(project)
        contentPanel.add(comboBox, BorderLayout.NORTH)

        val (nameAndVersionMap, apiDocContentTree) = ApiExplorer.apiDocContentTree()
        val scrollPane = FormatUtil.getScrollPane(apiDocContentTree)
        contentPanel.add(scrollPane)
        productClickListener(project, apiDocContentTree, nameAndVersionMap)

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
        nameAndVersionMap: MutableMap<String, Pair<String, String>>,
    ) {
        val selectionModel = tree.selectionModel
//        var toolWindow: ToolWindow? = null

        // 必须放在外层，否则切换产品会打开不同的tab
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
                    val productName = selectedNode.userObject as String
                    val name = nameAndVersionMap[productName]!!.first
                    val defaultVersion = nameAndVersionMap[productName]!!.second
                    val apiUrl = URL("${ApiConstants.API_DIR_URL}?product=$name&version=$defaultVersion")
                    val apiData = ProcessMeta.getApiListRequest(apiUrl)

                    val myIcon: Icon = IconLoader.getIcon("/icons/toolwindow.svg", javaClass)
                    val existToolWindow =
                        ToolWindowManager.getInstance(project).getToolWindow(ApiConstants.TOOLWINDOW_APIS)
                    val toolWindow: ToolWindow
                    if (existToolWindow == null) {
                        toolWindow = ToolWindowManager.getInstance(project).registerToolWindow(
                            RegisterToolWindowTask.closable(
                                id = ApiConstants.TOOLWINDOW_APIS,
                                icon = myIcon,
                                anchor = ToolWindowAnchor.RIGHT
                            )
                        )
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
                                ProcessMeta.showApiDoc(
                                    apiDocContent!!,
                                    contentManager,
                                    apiPanel,
                                    name,
                                    apiName,
                                    defaultVersion,
                                )
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
}
