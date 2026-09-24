package com.radixlab.app.data

import java.math.BigInteger

/**
 * 大数计算器引擎（App 端）。
 *
 * 与官网 [web/js/calculator.js] 使用**同一套算法与同一句文案**，两端结果一致。
 *
 * 设计要点：
 * - 内部一律用「有理数」表示 —— 分子 / 分母两个 [BigInteger]，每次运算后约分。
 *   好处是加减乘除、乘方、取余全部精确，不存在浮点误差；
 * - 能严格判断结果「是否除得尽」：把分母反复除以 2 和 5，最后剩 1 就是有限小数，
 *   否则一定除不尽 —— 不需要试算多少位才发现循环；
 * - 除得尽 → 输出精确值；除不尽 → 截断到 [DECIMAL_PLACES] 位小数，
 *   界面另附一句 [INFINITE_HINT]；
 * - 结果位数超过 [MAX_RESULT_DIGITS]、乘方指数超过 ±[MAX_EXPONENT] 时给出保护性提示。
 *
 * 纯函数，无 Android 依赖（可在 JVM 单元测试里直接跑）。
 */
object CalculatorEngine {

    /** 结果最多允许的位数，防止 10^99999 这类把内存撑爆 */
    const val MAX_RESULT_DIGITS = 20000

    /** 乘方指数的绝对值上限 */
    const val MAX_EXPONENT = 10000

    /** 除不尽时保留的小数位数 */
    const val DECIMAL_PLACES = 10

    /** 除不尽时展示的说明文案（与官网保持同一句） */
    const val INFINITE_HINT = "别看了，再显示下去你的手机存储空间会占满的"

    /** 算式里的 π 常量：按 [DECIMAL_PLACES] 位截断的近似有理数（π 是无理数，没有精确值） */
    private val PI_APPROX = Rational(BigInteger("6283185307"), BigInteger("2000000000"))

    // -----------------------------------------------------------------------
    // 结果模型
    // -----------------------------------------------------------------------

    sealed interface Result {

        /**
         * @param value 结果文本（十进制，含小数点与负号）
         * @param exact 是否精确值（false = 除不尽或算式里用了 π）
         * @param digits 结果的数字位数（不含符号与小数点）
         * @param approx true = 算式里用到了 π，结果是近似值
         */
        data class Success(
            val value: String,
            val exact: Boolean,
            val digits: Int,
            val approx: Boolean = false
        ) : Result

        /**
         * @param message 面向用户的中文说明
         * @param incomplete true = 算式还没输完（例如只打了「1+」），界面不必报错
         */
        data class Failure(val message: String, val incomplete: Boolean) : Result
    }

    // -----------------------------------------------------------------------
    // 内部：有理数与四则运算
    // -----------------------------------------------------------------------

    /** 计算相关的可预期错误（区别于代码 bug） */
    private class CalcException(message: String) : Exception(message)

    /** 有理数：分子 / 分母（分母恒 > 0，且已约分） */
    private data class Rational(val n: BigInteger, val d: BigInteger)

    private val ZERO = Rational(BigInteger.ZERO, BigInteger.ONE)
    private val ONE = Rational(BigInteger.ONE, BigInteger.ONE)
    private val TWO = BigInteger.valueOf(2)
    private val FIVE = BigInteger.valueOf(5)
    private val TEN = BigInteger.TEN
    private val MAX_EXP = BigInteger.valueOf(MAX_EXPONENT.toLong())

    private fun gcd(a0: BigInteger, b0: BigInteger): BigInteger {
        var a = a0.abs()
        var b = b0.abs()
        while (b != BigInteger.ZERO) {
            val t = a % b
            a = b
            b = t
        }
        return a
    }

    /** 规范化：分母转正、约分；分母为 0 直接报错 */
    private fun rational(n: BigInteger, d: BigInteger): Rational {
        if (d == BigInteger.ZERO) throw CalcException("除数不能为 0")
        var nn = n
        var dd = d
        if (dd.signum() < 0) {
            nn = nn.negate()
            dd = dd.negate()
        }
        if (nn == BigInteger.ZERO) return ZERO
        val g = gcd(nn, dd)
        return if (g > BigInteger.ONE) Rational(nn / g, dd / g) else Rational(nn, dd)
    }

    private fun add(a: Rational, b: Rational) =
        rational(a.n * b.d + b.n * a.d, a.d * b.d)

