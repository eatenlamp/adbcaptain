/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.ui.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape

/**
 * Material 3 Expressive silhouettes. Each helper resolves a [MaterialShapes] polygon into a
 * [Shape] that can be handed to any component accepting a `shape` parameter.
 */
object ExpressiveShape {

    @Composable
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    fun pill(startAngle: Int = 0): Shape = MaterialShapes.Pill.toShape(startAngle)

    @Composable
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    fun cookie(startAngle: Int = 0): Shape = MaterialShapes.Cookie9Sided.toShape(startAngle)

    @Composable
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    fun softBurst(startAngle: Int = 0): Shape = MaterialShapes.SoftBurst.toShape(startAngle)

    @Composable
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    fun slanted(startAngle: Int = 0): Shape = MaterialShapes.Slanted.toShape(startAngle)

    @Composable
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    fun gem(startAngle: Int = 0): Shape = MaterialShapes.Gem.toShape(startAngle)

    @Composable
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    fun circle(startAngle: Int = 0): Shape = MaterialShapes.Circle.toShape(startAngle)

    @Composable
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    fun sunny(startAngle: Int = 0): Shape = MaterialShapes.VerySunny.toShape(startAngle)

    @Composable
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    fun bun(startAngle: Int = 0): Shape = MaterialShapes.Bun.toShape(startAngle)
}