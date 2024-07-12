package com.alibabacloud.api.service.sdksample.document.python

import com.alibabacloud.api.service.sdksample.util.GenerateDocUtil
import com.intellij.lang.documentation.DocumentationProvider
import com.intellij.psi.PsiElement
import com.jetbrains.python.documentation.PythonDocumentationProvider
import java.util.regex.Pattern

class PythonCodeSampleDocument {
    companion object {
        private val defaultPyDocProvider: DocumentationProvider? = try {
            PythonDocumentationProvider()
        } catch (e: NoClassDefFoundError) {
            null
        }

        private fun getPyApiSamplesDoc(qualifiedName: String?): String? {
            val classInfo = qualifiedName
                ?.replace("models", "")
                ?.replace("Request", "")
                ?.replace("alibabacloud", "")
                ?.replace("client", "")
                ?.replace("Client", "")
                ?.replace("-", "")
                ?.replace("_", "")
                ?.replace(".", "")

            val pattern = Regex(".*\\d$")
            if (classInfo?.let { pattern.matches(it) } == true) {
                return GenerateDocUtil.generateProductDoc(classInfo)
            }

            return GenerateDocUtil.generateApiDoc(classInfo)
        }

        // 从原始文档中匹配出全限定名
        private fun getQualifiedPyName(originDoc: String?): String? {
            if (originDoc == null) {
                return null
            }

            val patternPackage =
                Pattern.compile("""<icon src="AllIcons.Nodes.Package"/>.*?<a href=psi_element://#module#([^"]+)">""")
            val matcherPackage = patternPackage.matcher(originDoc)
            if (matcherPackage.find()) {
                return matcherPackage.group(1)
            }

            val patternClass =
                Pattern.compile("""<icon src="AllIcons.Nodes.Class"/>.*?<a href="psi_element://#typename#([^"]+)">""")
            val matcherClass = patternClass.matcher(originDoc)

            if (matcherClass.find()) {
                return matcherClass.group(1)
            }

            val patternDefinition =
                Pattern.compile("""<div\s+class="definition">\s*<pre>(Package|Module)\s+<b>([^<]+)</b>""")
            val matcherDefinition = patternDefinition.matcher(originDoc)

            return if (matcherDefinition.find()) {
                matcherDefinition.group(2)
            } else {
                null
            }
        }

        internal fun generatePythonDoc(element: PsiElement): String? {
            val originalDoc = defaultPyDocProvider?.generateDoc(element, null)
            val qualifiedName = getQualifiedPyName(originalDoc)
            val apiSamplesDoc = getPyApiSamplesDoc(qualifiedName)
            return if (originalDoc == null && apiSamplesDoc == null) {
                null
            } else {
                listOfNotNull(apiSamplesDoc, originalDoc).joinToString("<br><br>")
            }
        }
    }
}