    private fun sub(a: Rational, b: Rational) =
        rational(a.n * b.d - b.n * a.d, a.d * b.d)

    private fun mul(a: Rational, b: Rational) =
        rational(a.n * b.n, a.d * b.d)

    private fun div(a: Rational, b: Rational): Rational {
        if (b.n == BigInteger.ZERO) throw CalcException("除数不能为 0")
        return rational(a.n * b.d, a.d * b.n)
    }

    private fun neg(a: Rational) = Rational(a.n.negate(), a.d)

    /** 向下取整的整数除法（b 必须为正） */
    private fun floorDiv(a: BigInteger, b: BigInteger): BigInteger {
        if (b.signum() <= 0) throw CalcException("内部错误：floorDiv 需要正除数")
        var q = a / b
        if (a % b != BigInteger.ZERO && a.signum() < 0) q -= BigInteger.ONE
        return q
    }

    /** 有理数的向下取整（自动处理分母为负的情况） */
    private fun floorRational(num0: BigInteger, den0: BigInteger): BigInteger {
        if (den0 == BigInteger.ZERO) throw CalcException("除数不能为 0")
        var num = num0
        var den = den0
        if (den.signum() < 0) {
            num = num.negate()
            den = den.negate()
        }
        return floorDiv(num, den)
    }

    /** 乘方：指数必须是整数，支持负指数 */
    private fun pow(base: Rational, exponent: BigInteger): Rational {
        if (exponent > MAX_EXP || exponent < MAX_EXP.negate()) {
            throw CalcException("乘方指数过大，请控制在 ±$MAX_EXPONENT 以内")
        }
        if (exponent == BigInteger.ZERO) return ONE

        val negative = exponent.signum() < 0
        val abs = exponent.abs()
        if (negative && base.n == BigInteger.ZERO) throw CalcException("0 不能作为负指数")

        val e = abs.toInt()
        val n = base.n.pow(e)
        val d = base.d.pow(e)
        return if (negative) rational(d, n) else rational(n, d)
    }

    /** 取余：a % b = a - floor(a / b) × b（除数为负也成立） */
    private fun rem(a: Rational, b: Rational): Rational {
        if (b.n == BigInteger.ZERO) throw CalcException("除数不能为 0")
        val q = floorRational(a.n * b.d, a.d * b.n)
        return sub(a, Rational(b.n * q, b.d))
    }

    // -----------------------------------------------------------------------
    // 词法：把算式字符串切成 token
    // -----------------------------------------------------------------------

