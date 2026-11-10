/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MacroVariablesTest {

    @Test
    fun collectsNamesInOrderWithoutSerial() {
        val macro = Macro(
            name = "install",
            steps = listOf(
                MacroStep("pm install {{apk}}"),
                MacroStep("am start {{package}} --serial {{serial}}"),
                MacroStep("echo {{apk}}")
            )
        )
        assertEquals(listOf("apk", "package"), MacroVariables.names(macro))
    }

    @Test
    fun resolvesKnownPlaceholdersAndKeepsUnknownOnes() {
        val result = MacroVariables.resolve(
            "echo {{name}} {{missing}}",
            mapOf("name" to "captain")
        )
        assertEquals("echo captain {{missing}}", result)
    }

    @Test
    fun detectsSerialUsage() {
        assertTrue(MacroVariables.usesSerial(listOf("echo {{serial}}")))
        assertFalse(MacroVariables.usesSerial(listOf("echo {{name}}")))
        assertFalse(MacroVariables.usesSerial(listOf("echo serial")))
    }
}
