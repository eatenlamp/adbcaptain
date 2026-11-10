/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.data.local

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import adb.captain.MainActivity
import adb.captain.R
import adb.captain.domain.model.Macro
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Publishes the saved macros as dynamic launcher shortcuts, so a workflow can
 * run with one tap without opening the app first.
 */
@Singleton
class MacroShortcuts @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    /** Replaces the current shortcuts with [macros]. Safe to call often. */
    fun sync(macros: List<Macro>) {
        runCatching {
            val limit = ShortcutManagerCompat.getMaxShortcutCountPerActivity(context)
                .coerceAtLeast(0)
            if (limit == 0) return

            val shortcuts = macros
                .filter { it.id > 0L }
                .take(limit)
                .mapIndexed { index, macro ->
                    ShortcutInfoCompat.Builder(context, "$ID_PREFIX${macro.id}")
                        .setShortLabel(macro.name)
                        .setLongLabel(macro.name)
                        .setRank(index)
                        .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                        .setIntent(
                            Intent(context, MainActivity::class.java)
                                .setAction(ACTION_RUN_MACRO)
                                .putExtra(EXTRA_MACRO_ID, macro.id)
                        )
                        .build()
                }
            ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
        }
    }

    companion object {
        const val ACTION_RUN_MACRO = "adb.captain.action.RUN_MACRO"
        const val EXTRA_MACRO_ID = "adb.captain.extra.MACRO_ID"
        private const val ID_PREFIX = "macro-"
    }
}
