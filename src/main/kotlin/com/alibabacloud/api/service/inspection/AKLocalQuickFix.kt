package com.alibabacloud.api.service.inspection

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

class AKLocalQuickFix : LocalQuickFix {
    companion object {
        val credentialsLink = mapOf(
            "java" to "https://help.aliyun.com/document_detail/378657.html",
            "python" to "https://help.aliyun.com/document_detail/378659.html",
            "go" to "https://help.aliyun.com/document_detail/378661.html",
            "php" to "https://help.aliyun.com/document_detail/311677.html",
            "js" to "https://help.aliyun.com/document_detail/378664.html"
        )
    }

    override fun getFamilyName(): String {
        return "查看更多凭据管理方式"
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val psiElement = descriptor.psiElement
        val document = getDocument(project, psiElement)
        val docLink = when (psiElement.language.id.lowercase()) {
                "python" -> credentialsLink["python"]
                "go" -> credentialsLink["go"]
                "javascript" -> credentialsLink["js"]
                "php" -> credentialsLink["php"]
                else -> credentialsLink["java"]
            } ?: credentialsLink["java"]

        document?.let {
            BrowserUtil.browse(docLink ?: credentialsLink["java"]!!)
        }
    }

    private fun getDocument(project: Project, psiElement: PsiElement): Document? {
        val virtualFile = psiElement.containingFile.virtualFile ?: return null
        val editor = FileEditorManager.getInstance(project).getSelectedEditor(virtualFile)
        return (editor as? TextEditor)?.editor?.document
    }
}
