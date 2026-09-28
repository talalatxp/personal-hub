package com.talania.personalhub.testing

import com.talania.personalhub.core.platform.Clock
import java.time.Instant
import java.time.ZoneId

class FakeClock(
    private var instant: Instant = Instant.parse("2026-09-28T10:00:00Z"),
    private var elapsed: Long = 0,
    private var boot: Int = 1,
    private var zoneId: ZoneId = ZoneId.of("Europe/Madrid"),
) : Clock {
    override fun now(): Instant = instant
    override fun elapsedRealtime(): Long = elapsed
    override fun bootCount(): Int = boot
    override fun zone(): ZoneId = zoneId

    fun advance(milliseconds: Long) {
        require(milliseconds >= 0)
        elapsed = Math.addExact(elapsed, milliseconds)
        instant = instant.plusMillis(milliseconds)
    }

    fun setCivilTime(value: Instant) {
        instant = value
    }
    fun setZone(value: ZoneId) {
        zoneId = value
    }
    fun reboot() {
        boot = Math.addExact(boot, 1)
        elapsed = 0
    }
}
