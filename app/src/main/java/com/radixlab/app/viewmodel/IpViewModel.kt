package com.radixlab.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.radixlab.app.data.IpUtils
import com.radixlab.app.data.model.ConversionRecord
import com.radixlab.app.data.model.RecordType
import com.radixlab.app.data.repository.HistoryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** IPv4 输入形式（遍历顺序即声明顺序，直接使用 IpInputType.entries） */
enum class IpInputType(val label: String, val hint: String) {
    DOTTED("点分十进制", "192.168.1.1"),
    DECIMAL("十进制整数", "3232235777"),
    BINARY("二进制", "11000000101010000000000100000001"),
    HEX("十六进制", "C0A80101")
}

/** IP 工具界面状态 */
data class IpUiState(
    // ---- IPv4 ----
    val ipv4Input: String = "",
    val ipv4InputType: IpInputType = IpInputType.DOTTED,
    val ipv4Error: String? = null,
    val dotted: String = "",
    val binary: String = "",
    val hex: String = "",
    val decimal: String = "",
    val ipv4Steps: List<String> = emptyList(),
    val showSteps: Boolean = false,
    // ---- IPv6 ----
    val ipv6Input: String = "",
    val ipv6Error: String? = null,
    val ipv6Compressed: String = "",
    val ipv6Expanded: String = "",
    val ipv6Grouped: String = ""
) {
    val ipv4HasResult: Boolean get() = dotted.isNotEmpty() && ipv4Error == null
    val ipv6HasResult: Boolean get() = ipv6Compressed.isNotEmpty() && ipv6Error == null
}

/**
 * IP 工具 ViewModel：IPv4 四向互转 + IPv6 压缩 / 展开。
 */
class IpViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = HistoryRepository.get(application)

    private val _uiState = MutableStateFlow(IpUiState())
    val uiState: StateFlow<IpUiState> = _uiState.asStateFlow()

    private var saveJob: Job? = null
    private var lastSavedSignature: String? = null

    // -----------------------------------------------------------------------
    // IPv4
    // -----------------------------------------------------------------------

    fun onIpv4InputChange(value: String) {
        _uiState.value = _uiState.value.copy(ipv4Input = value)
        recomputeIpv4()
    }

    fun onIpv4TypeChange(type: IpInputType) {
        _uiState.value = _uiState.value.copy(ipv4InputType = type, ipv4Input = "")
        recomputeIpv4()
    }

    /** 示例地址一键填充 */
    fun fillIpv4Sample(sample: String) {
        _uiState.value = _uiState.value.copy(ipv4InputType = IpInputType.DOTTED, ipv4Input = sample)
        recomputeIpv4()
    }

    fun clearIpv4() {
        saveJob?.cancel()
        lastSavedSignature = null
        _uiState.value = _uiState.value.copy(
            ipv4Input = "",
            ipv4Error = null,
            dotted = "",
            binary = "",
            hex = "",
            decimal = "",
            ipv4Steps = emptyList(),
            showSteps = false
        )
    }

    fun toggleIpv4Steps() {
        _uiState.value = _uiState.value.copy(showSteps = !_uiState.value.showSteps)
    }

    // -----------------------------------------------------------------------
    // IPv6
    // -----------------------------------------------------------------------

    fun onIpv6InputChange(value: String) {
        _uiState.value = _uiState.value.copy(ipv6Input = value)
        recomputeIpv6()
    }

    fun fillIpv6Sample(sample: String) {
        _uiState.value = _uiState.value.copy(ipv6Input = sample)
        recomputeIpv6()
    }

    fun clearIpv6() {
        _uiState.value = _uiState.value.copy(
            ipv6Input = "",
            ipv6Error = null,
            ipv6Compressed = "",
            ipv6Expanded = "",
            ipv6Grouped = ""
        )
    }

    /** 压缩形式回填为输入，便于反向验证 */
    fun useCompressedAsInput() {
        val compressed = _uiState.value.ipv6Compressed
        if (compressed.isNotEmpty()) fillIpv6Sample(compressed)
    }

    // -----------------------------------------------------------------------
    // 内部
    // -----------------------------------------------------------------------

    private fun recomputeIpv4() {
        val state = _uiState.value
        val raw = state.ipv4Input.trim().replace(" ", "")

        if (raw.isEmpty()) {
            _uiState.value = state.copy(
                ipv4Error = null, dotted = "", binary = "", hex = "", decimal = "", ipv4Steps = emptyList()
            )
            return
        }

        // 先把任意输入形式统一成点分十进制
        val canonical: String?
        val typeError: String?
        when (state.ipv4InputType) {
            IpInputType.DOTTED -> {
                typeError = IpUtils.validateIpv4(raw)
                canonical = if (typeError == null) raw else null
            }

            IpInputType.DECIMAL -> {
                canonical = IpUtils.decimalToIpv4(raw)
                typeError = if (canonical == null) "十进制整数需介于 0 到 4294967295 之间" else null
            }

            IpInputType.BINARY -> {
                canonical = IpUtils.binaryToIpv4(raw)
                typeError = if (canonical == null) "二进制需恰好 32 位，且只包含 0 和 1" else null
            }

            IpInputType.HEX -> {
                canonical = IpUtils.hexToIpv4(raw)
                typeError = if (canonical == null) "十六进制需为 1-8 位，字符范围 0-9 A-F" else null
            }
        }

        if (canonical == null) {
            _uiState.value = state.copy(
                ipv4Error = typeError ?: "无法解析，请检查输入",
                dotted = "", binary = "", hex = "", decimal = "", ipv4Steps = emptyList()
            )
            return
        }

        val binary = IpUtils.ipv4ToBinary(canonical)
        val hex = IpUtils.ipv4ToHex(canonical)
        val decimal = IpUtils.ipv4ToDecimal(canonical)

        if (binary == null || hex == null || decimal == null) {
            _uiState.value = state.copy(ipv4Error = "转换失败，请检查输入", dotted = "", binary = "", hex = "", decimal = "")
            return
        }

        _uiState.value = state.copy(
            ipv4Error = null,
            dotted = canonical,
            binary = binary,
            hex = hex,
            decimal = decimal,
            ipv4Steps = IpUtils.ipv4Steps(canonical)
        )

        scheduleSave(canonical, decimal)
    }

    private fun recomputeIpv6() {
        val state = _uiState.value
        val raw = state.ipv6Input.trim()

        if (raw.isEmpty()) {
            _uiState.value = state.copy(
                ipv6Error = null, ipv6Compressed = "", ipv6Expanded = "", ipv6Grouped = ""
            )
            return
        }

        val compressed = IpUtils.compressIpv6(raw)
        val expanded = IpUtils.expandIpv6(raw)
        val grouped = IpUtils.ipv6GroupHex(raw)

        if (compressed == null || expanded == null || grouped == null) {
            _uiState.value = state.copy(
                ipv6Error = IpUtils.validateIpv6(raw),
                ipv6Compressed = "", ipv6Expanded = "", ipv6Grouped = ""
            )
            return
        }

        _uiState.value = state.copy(
            ipv6Error = null,
            ipv6Compressed = compressed,
            ipv6Expanded = expanded,
            ipv6Grouped = grouped
        )
    }

    private fun scheduleSave(dotted: String, decimal: String) {
        val signature = "IP|$dotted"
        if (signature == lastSavedSignature) return

        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(SAVE_DEBOUNCE_MS)
            lastSavedSignature = signature
            repository.add(
                ConversionRecord(
                    id = System.currentTimeMillis(),
                    type = RecordType.IP,
                    input = dotted,
                    output = decimal,
                    fromLabel = "IPv4",
                    toLabel = "二进制 / 十六进制 / 整数",
                    timestamp = System.currentTimeMillis(),
                    detail = IpUtils.ipv4ToHex(dotted) ?: ""
                )
            )
        }
    }

    companion object {
        private const val SAVE_DEBOUNCE_MS = 700L

        /** 首页示例地址 */
        val IPV4_SAMPLES = listOf("192.168.1.1", "10.0.0.1", "255.255.255.0", "127.0.0.1")

        /** IPv6 示例 */
        val IPV6_SAMPLES = listOf("2001:0db8:0000:0000:0000:ff00:0042:8329", "fe80::1", "::1", "::ffff:192.168.1.1")
    }
}
