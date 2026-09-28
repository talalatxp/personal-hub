package com.talania.personalhub.feature.settings.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.talania.personalhub.core.model.DomainError
import com.talania.personalhub.core.model.DomainResult
import com.talania.personalhub.core.platform.StorageCoordinator
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

@Serializable
enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Serializable
data class HubPreferences(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
)

interface PreferencesRepository {
    fun observe(): Flow<HubPreferences>
    suspend fun save(preferences: HubPreferences): DomainResult<Unit>
}

class LocalPreferencesRepository internal constructor(
    private val store: () -> DataStore<Preferences>,
) : PreferencesRepository {
    @Inject constructor(storage: StorageCoordinator) : this({ storage.preferences })

    override fun observe(): Flow<HubPreferences> = store().data.map { values ->
        HubPreferences(
            theme = values[THEME]?.let(ThemeMode::valueOf) ?: ThemeMode.SYSTEM,
            soundEnabled = values[SOUND] ?: true,
            vibrationEnabled = values[VIBRATION] ?: true,
        )
    }

    override suspend fun save(preferences: HubPreferences): DomainResult<Unit> = try {
        store().edit { values ->
            values[THEME] = preferences.theme.name
            values[SOUND] = preferences.soundEnabled
            values[VIBRATION] = preferences.vibrationEnabled
        }
        DomainResult.Success(Unit)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        DomainResult.Failure(DomainError.Storage)
    }

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val SOUND = booleanPreferencesKey("sound_enabled")
        val VIBRATION = booleanPreferencesKey("vibration_enabled")
    }
}
