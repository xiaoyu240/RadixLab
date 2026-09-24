package com.radixlab.app.data

import java.math.BigInteger

/**
 * 圆周率（π）引擎（App 端）。
 *
 * 与官网 [web/js/pi.js] 使用**同一套算法、同一份常量、同一份文案**，两端结果与提示完全一致。
 *
 * 算法：Chudnovsky 公式 + 二进制分裂
 * ```
 * π = 426880 · √10005 · Q / T
 * S = Σ (−1)^k (6k)! (13591409 + 545140134k) / ((3k)! (k!)^3 640320^(3k)) = T / Q
 * ```
 * 每项约提供 14.1816 位十进制精度，项数 n ≈ 位数 / 14.1816。
 *
 * 整数开方自己实现（[BigInteger.sqrt] 需要 Android API 33+）：
 * 精度逐级翻倍的牛顿法，最后全精度收尾 + 平方比较兜底。
 *
 * 注意：计算很吃 CPU，务必放在后台线程（协程 [Dispatchers.Default]）里跑；
 * 长任务可通过 [computePi] 的 `cancelled` 回调协作取消。
 */
object PiEngine {

    private val ZERO: BigInteger = BigInteger.ZERO
    private val ONE: BigInteger = BigInteger.ONE
    private val TWO: BigInteger = BigInteger.valueOf(2)
    private val TEN: BigInteger = BigInteger.TEN

    /** (640320³) / 24 */
    private val C3_OVER_24: BigInteger = BigInteger.valueOf(640320).pow(3).divide(BigInteger.valueOf(24))

    /** 每项约提供的小数位数 */
    private const val DIGITS_PER_TERM = 14.181647462725477

    /** 计算时多留的保险位数 */
    private const val GUARD_DIGITS = 20

    /** 结果位数的硬上限（引擎自我保护，正常流程走不到这里） */
    const val PI_MAX_DIGITS = 12_000_000

    // -----------------------------------------------------------------------
    // 1 MB / 10 MB 分级（与官网 js/pi.js 完全一致的常量）
    // -----------------------------------------------------------------------

    /** 第一次提醒的位数：超过 50 位小数就弹「存储空间会占满」 */
    const val PI_FIRST_WARN_DIGITS = 50

    /** 第一档目标位数（实测约占用 1 MB） */
    const val PI_FIRST_TIER_DIGITS = 300_000

    /** 每档增加的占用（字节）= 1 MB */
    const val PI_TIER_BYTES = 1_048_576L

    /** 强制停止的占用（字节）= 10 MB */
    const val PI_CAP_BYTES = 10L * 1_048_576L

    /** 字节 → 「x.xx MB」 */
    fun formatMB(bytes: Long): String = String.format(java.util.Locale.US, "%.2f MB", bytes / 1048576.0)

    /** 下一档的位数：让占用再涨约 1 MB（按上一档的实测占用自适应） */
    fun nextTierDigits(digits: Int, occupiedBytes: Long): Int {
        val grow = (occupiedBytes + PI_TIER_BYTES).toDouble() / occupiedBytes
        return maxOf(digits + 1, Math.round(digits * grow).toInt())
    }

    // -----------------------------------------------------------------------
    // 提示文案 —— 与官网 js/pi.js 逐字一致
    // -----------------------------------------------------------------------

    /** 一个提醒弹窗的内容 */
    data class Dialog(val title: String, val body: String, val cancel: String?, val confirm: String)

    /** 第一次（50 位）提醒 */
    fun firstDialog(): Dialog = Dialog(
        title = "还要继续算下去吗？",
        body = "π 是无限不循环小数，现在已经算到 $PI_FIRST_WARN_DIGITS 位小数了。\n\n" +
            "再算下去，你的手机存储空间会占满的。\n" +
            "而且算得越多，内存和 CPU 占用都会越来越大，手机可能会变慢、发热。",
        cancel = "先算了",
        confirm = "继续算"
    )

