package com.alibabacloud.api.service.inlayhints

import com.alibabacloud.api.service.sdksample.util.GenerateDocUtil
import com.alibabacloud.models.api.ApiInfo
import com.alibabacloud.models.api.ShortApiInfo
import com.intellij.codeInsight.codeVision.CodeVisionEntry
import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import java.awt.event.MouseEvent

class JavaViewApiDocCodeVisionProvider : ViewApiDocCodeVisionProvider() {
    override fun isSupportedLanguage(languageId: String): Boolean {
        return "JAVA".equals(languageId, ignoreCase = true)
    }

    override fun isValidElement(element: PsiElement): Boolean {
        return element is PsiMethod
    }

    override fun getRegexList(languageId: String): List<Regex>? {
        val qualifiedNameJavaRegex =
            Regex("""new\s+(com\.aliyun)\.([a-z0-9_-]+)\.(models)\.([A-Za-z0-9_-]+Request)\(\)""")
        val qualifiedNameJavaAsyncRegex =
            Regex("""com\.aliyun(\.sdk\.service)\.([a-z0-9_-]+)\.(models)\.([A-Za-z0-9_-]+Request)\.builder\(\)""")
        val shortJavaRegex = Regex("""new\s+([a-zA-Z0-9_-]*Request)\(\)""")
        val shortJavaAsyncRegex = Regex("""([a-zA-Z0-9_-]*Request)\.builder\(\)""")
        if ("JAVA".equals(languageId, ignoreCase = true)) {
            return listOf(qualifiedNameJavaRegex, qualifiedNameJavaAsyncRegex, shortJavaRegex, shortJavaAsyncRegex)
        }
        return null
    }

    override fun findQualifiedApiInfoInCode(line: String, regexList: List<Regex>): ApiInfo? {
        for (regex in listOf(regexList[0], regexList[1])) {
            val matchResult = regex.find(line)
            if (matchResult != null) {
                val product = matchResult.groupValues[2].replace("-", "").replace("_", "")
                val api = matchResult.groupValues[4].removeSuffix("Request")

                val keyInfo = api + product
                val res = GenerateDocUtil.findMatchingKey(keyInfo, GenerateDocUtil.getIndex())?.split("::")
                if (res != null) {
                    return ApiInfo(apiName = res[0], productName = res[1], defaultVersion = res[2])
                }
            }
        }
        return null
    }

    override fun findShortApiInfoInCode(line: String, regexList: List<Regex>, file: PsiFile): ShortApiInfo? {
        for (regex in listOf(regexList[2], regexList[3])) {
            val shortMatchResult = regex.find(line)
            if (shortMatchResult != null) {
                val api = shortMatchResult.groupValues[1].removeSuffix("Request")
                val importStatements = (file as? PsiJavaFile)?.importList?.importStatements
                val isValidImport: Boolean
                var apiInfo: ApiInfo? = null
                var res: List<String>? = null
                importStatements?.forEach { import ->
                    import.qualifiedName?.let { importQualifiedName ->
                        val importRegex =
                            Regex("""^com\.aliyun(\.sdk\.service)?\.([a-zA-Z0-9_]+)(?:\.models)?(?:\.($api+Request|$))?$""")
                        val matchResult = importRegex.find(importQualifiedName)
                        if (matchResult != null) {
                            val keyInfo = api + matchResult.groupValues[2].replace("_", "")
                            res = GenerateDocUtil.findMatchingKey(keyInfo, GenerateDocUtil.getIndex())?.split("::")
                            if (res != null) {
                                apiInfo = ApiInfo(apiName = res!![0], productName = res!![1], defaultVersion = res!![2])
                            }
                        }
                    }
                }
                isValidImport = res != null
                return apiInfo?.let { ShortApiInfo(it, isValidImport) }
            }
        }
        return null
    }

    override fun addLens(
        lenses: MutableList<Pair<TextRange, CodeVisionEntry>>,
        apiInfo: ApiInfo,
        line: String,
        overallOffset: Int,
        elementText: String,
        project: Project
    ) {
        val startOffset = overallOffset + elementText.indexOf(line)
        val endOffset = startOffset + line.length
        val range = TextRange(startOffset, endOffset)
        val hint = name
        val clickHandler = { event: MouseEvent?, eventEditor: Editor ->
            ClickHandler(project, apiInfo.productName, apiInfo.defaultVersion, apiInfo.apiName)
                .invoke(event!!, eventEditor)
        }
        lenses.add(Pair(range, ClickableTextCodeVisionEntry(hint, id, clickHandler, null, hint, "", emptyList())))
    }
}