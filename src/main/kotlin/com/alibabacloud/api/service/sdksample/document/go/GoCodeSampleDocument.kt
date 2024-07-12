package com.alibabacloud.api.service.sdksample.document.go

import com.alibabacloud.api.service.sdksample.util.GenerateDocUtil
import com.goide.documentation.GoDocumentationProvider
import com.goide.psi.impl.GoImportSpecImpl
import com.goide.psi.impl.GoTypeSpecImpl
import com.intellij.lang.documentation.DocumentationProvider
import com.intellij.psi.PsiElement

class GoCodeSampleDocument {
    companion object {
        private val defaultGoDocProvider: DocumentationProvider? = try {
            GoDocumentationProvider()
        } catch (e: NoClassDefFoundError) {
            null
        }

        private fun getGoApiSamplesDoc(qualifiedName: String?): String? {
            val classInfo = qualifiedName?.replace("-", "")?.replace("_", "")?.replace(".", "")

            val pattern = Regex(".*\\d$")
            if (classInfo?.let { pattern.matches(it) } == true) {
                return GenerateDocUtil.generateProductDoc(classInfo)
            }

            return GenerateDocUtil.generateApiDoc(classInfo)
        }

        private fun getQualifiedGoName(element: PsiElement, originDoc: String?): String? {
            when (element) {
                is GoImportSpecImpl -> {
                    val import = element.text ?: return null
                    val regex = """(\w+)\s"github\.com/alibabacloud-go/([^/]+)/.*"""".toRegex()
                    val matchImport = regex.find(import)
                    if (matchImport != null) {
                        return matchImport.groupValues[2]
                    }
                    return null
                }

                is GoTypeSpecImpl -> {
                    val qualifiedName = element.qualifiedName ?: return null
                    originDoc ?: return null
                    if (element.qualifiedName?.startsWith("client.") == true) {
                        val apiName = qualifiedName.replace("client.", "").removeSuffix("Request")
                        val regex =
                            """<p>Package:.*?<a href="psi_element://PACKAGE:github\.com/alibabacloud-go/([^/]+)/.*?">""".toRegex(
                                RegexOption.DOT_MATCHES_ALL
                            )
                        val matchResult = regex.find(originDoc)
                        if (matchResult != null) {
                            val productVersion = matchResult.groupValues[1]
                            return productVersion + apiName
                        }
                    }
                    return null
                }

                else -> return null
            }
        }

        internal fun generateGoDoc(element: PsiElement): String? {
            val originalDoc = defaultGoDocProvider?.generateDoc(element, null)
            val qualifiedName = getQualifiedGoName(element, originalDoc)
            val apiSamplesDoc = getGoApiSamplesDoc(qualifiedName)
            return if (originalDoc == null && apiSamplesDoc == null) {
                null
            } else {
                listOfNotNull(apiSamplesDoc, originalDoc).joinToString("<br><br>")
            }
        }
    }
}