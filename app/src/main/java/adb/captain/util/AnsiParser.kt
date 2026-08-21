package adb.captain.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

/**
 * Парсер ANSI-кодов для цветовой подсветки терминала.
 */
object AnsiParser {
    private val ANSI_ESCAPE = Regex("\u001B\\[[;\\d]*m")

    fun parse(text: String): AnnotatedString = buildAnnotatedString {
        var lastIndex = 0
        ANSI_ESCAPE.findAll(text).forEach { match ->
            append(text.substring(lastIndex, match.range.first))
            val style = getStyleFromAnsi(match.value)
            // This is a very basic parser that just appends the next segment with the style
            // A real ANSI parser is much more complex
            val nextMatch = ANSI_ESCAPE.find(text, match.range.last + 1)
            val endOfStyledText = nextMatch?.range?.first ?: text.length
            
            withStyle(style) {
                append(text.substring(match.range.last + 1, endOfStyledText))
            }
            lastIndex = endOfStyledText
        }
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }

    private fun getStyleFromAnsi(ansi: String): SpanStyle {
        return when {
            ansi.contains("31m") -> SpanStyle(color = Color.Red)
            ansi.contains("32m") -> SpanStyle(color = Color.Green)
            ansi.contains("33m") -> SpanStyle(color = Color.Yellow)
            ansi.contains("34m") -> SpanStyle(color = Color.Blue)
            ansi.contains("1m") -> SpanStyle(color = Color.White) // Bold roughly
            else -> SpanStyle()
        }
    }
}
