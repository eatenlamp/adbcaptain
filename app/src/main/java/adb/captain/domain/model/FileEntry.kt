package adb.captain.domain.model

/**
 * Элемент файловой системы, полученный через ADB.
 */
data class FileEntry(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long = 0,
    val modified: String = ""
)
