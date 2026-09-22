package com.radixlab.app.data

/**
 * IP 地址工具。
 *
 * IPv4：点分十进制 ↔ 二进制 ↔ 十六进制 ↔ 十进制整数，四向互转，每段严格校验 0-255。
 * IPv6：压缩形式 ↔ 完整展开（RFC 5952 风格），支持内嵌 IPv4 与 zone id。
 *
 * 全部为纯函数，无副作用、无网络访问。
 */
object IpUtils {

    sealed interface Result {
        data class Success(val value: String) : Result
        data class Failure(val message: String) : Result
    }

    private const val IPV4_MAX = 0xFFFFFFFFL

    // =======================================================================
    // IPv4 —— 解析与校验
    // =======================================================================

    /** 解析点分十进制，合法返回长度为 4 的数组，否则返回 null */
    fun parseIpv4(input: String): IntArray? {
        if (validateIpv4(input) != null) return null
        return input.trim().replace(" ", "").split(".").map { it.toInt() }.toIntArray()
    }

    /** 校验并返回错误文案；合法返回 null */
    fun validateIpv4(input: String): String? {
        val text = input.trim().replace(" ", "")
        if (text.isEmpty()) return "请输入 IPv4 地址"
        val parts = text.split(".")
        if (parts.size != 4) {
            return "IPv4 必须由 4 段组成，以「.」分隔（当前 ${parts.size} 段）"
        }
        parts.forEachIndexed { index, part ->
            val position = index + 1
            when {
                part.isEmpty() -> return "第 $position 段为空"
                !part.all { it.isDigit() } -> return "第 $position 段「$part」含非数字字符"
                part.length > 3 -> return "第 $position 段「$part」位数过多（最多 3 位）"
                (part.toIntOrNull() ?: -1) !in 0..255 -> return "第 $position 段「$part」超出 0-255 范围"
            }
        }
        return null
    }

    fun isValidIpv4(input: String): Boolean = validateIpv4(input) == null

    // =======================================================================
    // IPv4 —— 各进制表示
    // =======================================================================

    /** 点分十进制 → 点分二进制，如 11000000.10101000.00000001.00000001 */
    fun ipv4ToBinary(input: String): String? {
        val octets = parseIpv4(input) ?: return null
        return octets.joinToString(".") { it.toString(2).padStart(8, '0') }
    }

    /** 点分十进制 → 点分十六进制，如 C0.A8.01.01 */
    fun ipv4ToHex(input: String): String? {
        val octets = parseIpv4(input) ?: return null
        return octets.joinToString(".") { it.toString(16).uppercase().padStart(2, '0') }
    }

    /** 点分十进制 → 32 位无符号整数（用 Long 承载） */
    fun ipv4ToLong(input: String): Long? {
        val octets = parseIpv4(input) ?: return null
        var value = 0L
        for (octet in octets) {
            value = (value shl 8) or octet.toLong()
        }
        return value
    }

    /** 点分十进制 → 十进制整数字符串 */
    fun ipv4ToDecimal(input: String): String? = ipv4ToLong(input)?.toString()

    /** 32 位整数 → 点分十进制 */
    fun longToIpv4(value: Long): String? {
        if (value < 0L || value > IPV4_MAX) return null
        return buildString {
            append((value shr 24) and 0xFF).append('.')
            append((value shr 16) and 0xFF).append('.')
            append((value shr 8) and 0xFF).append('.')
            append(value and 0xFF)
        }
    }

    /** 32 位二进制（可带点分隔）→ 点分十进制 */
    fun binaryToIpv4(input: String): String? {
        val text = input.trim().replace(" ", "").replace(".", "")
        if (text.length != 32) return null
        if (!text.all { it == '0' || it == '1' }) return null
        return (0 until 4).joinToString(".") { index ->
            text.substring(index * 8, index * 8 + 8).toInt(2).toString()
        }
    }

    /** 十六进制（可带点分隔 / 0x 前缀）→ 点分十进制 */
    fun hexToIpv4(input: String): String? {
        var text = input.trim().replace(" ", "").replace(".", "")
        text = text.removePrefix("0x").removePrefix("0X").removePrefix("0X")
        if (text.isEmpty() || text.length > 8) return null
        if (!text.all { it.isDigit() || it.uppercaseChar() in 'A'..'F' }) return null
        val value = text.toLongOrNull(16) ?: return null
        return longToIpv4(value)
    }

    /** 十进制整数（可带千分位）→ 点分十进制 */
    fun decimalToIpv4(input: String): String? {
        val text = input.trim().replace(" ", "").replace(",", "")
        if (text.isEmpty() || !text.all { it.isDigit() }) return null
        val value = text.toLongOrNull() ?: return null
        return longToIpv4(value)
    }

    /** 逐段演示，供界面展示「转换步骤」 */
    fun ipv4Steps(input: String): List<String> {
        val octets = parseIpv4(input) ?: return emptyList()
        val dottedBinary = octets.joinToString(".") { it.toString(2).padStart(8, '0') }
        val dottedHex = octets.joinToString(".") { it.toString(16).uppercase().padStart(2, '0') }
        val value = ipv4ToLong(input) ?: return emptyList()

        return listOf(
            "1. 校验：4 段均落在 0-255 范围内 ✓",
            "2. 逐段转二进制：" + octets.joinToString("，") { "$it → ${it.toString(2).padStart(8, '0')}" },
            "3. 拼接为 32 位二进制：$dottedBinary",
            "4. 逐段转十六进制：$dottedHex",
            "5. 合成 32 位整数：${octets[0]}×256³ + ${octets[1]}×256² + ${octets[2]}×256 + ${octets[3]} = $value",
            "✓ 该整数即十进制表示：$value"
        )
    }

