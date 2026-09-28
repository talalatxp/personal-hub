package com.talania.personalhub.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class DatabaseTest {
    @Test fun runtimeStateSurvivesReopeningOnAndroid() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "f01-synthetic-test.db"
        context.deleteDatabase(name)
        try {
            val first = Room.databaseBuilder(
                context,
                HubDatabase::class.java,
                name,
            ).openHelperFactory(PreservingOpenHelperFactory()).build()
            val expected =
                RuntimeEntity(
                    activeSessionId = "00000000-0000-0000-0000-000000000001",
                    revision = 4,
                )
            try {
                first.runtimeDao().initialize(expected)
            } finally {
                first.close()
            }
            val second = Room.databaseBuilder(
                context,
                HubDatabase::class.java,
                name,
            ).openHelperFactory(PreservingOpenHelperFactory()).build()
            try {
                assertEquals(expected, second.runtimeDao().observe().first())
            } finally {
                second.close()
            }
        } finally {
            context.deleteDatabase(name)
        }
    }
}
