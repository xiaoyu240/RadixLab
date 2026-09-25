package com.radixlab.app.ui.terminal

import com.radixlab.app.data.CalculatorEngine
import com.radixlab.app.data.ConversionEngine
import com.radixlab.app.data.IpUtils
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object TerminalEngine {

    const val USER = "radix"
    const val HOST = "void"
    const val HOME = "/home/$USER"

    enum class Kind { OUT, IN, ERR, OK, DIM }

    data class Line(val text: String, val kind: Kind = Kind.OUT)

    data class MatrixOptions(
        val async: Boolean = true,
        val bold: Boolean = true,
        val color: String = "green",
        val delayMs: Int = 33,
        val screensaver: Boolean = false,
        val rainbow: Boolean = false
    )

    sealed interface Action {
        data object Exit : Action

        data class Matrix(val options: MatrixOptions) : Action

        data object Clear : Action
    }

    data class Shell(
        val cwd: String = HOME,
        val history: List<String> = emptyList()
    )

    data class Outcome(
        val lines: List<Line>,
        val shell: Shell,
        val action: Action? = null,
        val echoAfterMatrix: Line? = null
    )

    private val DIRS: Map<String, List<String>> = mapOf(
        "/" to listOf("bin", "dev", "etc", "home", "proc", "usr", "var"),
        "/bin" to listOf("sh", "ls", "cat", "echo"),
        "/dev" to emptyList(),
        "/etc" to listOf("motd", "radixlab.conf"),
        "/home" to listOf("radix"),
        "/home/radix" to listOf(".bashrc", "README.md", "notes.txt", "radix.conf"),
        "/proc" to listOf("cpuinfo", "uptime", "version"),
        "/usr" to listOf("bin"),
        "/usr/bin" to listOf("calc", "cmatrix", "ip", "neofetch", "radix"),
        "/var" to listOf("log"),
        "/var/log" to listOf("radixlab.log")
    )

    private val FILES: Map<String, String> = mapOf(
        "/etc/motd" to
            """
            ========================================================
              RadixLab Virtual Terminal  ·  进制工坊虚拟终端
              一切运行在内存里，不接触真机。
              输入 help 看看能做什么。
            ========================================================
            """.trimIndent(),

        "/etc/radixlab.conf" to
            """
            # RadixLab 配置（虚拟）
            base.min = 2
            base.max = 36
            calc.precision = 10
            pi.tier.bytes = 1048576
            pi.cap.bytes = 10485760
            network = never            # 本应用从不联网
            permissions = none         # AndroidManifest 里一条权限都没申请
            mode = virtual             # 本终端不接触真实设备
            """.trimIndent(),

        "/home/radix/README.md" to
            """
            # 你好，${USER}
            这里是 $HOST 的虚拟家目录。
            所有文件都是假的 —— 你正在应用里，而非真机 shell。
            试试这些：
              neofetch        看一段「系统信息」（虚构的）
              cmatrix         看矩阵雨，Ctrl+C 或 q 退出
              radix 255 10 16 真算一次进制转换（用的是 App 里同一个引擎）
            """.trimIndent(),

        "/home/radix/notes.txt" to
            """
            进制工坊开发笔记（虚拟）
            ------------------------
            · 2-36 任意进制互转：BigInteger 精确运算，几百位不丢精度
            · 大数计算器：有理数四则运算，除不尽保留 ${CalculatorEngine.DECIMAL_PLACES} 位
            · π 无限计算：Chudnovsky 公式分级，每涨 1 MB 提醒一次
            · 全部计算在本机完成，不联网、不收集数据
            """.trimIndent(),

        "/home/radix/radix.conf" to
            """
            theme = v5-warm
            accent = #EA3417
            radius = 0
            font.mono = Azeret Mono
            """.trimIndent(),

        "/home/radix/.bashrc" to
            """
            # 虚拟 shell 没有真的在跑 bash，这里只是给 cat 看的
            export PS1='$USER@$HOST:\w\$ '
            alias cmatrix='cmatrix -ab -C green'
            """.trimIndent(),

        "/proc/version" to
            "Linux version 6.6.6-radix (virtual@void) #1 SMP PREEMPT  ——  想象中的内核",

        "/proc/cpuinfo" to
            """
            processor       : 0..7
            model name      : Virtual Quantum Core @ 0 THz（虚构）
            flags           : bigint chudnovsky ipv6 radix
            note            : 真机 CPU 未被触碰
            """.trimIndent(),

        "/proc/uptime" to "0.00 0.00   # 这个终端才刚出生",

        "/var/log/radixlab.log" to
            """
            [ok] engine.bigint    ready
            [ok] engine.pi        ready
            [ok] engine.ipv6      ready
            [ok] network          disabled by design
            [ok] device.link      refused (virtual terminal)
            """.trimIndent()
    )

    private val COMMANDS: Map<String, String> = mapOf(
        "help" to "列出可用命令",
        "clear" to "清屏",
        "echo" to "回显文本（支持 ${'$'}USER ${'$'}HOST ${'$'}PWD）",
        "pwd" to "显示当前目录",
        "ls" to "列出目录（-l -a）",
        "cd" to "切换目录",
        "cat" to "查看文件内容",
        "whoami" to "显示当前用户",
        "id" to "显示用户与组",
        "uname" to "系统信息（-a）",
        "hostname" to "显示主机名",
        "date" to "显示当前时间",
        "uptime" to "运行时长",
        "neofetch" to "系统信息速览",
        "history" to "命令历史",
        "radix" to "进制转换 radix <值> [源进制] [目标进制]",
        "calc" to "大数计算 calc <算式>",
        "ip" to "解析 IPv4 / IPv6",
        "cmatrix" to "矩阵雨（-a -b -C 颜色 -r -u 帧延迟 -s）",
        "sudo" to "试试看会发生什么",
        "exit" to "退出终端（等同 quit / logout）"
    )

    fun bootLog(): List<Line> = listOf(
        Line("RadixLab Virtual Terminal v1.0", Kind.OK),
        Line("[  OK  ] mounted virtual rootfs (memory only)", Kind.DIM),
        Line("[  OK  ] no real device attached  ·  sandbox = on", Kind.DIM),
        Line("[  OK  ] bigint / pi / ipv6 engines linked", Kind.DIM),
        Line("[ FAIL ] /dev/real  →  refused by design", Kind.ERR),
        Line("[  OK  ] shell  ready  ·  type help", Kind.OK)
    )

    fun welcome(): List<Line> = listOf(
        Line(""),
        Line("欢迎回来，$USER。这里的一切都在内存里跑。", Kind.OK),
        Line("输入 help 看命令 · cmatrix 看矩阵雨 · exit 回到首页", Kind.DIM),
        Line("")
    )

    fun prompt(cwd: String): String {
        val shown = if (cwd == HOME) "~" else cwd
        return "$USER@$HOST:$shown$ "
    }

    fun complete(input: String, shell: Shell): String? {
        val trimmed = input.trimStart()
        if (trimmed.contains(' ')) {
            val head = trimmed.substringBeforeLast(' ')
            val token = trimmed.substringAfterLast(' ')
            val resolved = resolve(shell.cwd, token)
            val candidates = (DIRS[resolved]?.map { "$resolved/$it" } ?: emptyList()) +
                FILES.keys.filter { it.startsWith(resolved) && it != resolved }
            val hit = candidates.filter { it.startsWith(resolved) }.toList()
            if (hit.size != 1) return null
            val suffix = if (hit[0] in DIRS) "/" else ""
            return "$head ${relativize(shell.cwd, hit[0])}$suffix"
        }
        val hit = COMMANDS.keys.filter { it.startsWith(trimmed) }.sorted()
        if (hit.size != 1) return null
        return hit[0]
    }

    fun execute(rawInput: String, shell: Shell): Outcome {
        val input = rawInput.trim()
        if (input.isEmpty()) return Outcome(emptyList(), shell)

        val history = (shell.history + input).takeLast(50)
        val promptLine = Line(prompt(shell.cwd) + input, Kind.IN)
        val parts = input.split(Regex("\\s+"))
        val cmd = parts.first().lowercase(Locale.ROOT)
        val args = parts.drop(1)

        fun out(lines: List<Line>) = Outcome(listOf(promptLine) + lines, shell.copy(history = history))
        fun text(vararg lines: String) = out(lines.map { Line(it) })

        return when (cmd) {
            "help", "?", "man" -> out(helpLines())

            "clear", "cls" -> Outcome(
                lines = emptyList(),
                shell = shell.copy(history = history),
                action = Action.Clear
            )

            "echo" -> text(expand(args.joinToString(" "), shell))

            "pwd" -> text(shell.cwd)

            "ls", "dir" -> out(lsLines(args, shell))

            "cd" -> {
                val target = args.firstOrNull() ?: HOME
                val path = if (target.startsWith("~")) target.replaceFirst("~", HOME) else target
                when (val resolved = resolve(shell.cwd, path)) {
                    in DIRS -> Outcome(listOf(promptLine), shell.copy(cwd = resolved, history = history))
                    else -> out(listOf(Line("cd: $target: 没有这个目录（这是虚拟文件系统，只有几个示例目录）", Kind.ERR)))
                }
            }

            "cat" -> {
                if (args.isEmpty()) {
                    out(listOf(Line("用法：cat <文件>", Kind.ERR)))
                } else {
                    out(args.flatMap { arg ->
                        val resolved = resolve(shell.cwd, arg)
                        when {
                            resolved in FILES -> FILES.getValue(resolved).split("\n").map { Line(it) }
                            resolved in DIRS -> listOf(Line("cat: $arg: 这是一个目录", Kind.ERR))
                            else -> listOf(Line("cat: $arg: 没有这个文件", Kind.ERR))
                        }
                    })
                }
            }

            "whoami" -> text(USER)

            "id" -> text("uid=1000($USER) gid=1000($USER) groups=1000($USER),999(virtual)")

            "uname" -> if (args.any { it == "-a" }) {
                text(
                    "Linux $HOST 6.6.6-radix #1 SMP PREEMPT virtual x86_64 GNU/Linux",
                    "（这一行是编出来的：应用不可能拿到你的真实内核信息）"
                )
            } else {
                text("Linux")
            }

            "hostname" -> text(HOST)

            "date" -> text(
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                "（读的是本机时间，只用于显示）"
            )

            "uptime" -> text("up 0 min,  1 user,  load average: 0.00, 0.00, 0.00")

            "neofetch" -> out(neofetchLines())

            "history" -> out(
                if (history.isEmpty()) listOf(Line("（还没有历史）", Kind.DIM))
                else history.mapIndexed { i, h -> Line("${(i + 1).toString().padStart(4)}  $h") }
            )

            "radix", "conv", "convert" -> out(radixLines(args))

            "calc" -> out(calcLines(args))

            "ip", "ipcalc" -> out(ipLines(args))

            "cmatrix", "matrix" -> {
                val (options, errors) = parseMatrix(args)
                if (options == null) {
                    out(errors)
                } else {
                    Outcome(
                        lines = listOf(promptLine),
                        shell = shell.copy(history = history),
                        action = Action.Matrix(options),
                        echoAfterMatrix = Line("cmatrix: 已退出 —— 虚拟矩阵雨，真机 CPU 全程没被碰过", Kind.DIM)
                    )
                }
            }

            "sudo" -> out(
                listOf(
                    Line("$USER 不在 sudoers 文件中。此事将被记录。", Kind.ERR),
                    Line("……不过这里根本没有 sudo，也没有真机 —— 你安全得很。", Kind.DIM)
                )
            )

            "rm" -> out(
                if (args.any { it.contains("/") || it == "-rf" || it == "-fr" }) {
                    listOf(
                        Line("rm: 拒绝执行：本终端不挂载任何真实文件系统", Kind.ERR),
                        Line("放心，你的照片和聊天记录都在原地。", Kind.DIM)
                    )
                } else {
                    listOf(Line("rm: 这里是虚拟文件系统，没有东西可以删", Kind.DIM))
                }
            )

            "exit", "quit", "logout" -> Outcome(
                lines = listOf(promptLine, Line("再见。虚拟终端即将关闭……", Kind.DIM)),
                shell = shell.copy(history = history),
                action = Action.Exit
            )

            else -> out(
                listOf(
                    Line("command not found: $cmd", Kind.ERR),
                    Line("输入 help 看看有哪些命令", Kind.DIM)
                )
            )
        }
    }

    private fun helpLines(): List<Line> {
        val lines = mutableListOf(Line("可用命令：", Kind.OK))
        COMMANDS.forEach { (name, desc) ->
            lines += Line("  ${name.padEnd(11)}$desc")
        }
        lines += Line("")
        lines += Line("提示：TAB 补全 · ↑ ↓ 翻历史 · CTRL 是修饰键（配合 C 表示中断）", Kind.DIM)
        return lines
    }

    private fun lsLines(args: List<String>, shell: Shell): List<Line> {
        val long = args.any { it == "-l" || it == "-la" || it == "-al" }
        val all = args.any { it.startsWith("-") && it.contains("a") }
        val pathArg = args.firstOrNull { !it.startsWith("-") }
        val path = if (pathArg == null) shell.cwd else resolve(shell.cwd, pathArg)

        val entries = DIRS[path] ?: return listOf(Line("ls: $path: 没有这个目录", Kind.ERR))
        val hidden = if (path == HOME) listOf(".bashrc") else emptyList()
        val shown = (entries + hidden.filter { all || it.startsWith(".") }).distinct()

        return if (long) {
            shown.map { name ->
                val childPath = normalise("$path/$name")
                val isDir = childPath in DIRS
                val size = if (isDir) 4096 else FILES[childPath]?.length ?: 0
                Line("${if (isDir) "drwxr-xr-x" else "-rw-r--r--"}  $USER  $USER  ${size.toString().padStart(6)}  $name")
            }
        } else {
            listOf(Line(shown.joinToString("  ") { if (normalise("$path/$it") in DIRS) "$it/" else it }))
        }
    }

    private fun neofetchLines(): List<Line> = listOf(
        Line("        ▄▄▄▄▄        $USER@$HOST"),
        Line("      ▄█████████▄    ──────────────────────────"),
        Line("     ████  ██  ████   OS       RadixLab Virtual 1.0 (memory only)"),
        Line("     ██████████████   Host     no real device attached"),
        Line("     ████  ██  ████   Kernel   6.6.6-radix (invented)"),
        Line("      ▀█████████▀    Uptime   since session start"),
        Line("        ▀▀▀▀▀▀▀      Shell    radix-sh (virtual)"),
        Line("                     Font     Azeret Mono"),
        Line("                     Network  disabled by design"),
        Line("                     Theme    v5-warm  ·  accent #EA3417"),
        Line("", Kind.DIM),
        Line("                     真实设备：未连接。本终端不会读写你的手机。", Kind.DIM)
    )

    private fun radixLines(args: List<String>): List<Line> {
        if (args.isEmpty()) return listOf(Line("用法：radix <值> [源进制] [目标进制]    例：radix 255 10 16", Kind.ERR))
        val value = args[0]
        val from = args.getOrNull(1)?.toIntOrNull() ?: 10
        val to = args.getOrNull(2)?.toIntOrNull() ?: guessTarget(from)

        if (!ConversionEngine.isValidBase(from) || !ConversionEngine.isValidBase(to)) {
            return listOf(Line("进制范围是 2 - 36", Kind.ERR))
        }
        val result = ConversionEngine.convert(value, from, to)
        return when (result) {
            is ConversionEngine.Result.Success -> listOf(
                Line("${ConversionEngine.baseName(from)} $value  →  ${ConversionEngine.baseName(to)}", Kind.OK),
                Line(result.output, Kind.OK),
                Line("十进制中间值：${result.decimal}", Kind.DIM)
            )
            else -> listOf(Line("无法转换：$value 不是合法的 ${from} 进制数", Kind.ERR))
        }
    }

    private fun guessTarget(from: Int): Int = when (from) {
        16 -> 10; 10 -> 16; 2 -> 16; 8 -> 10; else -> 10
    }

    private fun calcLines(args: List<String>): List<Line> {
        if (args.isEmpty()) return listOf(Line("用法：calc <算式>    例：calc (1+2)*3^10", Kind.ERR))
        val expression = args.joinToString(" ")
        return when (val result = CalculatorEngine.evaluate(expression)) {
            is CalculatorEngine.Result.Success -> listOf(
                Line("= ${result.value}", Kind.OK),
                Line(
                    if (result.approx) "近似值（算式里含 π），共 ${result.digits} 位数字"
                    else if (!result.exact) "除不尽，保留 ${CalculatorEngine.DECIMAL_PLACES} 位小数，共 ${result.digits} 位"
                    else "精确值，共 ${result.digits} 位数字",
                    Kind.DIM
                )
            )
            is CalculatorEngine.Result.Failure -> listOf(
                Line(
                    if (result.incomplete) "算式还没输完：${result.message}" else result.message,
                    Kind.ERR
                )
            )
        }
    }

    private fun ipLines(args: List<String>): List<Line> {
        if (args.isEmpty()) return listOf(Line("用法：ip <地址>    例：ip 192.168.1.1 / ip 2001:db8::1", Kind.ERR))
        val input = args[0]
        return when {
            IpUtils.isValidIpv4(input) -> listOf(
                Line("类型：IPv4", Kind.OK),
                Line("二进制：${IpUtils.ipv4ToBinary(input) ?: "-"}"),
                Line("十六进制：${IpUtils.ipv4ToHex(input) ?: "-"}"),
                Line("十进制：${IpUtils.ipv4ToDecimal(input) ?: "-"}")
            )
            IpUtils.isValidIpv6(input) -> listOf(
                Line("类型：IPv6", Kind.OK),
                Line("规范化：${IpUtils.parseIpv6(input)?.joinToString(":") { "%04x".format(it) } ?: "-"}"),
                Line("判断：${IpUtils.describe(input)}", Kind.DIM)
            )
            else -> listOf(
                Line("无法识别：$input", Kind.ERR),
                Line("判断：${IpUtils.describe(input)}", Kind.DIM)
            )
        }
    }

    private fun parseMatrix(args: List<String>): Pair<MatrixOptions?, List<Line>> {
        if (args.any { it == "-h" || it == "--help" }) {
            return null to listOf(
                Line("cmatrix —— 矩阵雨（虚拟实现，限帧限列，退出即停）", Kind.OK),
                Line("  -a          异步滚动（默认开）"),
                Line("  -b          粗体（默认开）"),
                Line("  -C <颜色>   green/red/blue/cyan/magenta/yellow/white"),
                Line("  -r          rainbow 彩色模式"),
                Line("  -u <0-10>   帧延迟档位，数字越大越慢（默认 3）"),
                Line("  -s          屏保模式：任意键退出"),
                Line("  -h          显示这份帮助"),
                Line("退出：Ctrl+C 或 q（屏保模式下任意键）", Kind.DIM)
            )
        }

        val base = MatrixOptions()
        var async = base.async
        var bold = base.bold
        var color = base.color
        var delay = base.delayMs
        var screensaver = base.screensaver
        var rainbow = false

        var i = 0
        while (i < args.size) {
            val arg = args[i]
            when (arg) {
                "-a" -> async = true
                "-b" -> bold = true
                "-r" -> rainbow = true
                "-s" -> screensaver = true
                "-C" -> {
                    val next = args.getOrNull(i + 1)?.lowercase(Locale.ROOT)
                    if (next == null || next !in MATRIX_COLORS) {
                        return null to listOf(Line("cmatrix: -C 需要颜色名：${MATRIX_COLORS.joinToString("/")}", Kind.ERR))
                    }
                    color = next
                    i++
                }
                "-u" -> {
                    val next = args.getOrNull(i + 1)?.toIntOrNull()
                    if (next == null || next !in 0..10) {
                        return null to listOf(Line("cmatrix: -u 需要 0-10 之间的数字", Kind.ERR))
                    }
                    delay = mapDelay(next)
                    i++
                }
                else -> if (arg.startsWith("-") && arg.length > 2) {
                    for (ch in arg.drop(1)) {
                        when (ch) {
                            'a' -> async = true
                            'b' -> bold = true
                            'r' -> rainbow = true
                            's' -> screensaver = true
                        }
                    }
                }
            }
            i++
        }

        return MatrixOptions(async, bold, color, delay, screensaver, rainbow) to emptyList()
    }

    private val MATRIX_COLORS = listOf("green", "red", "blue", "cyan", "magenta", "yellow", "white")

    private fun mapDelay(level: Int): Int = when (level) {
        0 -> 16
        1 -> 20
        2 -> 26
        3 -> 33
        4 -> 42
        5 -> 52
        6 -> 64
        7 -> 78
        8 -> 96
        9 -> 110
        else -> 120
    }

    private fun expand(text: String, shell: Shell): String =
        text.replace("${'$'}USER", USER)
            .replace("${'$'}HOST", HOST)
            .replace("${'$'}HOME", HOME)
            .replace("${'$'}SHELL", "/usr/bin/radix-sh")
            .replace("${'$'}PWD", shell.cwd)

    private fun resolve(cwd: String, path: String): String {
        if (path.isEmpty()) return cwd
        val base = if (path.startsWith("/")) path else "$cwd/$path"
        return normalise(base)
    }

    private fun normalise(path: String): String {
        val out = mutableListOf<String>()
        path.split("/").forEach { seg ->
            when (seg) {
                "", "." -> Unit
                ".." -> if (out.isNotEmpty()) out.removeAt(out.size - 1)
                else -> out += seg
            }
        }
        return "/" + out.joinToString("/")
    }

    private fun relativize(cwd: String, path: String): String =
        if (path.startsWith("$cwd/")) path.removePrefix("$cwd/") else path
}
