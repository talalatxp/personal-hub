package com.talania.personalhub.app

import com.talania.personalhub.core.database.RuntimeRepository
import com.talania.personalhub.feature.settings.data.PreferencesRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// Debug-only bridge: the test APK cannot add interfaces to the already built app component.
@EntryPoint
@InstallIn(SingletonComponent::class)
interface PersistenceTestDependencies {
    fun runtime(): RuntimeRepository
    fun preferences(): PreferencesRepository
}
