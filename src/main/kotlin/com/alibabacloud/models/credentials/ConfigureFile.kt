package com.alibabacloud.models.credentials

import com.alibabacloud.credentials.constants.CredentialsConstants
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.util.ArrayList

data class ConfigureFile(
    var current: String,
    val profiles: List<Profile>,
    val meta_path: String,
) {

    companion object {
        fun getDefaultPath(): String {
            return CredentialsConstants.CONFIG_DIR + File.separator + "config.json"
        }

        fun loadConfigureFile(path: String = getDefaultPath()): ConfigureFile {
            val configFile = File(path)
            if (!configFile.exists()) {
                return ConfigureFile("", ArrayList(), "")
            }
            val fileContent = configFile.readText()
            return Gson().fromJson(fileContent, ConfigureFile::class.java)
        }

        fun saveConfigureFile(configure: ConfigureFile, path: String = getDefaultPath()) {
            val gson = GsonBuilder().setPrettyPrinting().create()
            val configJson = gson.toJson(configure)
            val configFile = File(path)
            val configDir = File(CredentialsConstants.CONFIG_DIR)
            if (!configDir.exists()) {
                configDir.mkdir()
            }
            configFile.writeText(configJson)
        }
    }

    data class Profile(
        var name: String = "",
        var access_key_id: String = "",
        var access_key_secret: String = "",
        var mode: String = "",
        var sts_token: String = "",
        var sts_region: String = "",
        var ram_role_name: String = "",
        var ram_role_arn: String = "",
        var ram_session_name: String = "",
        var source_profile: String = "",
        var private_key: String = "",
        var key_pair_name: String = "",
        var expired_seconds: Int = 0,
        var verified: String = "",
        var region_id: String = "",
        var output_format: String = "",
        var language: String = "",
        var site: String = "",
        var retry_timeout: Int = 0,
        var connect_timeout: Int = 0,
        var retry_count: Int = 0,
        var process_command: String = "",
        var credentials_uri: String = "",
    )
}