package com.talania.personalhub.app

import com.talania.personalhub.core.database.RoomRuntimeRepository
import com.talania.personalhub.core.database.RuntimeRepository
import com.talania.personalhub.core.platform.AndroidClock
import com.talania.personalhub.core.platform.AndroidPrivateFiles
import com.talania.personalhub.core.platform.Clock
import com.talania.personalhub.core.platform.IdGenerator
import com.talania.personalhub.core.platform.IoDispatcher
import com.talania.personalhub.core.platform.PrivateFiles
import com.talania.personalhub.core.platform.UuidGenerator
import com.talania.personalhub.feature.settings.data.LocalPreferencesRepository
import com.talania.personalhub.feature.settings.data.PreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(SingletonComponent::class)
abstract class PlatformModule {
    @Binds @Singleton
    abstract fun clock(implementation: AndroidClock): Clock

    @Binds @Singleton
    abstract fun ids(implementation: UuidGenerator): IdGenerator

    @Binds @Singleton
    abstract fun files(implementation: AndroidPrivateFiles): PrivateFiles

    @Binds @Singleton
    abstract fun runtime(implementation: RoomRuntimeRepository): RuntimeRepository

    @Binds @Singleton
    abstract fun preferences(implementation: LocalPreferencesRepository): PreferencesRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {
    @Provides @IoDispatcher
    fun io(): CoroutineDispatcher = Dispatchers.IO
}
