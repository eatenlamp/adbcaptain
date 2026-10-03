/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain

import adb.captain.domain.model.LogLevel
import adb.captain.util.LogcatParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LogcatParserTest {

    @Test
    fun parsesThreadtimeFormat() {
        val entry = LogcatParser.parse("09-29 09:35:00.109  1234  5678 E ActivityManager: something failed")
        assertEquals("09-29 09:35:00.109", entry?.timestamp)
        assertEquals(LogLevel.ERROR, entry?.level)
        assertEquals("ActivityManager", entry?.tag)
        assertEquals("something failed", entry?.message)
        assertEquals(1234, entry?.pid)
        assertEquals(5678, entry?.tid)
    }

    @Test
    fun parsesTimeFormat() {
        val entry = LogcatParser.parse("09-29 09:35:00.109 I/ActivityManager( 1234): Activity started")
        assertEquals(LogLevel.INFO, entry?.level)
        assertEquals("ActivityManager", entry?.tag)
        assertEquals("Activity started", entry?.message)
        assertEquals(1234, entry?.pid)
    }

    @Test
    fun parsesLegacyFormat() {
        val entry = LogcatParser.parse("E 09-29 09:35:00.109 MyTag PID:4321 boom")
        assertEquals(LogLevel.ERROR, entry?.level)
        assertEquals("MyTag", entry?.tag)
        assertEquals("boom", entry?.message)
        assertEquals(4321, entry?.pid)
    }

    @Test
    fun ignoresSeparatorAndBlankLines() {
        assertNull(LogcatParser.parse("--------- beginning of main"))
        assertNull(LogcatParser.parse(""))
        assertNull(LogcatParser.parse("   "))
        assertNull(LogcatParser.parse("not a log line"))
    }
}
