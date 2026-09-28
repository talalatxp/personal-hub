package com.talania.personalhub.core.database

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.talania.personalhub.core.model.DomainError
import com.talania.personalhub.core.model.DomainResult
import com.talania.personalhub.core.platform.AndroidPrivateFiles
import com.talania.personalhub.core.platform.StorageCoordinator
import com.talania.personalhub.testing.FakeClock
import com.talania.personalhub.testing.FakeIdGenerator
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class StorageTest {
    private lateinit var context: Context
    private val opened = mutableListOf<StorageCoordinator>()

    @Before fun prepare() {
        context = ApplicationProvider.getApplicationContext()
        File(context.noBackupFilesDir, "hub").deleteRecursively()
    }

    @After fun close() {
        opened.forEach { it.database.close() }
        File(context.noBackupFilesDir, "hub").deleteRecursively()
    }

    private fun storage() = StorageCoordinator(context, Dispatchers.IO).also(opened::add)
    private fun repository(storage: StorageCoordinator) =
        RoomRuntimeRepository(storage, FakeClock(), Dispatchers.IO)

    @Test fun realDatabaseSurvivesClosingAndReopeningWithoutReinitializingMetadata() = runBlocking {
        val first = storage()
        val repo = repository(first)
        assertEquals(DomainResult.Success(Unit), repo.initialize())
        val ids = FakeIdGenerator()
        val state = AppRuntimeState(ids.next(), ids.next())
        assertEquals(DomainResult.Success(Unit), repo.update(state, 0))
        val metadata = first.database.runtimeDao().metadata()
        first.database.close()

        val second = storage()
        val reopened = repository(second)
        assertEquals(DomainResult.Success(Unit), reopened.initialize())
        assertEquals(state.copy(revision = 1), reopened.observe().first())
        assertEquals(metadata, second.database.runtimeDao().metadata())
        assertEquals(1, second.database.openHelper.readableDatabase.version)
        assertTrue(File(context.noBackupFilesDir, "hub/hub.db").exists())
    }

    @Test fun staleRevisionCannotOverwriteStoredState() = runBlocking {
        val repo = repository(storage())
        repo.initialize()
        val state = AppRuntimeState(activeSessionId = FakeIdGenerator().next())
        assertEquals(DomainResult.Success(Unit), repo.update(state, 0))
        assertEquals(DomainResult.Failure(DomainError.Conflict), repo.update(AppRuntimeState(), 0))
        assertEquals(state.copy(revision = 1), repo.observe().first())
        assertEquals(
            DomainResult.Failure(DomainError.Validation),
            repo.update(state, Long.MAX_VALUE),
        )
    }

    @Test fun missingMigrationReturnsErrorAndPreservesDatabase() = runBlocking {
        val first = storage()
        val repo = repository(first)
        repo.initialize()
        val state = AppRuntimeState(activeTimerId = FakeIdGenerator().next())
        repo.update(state, 0)
        first.database.openHelper.writableDatabase.version = 2
        first.database.close()
        val dbFile = File(context.noBackupFilesDir, "hub/hub.db")
        val before = dbFile.readBytes()
        assertEquals(DomainResult.Failure(DomainError.Storage), repository(storage()).initialize())
        assertArrayEquals(before, dbFile.readBytes())
    }

    @Test fun corruptDatabaseIsNotDeletedOrReplaced() = runBlocking {
        val dbFile = File(context.noBackupFilesDir, "hub/hub.db")
        dbFile.parentFile?.mkdirs()
        val bytes = ByteArray(4096) { 42 }
        dbFile.writeBytes(bytes)
        assertEquals(DomainResult.Failure(DomainError.Storage), repository(storage()).initialize())
        assertArrayEquals(bytes, dbFile.readBytes())
    }

    @Test fun filesStayPrivateAndRejectTraversal() = runBlocking {
        val files = AndroidPrivateFiles(storage(), Dispatchers.IO)
        val bytes = byteArrayOf(1, 2, 3)
        assertEquals(DomainResult.Success(Unit), files.write("synthetic/item.bin", bytes))
        val read = files.read("synthetic/item.bin") as DomainResult.Success
        assertArrayEquals(bytes, read.value)
        assertTrue(File(context.noBackupFilesDir, "hub/media/synthetic/item.bin").exists())
        for (path in listOf("../outside", "/outside", "dir/../outside", "dir\\outside", "")) {
            assertEquals(DomainResult.Failure(DomainError.Validation), files.write(path, bytes))
        }
        assertEquals(DomainResult.Failure(DomainError.NotFound), files.read("missing.bin"))
    }
}
