package com.talania.personalhub.app

import android.content.Context
import android.os.Bundle
import android.os.Process
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.talania.personalhub.core.model.DomainResult
import com.talania.personalhub.core.model.HubId
import com.talania.personalhub.feature.settings.data.HubPreferences
import com.talania.personalhub.feature.settings.data.ThemeMode
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ProcessPersistenceTest {
    // Run with -e phase write then -e phase read in separate instrumentation processes.
    @Test fun persistedValues() = runBlocking {
        val phase = InstrumentationRegistry.getArguments().getString("phase") ?: "roundTrip"
        require(phase in setOf("write", "read", "roundTrip"))
        println("F01_PID=${Process.myPid()} phase=$phase")
        InstrumentationRegistry.getInstrumentation().sendStatus(
            2,
            Bundle().apply {
                putInt("f01_pid", Process.myPid())
                putString("f01_phase", phase)
            },
        )
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dependencies = EntryPointAccessors.fromApplication(
            context,
            PersistenceTestDependencies::class.java,
        )
        val runtime = dependencies.runtime()
        val preferences = dependencies.preferences()
        assertEquals(DomainResult.Success(Unit), runtime.initialize())
        val marker = HubId("00000000-0000-0000-0000-000000000001")
        val selected =
            HubPreferences(ThemeMode.DARK, soundEnabled = false, vibrationEnabled = false)
        if (phase != "read") {
            val current = runtime.observe().first()
            assertEquals(
                DomainResult.Success(Unit),
                runtime.update(current.copy(activeSessionId = marker), current.revision),
            )
            assertEquals(DomainResult.Success(Unit), preferences.save(selected))
        }
        if (phase != "write") {
            val current = runtime.observe().first()
            assertEquals(marker, current.activeSessionId)
            assertEquals(selected, preferences.observe().first())
            assertEquals(
                DomainResult.Success(Unit),
                runtime.update(current.copy(activeSessionId = null), current.revision),
            )
            assertEquals(DomainResult.Success(Unit), preferences.save(HubPreferences()))
        }
    }
}
