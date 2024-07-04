package com.alibabacloud.credentials.constants

import java.io.File

object CredentialsConstants {
    val CONFIG_DIR = System.getProperty("user.home") + File.separator + ".aliyun"
    val CREATE_USER = "新增 AK 凭证配置"
}
