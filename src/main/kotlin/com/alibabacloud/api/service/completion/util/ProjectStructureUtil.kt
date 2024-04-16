package com.alibabacloud.api.service.completion.util

import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.wm.IdeFocusManager
import org.apache.commons.lang3.ObjectUtils


object ProjectStructureUtil {
    private val activeProject: Project
        get() = if (ObjectUtils.isEmpty(IdeFocusManager.findInstance().lastFocusedFrame)) ProjectManager.getInstance().defaultProject else IdeFocusManager.findInstance().lastFocusedFrame!!.project!!

    private fun getEditingProject(insertionContext: InsertionContext): Project =
        runCatching { insertionContext.project }.getOrElse { activeProject }

    private fun getEditingModule(insertionContext: InsertionContext): com.intellij.openapi.module.Module? =
        runCatching {
            val insertContextFile = insertionContext.file.virtualFile
            ModuleUtil.findModuleForFile(insertContextFile, getEditingProject(insertionContext))
        }.getOrNull()

    fun getEditingSdk(insertionContext: InsertionContext): Sdk? =
        getEditingModule(insertionContext)?.let {
            ModuleRootManager.getInstance(it).sdk
        } ?: runCatching {
            ProjectRootManager.getInstance(getEditingProject(insertionContext)).projectSdk
        }.getOrNull()

    fun getEditingSdk(project: Project): Sdk? =
        runCatching {
            ProjectRootManager.getInstance(project).projectSdk
        }.getOrNull()
}