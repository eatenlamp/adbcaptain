/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AnsiParserTest {

    @Test
    fun removesEscapeSequencesFromTheText() {
        val parsed = AnsiParser.parse("a\u001B[31mred\u001B[0mb")
        assertEquals("aredb", parsed.text)
    }

    @Test
    fun mapsBasicColours() {
        val parsed = AnsiParser.parse("\u001B[31mred")
        val span = parsed.spanStyles.firstOrNull { it.item.color != Color.Unspecified }
        assertNotNull(span)
        assertEquals(Color(0xFFE06C75), span?.item?.color)
    }

    @Test
    fun mapsBrightColours() {
        val parsed = AnsiParser.parse("\u001B[91mred")
        val span = parsed.spanStyles.firstOrNull { it.item.color != Color.Unspecified }
        assertEquals(Color(0xFFBE5046), span?.item?.color)
    }

    @Test
    fun mapsBackgroundAndBold() {
        val parsed = AnsiParser.parse("\u001B[1;41mX")
        val span = parsed.spanStyles.firstOrNull { it.item.fontWeight == FontWeight.Bold }
        assertNotNull(span)
        assertEquals(Color(0xFFE06C75), span?.item?.background)
    }

    @Test
    fun maps256ColourPalette() {
        val parsed = AnsiParser.parse("\u001B[38;5;196mX")
        val span = parsed.spanStyles.firstOrNull { it.item.color != Color.Unspecified }
        assertEquals(Color(255, 0, 0), span?.item?.color)
    }

    @Test
    fun maps24BitColour() {
        val parsed = AnsiParser.parse("\u001B[38;2;10;20;30mX")
        val span = parsed.spanStyles.firstOrNull { it.item.color != Color.Unspecified }
        assertEquals(Color(10, 20, 30), span?.item?.color)
    }

    @Test
    fun resetClearsTheColour() {
        val parsed = AnsiParser.parse("\u001B[31mred\u001B[0mplain")
        val plain = parsed.spanStyles.lastOrNull { it.start >= "red".length }
        assertEquals(Color.Unspecified, plain?.item?.color)
    }
}
