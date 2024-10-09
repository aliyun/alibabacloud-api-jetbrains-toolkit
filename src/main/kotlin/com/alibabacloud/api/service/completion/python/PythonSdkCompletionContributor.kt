package com.alibabacloud.api.service.completion.python

import com.alibabacloud.api.service.completion.SdkCompletionContributor
import com.alibabacloud.api.service.completion.util.LookupElementUtil
import com.alibabacloud.api.service.sdksample.util.AutoInstallPkgUtil
import com.alibabacloud.i18n.I18nUtils
import com.alibabacloud.icons.ToolkitIcons
import com.google.gson.JsonArray
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.psi.PyParenthesizedExpression
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.python.psi.impl.PyFormattedStringElementImpl
import okhttp3.Request
import java.util.*

class PythonSdkCompletionContributor : SdkCompletionContributor() {
    override fun addElements(
        result: CompletionResultSet,
        key: String,
        value: String,
        document: Document,
        request: Request
    ) {
        val apiInfo = LookupElementUtil.getFormat(key)
        val description = if (I18nUtils.getLocale() == Locale.CHINA) "::${value}" else ""
        result.addElement(
            LookupElementBuilder.create(key)
                .withPresentableText(apiInfo.apiName)
                .withTailText("  ${apiInfo.productName}::${apiInfo.defaultVersion}$description")
                .withIcon(ToolkitIcons.LOGO_ICON)
                .withInsertHandler { insertionContext, _ ->
                    insertHandler(insertionContext, document, request, "python") { sdkInfo ->
                        if (!sdkInfo[0].asString.contains(I18nUtils.getMsg("sdk.not.exist.prefix"))) {
                            checkAndNotifyDependency(insertionContext, apiInfo.productName, apiInfo.defaultVersion, sdkInfo, "python")
                        }
                    }
                }
        )
    }

    override fun isInvalidInsertionLocation(editor: Editor, offset: Int): Boolean {
        val document = editor.document
        val text = document.text

        val project = editor.project ?: return true
        if (offset < 0 || offset > text.length) return true
        if (offset == text.length) return false
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document) as? PyFile ?: return true
        val element = psiFile.findElementAt(offset) ?: return true
        val elementType = element.node.elementType.toString()

        // 字符串、注释或字面量的一部分
        if (element.parent is PyStringLiteralExpression ||
            element.parent is PyFormattedStringElementImpl ||
            element.parent is PyParenthesizedExpression ||
            elementType == "Py:END_OF_LINE_COMMENT" ||
            elementType == "Py:FSTRING_END" ||
            elementType == "Py:FSTRING_TEXT" ||
            psiFile.findElementAt(offset - 1)?.node?.elementType.toString() == "Py:END_OF_LINE_COMMENT" ||
            PsiTreeUtil.getParentOfType(element, PsiComment::class.java) != null
        ) return true

        // 顶层作用域
        if (element.parent is PyFile) return false

        // 函数内部，且不在函数声明中
        val function = PsiTreeUtil.getParentOfType(element, PyFunction::class.java)
        if (function != null && function.statementList.textRange.contains(element.textRange)) return false

        val charBefore = text[offset - 1]
        val charAfter = text.getOrNull(offset) ?: return true

        val isCharBeforeIdentifierPart = charBefore.isPythonIdentifierPart()
        val isCharAfterIdentifierPart = charAfter.isPythonIdentifierPart()
        return isCharBeforeIdentifierPart && isCharAfterIdentifierPart
    }

    override fun checkAndNotifyDependency(
        context: InsertionContext,
        productName: String,
        defaultVersion: String,
        sdkInfo: JsonArray,
        lang: String
    ) {
        AutoInstallPkgUtil.installPyPkg(context.project, productName, defaultVersion, sdkInfo[2].asString)
    }

    private fun Char.isPythonIdentifierPart(): Boolean {
        return this.isLetterOrDigit() || this == '_'
    }
}
