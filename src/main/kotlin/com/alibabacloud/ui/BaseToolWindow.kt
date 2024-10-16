package com.alibabacloud.ui

import com.alibabacloud.api.service.ApiExplorer
import com.alibabacloud.api.service.ApiPage
import com.alibabacloud.api.service.SearchHelper
import com.alibabacloud.api.service.completion.DataService
import com.alibabacloud.api.service.constants.ApiConstants
import com.alibabacloud.api.service.constants.NotificationGroups
import com.alibabacloud.api.service.notification.NormalNotification
import com.alibabacloud.api.service.util.CacheUtil
import com.alibabacloud.api.service.util.FormatUtil
import com.alibabacloud.constants.PropertiesConstants
import com.alibabacloud.credentials.util.ConfigFileUtil
import com.alibabacloud.i18n.I18nUtils
import com.alibabacloud.icons.ToolkitIcons
import com.alibabacloud.models.credentials.ConfigureFile
import com.alibabacloud.states.ToolkitSettingsState
import com.alibabacloud.telemetry.ExperienceQuestionnaire
import com.alibabacloud.telemetry.TelemetryDialog
import com.google.gson.JsonArray
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.*
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
import java.net.URI
import java.time.LocalDateTime
import java.util.*
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.JTree
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
        comboBox.border = BorderFactory.createEmptyBorder()
        val profilePanel = JPanel()
        profilePanel.layout = BoxLayout(profilePanel, BoxLayout.X_AXIS)
        profilePanel.add(comboBox)
        profilePanel.border = BorderFactory.createTitledBorder(I18nUtils.getMsg("credentials.current.profile"))
        contentPanel.add(profilePanel)

        val collapsibleInputPanel = CollapsibleInputPanel(project)
        contentPanel.add(collapsibleInputPanel, BorderLayout.NORTH)

        val addProfileAction = AddProfileAction(collapsibleInputPanel)
        val feedbackAction = FeedbackAction()
        credentialsContentListener(project, collapsibleInputPanel, comboBox)

        val searchField = SearchTextField()
        searchField.name = "searchProduct"
        searchField.textEditor.emptyText.text = I18nUtils.getMsg("toolwindow.search.product")
        searchField.maximumSize = Dimension(Integer.MAX_VALUE, 50)
        contentPanel.add(searchField)

        val searchApiField = SearchTextField()
        searchApiField.name = "searchAPI"
        searchApiField.textEditor.emptyText.text = I18nUtils.getMsg("toolwindow.search.api")
        searchApiField.maximumSize = Dimension(Integer.MAX_VALUE, 50)
        contentPanel.add(searchApiField)

        val refreshToolkitAction = RefreshToolkitAction(project, contentPanel, searchField)
        refreshToolkitAction.templatePresentation.icon = AllIcons.Actions.Refresh
        refreshToolkitAction.templatePresentation.text = I18nUtils.getMsg("action.refresh")

        val languageSwitchAction = LanguageSwitchAction(project, profilePanel, searchField, searchApiField, collapsibleInputPanel, contentPanel)

        val viewDocumentationAction = ViewDocumentationAction()
        viewDocumentationAction.templatePresentation.text = I18nUtils.getMsg("action.view.user.manual")

        val newIssueAction = NewIssueAction()
        newIssueAction.templatePresentation.icon = AllIcons.Vcs.Vendors.Github
        newIssueAction.templatePresentation.text = I18nUtils.getMsg("action.new.issue")

        toolWindow.setTitleActions(listOf(addProfileAction, feedbackAction, refreshToolkitAction, languageSwitchAction))
        toolWindow.setAdditionalGearActions(
            DefaultActionGroup().apply {
                add(viewDocumentationAction)
                add(newIssueAction)
            }
        )

        val locale = if (I18nUtils.getLocale() == Locale.CHINA) "" else "-en"
        val cacheTreeFile = File(ApiConstants.CACHE_PATH, "tree1$locale")
        var cacheNameAndVersionMap: MutableMap<String, List<String>>? = null
        var cacheTree: Tree? = null
        try {
            val nameAndVersionFile = if (I18nUtils.getLocale() == Locale.CHINA) {
                File(ApiConstants.CACHE_PATH, "nameAndVersion1")
            } else {
                File(ApiConstants.CACHE_PATH, "nameAndVersion1-en")
            }
            if (nameAndVersionFile.exists() && nameAndVersionFile.lastModified() + ApiConstants.ONE_DAY.toMillis() > System.currentTimeMillis()) {
                cacheNameAndVersionMap = CacheUtil.readMapCache(nameAndVersionFile)
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
            SearchHelper.globalSearchApi(project, searchApiField)
        } else {
            var nameAndVersionMap = mutableMapOf<String, List<String>>()
            var apiDocContentTree = Tree()
            ProgressManager.getInstance()
                .run(object : Task.Backgroundable(project, I18nUtils.getMsg("load.product.list"), true) {
                    override fun run(indicator: ProgressIndicator) {
                        val explorer = ApiExplorer.apiDocContentTree(project)
                        nameAndVersionMap = explorer.first
                        apiDocContentTree = explorer.second
                        val treeRenderer = ProductTreeCellRenderer()
                        apiDocContentTree.cellRenderer = treeRenderer
                    }

                    override fun onSuccess() {
                        val scrollPane = FormatUtil.getScrollPane(apiDocContentTree)
                        scrollPane.name = "productTree"
                        contentPanel.add(scrollPane)
                        productClickListener(project, apiDocContentTree, nameAndVersionMap)
                        SearchHelper.search(nameAndVersionMap, apiDocContentTree, searchField)
                        SearchHelper.globalSearchApi(project, searchApiField)
                    }
                })
        }

        toolWindow.contentManager.apply {
            val content = factory.createContent(contentPanel, null, false)
            toolWindow.contentManager.addContent(content)
        }

        ExperienceQuestionnaire(project).checkAndShowNotification()
        val settings = ToolkitSettingsState.getInstance()
        if (!settings.state.isTelemetryEnabled) {
            TelemetryDialog(project).checkAndShowNotification()
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
            comboBox.selectedItem = I18nUtils.getMsg("credentials.new.profile")
        }
        comboBox.addActionListener {
            val selectedProfile = comboBox.selectedItem as String
            if (selectedProfile == I18nUtils.getMsg("credentials.new.profile")) {
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

    fun productClickListener(
        project: Project,
        tree: JTree,
        nameAndVersionMap: MutableMap<String, List<String>>,
    ) {
        val selectionModel = tree.selectionModel
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
                    val locale = if (I18nUtils.getLocale() == Locale.CHINA) "" else "&lang=EN_US"
                    val preferLocale = if (locale == "") "" else "-en"
                    val apiUrl =
                        "https://api.aliyun.com/api/product/apiDir?product=$productName&version=$defaultVersion$locale"
                    val cacheApiDataFile = File(ApiConstants.CACHE_PATH, "$productName-api-list$preferLocale")
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
                        val cacheApiListFile = File(ApiConstants.CACHE_PATH, "$productName-api-list$preferLocale")
                        if (apiData.size() > 0) {
                            try {
                                CacheUtil.writeApiListCache(cacheApiListFile, apiData)
                            } catch (e: IOException) {
                                cacheApiListFile.delete()
                                NormalNotification.showMessage(
                                    project,
                                    NotificationGroups.CACHE_NOTIFICATION_GROUP,
                                    I18nUtils.getMsg("cache.write.fail"),
                                    "",
                                    NotificationType.ERROR
                                )
                            }
                        }
                    }

                    val toolWindow = registerToolWindow(project)
                    val contentManager = toolWindow.contentManager
                    val (selectionApi, apiTree) = openWebToolWindow(productName, apiData, toolWindow)

                    selectionApi.addTreeSelectionListener {
                        val selectedApi = apiTree.lastSelectedPathComponent as? DefaultMutableTreeNode
                        if (selectedApi != null) {
                            setToolWindowActions(contentManager, toolWindow)
                            if (selectedApi.isLeaf) {
                                val apiPanel = JPanel()
                                apiPanel.layout = BoxLayout(apiPanel, BoxLayout.Y_AXIS)

                                apiName = (selectedApi.userObject as String).split("  ", limit = 2)[0]
                                apiDocContent = toolWindow.contentManager.factory.createContent(
                                    apiPanel,
                                    "$productName-$apiName",
                                    false,
                                )
                                apiDocContent!!.tabName = "$productName::$apiName::$defaultVersion"
                                contentManager.addContent(apiDocContent!!)

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

    internal fun setToolWindowActions(
        contentManager: ContentManager,
        toolWindow: ToolWindow,
    ) {
        val refreshRightToolWindowAction = RefreshRightToolWindowAction(contentManager)
        refreshRightToolWindowAction.templatePresentation.icon = AllIcons.Actions.Refresh
        refreshRightToolWindowAction.templatePresentation.text = I18nUtils.getMsg("api.page.refresh")

        val closeAllTabsAction = CloseAllTabsAction(contentManager)
        closeAllTabsAction.templatePresentation.icon = AllIcons.Actions.Close
        closeAllTabsAction.templatePresentation.text = I18nUtils.getMsg("api.page.close.all.tabs")

        val rightToolWindowActions = listOf(refreshRightToolWindowAction, closeAllTabsAction)
        toolWindow.setTitleActions(rightToolWindowActions)
    }

    internal fun registerToolWindow(project: Project): ToolWindow {
        val toolWindow: ToolWindow
        val existToolWindow = ToolWindowManager.getInstance(project).getToolWindow(ApiConstants.TOOLWINDOW_APIS)
        if (existToolWindow == null) {
            val builder: RegisterToolWindowTaskBuilder.() -> Unit = {
                icon = ToolkitIcons.TOOLWINDOW_ICON
                anchor = ToolWindowAnchor.RIGHT
                canCloseContent = true
            }
            toolWindow =
                ToolWindowManager.getInstance(project).registerToolWindow(ApiConstants.TOOLWINDOW_APIS, builder)
        } else {
            toolWindow = existToolWindow
        }
        return toolWindow
    }

    private fun openWebToolWindow(
        productName: String,
        apiData: JsonArray,
        toolWindow: ToolWindow,
    ): Pair<TreeSelectionModel, Tree> {
        val icon = ToolkitIcons.TOOLWINDOW_ICON
        toolWindow.setIcon(icon)

        val contentManager = toolWindow.contentManager
        val content: Content?
        val tree: Tree
        val panel = JPanel(BorderLayout())
        tree = ApiExplorer.explorerTree(apiData, panel)
        val scrollPane = FormatUtil.getScrollPane(panel)

        content = toolWindow.contentManager.factory.createContent(scrollPane, "$productName-${I18nUtils.getMsg("toolwindow.api.overview")}", false)
        contentManager.addContent(content)
        contentManager.setSelectedContent(content, true)
        toolWindow.show()
        val selectionModel = tree.selectionModel
        return Pair(selectionModel, tree)
    }

    private class AddProfileAction(private val collapsibleInputPanel: CollapsibleInputPanel) :
        AnAction(
            I18nUtils.getMsg("credentials.new.profile"),
            I18nUtils.getMsg("credentials.new.profile"),
            IconLoader.getIcon("/icons/new_profile.svg", BaseToolWindow::class.java)
        ) {
        override fun actionPerformed(e: AnActionEvent) {
            collapsibleInputPanel.clearFields()
            collapsibleInputPanel.expandForAddProfile()
        }
    }

    private class FeedbackAction :
        AnAction(
            I18nUtils.getMsg("action.feedback"),
            I18nUtils.getMsg("action.feedback"),
            IconLoader.getIcon("/icons/feedback.svg", BaseToolWindow::class.java)
        ) {
        override fun actionPerformed(e: AnActionEvent) {
            BrowserUtil.browse(URI(ExperienceQuestionnaire.QUESTIONNAIRE_LINK))
            val properties = PropertiesComponent.getInstance()
            properties.setValue(PropertiesConstants.QUESTIONNAIRE_EXPIRATION_KEY, 30 * 24, 30 * 24)
            properties.setValue(PropertiesConstants.QUESTIONNAIRE_LAST_PROMPT_KEY, LocalDateTime.now().toString())
        }
    }

    private class RefreshToolkitAction(
        val project: Project,
        val contentPanel: JPanel,
        val searchField: SearchTextField,
    ) : AnAction() {
        override fun actionPerformed(e: AnActionEvent) {
            Util.refreshProductPanel(project, contentPanel, searchField)
            DataService.refreshMeta(project)
        }

        override fun getActionUpdateThread(): ActionUpdateThread {
            return ActionUpdateThread.EDT
        }
    }

    private class LanguageSwitchAction(
        val project: Project,
        val profilePanel: JPanel,
        val searchField: SearchTextField,
        val searchApiField: SearchTextField,
        val collapsibleInputPanel: JPanel,
        val contentPanel: JPanel
    ) : ToggleAction(I18nUtils.getMsg("language.switch")) {
        private val chineseIcon = IconLoader.getIcon("/icons/zh.svg", javaClass)
        private val englishIcon = IconLoader.getIcon("/icons/en.svg", javaClass)
        private val properties = PropertiesComponent.getInstance()

        init {
            templatePresentation.icon = if (I18nUtils.getLocale() == Locale.CHINA) chineseIcon else englishIcon
        }

        override fun isSelected(e: AnActionEvent): Boolean {
            return I18nUtils.getLocale() == Locale.CHINA
        }

        override fun setSelected(e: AnActionEvent, state: Boolean) {
            if (state) {
                e.presentation.icon = chineseIcon
                properties.setValue(PropertiesConstants.PREFERENCE_LANGUAGE, "zh_CN", "default")
                e.presentation.text = I18nUtils.getMsg("language.switch")
            } else {
                e.presentation.icon = englishIcon
                properties.setValue(PropertiesConstants.PREFERENCE_LANGUAGE, "en_US", "default")
                e.presentation.text = I18nUtils.getMsg("language.switch")
            }

            profilePanel.border = BorderFactory.createTitledBorder(I18nUtils.getMsg("credentials.current.profile"))
            profilePanel.revalidate()
            profilePanel.repaint()
            searchField.textEditor.emptyText.text = I18nUtils.getMsg("toolwindow.search.product")
            searchField.revalidate()
            searchField.repaint()
            searchApiField.textEditor.emptyText.text = I18nUtils.getMsg("toolwindow.search.api")
            searchApiField.revalidate()
            searchApiField.repaint()
            CollapsibleInputPanel(project)
            collapsibleInputPanel.revalidate()
            collapsibleInputPanel.repaint()
            Util.refreshProductPanel(project, contentPanel, searchField)
        }

        override fun getActionUpdateThread(): ActionUpdateThread {
            return ActionUpdateThread.EDT
        }
    }

    private class ViewDocumentationAction : AnAction() {
        override fun actionPerformed(e: AnActionEvent) {
            BrowserUtil.browse(URI("https://help.aliyun.com/zh/openapi/user-guide/using-the-alibaba-cloud-developer-toolkit-plugin-in-jetbrains-ides"))
        }

        override fun getActionUpdateThread(): ActionUpdateThread {
            return ActionUpdateThread.EDT
        }
    }

    private class NewIssueAction : AnAction() {
        override fun actionPerformed(e: AnActionEvent) {
            BrowserUtil.browse(URI("https://github.com/aliyun/alibabacloud-api-jetbrains-toolkit/issues"))
        }

        override fun getActionUpdateThread(): ActionUpdateThread {
            return ActionUpdateThread.EDT
        }
    }

    class CloseAllTabsAction(private val contentManager: ContentManager) : AnAction() {
        override fun actionPerformed(e: AnActionEvent) {
            contentManager.removeAllContents(true)
        }

        override fun getActionUpdateThread(): ActionUpdateThread {
            return ActionUpdateThread.EDT
        }
    }

    class RefreshRightToolWindowAction(
        private val contentManager: ContentManager,
    ) : AnAction() {
        override fun actionPerformed(e: AnActionEvent) {
            val project = e.project ?: return
            val currentContent = contentManager.selectedContent
            if (currentContent != null && currentContent.component is JPanel) {
                val tabName = currentContent.tabName.split("::")
                val productName = tabName[0]
                val apiName = tabName[1]
                val defaultVersion = tabName[2]
                val apiPanel = currentContent.component as JPanel
                ApiPage.showApiDetail(
                    currentContent,
                    contentManager,
                    apiPanel,
                    productName,
                    apiName,
                    defaultVersion,
                    project,
                    false,
                )
            }
        }
    }

    object Util {
        fun refreshProductPanel(
            project: Project,
            contentPanel: JPanel,
            searchField: SearchTextField
        ) {
            contentPanel.components.filter { it.name == "productTree" }.forEach {
                contentPanel.remove(it)
            }
            contentPanel.revalidate()
            contentPanel.repaint()

            var nameAndVersionMap = mutableMapOf<String, List<String>>()
            var apiDocContentTree = Tree()
            ProgressManager.getInstance()
                .run(object : Task.Backgroundable(project, I18nUtils.getMsg("load.product.list"), true) {
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
}