    /** 每一档（每 1 MB）提醒 */
    fun tierDialog(digits: Int, mb: String): Dialog = Dialog(
        title = "已经占到 $mb 啦",
        body = "π 已经算到 $digits 位，占用约 $mb。\n\n" +
            "内存和 CPU 占用还在继续变大，手机可能会变慢、发热。\n" +
            "还要继续算下去吗？",
        cancel = "停下来",
        confirm = "继续算"
    )

    /** 到达 10 MB 强制停止 */
    fun capDialog(digits: Int, mb: String): Dialog = Dialog(
        title = "已经 10 MB 啦，不能再算啦！",
        body = "π 已经算到 $digits 位，占用约 $mb。\n\n" +
            "再算下去手机就该冒烟了，到此为止吧。\n" +
            "算出来的 π 还在，可以复制带走。",
        cancel = null,
        confirm = "好的，收下"
    )

    // -----------------------------------------------------------------------
    // 结果
    // -----------------------------------------------------------------------

    /**
     * @param value        '3.1415926535…'，共 [digits] 位小数
     * @param digits       小数位数
     * @param terms        Chudnovsky 用了多少项
     * @param bigBytes     参与运算的大数实际占用（字节）
     * @param textBytes    结果文本占用（1 位 = 1 字节）
     * @param occupiedBytes 上面两项之和 = 本次真实占用
     */
    data class Pi(
        val value: String,
        val digits: Int,
        val terms: Int,
        val bigBytes: Long,
        val textBytes: Long,
        val occupiedBytes: Long
    )

    /** 用户中途放弃（协作取消）时抛出，界面据此静默收尾 */
    class Cancelled : RuntimeException("π 计算已取消")

    // -----------------------------------------------------------------------
    // 二进制分裂：把 sum_{k=a}^{b-1} 拆成 [P, Q, T]，S = T / Q
    // -----------------------------------------------------------------------

    private fun bs(a: Long, b: Long, cancelled: (() -> Boolean)?, size: Long): Array<BigInteger> {
        // 只在区间够大时查取消，避免递归开销
        if (cancelled != null && b - a > 1024 && cancelled()) throw Cancelled()

        if (b - a == 1L) {
            if (a == 0L) {
                return arrayOf(ONE, ONE, BigInteger.valueOf(13591409L))
            }
            val k = BigInteger.valueOf(a)
            val pab = k.multiply(BigInteger.valueOf(6)).subtract(BigInteger.valueOf(5))
                .multiply(k.multiply(TWO).subtract(ONE))
                .multiply(k.multiply(BigInteger.valueOf(6)).subtract(ONE))
            val qab = k.pow(3).multiply(C3_OVER_24)
            var tab = pab.multiply(BigInteger.valueOf(13591409L).add(BigInteger.valueOf(545140134L).multiply(k)))
            if (a and 1L == 1L) tab = tab.negate()
            return arrayOf(pab, qab, tab)
        }

        val m = (a + b) ushr 1
        val left = bs(a, m, cancelled, size)
        val right = bs(m, b, cancelled, size)
        return arrayOf(
            left[0].multiply(right[0]),
            left[1].multiply(right[1]),
            right[1].multiply(left[2]).add(left[0].multiply(right[2]))
        )
    }

    // -----------------------------------------------------------------------
    // 整数开方
    // -----------------------------------------------------------------------

    /** 不超过 2^62 的整数开方 */
    private fun isqrtSmall(n: BigInteger): BigInteger {
        if (n.signum() == 0) return ZERO
        var v = BigInteger.valueOf(Math.sqrt(n.toDouble()).toLong())
        if (v.signum() < 1) v = ONE
        while (v.multiply(v).compareTo(n) > 0) v = v.subtract(ONE)
        while (v.add(ONE).multiply(v.add(ONE)).compareTo(n) <= 0) v = v.add(ONE)
        return v
    }

