package com.radixlab.app.data

import java.math.BigInteger

/**
 * 通用进制转换引擎（2–36 任意进制互转）。
 *
 * 设计要点：
 * - 全程使用 [BigInteger]，超长二进制串（几百位）也不会丢精度；
 * - 先按源进制解析为十进制中间值，再做「除基取余」得到目标进制，保证结果可解释；
 * - 返回 [Result.Success.steps] 便于界面展示完整的转换步骤。
 */
object ConversionEngine {

    /** 支持的进制下界 */
    const val MIN_BASE = 2

    /** 支持的进制上界 */
    const val MAX_BASE = 36

    /** 0-9 A-Z 依次对应数值 0-35 */
    private const val DIGITS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"

    /** 步骤中最多展示多少次除法，避免超长数字把界面撑爆 */
    private const val MAX_DIVISION_STEPS = 24

    /** 展开式中最多展示多少位 */
    private const val MAX_EXPANSION_DIGITS = 16

    // -----------------------------------------------------------------------
    // 结果模型
    // -----------------------------------------------------------------------

    sealed interface Result {
        /**
         * @param output 目标进制结果（不含前导零，负数带 -）
         * @param decimal 十进制中间值
         * @param steps 逐步计算过程
         * @param normalizedInput 清洗后的输入（去空格、前缀、统一大写）
         */
        data class Success(
            val output: String,
            val decimal: String,
            val steps: List<String>,
            val normalizedInput: String
        ) : Result

        data class Failure(val message: String) : Result
    }

    // -----------------------------------------------------------------------
    // 基础工具
    // -----------------------------------------------------------------------

    /** 字符 → 数值，非法返回 -1 */
    fun digitValue(char: Char): Int = DIGITS.indexOf(char.uppercaseChar())

    /** 数值 → 字符 */
    fun digitChar(value: Int): Char = DIGITS[value.coerceIn(0, MAX_BASE - 1)]

    fun isValidBase(base: Int): Boolean = base in MIN_BASE..MAX_BASE

    /** 中文名称 */
    fun baseName(base: Int): String = when (base) {
        2 -> "二进制"
        8 -> "八进制"
        10 -> "十进制"
        16 -> "十六进制"
        else -> "$base 进制"
    }

    /** 界面徽标：二进制 2 / 八进制 8 / 十进制 10 / 十六进制 16 / 5 进制 */
    fun baseLabel(base: Int): String = when (base) {
        2 -> "二进制 2"
        8 -> "八进制 8"
        10 -> "十进制 10"
        16 -> "十六进制 16"
        else -> "$base 进制"
    }

    /** 英文缩写，用于结果卡副标题 */
    fun baseShortName(base: Int): String = when (base) {
        2 -> "BIN"
        8 -> "OCT"
        10 -> "DEC"
        16 -> "HEX"
        else -> "BASE $base"
    }

    // -----------------------------------------------------------------------
    // 核心转换
    // -----------------------------------------------------------------------

    /**
     * 把 [raw] 从 [fromBase] 转为 [toBase]。
     * 输入容错：自动忽略空格、下划线、千分位逗号，以及 0x / 0b / 0o 前缀。
     */
    fun convert(raw: String, fromBase: Int, toBase: Int): Result {
        if (!isValidBase(fromBase) || !isValidBase(toBase)) {
            return Result.Failure("进制必须介于 $MIN_BASE 到 $MAX_BASE 之间")
        }

        var text = raw.trim().replace(" ", "").replace("_", "").replace(",", "")
        if (text.isEmpty()) return Result.Failure("请输入待转换的数值")

        val negative = text.startsWith("-")
        text = text.removePrefix("-").removePrefix("+").uppercase()

        text = when {
            fromBase == 16 && text.startsWith("0X") -> text.removePrefix("0X")
            fromBase == 2 && text.startsWith("0B") -> text.removePrefix("0B")
            fromBase == 8 && text.startsWith("0O") -> text.removePrefix("0O")
            else -> text
        }

        if (text.isEmpty()) return Result.Failure("请输入待转换的数值")

        // 非法字符校验：逐个字符比对
        text.forEachIndexed { index, char ->
            val value = digitValue(char)
            if (value < 0) {
                return Result.Failure(
                    "第 ${index + 1} 位「$char」不是合法的进制字符；" +
                        "${fromBase} 进制可用字符为 0-${digitChar(fromBase - 1)}"
                )
            }
            if (value >= fromBase) {
                return Result.Failure(
                    "第 ${index + 1} 位「$char」超出 ${fromBase} 进制范围；" +
                        "${fromBase} 进制最大字符为 ${digitChar(fromBase - 1)}"
                )
            }
        }

        val decimal = toDecimal(text, fromBase)
        val magnitude = fromDecimal(decimal, toBase)
        val isZero = decimal.signum() == 0

        return Result.Success(
            output = if (negative && !isZero) "-$magnitude" else magnitude,
            decimal = if (negative && !isZero) "-$decimal" else decimal.toString(),
            steps = buildSteps(text, fromBase, toBase, decimal, magnitude, negative),
            normalizedInput = if (negative && !isZero) "-$text" else text
        )
    }

