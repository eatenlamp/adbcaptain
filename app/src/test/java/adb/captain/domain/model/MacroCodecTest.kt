/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MacroCodecTest {

    private val macro = Macro(
        name = "Restart \"app\"",
        steps = listOf(
            MacroStep("am force-stop {{package}}"),
            MacroStep(
                command = "am start -n {{package}}/.Main",
                condition = StepCondition.OUTPUT_NOT_EMPTY,
                onError = StepFailure.CONTINUE,
                waitMs = 1500L
            )
        )
    )

    @Test
    fun roundTripKeepsEveryField() {
        val decoded = MacroCodec.decode(MacroCodec.encode(macro))

        assertEquals(macro.name, decoded?.name)
        assertEquals(2, decoded?.steps?.size)
        assertEquals("am force-stop {{package}}", decoded?.steps?.get(0)?.command)
        assertEquals(StepCondition.OUTPUT_NOT_EMPTY, decoded?.steps?.get(1)?.condition)
        assertEquals(StepFailure.CONTINUE, decoded?.steps?.get(1)?.onError)
        assertEquals(1500L, decoded?.steps?.get(1)?.waitMs)
    }

    @Test
    fun rejectsForeignOrBrokenFiles() {
        assertNull(MacroCodec.decode(""))
        assertNull(MacroCodec.decode("not json at all"))
        assertNull(MacroCodec.decode("""{"format":"something-else","name":"x","steps":[]}"""))
        assertNull(MacroCodec.decode("""{"format":"adb-captain-macro","name":"x","steps":[]}"""))
        assertNull(MacroCodec.decode("""{"format":"adb-captain-macro","name":"x","steps":[{"command":""}]}"""))
    }

    @Test
    fun unknownEnumsFallBackToSafeDefaults() {
        val decoded = MacroCodec.decode(
            """{"format":"adb-captain-macro","name":"M","steps":[
                |{"command":"id","condition":"SOMETHING","onError":"MAYBE","waitMs":-5}
                |]}""".trimMargin()
        )
        assertEquals(StepCondition.ALWAYS, decoded?.steps?.first()?.condition)
        assertEquals(StepFailure.STOP, decoded?.steps?.first()?.onError)
        assertEquals(-5L, decoded?.steps?.first()?.waitMs)
    }
}
