package com.talania.personalhub.app

import androidx.lifecycle.ViewModelStore
import com.talania.personalhub.core.database.AppRuntimeState
import com.talania.personalhub.core.database.RuntimeRepository
import com.talania.personalhub.core.model.DomainError
import com.talania.personalhub.core.model.DomainResult
import com.talania.personalhub.feature.settings.data.HubPreferences
import com.talania.personalhub.feature.settings.data.PreferencesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BootstrapViewModelTest {
    private class RuntimeFake : RuntimeRepository {
        var result: DomainResult<Unit> = DomainResult.Failure(DomainError.Storage)
        var cancelled = false
        override suspend fun initialize(): DomainResult<Unit> {
            if (cancelled) throw CancellationException()
            return result
        }
        override fun observe(): Flow<AppRuntimeState> = flowOf(AppRuntimeState())
        override suspend fun update(
            state: AppRuntimeState,
            expectedRevision: Long,
        ): DomainResult<Unit> = result
    }

    private class PreferencesFake(val broken: Boolean = false) : PreferencesRepository {
        override fun observe(): Flow<HubPreferences> = flow {
            if (broken) error("Storage unavailable")
            emit(HubPreferences())
        }
        override suspend fun save(preferences: HubPreferences): DomainResult<Unit> =
            DomainResult.Success(Unit)
    }

    @Test fun failedOpenCanBeRetriedWithoutDeletingData() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val runtime = RuntimeFake()
            val model = BootstrapViewModel(runtime, PreferencesFake())
            store.put("test", model)
            advanceUntilIdle()
            assertEquals(BootstrapState.StorageError, model.state.value)
            runtime.result = DomainResult.Success(Unit)
            model.retry()
            advanceUntilIdle()
            assertEquals(BootstrapState.Ready(HubPreferences()), model.state.value)
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }

    @Test fun preferenceFailureIsVisibleAndNotTreatedAsEmptySettings() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val runtime = RuntimeFake().also { it.result = DomainResult.Success(Unit) }
            val model = BootstrapViewModel(runtime, PreferencesFake(broken = true))
            store.put("test", model)
            advanceUntilIdle()
            assertEquals(BootstrapState.StorageError, model.state.value)
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }

    @Test fun cancellationIsNotConvertedIntoAStorageError() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val model =
                BootstrapViewModel(RuntimeFake().also { it.cancelled = true }, PreferencesFake())
            store.put("test", model)
            advanceUntilIdle()
            assertEquals(BootstrapState.Loading, model.state.value)
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }
}