    /** 按权展开求和 → 十进制 */
    private fun toDecimal(text: String, base: Int): BigInteger {
        val bigBase = BigInteger.valueOf(base.toLong())
        var accumulator = BigInteger.ZERO
        for (char in text) {
            accumulator = accumulator
                .multiply(bigBase)
                .add(BigInteger.valueOf(digitValue(char).toLong()))
        }
        return accumulator
    }

    /** 除基取余 → 目标进制（不含符号） */
    private fun fromDecimal(value: BigInteger, base: Int): String {
        if (value.signum() == 0) return "0"
        val bigBase = BigInteger.valueOf(base.toLong())
        val builder = StringBuilder()
        var current = value
        while (current.signum() > 0) {
            val (quotient, remainder) = current.divideAndRemainder(bigBase)
            builder.append(digitChar(remainder.toInt()))
            current = quotient
        }
        return builder.reverse().toString()
    }

    // -----------------------------------------------------------------------
    // 步骤说明
    // -----------------------------------------------------------------------

    private fun buildSteps(
        input: String,
        fromBase: Int,
        toBase: Int,
        decimal: BigInteger,
        magnitude: String,
        negative: Boolean
    ): List<String> {
        val steps = mutableListOf<String>()

        steps += "1. 读取输入：$input（${baseName(fromBase)}，共 ${input.length} 位数字）"

        when {
            fromBase == 10 -> steps += "2. 输入已是十进制，无需换算"
            input.length <= MAX_EXPANSION_DIGITS -> {
                val terms = input.mapIndexed { index, char ->
                    val power = input.length - 1 - index
                    val value = digitValue(char)
                    if (power == 0) "$value" else "$value×$fromBase^$power"
                }
                steps += "2. 按权展开求和：${terms.joinToString(" + ")} = $decimal"
            }
            else -> steps += "2. 按权展开求和（位数较多，展开式已略）→ 十进制 $decimal"
        }

        steps += "3. 十进制中间值：${if (negative) "-" else ""}$decimal"

        if (toBase == 10) {
            steps += "4. 目标为十进制，直接输出结果"
        } else {
            steps += "4. 除基取余（不断 ÷ $toBase，记录余数）"
            val bigBase = BigInteger.valueOf(toBase.toLong())
            var current = decimal
            var count = 0
            while (current.signum() > 0 && count < MAX_DIVISION_STEPS) {
                val (quotient, remainder) = current.divideAndRemainder(bigBase)
                steps += "　　$current ÷ $toBase = $quotient 余 $remainder（${digitChar(remainder.toInt())}）"
                current = quotient
                count++
            }
            if (current.signum() > 0) {
                steps += "　　…… 剩余步骤已省略（数字过长）"
            }
            steps += "5. 余数逆序拼接 → ${baseName(toBase)}"
        }

        steps += "✓ 结果：${if (negative) "-" else ""}$magnitude"
        return steps
    }

    // -----------------------------------------------------------------------
    // 预览辅助
    // -----------------------------------------------------------------------

    /**
     * 一次性算出 2 / 8 / 10 / 16 四种常用进制的值，用于界面上的「顺手一看」。
     * 输入非法时返回空 Map。
     */
    fun previewCommonBases(raw: String, fromBase: Int): Map<Int, String> {
        val success = convert(raw, fromBase, 10) as? Result.Success ?: return emptyMap()
        val decimal = success.decimal
        return listOf(2, 8, 10, 16).associateWith { base ->
            (convert(decimal, 10, base) as? Result.Success)?.output ?: ""
        }
    }
}
