package com.talania.personalhub.core.database

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "app_runtime_state")
internal data class RuntimeEntity(
    @PrimaryKey val singletonId: Int = 1,
    val activeSessionId: String? = null,
    val activeTimerId: String? = null,
    val revision: Long = 0,
)

@Entity(tableName = "storage_metadata")
internal data class StorageMetadata(
    @PrimaryKey val singletonId: Int = 1,
    val dataVersion: Int = 1,
    val createdAtEpochMs: Long,
)

@Dao
internal interface RuntimeDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun initialize(state: RuntimeEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun initializeMetadata(metadata: StorageMetadata)

    @Query("SELECT * FROM app_runtime_state WHERE singletonId = 1")
    fun observe(): Flow<RuntimeEntity?>

    @Query("SELECT * FROM storage_metadata WHERE singletonId = 1")
    suspend fun metadata(): StorageMetadata?

    @Query(
        "UPDATE app_runtime_state SET activeSessionId = :sessionId, activeTimerId = :timerId, revision = revision + 1 WHERE singletonId = 1 AND revision = :expectedRevision",
    )
    suspend fun update(sessionId: String?, timerId: String?, expectedRevision: Long): Int
}

@Database(
    entities = [RuntimeEntity::class, StorageMetadata::class],
    version = 1,
    exportSchema = true,
)
internal abstract class HubDatabase : RoomDatabase() {
    abstract fun runtimeDao(): RuntimeDao
}
