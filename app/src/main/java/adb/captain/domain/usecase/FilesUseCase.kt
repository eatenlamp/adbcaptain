package adb.captain.domain.usecase

import adb.captain.domain.model.FileEntry
import adb.captain.domain.repository.AdbRepository
import javax.inject.Inject

/**
 * UseCase для работы с файловой системой через ADB.
 */
class FilesUseCase @Inject constructor(
    private val repository: AdbRepository
) {
    suspend fun listFiles(path: String): List<FileEntry> = repository.listFiles(path)
    suspend fun delete(path: String) = repository.deleteFile(path)
    suspend fun createDirectory(path: String) = repository.createDirectory(path)
}
