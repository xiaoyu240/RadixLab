package com.radixlab.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.radixlab.app.data.ConversionEngine
import com.radixlab.app.data.model.ConversionRecord
import com.radixlab.app.data.model.RecordType
import com.radixlab.app.data.repository.HistoryRepository
import com.radixlab.app.ui.theme.ThemeMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 首页（进制转换）界面状态 */
data class ConverterUiState(
    val input: String = "",
    val fromBase: Int = 2,
    val toBase: Int = 10,
    val fromCustom: Boolean = false,
    val toCustom: Boolean = false,
    val output: String = "",
    val decimal: String = "",
    val error: String? = null,
    val steps: List<String> = emptyList(),
    val showSteps: Boolean = false
) {
    /** 是否已有可用结果 */
    val hasResult: Boolean get() = output.isNotEmpty() && error == null

    /** 结果值是否超长（用于是否提示滚动查看） */
    val isLongResult: Boolean get() = output.length > 32
}

/** 预设进制，用于快捷 Chip */
val PRESET_BASES = listOf(2, 8, 10, 16)

/**
 * 首页 ViewModel：负责进制转换 + 历史记录 + 主题设置。
 *
 * 转换在输入变化时即时计算（本地纯函数，无 IO 开销）；
 * 历史记录做 700ms 防抖后落盘，避免连续输入产生大量垃圾记录。
 */
class ConverterViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = HistoryRepository.get(application)

    private val _uiState = MutableStateFlow(ConverterUiState())
    val uiState: StateFlow<ConverterUiState> = _uiState.asStateFlow()

    /** 全部历史（倒序） */
    val history: StateFlow<List<ConversionRecord>> = repository.records
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 收藏记录 */
    val favorites: StateFlow<List<ConversionRecord>> = repository.records
        .map { list -> list.filter { it.favorite } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 主题模式 */
    val themeMode: StateFlow<Int> = repository.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    private var saveJob: Job? = null
    private var lastSavedSignature: String? = null

    init {
        recompute()
    }

    // -----------------------------------------------------------------------
    // 输入与进制选择
    // -----------------------------------------------------------------------

    fun onInputChange(value: String) {
        _uiState.value = _uiState.value.copy(input = value)
        recompute()
    }

    fun onFromPresetSelected(base: Int) {
        _uiState.value = _uiState.value.copy(fromBase = base, fromCustom = false)
        recompute()
    }

    fun onToPresetSelected(base: Int) {
        _uiState.value = _uiState.value.copy(toBase = base, toCustom = false)
        recompute()
    }

    /** 切到「自定义」：保留当前进制，同时展开滑杆 */
    fun onFromCustomSelected() {
        _uiState.value = _uiState.value.copy(fromCustom = true)
        recompute()
    }

    fun onToCustomSelected() {
        _uiState.value = _uiState.value.copy(toCustom = true)
        recompute()
    }

    fun onFromBaseChanged(base: Int) {
        _uiState.value = _uiState.value.copy(fromBase = base.coerceIn(ConversionEngine.MIN_BASE, ConversionEngine.MAX_BASE), fromCustom = true)
        recompute()
    }

    fun onToBaseChanged(base: Int) {
        _uiState.value = _uiState.value.copy(toBase = base.coerceIn(ConversionEngine.MIN_BASE, ConversionEngine.MAX_BASE), toCustom = true)
        recompute()
    }

    /** 交换源进制与目标进制 */
    fun swapBases() {
        val state = _uiState.value
        _uiState.value = state.copy(
            fromBase = state.toBase,
            toBase = state.fromBase,
            fromCustom = state.toCustom,
            toCustom = state.fromCustom
        )
        recompute()
    }

    fun clearInput() {
        saveJob?.cancel()
        lastSavedSignature = null
        _uiState.value = _uiState.value.copy(
            input = "",
            output = "",
            decimal = "",
            error = null,
            steps = emptyList(),
            showSteps = false
        )
    }

    fun toggleSteps() {
        _uiState.value = _uiState.value.copy(showSteps = !_uiState.value.showSteps)
    }

    /** 当前结果的完整文案，供复制使用 */
    fun currentResultText(): String = _uiState.value.output

    // -----------------------------------------------------------------------
    // 历史记录操作
    // -----------------------------------------------------------------------

    fun removeRecord(id: Long) {
        viewModelScope.launch { repository.remove(id) }
    }

    fun toggleFavorite(id: Long) {
        viewModelScope.launch { repository.toggleFavorite(id) }
    }

    fun clearHistory() {
        viewModelScope.launch { repository.clearAll() }
        lastSavedSignature = null
    }

    fun clearNotFavorites() {
        viewModelScope.launch { repository.clearNotFavorites() }
        lastSavedSignature = null
    }

    /** 点击历史记录：回填到首页继续编辑 */
    fun applyRecord(record: ConversionRecord) {
        if (record.type != RecordType.RADIX) return
        val from = if (record.fromBase in ConversionEngine.MIN_BASE..ConversionEngine.MAX_BASE) record.fromBase else 10
        val to = if (record.toBase in ConversionEngine.MIN_BASE..ConversionEngine.MAX_BASE) record.toBase else 10
        _uiState.value = _uiState.value.copy(
            input = record.input,
            fromBase = from,
            toBase = to,
            fromCustom = from !in PRESET_BASES,
            toCustom = to !in PRESET_BASES
        )
        recompute()
        lastSavedSignature = "${record.input}|$from|$to"
    }

    // -----------------------------------------------------------------------
    // 主题
    // -----------------------------------------------------------------------

    fun setThemeMode(mode: Int) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    // -----------------------------------------------------------------------
    // 内部：计算与落盘
    // -----------------------------------------------------------------------

    private fun recompute() {
        val state = _uiState.value
        val raw = state.input.trim()

        if (raw.isEmpty()) {
            _uiState.value = state.copy(output = "", decimal = "", error = null, steps = emptyList())
            return
        }

        when (val result = ConversionEngine.convert(raw, state.fromBase, state.toBase)) {
            is ConversionEngine.Result.Success -> {
                _uiState.value = state.copy(
                    output = result.output,
                    decimal = result.decimal,
                    error = null,
                    steps = result.steps
                )
                scheduleSave(result)
            }

            is ConversionEngine.Result.Failure -> {
                _uiState.value = state.copy(
                    output = "",
                    decimal = "",
                    error = result.message,
                    steps = emptyList()
                )
            }
        }
    }

    private fun scheduleSave(result: ConversionEngine.Result.Success) {
        val state = _uiState.value
        val signature = "${result.normalizedInput}|${state.fromBase}|${state.toBase}"
        if (signature == lastSavedSignature) return

        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(SAVE_DEBOUNCE_MS)
            lastSavedSignature = signature
            repository.add(
                ConversionRecord(
                    id = System.currentTimeMillis(),
                    type = RecordType.RADIX,
                    input = result.normalizedInput,
                    output = result.output,
                    fromLabel = ConversionEngine.baseLabel(state.fromBase),
                    toLabel = ConversionEngine.baseLabel(state.toBase),
                    timestamp = System.currentTimeMillis(),
                    fromBase = state.fromBase,
                    toBase = state.toBase
                )
            )
        }
    }

    companion object {
        private const val SAVE_DEBOUNCE_MS = 700L
    }
}
