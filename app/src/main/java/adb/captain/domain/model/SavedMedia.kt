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
