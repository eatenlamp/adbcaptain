/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

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

    suspend fun copy(source: String, destinationDir: String): String =
        repository.copyPath(source, destinationDir)

    suspend fun move(source: String, destinationDir: String): String =
        repository.movePath(source, destinationDir)

    suspend fun rename(path: String, newName: String): String =
        repository.renamePath(path, newName)

    suspend fun properties(path: String): String? = repository.fileProperties(path)

    suspend fun readText(path: String, maxBytes: Long = 262_144): String? =
        repository.readTextFile(path, maxBytes)

    suspend fun writeText(path: String, content: String): String =
        repository.writeTextFile(path, content)

    suspend fun pull(path: String): ByteArray? = repository.pullFile(path)

    suspend fun push(remotePath: String, bytes: ByteArray): String =
        repository.pushFile(remotePath, bytes)
}
