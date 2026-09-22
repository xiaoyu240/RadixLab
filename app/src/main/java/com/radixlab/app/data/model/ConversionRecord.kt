package com.radixlab.app.data.model

import org.json.JSONArray
import org.json.JSONObject

/** 记录类型 */
enum class RecordType {
    /** 通用进制转换 */
    RADIX,

    /** IP 转换 */
    IP
}

/**
 * 一条转换记录。
 *
 * 存储在本地 DataStore 中（JSON 字符串），不上传、不外传。
 */
data class ConversionRecord(
    val id: Long,
    val type: RecordType,
    val input: String,
    val output: String,
    val fromLabel: String,
    val toLabel: String,
    val timestamp: Long,
    val fromBase: Int = 0,
    val toBase: Int = 0,
    val detail: String = "",
    val favorite: Boolean = false
) {

    /** 形如「二进制 2 → 十进制 10」 */
    val expression: String get() = "$fromLabel → $toLabel"

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("type", type.name)
        put("input", input)
        put("output", output)
        put("fromLabel", fromLabel)
        put("toLabel", toLabel)
        put("timestamp", timestamp)
        put("fromBase", fromBase)
        put("toBase", toBase)
        put("detail", detail)
        put("favorite", favorite)
    }

    companion object {

        fun fromJson(json: JSONObject): ConversionRecord? = runCatching {
            ConversionRecord(
                id = json.optLong("id", System.currentTimeMillis()),
                type = runCatching { RecordType.valueOf(json.optString("type", RecordType.RADIX.name)) }
                    .getOrDefault(RecordType.RADIX),
                input = json.optString("input", ""),
                output = json.optString("output", ""),
                fromLabel = json.optString("fromLabel", ""),
                toLabel = json.optString("toLabel", ""),
                timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                fromBase = json.optInt("fromBase", 0),
                toBase = json.optInt("toBase", 0),
                detail = json.optString("detail", ""),
                favorite = json.optBoolean("favorite", false)
            )
        }.getOrNull()

        /** 序列化整份历史 */
        fun encode(records: List<ConversionRecord>): String {
            val array = JSONArray()
            records.forEach { array.put(it.toJson()) }
            return array.toString()
        }

        /** 反序列化；任何异常都退化为空列表，绝不让历史数据把应用拖崩 */
        fun decode(raw: String?): List<ConversionRecord> {
            if (raw.isNullOrBlank()) return emptyList()
            return runCatching {
                val array = JSONArray(raw)
                (0 until array.length()).mapNotNull { index ->
                    fromJson(array.optJSONObject(index) ?: return@mapNotNull null)
                }
            }.getOrDefault(emptyList())
        }
    }
}