    /**
     * 整数开方：返回 floor(√n)。
     *
     * 不变量：x = floor(√M)，M = n >> 2·(half − p)
     * 移位量 2·(half − p) 恒为偶数 —— 这是关键：若改用 n >> (bits − 2p)，
     * bits 为奇数时缩放关系会差一个 √2（41% 误差），牛顿法要爬十几轮才收敛。
     */
    internal fun isqrt(n: BigInteger, cancelled: (() -> Boolean)? = null): BigInteger {
        if (n.signum() < 0) throw ArithmeticException("isqrt: 不能对负数开方")
        if (n.signum() == 0) return ZERO

        val bits = n.bitLength()
        if (bits <= 62) return isqrtSmall(n)

        val half = (bits + 1) ushr 1              // floor(√n) 的位长
        var cur = 32
        var x = isqrtSmall(n.shiftRight(2 * (half - cur)))

        while (cur < half) {
            val next = Math.min(half, cur * 2)
            if (cancelled != null && cancelled()) throw Cancelled()
            val ns = n.shiftRight(2 * (half - next))          // M_next
            var xs = x.shiftLeft(next - cur).add(ONE)         // 相对误差 ~2^-cur
            xs = xs.add(ns.divide(xs)).shiftRight(1)          // 牛顿 → 2^-2cur
            if (next == cur * 2) xs = xs.add(ns.divide(xs)).shiftRight(1)
            x = xs
            cur = next
        }

        // 收尾：1~2 次牛顿到 floor(√n)，再用平方比较兜底保证精确
        for (i in 0 until 4) {
            val y = x.add(n.divide(x)).shiftRight(1)
            if (i > 0 && y.compareTo(x) >= 0) break
            x = y
        }
        while (x.multiply(x).compareTo(n) > 0) x = x.subtract(ONE)
        while (x.add(ONE).multiply(x.add(ONE)).compareTo(n) <= 0) x = x.add(ONE)
        return x
    }

    // -----------------------------------------------------------------------
    // 算 π
    // -----------------------------------------------------------------------

    /**
     * 把 π 算到指定小数位数（真算，不是查表）。
     *
     * @param digits    小数位数
     * @param cancelled 长任务协作取消：返回 true 则抛 [Cancelled]；传 null 表示不取消
     */
    fun computePi(digits: Int, cancelled: (() -> Boolean)? = null): Pi {
        if (digits <= 0) throw IllegalArgumentException("位数必须是正整数")
        if (digits > PI_MAX_DIGITS) throw IllegalArgumentException("一次最多算 $PI_MAX_DIGITS 位，请调小一点")

        val d = digits + GUARD_DIGITS
        val terms = Math.ceil(d / DIGITS_PER_TERM).toLong() + 1

        val split = bs(0L, terms, cancelled, terms)
        val p = split[0]
        val q = split[1]
        val t = split[2]

        // √(10005 · 10^(2d)) = √10005 放大 d 位
        val sqrtC = isqrt(BigInteger.valueOf(10005).multiply(TEN.pow(2 * d)), cancelled)

        val piScaled = BigInteger.valueOf(426880).multiply(q).multiply(sqrtC)
            .divide(t)
            .divide(TEN.pow(GUARD_DIGITS))

        var text = piScaled.toString()
        if (text.length < digits + 1) text = text.padStart(digits + 1, '0')
        val value = text.substring(0, 1) + "." + text.substring(1)

        val bigBits = (p.bitLength() + q.bitLength() + t.bitLength() + sqrtC.bitLength()).toLong()
        val bigBytes = (bigBits + 7) / 8
        val textBytes = text.length.toLong()

        return Pi(
            value = value,
            digits = digits,
            terms = terms.toInt(),
            bigBytes = bigBytes,
            textBytes = textBytes,
            occupiedBytes = bigBytes + textBytes
        )
    }
}
