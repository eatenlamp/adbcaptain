package adb.captain.domain.model

/**
 * Модель записи в Logcat.
 */
data class LogEntry(
    val timestamp: String,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val pid: Int = 0,
    val tid: Int = 0
)

enum class LogLevel {
    VERBOSE, DEBUG, INFO, WARN, ERROR, FATAL;

    companion object {
        fun fromChar(c: Char): LogLevel = when (c.uppercaseChar()) {
            'V' -> VERBOSE
            'D' -> DEBUG
            'I' -> INFO
            'W' -> WARN
            'E' -> ERROR
            'F' -> FATAL
            else -> INFO
        }
    }
}
