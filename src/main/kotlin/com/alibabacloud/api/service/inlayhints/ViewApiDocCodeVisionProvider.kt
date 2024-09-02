package com.alibabacloud.api.service.inlayhints

import com.alibabacloud.api.service.SearchHelper
import com.alibabacloud.models.api.ApiInfo
import com.alibabacloud.models.api.ShortApiInfo
import com.intellij.codeInsight.codeVision.CodeVisionAnchorKind
import com.intellij.codeInsight.codeVision.CodeVisionEntry
import com.intellij.codeInsight.codeVision.CodeVisionRelativeOrdering
import com.intellij.codeInsight.codeVision.CodeVisionRelativeOrdering.CodeVisionRelativeOrderingFirst
import com.intellij.codeInsight.codeVision.ui.model.CodeVisionPredefinedActionEntry
import com.intellij.codeInsight.hints.codeVision.DaemonBoundCodeVisionProvider
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SyntaxTraverser
import java.awt.event.MouseEvent

abstract class ViewApiDocCodeVisionProvider : DaemonBoundCodeVisionProvider {
    override val id: String
        get() = "alibabacloud.developer.toolkit"

    override val name: String
        get() = "Alibaba Cloud: View API Info"

    override val groupId: String
        get() = "com.alibabcloud.api.service.inlay"

    override val defaultAnchor: CodeVisionAnchorKind
        get() = CodeVisionAnchorKind.Top

    override val relativeOrderings: List<CodeVisionRelativeOrdering>
        get() = listOf<CodeVisionRelativeOrdering>(CodeVisionRelativeOrderingFirst)

    protected abstract fun isSupportedLanguage(languageId: String): Boolean

    protected abstract fun isValidElement(element: PsiElement): Boolean

    internal abstract fun getRegexList(languageId: String): List<Regex>?

    internal abstract fun findQualifiedApiInfoInCode(line: String, regexList: List<Regex>): ApiInfo?

    internal abstract fun findShortApiInfoInCode(line: String, regexList: List<Regex>, file: PsiFile): ShortApiInfo?

    internal abstract fun addLens(
        lenses: MutableList<Pair<TextRange, CodeVisionEntry>>,
        apiInfo: ApiInfo,
        line: String,
        overallOffset: Int,
        elementText: String,
        project: Project
    )

    override fun computeForEditor(editor: Editor, file: PsiFile): List<Pair<TextRange, CodeVisionEntry>> {
        val lenses: MutableList<Pair<TextRange, CodeVisionEntry>> = ArrayList()
        val languageId = file.language.id
        if (!isSupportedLanguage(languageId)) {
            return lenses
        }
        val traverser = SyntaxTraverser.psiTraverser(file)
        for (element in traverser) {
            if (isValidElement(element)) {
                val elementText = element.text
                val lines = elementText.lines()
                val overallOffset = element.textOffset
                val regexList = getRegexList(languageId)
                for (line in lines) {
                    if (regexList?.isNotEmpty() == true) {
                        val apiInfo = findQualifiedApiInfoInCode(line, regexList)
                        if (apiInfo != null) {
                            addLens(lenses, apiInfo, line, overallOffset, elementText, element.project)
                            continue
                        }
                        val shortApiInfo = findShortApiInfoInCode(line, regexList, file)
                        if (shortApiInfo != null && shortApiInfo.isValidImport) {
                            addLens(lenses, shortApiInfo.apiInfo, line, overallOffset, elementText, element.project)
                        }
                    }
                }
            }
        }
        return lenses
    }

    override fun handleClick(editor: Editor, textRange: TextRange, entry: CodeVisionEntry) {
        if (entry is CodeVisionPredefinedActionEntry) {
            (entry as CodeVisionPredefinedActionEntry).onClick(editor)
        }
    }

    internal class ClickHandler(
        private val project: Project,
        val productName: String,
        val version: String,
        val apiName: String
    ) : Function2<MouseEvent, Editor, Unit> {
        override fun invoke(event: MouseEvent, editor: Editor) {
            SearchHelper.navigateToApiInfo(project, productName, version, apiName)
        }
    }
}