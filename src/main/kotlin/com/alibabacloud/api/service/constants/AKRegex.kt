package com.alibabacloud.api.service.constants

object AKRegex {
    const val COMMON_AK_REGEX = "(?<keyword>access|key|ak|accessKeyId|access_key_id)[^\\w\\n]*(?:\\n)?(?<separator>[\"'\\s]*[:=@,]\\s*[\"']?|\\w*\"\\s*?,\\s*?\")[\\s\"']*(?<key>LTAI[0-9A-Za-z]{12,22})(?<suffix>[\"'\\s]*)"

    const val COMMON_SK_PATTERN = "(?<keyword>access|key|secret|scret|sk|accessKeySecret|access_key_secret)[^\\w\\n]*(?:\\n)?(?<separator>[\"'\\s]*[:=@,]\\s*[\"']?|\\w*\"\\s*?,\\s*?\")[\\s\"']*(?<key>[0-9A-Za-z]{30})(?<suffix>[\"'\\s]*)"

    const val NEW_AK_REGEX = """(?<key>LTAI(?=.*[A-Z])(?=.*[a-z])(?=.*\d)([A-Za-z\d]{12}|[A-Za-z\d]{16}|[A-Za-z\d]{18}|[A-Za-z\d]{20}|[A-Za-z\d]{22}))"""

    const val JAVA_AK_REGEX ="""(?<keyword>\.(setAccessKeyId|accessKeyId))\s*\("(?<key>LTAI([a-zA-Z0-9]{12}|[a-zA-Z0-9]{16}|[a-zA-Z0-9]{18}|[a-zA-Z0-9]{20}|[a-zA-Z0-9]{22}))"\)"""

    const val JAVA_SK_REGEX = """(?<keyword>\.(setAccessKeySecret|accessKeySecret))\s*\("(?<key>([a-zA-Z0-9]{30}))"\)"""

    const val GO_AK_REGEX = """(?<keyword>AccessKeyId:\s*tea\.String)\s*\("(?<key>LTAI[a-zA-Z0-9]{12}|[a-zA-Z0-9]{16}|[a-zA-Z0-9]{18}|[a-zA-Z0-9]{20}|[a-zA-Z0-9]{22})"\)"""

    const val GO_SK_REGEX = """(?<keyword>AccessKeySecret:\s*tea\.String)\s*\("(?<key>([a-zA-Z0-9]{30}))"\)"""
}