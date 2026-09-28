package com.talania.personalhub.core.database

import androidx.room.withTransaction
import com.talania.personalhub.core.model.DomainError
import com.talania.personalhub.core.model.DomainResult
import com.talania.personalhub.core.model.HubId
import com.talania.personalhub.core.platform.Clock
import com.talania.personalhub.core.platform.IoDispatcher
import com.talania.personalhub.core.platform.StorageCoordinator
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class AppRuntimeState(
    val activeSessionId: HubId? = null,
    val activeTimerId: HubId? = null,
    val revision: Long = 0,
)

interface RuntimeRepository {
    suspend fun initialize(): DomainResult<Unit>
    fun observe(): Flow<AppRuntimeState>
    suspend fun update(state: AppRuntimeState, expectedRevision: Long): DomainResult<Unit>
}

class RoomRuntimeRepository @Inject constructor(
    private val storage: StorageCoordinator,
    private val clock: Clock,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
) : RuntimeRepository {
    override suspend fun initialize(): DomainResult<Unit> = access {
        storage.database.withTransaction {
            val dao = storage.database.runtimeDao()
            dao.initialize(RuntimeEntity())
            dao.initializeMetadata(StorageMetadata(createdAtEpochMs = clock.now().toEpochMilli()))
            check(dao.metadata()?.dataVersion == 1) { "Unsupported data version" }
        }
        DomainResult.Success(Unit)
    }

    override fun observe(): Flow<AppRuntimeState> = flow {
        emitAll(storage.database.runtimeDao().observe())
    }.map { entity ->
        checkNotNull(entity) { "Storage must be initialized" }
        AppRuntimeState(
            entity.activeSessionId?.let(::HubId),
            entity.activeTimerId?.let(::HubId),
            entity.revision,
        )
    }.flowOn(dispatcher)

    override suspend fun update(
        state: AppRuntimeState,
        expectedRevision: Long,
    ): DomainResult<Unit> {
        if (expectedRevision < 0 || expectedRevision == Long.MAX_VALUE ||
            state.revision != expectedRevision
        ) {
            return DomainResult.Failure(DomainError.Validation)
        }
        return access {
            val changed = storage.database.runtimeDao().update(
                state.activeSessionId?.value,
                state.activeTimerId?.value,
                expectedRevision,
            )
            if (changed == 1) {
                DomainResult.Success(Unit)
            } else {
                DomainResult.Failure(DomainError.Conflict)
            }
        }
    }

    private suspend fun <T> access(block: suspend () -> DomainResult<T>): DomainResult<T> =
        withContext(dispatcher) {
            try {
                block()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                DomainResult.Failure(DomainError.Storage)
            }
        }
}
