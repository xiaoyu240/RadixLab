package com.radixlab.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.radixlab.app.data.CalculatorEngine
import com.radixlab.app.data.PiEngine
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 大数计算器界面状态。
 *
 * @param expression 当前算式（用户可见、可编辑）
 * @param result 结果文本（十进制）
 * @param exact false = 除不尽或算式里用了 π，已按 [CalculatorEngine.DECIMAL_PLACES] 截断
 * @param approx true = 算式里用到了 π，结果是近似值
 * @param digits 结果的数字位数
 * @param error 错误文案（null 表示无错）
 * @param incomplete true = 算式还没输完（例如只打了「1+」），界面不报错
 * @param piPanelVisible π 无限计算面板是否可见
 * @param piRunning π 是否正在算
 * @param piMeter 面板当前那行说明
 * @param piDigits 已算出的 π（界面只渲染前面一段，完整结果在 [piValue]）
 * @param piValue 已算出的 π 完整文本，供复制
 * @param piElapsedSeconds 本次 π 计算已经跑了多少秒
 * @param piDialog 当前要弹的提醒（null = 不弹）
 */
data class CalculatorUiState(
    val expression: String = "",
    val result: String = "",
    val exact: Boolean = true,
    val approx: Boolean = false,
    val digits: Int = 0,
    val error: String? = null,
    val incomplete: Boolean = false,
    val piPanelVisible: Boolean = false,
    val piRunning: Boolean = false,
    val piMeter: String = "",
    val piDigits: String = "",
    val piValue: String = "",
    val piElapsedSeconds: Int = 0,
    val piDialog: PiEngine.Dialog? = null
) {
    /** 是否已有可用结果 */
    val hasResult: Boolean get() = result.isNotEmpty() && error == null

    /** 是否需要提示「除不尽、已截断」 */
    val showInfiniteHint: Boolean get() = hasResult && !exact

    /** 结果是否较长（提示可点击复制 / 可滚动） */
    val isLongResult: Boolean get() = result.length > 48
}

/**
 * 大数计算器 ViewModel。
 *
 * 设计约定：
 * - **不写入历史记录** —— 计算器只做即时计算，结果不留痕；
 * - 输入变化就即时计算（本地纯函数，无 IO）；
 * - 未输完的算式（如「1+」「(」）只当没结果，不弹错误；
 * - π 的「一直算下去」跑在后台协程里：一档一档真算，占满 1 MB 就弹一次提醒，
 *   到 10 MB 强制停止；用户随时可以停（协程取消 → 引擎协作中断）。
 */
class CalculatorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    private var piJob: Job? = null
    private var piTicker: Job? = null
    private var dialogAnswer: CompletableDeferred<Boolean>? = null

    // -----------------------------------------------------------------------
    // 输入
    // -----------------------------------------------------------------------

    /** 输入框直接编辑 */
    fun onExpressionChange(value: String) {
        stopPi()
        _uiState.value = _uiState.value.copy(expression = value)
        recompute()
    }

    /** 点键盘：在算式末尾追加一个片段（数字 / 运算符 / 括号） */
    fun append(token: String) {
        stopPi()
        val state = _uiState.value
        _uiState.value = state.copy(expression = state.expression + token)
        recompute()
    }

    /** 退格：删掉最后一个字符 */
    fun backspace() {
        stopPi()
        val state = _uiState.value
        if (state.expression.isEmpty()) return
        _uiState.value = state.copy(expression = state.expression.dropLast(1))
        recompute()
    }

    /** 清空一切，回到初始状态 */
    fun clear() {
        stopPi()
        _uiState.value = CalculatorUiState()
    }

    /** 当前结果的完整文本，供复制使用 */
    fun currentResultText(): String = _uiState.value.result

    /** π 无限计算出来的完整文本，供复制使用 */
    fun currentPiText(): String = _uiState.value.piValue

    // -----------------------------------------------------------------------
    // π：一直算下去
    // -----------------------------------------------------------------------

    /**
     * 开始「π 一直算下去」。
     *
     * 流程：50 位提醒 →（同意后）每档真算到占用涨约 1 MB 就提醒一次 → 10 MB 强制停止。
     */
    fun startPi() {
        if (piJob?.isActive == true) return

        // 算式不是 π 就先换成 π，让「一直算下去」有明确对象
        if (!_uiState.value.approx) {
            _uiState.value = _uiState.value.copy(expression = "π")
        }
        recompute()

        _uiState.value = _uiState.value.copy(
            piPanelVisible = true,
            piRunning = false,
            piMeter = "准备开始…",
            piDigits = "",
            piValue = "",
            piElapsedSeconds = 0
        )

        piJob = viewModelScope.launch {
            val startedAt = System.currentTimeMillis()
            ticker(startedAt)

            // 第一次提醒：已经算到 50 位
            if (!askDialog(PiEngine.firstDialog())) {
                finishPi("")
                return@launch
            }

            _uiState.value = _uiState.value.copy(piRunning = true)

            var digits = PiEngine.PI_FIRST_TIER_DIGITS
            var value = ""

            while (true) {
                setMeter("正在从零开始算 $digits 位 π…")

                val outcome = try {
                    val t0 = System.currentTimeMillis()
                    val pi = withContext(Dispatchers.Default) {
                        PiEngine.computePi(digits) { !isActive }
                    }
                    TierOutcome(pi, System.currentTimeMillis() - t0)
                } catch (cancelled: PiEngine.Cancelled) {
                    break
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    break
                } catch (error: Throwable) {
                    setMeter("算不动了：${error.message ?: "未知错误"}")
                    break
                }

                val pi = outcome.pi
                value = pi.value
                val mb = PiEngine.formatMB(pi.occupiedBytes)

                _uiState.value = _uiState.value.copy(
                    piValue = value,
                    piDigits = value,
                    piMeter = "已算到 ${pi.digits} 位 · 占用 $mb" +
                        "（结果文本 ${PiEngine.formatMB(pi.textBytes)}" +
                        " + 大数 ${PiEngine.formatMB(pi.bigBytes)}）" +
                        " · 本档耗时 " + String.format(java.util.Locale.US, "%.1f", outcome.elapsedMs / 1000.0) + " 秒"
                )

                // 到 10 MB → 强制停止
                if (pi.occupiedBytes >= PiEngine.PI_CAP_BYTES) {
                    askDialog(PiEngine.capDialog(pi.digits, mb))
                    setMeter("已经算到 ${pi.digits} 位，占用 $mb —— 到 10 MB 上限，停在这里了。")
                    break
                }

                // 每一档提醒一次
                if (!askDialog(PiEngine.tierDialog(pi.digits, mb))) {
                    setMeter("停在 ${pi.digits} 位 · 占用 $mb（完整结果可以复制）")
                    break
                }

                val next = PiEngine.nextTierDigits(pi.digits, pi.occupiedBytes)
                if (next >= PiEngine.PI_MAX_DIGITS) {
                    setMeter("已经算到 ${pi.digits} 位，占用 $mb —— 到引擎上限，停在这里了。")
                    break
                }
                digits = next
            }

            finishPi(value)
        }
    }

    /** 停止 π 计算（立刻生效：引擎在下一处检查点中断） */
    fun stopPi() {
        dialogAnswer?.complete(false)
        dialogAnswer = null
        piTicker?.cancel()
        piTicker = null
        piJob?.cancel()
        piJob = null
        if (_uiState.value.piPanelVisible) {
            _uiState.value = _uiState.value.copy(piRunning = false, piDialog = null)
        }
    }

    /** 用户在提醒弹窗上做了选择 */
    fun answerPiDialog(goOn: Boolean) {
        dialogAnswer?.complete(goOn)
        dialogAnswer = null
        _uiState.value = _uiState.value.copy(piDialog = null)
    }

    /** 收起 π 面板 */
    fun hidePiPanel() {
        stopPi()
        _uiState.value = _uiState.value.copy(
            piPanelVisible = false,
            piMeter = "",
            piDigits = "",
            piValue = "",
            piElapsedSeconds = 0
        )
    }

    // -----------------------------------------------------------------------
    // π 内部
    // -----------------------------------------------------------------------

    /** 一档算完的产物 */
    private class TierOutcome(val pi: PiEngine.Pi, val elapsedMs: Long)

    /** 弹一个提醒，挂起等用户点按钮 */
    private suspend fun askDialog(dialog: PiEngine.Dialog): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        dialogAnswer = deferred
        _uiState.value = _uiState.value.copy(piDialog = dialog)
        return try {
            deferred.await()
        } finally {
            dialogAnswer = null
            _uiState.value = _uiState.value.copy(piDialog = null)
        }
    }

    private fun setMeter(text: String) {
        _uiState.value = _uiState.value.copy(piMeter = text)
    }

    /** 每秒刷新一次「已用时」，长任务等待时不至于像卡死 */
    private fun ticker(startedAt: Long) {
        piTicker?.cancel()
        piTicker = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _uiState.value = _uiState.value.copy(
                    piElapsedSeconds = ((System.currentTimeMillis() - startedAt) / 1000).toInt()
                )
            }
        }
    }

    private fun finishPi(value: String) {
        piTicker?.cancel()
        piTicker = null
        piJob = null
        dialogAnswer = null
        _uiState.value = _uiState.value.copy(
            piRunning = false,
            piDialog = null,
            piValue = value,
            piDigits = value
        )
    }

    // -----------------------------------------------------------------------
    // 内部
    // -----------------------------------------------------------------------

    private fun recompute() {
        val state = _uiState.value
        val raw = state.expression

        if (raw.isBlank()) {
            _uiState.value = state.copy(
                result = "",
                exact = true,
                approx = false,
                digits = 0,
                error = null,
                incomplete = false
            )
            return
        }

        when (val outcome = CalculatorEngine.evaluate(raw)) {
            is CalculatorEngine.Result.Success -> {
                _uiState.value = state.copy(
                    result = outcome.value,
                    exact = outcome.exact,
                    approx = outcome.approx,
                    digits = outcome.digits,
                    error = null,
                    incomplete = false
                )
            }

            is CalculatorEngine.Result.Failure -> {
                // 算式还没输完（「1+」「(」）时不算错，只是暂时没有结果
                _uiState.value = state.copy(
                    result = "",
                    exact = true,
                    approx = false,
                    digits = 0,
                    error = if (outcome.incomplete) null else outcome.message,
                    incomplete = outcome.incomplete
                )
            }
        }
    }
}
