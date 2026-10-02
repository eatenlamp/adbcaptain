package adb.captain.domain.model

/**
 * Параметры записи экрана для screenrecord.
 *
 * У screenrecord нет флага частоты кадров: он всегда пишет с частотой
 * обновления экрана. Поэтому "максимальные кадры" = снимать в нативном
 * разрешении и с максимальным битрейтом, а переключатель кадров
 * ограничивает поток, чтобы не выбивать кодировщик.
 */
data class RecordOptions(
    val width: Int = 0,
    val height: Int = 0,
    val bitRateMbps: Int = 20
) {
    val sizeArg: String?
        get() = if (width > 0 && height > 0) "${width}x${height}" else null

    companion object {
        val NATIVE_MAX = RecordOptions(0, 0, 20)
        val BALANCED = RecordOptions(0, 0, 12)
        val COMPACT = RecordOptions(720, 720, 8)
        val LOW = RecordOptions(480, 480, 4)
    }
}
