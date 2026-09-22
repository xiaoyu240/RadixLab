package com.radixlab.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.radixlab.app.data.model.ConversionRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * 应用级单例 DataStore。
 * 使用 preferencesDataStore 委托，保证同一进程内只有一份实例。
 */
private val Context.radixLabStore: DataStore<Preferences> by preferencesDataStore(name = "radixlab_store")

/**
 * 历史记录仓库 —— 数据只写入应用私有目录，不上传、不外传。
 *
 * 同时承载一个轻量设置项：主题模式（theme_mode）。
 */
class HistoryRepository private constructor(private val context: Context) {

    private val keyRecords = stringPreferencesKey("conversion_records")
    private val keyThemeMode = intPreferencesKey("theme_mode")

    /** 全部记录，按时间倒序 */
    val records: Flow<List<ConversionRecord>> = context.radixLabStore.data
        .map { preferences -> ConversionRecord.decode(preferences[keyRecords]) }

    /** 主题模式：0 跟随系统 / 1 浅色 / 2 深色（与 ui.theme.ThemeMode 保持一致） */
    val themeMode: Flow<Int> = context.radixLabStore.data
        .map { preferences -> preferences[keyThemeMode] ?: DEFAULT_THEME_MODE }

    /** 追加一条记录（自动去重、自动裁剪） */
    suspend fun add(record: ConversionRecord) {
        context.radixLabStore.edit { preferences ->
            val current = ConversionRecord.decode(preferences[keyRecords]).toMutableList()

            // 同一表达式 + 同一输入的重复记录：仅保留最新一条，并继承收藏状态
            val duplicateIndex = current.indexOfFirst {
                it.type == record.type && it.input == record.input && it.expression == record.expression
            }
            var incoming = record
            if (duplicateIndex >= 0) {
                val existed = current.removeAt(duplicateIndex)
                if (existed.favorite) {
                    incoming = incoming.copy(id = existed.id, favorite = true)
                }
            }

            current.add(0, incoming)
            preferences[keyRecords] = ConversionRecord.encode(trim(current))
        }
    }

    suspend fun remove(id: Long) {
        context.radixLabStore.edit { preferences ->
            val current = ConversionRecord.decode(preferences[keyRecords])
                .filterNot { it.id == id }
            preferences[keyRecords] = ConversionRecord.encode(current)
        }
    }

    /** 清空全部记录 */
    suspend fun clearAll() {
        context.radixLabStore.edit { preferences ->
            preferences[keyRecords] = ConversionRecord.encode(emptyList())
        }
    }

    /** 只清空未收藏的记录 */
    suspend fun clearNotFavorites() {
        context.radixLabStore.edit { preferences ->
            val current = ConversionRecord.decode(preferences[keyRecords])
                .filter { it.favorite }
            preferences[keyRecords] = ConversionRecord.encode(current)
        }
    }

    suspend fun toggleFavorite(id: Long) {
        context.radixLabStore.edit { preferences ->
            val current = ConversionRecord.decode(preferences[keyRecords]).map { record ->
                if (record.id == id) record.copy(favorite = !record.favorite) else record
            }
            preferences[keyRecords] = ConversionRecord.encode(current)
        }
    }

    suspend fun setThemeMode(mode: Int) {
        context.radixLabStore.edit { preferences ->
            preferences[keyThemeMode] = mode
        }
    }

    /** 一次性读取（用于冷启动时判断主题） */
    suspend fun currentThemeMode(): Int =
        context.radixLabStore.data.first()[keyThemeMode] ?: DEFAULT_THEME_MODE

    /** 裁剪：优先保留收藏，其余保留最新的若干条 */
    private fun trim(list: List<ConversionRecord>): List<ConversionRecord> {
        if (list.size <= MAX_RECORDS) return list
        val favorites = list.filter { it.favorite }
        val remaining = (MAX_RECORDS - favorites.size).coerceAtLeast(0)
        val others = list.filterNot { it.favorite }.take(remaining)
        return (favorites + others).sortedByDescending { it.timestamp }
    }

    companion object {
        /** 最多保留的记录条数 */
        const val MAX_RECORDS = 300

        /** 默认主题：跟随系统（与 ThemeMode.SYSTEM 一致） */
        const val DEFAULT_THEME_MODE = 0

        @Volatile
        private var instance: HistoryRepository? = null

        /** 进程内单例 */
        fun get(context: Context): HistoryRepository =
            instance ?: synchronized(this) {
                instance ?: HistoryRepository(context.applicationContext).also { instance = it }
            }
    }
}
