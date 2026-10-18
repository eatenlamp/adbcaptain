/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Material 3 Expressive loading indicators shared by every screen, so that progress
 * looks the same across the app instead of each screen rolling its own spinner.
 */
object ExpressiveProgress {

    /** Morphing indicator for compact slots such as list rows and inline buttons. */
    @Composable
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    fun indicator(size: Dp = 18.dp, color: Color = Color.Unspecified, modifier: Modifier = Modifier) {
        LoadingIndicator(
            modifier = modifier.size(size),
            color = color
        )
    }

    /** Morphing indicator on a shaped container, used for full screen busy states. */
    @Composable
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    fun contained(size: Dp = 72.dp, modifier: Modifier = Modifier) {
        ContainedLoadingIndicator(modifier = modifier.size(size))
    }

    /** Centered contained indicator for empty and loading placeholders. */
    @Composable
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    fun centeredContained(size: Dp = 72.dp) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            ContainedLoadingIndicator(modifier = Modifier.size(size))
        }
    }

    /** Thin determinate bar for operations that report progress. */
    @Composable
    fun bar(progress: Float, modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = modifier,
            color = color,
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
            gapSize = 4.dp,
            drawStopIndicator = {}
        )
    }

    /** Indeterminate bar for operations without a known total. */
    @Composable
    fun indeterminateBar(modifier: Modifier = Modifier) {
        LinearProgressIndicator(modifier = modifier)
    }
}