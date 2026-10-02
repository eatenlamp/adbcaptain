package adb.captain.data.media

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import adb.captain.domain.model.SavedMedia
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Кладёт снимки и записи в галерею.
 *
 * На Android 10+ (API 29) файлы вставляются в MediaStore через
 * RELATIVE_PATH + IS_PENDING, поэтому дополнительные разрешения не нужны.
 * На Android 7-9 (API 24-28) MediaStore ещё требует WRITE_EXTERNAL_STORAGE,
 * там пишем в публичную папку и сканируем её.
 */
class MediaStoreWriter @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun timestamp(): String =
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

    fun saveScreenshot(bytes: ByteArray, name: String = "Screenshot_${timestamp()}.png"): SavedMedia? =
        save(bytes, name, MIME_PNG, Environment.DIRECTORY_PICTURES)

    fun saveRecording(bytes: ByteArray, name: String = "Record_${timestamp()}.mp4"): SavedMedia? =
        save(bytes, name, MIME_MP4, Environment.DIRECTORY_MOVIES)

    fun needsLegacyStoragePermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

    fun hasLegacyStoragePermission(): Boolean {
        if (!needsLegacyStoragePermission()) return true
        return context.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    /**
     * URI, который можно отдать в ACTION_SEND. Для файлов, сохранённых
     * минуя MediaStore (Android 7-9), отдаём копию через FileProvider,
     * иначе Android запретит передавать file:// в другие приложения.
     */
    fun shareUri(media: SavedMedia): Uri? {
        if (media.uri.scheme == "content") return media.uri
        val source = media.uri.path?.let { File(it) } ?: return null
        if (!source.exists()) return null
        return try {
            val shareDir = File(context.cacheDir, "share").apply { mkdirs() }
            val copy = File(shareDir, source.name)
            source.inputStream().use { input -> copy.outputStream().use { input.copyTo(it) } }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", copy)
        } catch (e: Exception) {
            null
        }
    }

    private fun save(bytes: ByteArray, name: String, mimeType: String, legacyDir: String): SavedMedia? {
        if (bytes.isEmpty()) return null
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveScoped(bytes, name, mimeType, legacyDir)
            } else if (hasLegacyStoragePermission()) {
                saveLegacy(bytes, name, mimeType, legacyDir)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun saveScoped(bytes: ByteArray, name: String, mimeType: String, legacyDir: String): SavedMedia? {
        val resolver = context.contentResolver
        val collection = if (mimeType == MIME_PNG) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "$legacyDir/$ALBUM")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, values) ?: return null
        val written = try {
            (resolver.openOutputStream(uri)?.use { it.write(bytes) } != null)
        } catch (e: Exception) {
            false
        }
        if (!written) {
            runCatching { resolver.delete(uri, null, null) }
            return null
        }
        values.clear()
        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return SavedMedia(uri, "$legacyDir/$ALBUM/$name", mimeType)
    }

    @Suppress("DEPRECATION")
    private fun saveLegacy(bytes: ByteArray, name: String, mimeType: String, legacyDir: String): SavedMedia? {
        val dir = File(Environment.getExternalStoragePublicDirectory(legacyDir), ALBUM)
        if (!dir.exists() && !dir.mkdirs()) return null
        val file = File(dir, name)
        return try {
            file.writeBytes(bytes)
            MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf(mimeType), null)
            SavedMedia(Uri.fromFile(file), "${dir.absolutePath}/$name", mimeType)
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        const val ALBUM = "ADBCaptain"
        const val MIME_PNG = "image/png"
        const val MIME_MP4 = "video/mp4"
    }
}