    // =======================================================================
    // IPv6
    // =======================================================================

    /** 解析为 8 组 16 位整数；非法返回 null */
    fun parseIpv6(input: String): IntArray? {
        var text = input.trim().replace(" ", "").lowercase()
        if (text.isEmpty()) return null
        if (text.startsWith("[") && text.endsWith("]")) {
            text = text.substring(1, text.length - 1)
        }
        if (text.contains("%")) {
            text = text.substringBefore("%") // 去掉 zone id，如 fe80::1%wlan0
        }
        if (text.isEmpty()) return null

        // 兼容内嵌 IPv4，如 ::ffff:192.168.1.1
        var head = text
        if (text.contains(".")) {
            val lastColon = text.lastIndexOf(':')
            if (lastColon < 0) return null
            val octets = parseIpv4(text.substring(lastColon + 1)) ?: return null
            val high = ((octets[0] shl 8) or octets[1]).toString(16)
            val low = ((octets[2] shl 8) or octets[3]).toString(16)
            head = text.substring(0, lastColon + 1) + high + ":" + low
        }

        val doubleColonIndex = head.indexOf("::")
        val hasCompression = doubleColonIndex >= 0
        if (hasCompression && head.indexOf("::", doubleColonIndex + 1) >= 0) return null
        if (head.contains(":::")) return null

        val left: List<String>
        val right: List<String>
        if (hasCompression) {
            left = head.substring(0, doubleColonIndex).split(":").filter { it.isNotEmpty() }
            right = head.substring(doubleColonIndex + 2).split(":").filter { it.isNotEmpty() }
        } else {
            left = head.split(":").filter { it.isNotEmpty() }
            right = emptyList()
        }

        val leftValues = mutableListOf<Int>()
        for (token in left) {
            leftValues.add(parseIpv6Group(token) ?: return null)
        }
        val rightValues = mutableListOf<Int>()
        for (token in right) {
            rightValues.add(parseIpv6Group(token) ?: return null)
        }

        if (!hasCompression && leftValues.size != 8) return null
        if (hasCompression && leftValues.size + rightValues.size > 7) return null

        val zeroCount = 8 - leftValues.size - rightValues.size
        val all = leftValues + List(zeroCount) { 0 } + rightValues
        return all.toIntArray()
    }

    private fun parseIpv6Group(token: String): Int? {
        if (token.isEmpty() || token.length > 4) return null
        if (!token.all { it.isDigit() || it in 'a'..'f' }) return null
        return token.toIntOrNull(16)
    }

    fun isValidIpv6(input: String): Boolean = parseIpv6(input) != null

    /** 压缩形式（RFC 5952：最长连续 0 段用 :: 代替，并取最靠前的一段） */
    fun compressIpv6(input: String): String? {
        val groups = parseIpv6(input) ?: return null

        var bestStart = -1
        var bestLength = 0
        var index = 0
        while (index < groups.size) {
            if (groups[index] == 0) {
                var end = index
                while (end < groups.size && groups[end] == 0) end++
                val length = end - index
                if (length > bestLength) {
                    bestLength = length
                    bestStart = index
                }
                index = end
            } else {
                index++
            }
        }

        val parts = groups.map { Integer.toHexString(it) }
        return if (bestLength >= 2) {
            val head = parts.subList(0, bestStart).joinToString(":")
            val tail = parts.subList(bestStart + bestLength, groups.size).joinToString(":")
            "$head::$tail"
        } else {
            parts.joinToString(":")
        }
    }

    /** 完整展开形式：8 组、每组 4 位十六进制 */
    fun expandIpv6(input: String): String? {
        val groups = parseIpv6(input) ?: return null
        return groups.joinToString(":") { Integer.toHexString(it).padStart(4, '0') }
    }

    /** 逐组十六进制，便于观察分段 */
    fun ipv6GroupHex(input: String): String? {
        val groups = parseIpv6(input) ?: return null
        return groups.joinToString(" ") { Integer.toHexString(it).padStart(4, '0') }
    }

    /** 校验并返回 IPv6 错误文案；合法返回 null */
    fun validateIpv6(input: String): String? {
        val text = input.trim()
        if (text.isEmpty()) return "请输入 IPv6 地址"
        if (parseIpv6(text) != null) return null
        return when {
            text.contains(":::") -> "出现连续的「:::」，双冒号最多只能出现一次"
            text.count { it == ':' } > 7 && !text.contains("::") -> "段数过多：IPv6 最多 8 组"
            text.contains("::") && text.split("::").size > 2 -> "双冒号「::」最多只能出现一次"
            !text.contains(":") -> "IPv6 地址至少需要包含「:」"
            else -> "不是合法的 IPv6 地址，请检查分组是否超过 4 位或含非法字符"
        }
    }

    /** 简易类型判断，用于界面上的自动识别提示 */
    fun describe(input: String): String = when {
        input.isBlank() -> "等待输入"
        isValidIpv4(input) -> "IPv4 地址"
        isValidIpv6(input) -> "IPv6 地址"
        else -> "无法识别的地址"
    }
}
