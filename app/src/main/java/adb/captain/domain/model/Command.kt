package adb.captain.domain.model

import java.util.Date

/**
 * Модель команды ADB.
 */
data class Command(
    val id: Long = 0,
    val text: String,
    val timestamp: Date = Date(),
    val isSuccess: Boolean = true,
    val output: String = ""
)
