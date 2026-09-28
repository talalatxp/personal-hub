package com.talania.personalhub.core.platform

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

interface Clock {
    fun now(): Instant
    fun elapsedRealtime(): Long
    fun zone(): ZoneId

    // Null means Android could not supply an identity; never infer it from civil time.
    fun bootCount(): Int?
}

class AndroidClock @Inject constructor(@param:ApplicationContext private val context: Context) :
    Clock {
    override fun now(): Instant = Instant.now()
    override fun elapsedRealtime(): Long = SystemClock.elapsedRealtime()
    override fun zone(): ZoneId = ZoneId.systemDefault()
    override fun bootCount(): Int? =
        Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)
            .takeIf { it >= 0 }
}
