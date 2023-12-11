package com.alibabacloud.models.api

data class ParamList(
    val fieldName: String,
    val fieldDetail: String,
    val level: Int = 0,
    val className: String? = "",
    val buttonId: String? = "",
)