    /** 全角 → 半角，去掉空格、千分位逗号与下划线（与官网 normalize 完全一致） */
    fun normalize(expression: String?): String {
        val raw = expression ?: ""
        val sb = StringBuilder(raw.length)
        for (ch in raw) {
            when {
                ch in '\uFF10'..'\uFF19' -> sb.append((ch.code - 0xFEE0).toChar())
                ch == '\uFF0E' -> sb.append('.')                                    // ．
                ch == '×' -> sb.append('*')
                ch == '÷' -> sb.append('/')
                ch == '\u2212' || ch == '\u2013' || ch == '\u2014' || ch == '\uFF0D' -> sb.append('-')
                ch == '\uFF0B' -> sb.append('+')                                    // ＋
                ch == '\uFF0A' -> sb.append('*')                                    // ＊
                ch == '\uFF0F' -> sb.append('/')                                    // ／
                ch == '\uFF05' -> sb.append('%')                                    // ％
                ch == '\uFF3E' -> sb.append('^')                                    // ＾
                ch == '\uFF08' -> sb.append('(')                                    // （
                ch == '\uFF09' -> sb.append(')')                                    // ）
                ch == ',' || ch == '_' || ch.isWhitespace() -> Unit
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    private sealed interface Token {
        class Num(val value: Rational, val pi: Boolean = false) : Token
        class Op(val symbol: Char) : Token
        object LParen : Token
        object RParen : Token
    }

    private fun tokenize(source: String): List<Token> {
        val text = normalize(source)
        val tokens = ArrayList<Token>(text.length)
        var i = 0

        while (i < text.length) {
            val ch = text[i]

            if (ch in '0'..'9' || ch == '.') {
                var j = i
                if (ch == '.') {
                    j = i + 1
                    while (j < text.length && text[j] in '0'..'9') j++
                    if (j == i + 1) throw CalcException("第 ${i + 1} 个字符「$ch」不是合法的数字")
                } else {
                    while (j < text.length && text[j] in '0'..'9') j++
                    if (j < text.length && text[j] == '.') {
                        j++
                        while (j < text.length && text[j] in '0'..'9') j++
                    }
                }
                val literal = text.substring(i, j)
                if (literal.count { it == '.' } > 1) {
                    throw CalcException("数字「$literal」里出现了多个小数点")
                }
                tokens.add(Token.Num(literalToRational(literal)))
                i = j
                continue
            }

            if (ch in "+-*/%^") {
                tokens.add(Token.Op(ch))
                i++
                continue
            }

            // π 常量：π / Π 一个字符，或 pi / PI 两个字母，都认
            if (ch == 'π' || ch == 'Π') {
                tokens.add(Token.Num(PI_APPROX, pi = true))
                i++
                continue
            }
            if ((ch == 'p' || ch == 'P') && i + 1 < text.length &&
                (text[i + 1] == 'i' || text[i + 1] == 'I')
            ) {
                tokens.add(Token.Num(PI_APPROX, pi = true))
                i += 2
                continue
            }

            if (ch == '(') {
                tokens.add(Token.LParen)
                i++
                continue
            }

            if (ch == ')') {
                tokens.add(Token.RParen)
                i++
                continue
            }

            throw CalcException("算式里有无法识别的字符「$ch」")
        }

        return tokens
    }

    /** '12.34' → 1234 / 100 */
    private fun literalToRational(literal: String): Rational {
        val dot = literal.indexOf('.')
        if (dot < 0) return Rational(BigInteger(literal), BigInteger.ONE)

        val intPart = if (dot == 0) "0" else literal.substring(0, dot)
        val fracPart = literal.substring(dot + 1)
        return rational(BigInteger(intPart + fracPart), TEN.pow(fracPart.length))
    }

    // -----------------------------------------------------------------------
    // 语法（递归下降）
    //   expr    := term (('+' | '-') term)*
    //   term    := unary (('*' | '/' | '%') unary)*
    //   unary   := ('-' | '+') unary | factor
    //   factor  := primary ('^' unary)?        右结合，指数可为负
    //   primary := number | '(' expr ')'
    // -----------------------------------------------------------------------

    private class Parser(private val tokens: List<Token>) {

        private var pos = 0

        /** 算式里是否用到了 π（用到就说明结果是近似值） */
        var usedPi = false
            private set

        private fun peek(): Token? = if (pos < tokens.size) tokens[pos] else null
        private fun next(): Token? = if (pos < tokens.size) tokens[pos++] else null

        fun parse(): Rational {
            val value = parseExpr()
            val rest = peek()
            if (rest != null) {
                throw CalcException(
                    if (rest is Token.RParen) "多了一个「)」" else "算式第 ${pos + 1} 处无法解析"
                )
            }
            return value
        }

        private fun parseExpr(): Rational {
            var left = parseTerm()
            while (true) {
                val token = peek()
                if (token !is Token.Op || (token.symbol != '+' && token.symbol != '-')) break
                next()
                val right = parseTerm()
                left = if (token.symbol == '+') add(left, right) else sub(left, right)
            }
            return left
        }

        private fun parseTerm(): Rational {
            var left = parseUnary()
            while (true) {
                val token = peek()
                if (token !is Token.Op || token.symbol !in "*/%") break
                next()
                val right = parseUnary()
                left = when (token.symbol) {
                    '*' -> mul(left, right)
                    '/' -> div(left, right)
                    else -> rem(left, right)
                }
            }
            return left
        }

        private fun parseUnary(): Rational {
            val token = peek()
            if (token is Token.Op && (token.symbol == '-' || token.symbol == '+')) {
                next()
                val value = parseUnary()
                return if (token.symbol == '-') neg(value) else value
            }
            return parseFactor()
        }

        private fun parseFactor(): Rational {
            val base = parsePrimary()
            val token = peek()
            if (token is Token.Op && token.symbol == '^') {
                next()
                val exponent = parseUnary() // 允许 2^-1
                if (exponent.d != BigInteger.ONE) {
                    throw CalcException("乘方的指数必须是整数，暂不支持 2^0.5 这类开方")
                }
                return pow(base, exponent.n)
            }
            return base
        }

        private fun parsePrimary(): Rational {
            val token = next()
                ?: throw CalcException("算式意外结束，请检查是否漏了数字或右括号")
            return when (token) {
                is Token.Num -> {
                    if (token.pi) usedPi = true
                    token.value
                }
                is Token.LParen -> {
                    val value = parseExpr()
                    val close = next()
                    if (close !is Token.RParen) throw CalcException("括号没有成对，缺少「)」")
                    value
                }
                is Token.RParen -> throw CalcException("多了一个「)」")
                is Token.Op -> throw CalcException("运算符「${token.symbol}」的位置不对")
            }
        }
    }

    // -----------------------------------------------------------------------
    // 输出：有理数 → 十进制字符串
    // -----------------------------------------------------------------------

    /** 把缩放后的整数还原成带小数点的字符串（可自动去掉末尾多余的 0） */
    private fun withDecimalPoint(value: BigInteger, scale: Int, trimZeros: Boolean): String {
        var text = value.toString()
        if (scale <= 0) return text

        text = text.padStart(scale + 1, '0')
        val cut = text.length - scale
        val intPart = text.substring(0, cut)
        var fracPart = text.substring(cut)
        if (trimZeros) fracPart = fracPart.trimEnd('0')
        return if (fracPart.isNotEmpty()) "$intPart.$fracPart" else intPart
    }

    /** 有理数 → 显示文本；second = false 表示除不尽、已截断 */
    private fun toDecimalString(value: Rational): Pair<String, Boolean> {
        val negative = value.n.signum() < 0
        val sign = if (negative) "-" else ""
        val absN = if (negative) value.n.negate() else value.n
        val d = value.d

        // 整数
        if (d == BigInteger.ONE) return (sign + absN.toString()) to true

        // 分母的质因数只有 2 / 5 → 有限小数
        var rest = d
        var twos = 0
        var fives = 0
        while (rest % TWO == BigInteger.ZERO) {
            rest /= TWO
            twos++
        }
        while (rest % FIVE == BigInteger.ZERO) {
            rest /= FIVE
            fives++
        }

        if (rest == BigInteger.ONE) {
            val scale = maxOf(twos, fives)
            val scaled = absN * TEN.pow(scale) / d
            return (sign + withDecimalPoint(scaled, scale, trimZeros = true)) to true
        }

        // 无限小数：截断到 DECIMAL_PLACES 位
        val scale = DECIMAL_PLACES
        val scaled = absN * TEN.pow(scale) / d
        return (sign + withDecimalPoint(scaled, scale, trimZeros = false)) to false
    }

    /** 数字位数（不含符号与小数点） */
    private fun countDigits(text: String): Int = text.count { it in '0'..'9' }

    /** 括号是否没闭合 / 是否以运算符结尾 —— 用于输入中途的「未完成」判定 */
    private fun looksIncomplete(source: String): Boolean {
        val text = normalize(source)
        if (text.isEmpty()) return true

        var depth = 0
        for (ch in text) {
            if (ch == '(') depth++
            else if (ch == ')') depth--
            if (depth < 0) return false
        }
        if (depth > 0) return true

        return text.last() in "+-*/%^("
    }

    // -----------------------------------------------------------------------
    // 对外入口
    // -----------------------------------------------------------------------

    /**
     * 计算一个算式。输入可以是半角也可以是全角，允许空格与千分位逗号。
     *
     * 不抛异常：[Result.Failure] 承载所有可预期错误，界面直接展示 [Result.Failure.message]。
     */
    fun evaluate(expression: String?): Result {
        val source = expression ?: ""
        if (normalize(source).isEmpty()) {
            return Result.Failure("请输入算式", incomplete = true)
        }

        return try {
            val parser = Parser(tokenize(source))
            val value = parser.parse()
            val (text, exact) = toDecimalString(value)

            val digits = countDigits(text)
            if (digits > MAX_RESULT_DIGITS) {
                throw CalcException(
                    "结果有 $digits 位数字，超过 $MAX_RESULT_DIGITS 位的上限，请把算式拆小一点"
                )
            }

            Result.Success(
                value = text,
                exact = exact && !parser.usedPi,
                digits = digits,
                approx = parser.usedPi
            )
        } catch (error: CalcException) {
            Result.Failure(
                message = error.message ?: "算式无法解析，请检查括号与运算符",
                incomplete = looksIncomplete(source)
            )
        } catch (error: ArithmeticException) {
            // BigInteger 过大 / 指数爆炸
            Result.Failure("结果过大，已超出计算范围，请把算式拆小一点", incomplete = false)
        }
    }
}
