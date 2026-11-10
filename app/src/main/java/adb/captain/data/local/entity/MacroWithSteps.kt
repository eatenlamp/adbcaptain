/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation
import adb.captain.domain.model.Macro
import adb.captain.domain.model.MacroStep
import adb.captain.domain.model.StepCondition
import adb.captain.domain.model.StepFailure

/** A macro together with its ordered steps. */
data class MacroWithSteps(
    @Embedded val macro: MacroEntity,
    @Relation(parentColumn = "id", entityColumn = "macroId")
    val steps: List<MacroStepEntity>
) {
    fun toModel(): Macro = Macro(
        id = macro.id,
        name = macro.name,
        steps = steps.sortedBy { it.position }.map { it.toModel() },
        createdAt = macro.createdAt
    )
}

fun MacroStepEntity.toModel(): MacroStep = MacroStep(
    command = command,
    condition = enumOr(condition, StepCondition.ALWAYS),
    conditionValue = conditionValue,
    onError = enumOr(onError, StepFailure.STOP),
    waitMs = waitMs
)

private inline fun <reified T : Enum<T>> enumOr(name: String, fallback: T): T =
    runCatching { enumValueOf<T>(name) }.getOrDefault(fallback)
