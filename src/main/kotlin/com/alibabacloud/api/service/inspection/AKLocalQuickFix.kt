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
    override fun getFamilyName(): String {
        return "查看更多凭据管理方式"
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val psiElement = descriptor.psiElement
        val document = getDocument(project, psiElement)

        document?.let {
            BrowserUtil.browse("https://help.aliyun.com/document_detail/378657.html")
        }
    }

    private fun getDocument(project: Project, psiElement: PsiElement): Document? {
        val virtualFile = psiElement.containingFile.virtualFile ?: return null
        val editor = FileEditorManager.getInstance(project).getSelectedEditor(virtualFile)
        return (editor as? TextEditor)?.editor?.document
    }
}
