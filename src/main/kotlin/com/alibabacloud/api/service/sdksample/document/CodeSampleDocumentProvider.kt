package com.alibabacloud.api.service.sdksample.document

import com.alibabacloud.api.service.sdksample.document.go.GoCodeSampleDocument
import com.alibabacloud.api.service.sdksample.document.java.JavaCodeSampleDocument
import com.alibabacloud.api.service.sdksample.document.python.PythonCodeSampleDocument
import com.intellij.lang.documentation.DocumentationProvider
import com.intellij.psi.PsiElement


class CodeSampleDocumentProvider : DocumentationProvider {
    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        element ?: return null
        val doc = when (element.language.id) {
            "JAVA" -> JavaCodeSampleDocument.generateJavaDoc(element)
            "Python" -> PythonCodeSampleDocument.generatePythonDoc(element)
            "go" -> GoCodeSampleDocument.generateGoDoc(element)
            else -> null
        }
        return doc
    }
}