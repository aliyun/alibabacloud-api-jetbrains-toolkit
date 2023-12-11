package com.alibabacloud.models.api

import com.google.gson.JsonArray
import com.google.gson.annotations.SerializedName

data class Parameter(
    val name: String,
    val `in`: String,
    val schema: Schema,
) {
    data class Schema(
        val description: String?,
        val type: String?,
        val required: Boolean?,
        val example: String?,
        val maximum: String?,
        val minimum: String?,
        val maxItems: String?,
        val format: String?,
        val properties: MutableMap<String, Schema>?,
        val additionalProperties: Schema?,
        val items: Schema?,
        @SerializedName("\$ref")
        val ref: String?,
        val enum: JsonArray?,
    )
}
