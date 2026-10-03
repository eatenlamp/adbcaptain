/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.domain.model

import android.net.Uri

/**
 * Файл, сохранённый в галерею через MediaStore.
 */
data class SavedMedia(
    val uri: Uri,
    val displayPath: String,
    val mimeType: String
)
