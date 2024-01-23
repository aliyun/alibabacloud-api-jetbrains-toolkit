package com.alibabacloud.models.credentials

import com.alibabacloud.credentials.constants.CredentialsConstants
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

data class ConfigureFile(
    var current: String,
    val profiles: MutableList<Profile>,
    val meta_path: String,
) {

    companion object {
        fun getDefaultPath(): String {
            return CredentialsConstants.CONFIG_DIR + File.separator + "config.json"
        }

        fun loadConfigureFile(path: String = getDefaultPath()): ConfigureFile? {
            val configFile = File(path)
            return if (configFile.exists()) {
                val fileContent = configFile.readText()
                Gson().fromJson(fileContent, ConfigureFile::class.java)
            } else {
                null
            }
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
        var access_key_id: String? = null,
        var access_key_secret: String? = null,
        var mode: String? = null,
        var sts_token: String? = null,
        var sts_region: String? = null,
        var ram_role_name: String? = null,
        var ram_role_arn: String? = null,
        var ram_session_name: String? = null,
        var source_profile: String? = null,
        var private_key: String? = null,
        var key_pair_name: String? = null,
        var expired_seconds: Int? = null,
        var verified: String? = null,
        var region_id: String = "",
        var output_format: String? = null,
        var language: String? = null,
        var site: String? = null,
        var retry_timeout: Int? = null,
        var connect_timeout: Int? = null,
        var retry_count: Int? = null,
        var process_command: String? = null,
        var credentials_uri: String? = null,
    )
}