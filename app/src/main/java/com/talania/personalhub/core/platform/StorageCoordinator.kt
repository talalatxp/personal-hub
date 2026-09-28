package com.talania.personalhub.core.platform

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import com.talania.personalhub.core.database.HubDatabase
import com.talania.personalhub.core.database.PreservingOpenHelperFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

@Singleton
class StorageCoordinator @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @IoDispatcher dispatcher: CoroutineDispatcher,
) {
    private val root = File(context.noBackupFilesDir, "hub")
    private val databaseFile get() = File(root, "hub.db")

    internal val database: HubDatabase by lazy {
        ensureRoot()
        Room.databaseBuilder(context, HubDatabase::class.java, databaseFile.absolutePath)
            .openHelperFactory(PreservingOpenHelperFactory())
            .build()
    }

    // F09 will own replacing generations; consumers retain repositories, never paths/DAOs.
    internal val preferences by lazy {
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(SupervisorJob() + dispatcher),
            produceFile = {
                ensureRoot()
                File(root, "settings.preferences_pb")
            },
        )
    }

    internal fun mediaRoot(): File {
        ensureRoot()
        return File(root, "media").also { check(it.isDirectory || it.mkdirs()) }
    }

    private fun ensureRoot() {
        check(root.isDirectory || root.mkdirs()) { "Private storage unavailable" }
    }
}
