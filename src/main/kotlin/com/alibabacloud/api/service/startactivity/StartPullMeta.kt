package com.alibabacloud.api.service.startactivity

import com.alibabacloud.api.service.completion.DataService
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class StartPullMeta : StartupActivity {
    override fun runActivity(project: Project) {
        DataService.loadMeta(project, false)
    }
}
