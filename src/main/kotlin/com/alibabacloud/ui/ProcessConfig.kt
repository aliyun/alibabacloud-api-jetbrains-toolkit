package com.alibabacloud.ui

import com.alibabacloud.models.credentials.ConfigureFile
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

class ProcessConfig {
    companion object {
        fun createNewProfile(project: Project, defaultProfile: ConfigureFile.Profile): ConfigureFile.Profile? {
            val name = Messages.showInputDialog(project, "Profile: ", "Add Profile", null)
            if (name != null) {
                val accessKeyId = Messages.showInputDialog(project, "AccessKeyId: ", "Add Profile", null)
                if (accessKeyId != null) {
                    val accessKeySecret = Messages.showInputDialog(project, "AccessKeySecret:", "Add Profile", null)
                    if (accessKeySecret != null) {
                        val profile = ConfigureFile.Profile()
                        profile.name = name
                        profile.access_key_id = accessKeyId
                        profile.access_key_secret = accessKeySecret
                        profile.mode = defaultProfile.mode
                        profile.sts_token = defaultProfile.sts_token
                        profile.sts_region = defaultProfile.sts_region
                        profile.ram_role_name = defaultProfile.ram_role_name
                        profile.ram_role_arn = defaultProfile.ram_role_arn
                        profile.ram_session_name = defaultProfile.ram_session_name
                        profile.source_profile = defaultProfile.source_profile
                        profile.private_key = defaultProfile.private_key
                        profile.key_pair_name = defaultProfile.key_pair_name
                        profile.expired_seconds = defaultProfile.expired_seconds
                        profile.verified = defaultProfile.verified
                        profile.region_id = defaultProfile.region_id
                        profile.output_format = defaultProfile.output_format
                        profile.language = defaultProfile.language
                        profile.site = defaultProfile.site
                        profile.retry_timeout = defaultProfile.retry_timeout
                        profile.connect_timeout = defaultProfile.connect_timeout
                        profile.retry_count = defaultProfile.retry_count
                        profile.process_command = defaultProfile.process_command
                        profile.credentials_uri = defaultProfile.credentials_uri
                        return profile
                    }
                }
            }
            return null
        }
    }
}
