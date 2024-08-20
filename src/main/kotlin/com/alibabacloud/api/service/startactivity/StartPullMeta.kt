package com.alibabacloud.api.service.startactivity

import com.alibabacloud.api.service.completion.DataService
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class StartPullMeta : ProjectActivity {
    override suspend fun execute(project: Project) {
        DataService.loadMeta(project, false)
    }
}
