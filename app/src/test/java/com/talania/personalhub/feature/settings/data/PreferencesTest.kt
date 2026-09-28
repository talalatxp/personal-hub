package com.talania.personalhub.feature.settings.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.talania.personalhub.core.model.DomainError
import com.talania.personalhub.core.model.DomainResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PreferencesTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun preferencesSurviveNewStoreAndScope() = runBlocking {
        val file = temporary.root.resolve("test.preferences_pb")
        val firstJob = Job()
        val firstStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(
                firstJob + Dispatchers.IO,
            ),
            produceFile = {
                file
            },
        )
        val first = LocalPreferencesRepository { firstStore }
        assertEquals(HubPreferences(), first.observe().first())
        val selected =
            HubPreferences(ThemeMode.DARK, soundEnabled = false, vibrationEnabled = false)
        assertEquals(DomainResult.Success(Unit), first.save(selected))
        firstJob.cancelAndJoin()

        val secondJob = Job()
        try {
            val secondStore = PreferenceDataStoreFactory.create(
                scope = CoroutineScope(
                    secondJob + Dispatchers.IO,
                ),
                produceFile = {
                    file
                },
            )
            assertEquals(selected, LocalPreferencesRepository { secondStore }.observe().first())
        } finally {
            secondJob.cancelAndJoin()
        }
    }

    @Test fun corruptPreferencesAreNotReplacedWithDefaults() = runBlocking {
        val file = temporary.root.resolve("test.preferences_pb")
        val corrupt = byteArrayOf(-1, -1, -1, -1)
        file.writeBytes(corrupt)
        val job = Job()
        try {
            val store = PreferenceDataStoreFactory.create(
                scope = CoroutineScope(
                    job + Dispatchers.IO,
                ),
                produceFile = {
                    file
                },
            )
            val repo = LocalPreferencesRepository { store }
            assertEquals(DomainResult.Failure(DomainError.Storage), repo.save(HubPreferences()))
            assertArrayEquals(corrupt, file.readBytes())
        } finally {
            job.cancelAndJoin()
        }
    }
}
