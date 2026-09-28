package com.talania.personalhub.core.platform

import android.util.AtomicFile
import com.talania.personalhub.core.model.DomainError
import com.talania.personalhub.core.model.DomainResult
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

interface PrivateFiles {
    suspend fun read(relativePath: String): DomainResult<ByteArray>
    suspend fun write(relativePath: String, bytes: ByteArray): DomainResult<Unit>
}

class AndroidPrivateFiles @Inject constructor(
    private val storage: StorageCoordinator,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
) : PrivateFiles {
    private val mutex = Mutex()
    override suspend fun read(relativePath: String): DomainResult<ByteArray> =
        access(relativePath) { file ->
            val atomic = AtomicFile(file)
            if (!file.exists() && !File(file.path + ".bak").exists()) {
                DomainResult.Failure(DomainError.NotFound)
            } else {
                atomic.openRead().use { input ->
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (output.size() <= MAX_BYTES) {
                        val count = input.read(
                            buffer,
                            0,
                            minOf(
                                buffer.size,
                                MAX_BYTES + 1 - output.size(),
                            ),
                        )
                        if (count < 0) break
                        output.write(buffer, 0, count)
                    }
                    val bytes = output.toByteArray()
                    if (bytes.size >
                        MAX_BYTES
                    ) {
                        DomainResult.Failure(DomainError.Storage)
                    } else {
                        DomainResult.Success(bytes)
                    }
                }
            }
        }

    override suspend fun write(relativePath: String, bytes: ByteArray): DomainResult<Unit> {
        if (bytes.size > MAX_BYTES) return DomainResult.Failure(DomainError.Validation)
        return access(relativePath) { file ->
            check(file.parentFile?.let { it.isDirectory || it.mkdirs() } == true)
            val atomic = AtomicFile(file)
            val output = atomic.startWrite()
            try {
                output.write(bytes)
                atomic.finishWrite(output)
            } catch (error: Exception) {
                atomic.failWrite(output)
                throw error
            }
            DomainResult.Success(Unit)
        }
    }

    private suspend fun <T> access(
        path: String,
        block: (File) -> DomainResult<T>,
    ): DomainResult<T> = withContext(dispatcher) {
        if (path.isBlank() || path.contains('\\') ||
            path.split('/').any { it.isEmpty() || it == "." || it == ".." } ||
            File(path).isAbsolute
        ) {
            return@withContext DomainResult.Failure(DomainError.Validation)
        }
        try {
            val root = storage.mediaRoot().canonicalFile
            val file = File(root, path).canonicalFile
            if (!file.toPath().startsWith(
                    root.toPath(),
                )
            ) {
                return@withContext DomainResult.Failure(DomainError.Validation)
            }
            mutex.withLock { block(file) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            DomainResult.Failure(DomainError.Storage)
        }
    }

    private companion object {
        const val MAX_BYTES = 16 * 1024 * 1024
    }
}
