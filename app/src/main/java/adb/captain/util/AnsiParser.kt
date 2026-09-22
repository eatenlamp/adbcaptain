package adb.captain.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * Состоятельный парсер ANSI SGR-кодов для цветовой подсветки терминала.
 * Поддерживает базовые цвета, яркие, 256/24-битные цвета, bold и сброс.
 */
object AnsiParser {
    private val SGR = Regex("\\x1B\\[([0-9;:]*)m")

    fun parse(text: String): AnnotatedString = buildAnnotatedString {
        var style = SpanStyle()
        var start = 0
        for (match in SGR.findAll(text).toList()) {
            val segment = text.substring(start, match.range.first)
            if (segment.isNotEmpty()) withStyle(style) { append(segment) }
            style = apply(style, match.groupValues[1])
            start = match.range.last + 1
        }
        if (start < text.length) withStyle(style) { append(text.substring(start)) }
    }

    private fun apply(style: SpanStyle, params: String): SpanStyle {
        if (params.isBlank()) return SpanStyle()
        val codes = params.split(';').map { it.toIntOrNull() ?: 0 }
        var s = style
        var i = 0
        while (i < codes.size) {
            val c = codes[i]
            when {
                c == 0 -> s = SpanStyle()
                c == 1 -> s = s.copy(fontWeight = FontWeight.Bold)
                c == 22 -> s = s.copy(fontWeight = FontWeight.Normal)
                c in 30..37 -> s = s.copy(color = basic(c - 30, bright = false))
                c in 90..97 -> s = s.copy(color = basic(c - 90, bright = true))
                c == 39 -> s = s.copy(color = Color.Unspecified)
                c == 38 -> {
                    when {
                        codes.getOrNull(i + 1) == 5 && codes.getOrNull(i + 2) != null -> {
                            s = s.copy(color = palette256(codes[i + 2])); i += 2
                        }
                        codes.getOrNull(i + 1) == 2 && codes.getOrNull(i + 4) != null -> {
                            s = s.copy(color = rgb(codes[i + 2], codes[i + 3], codes[i + 4])); i += 4
                        }
                    }
                }
                c in 40..47 -> s = s.copy(background = basic(c - 40, bright = false))
                c == 49 -> s = s.copy(background = Color.Unspecified)
            }
            i++
        }
        return s
    }

    private fun basic(index: Int, bright: Boolean): Color {
        val dark = listOf(
            Color(0xFF1E1E1E), Color(0xFFE06C75), Color(0xFF98C379), Color(0xFFE5C07B),
            Color(0xFF61AFEF), Color(0xFFC678DD), Color(0xFF56B6C2), Color(0xFFD7D7D7)
        )
        val light = listOf(
            Color(0xFF5C6370), Color(0xFFBE5046), Color(0xFF90C77B), Color(0xFFF3C04B),
            Color(0xFF78B9FF), Color(0xFFD37AC8), Color(0xFF6BC6D0), Color(0xFFFFFFFF)
        )
        return (if (bright) light else dark).getOrElse(index) { Color.Unspecified }
    }

    private fun palette256(index: Int): Color = when {
        index < 8 -> basic(index, bright = false)
        index < 16 -> basic(index - 8, bright = true)
        index < 232 -> {
            val i = index - 16
            rgb((i / 36) * 51, ((i / 6) % 6) * 51, (i % 6) * 51)
        }
        else -> {
            val level = 8 + (index - 232) * 10
            rgb(level, level, level)
        }
    }

    private fun rgb(r: Int, g: Int, b: Int): Color =
        Color(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
}
