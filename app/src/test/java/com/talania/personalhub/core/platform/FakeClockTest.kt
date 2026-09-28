package com.talania.personalhub.core.platform

import com.talania.personalhub.testing.FakeClock
import com.talania.personalhub.testing.FakeIdGenerator
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class FakeClockTest {
    @Test fun civilTimeAndZoneCannotAlterElapsedDurationOrBoot() {
        val clock = FakeClock()
        clock.advance(60_000)
        clock.setCivilTime(Instant.parse("2030-01-01T00:00:00Z"))
        clock.setZone(ZoneId.of("America/New_York"))
        assertEquals(60_000L, clock.elapsedRealtime())
        assertEquals(1, clock.bootCount())
        assertEquals(ZoneId.of("America/New_York"), clock.zone())
    }

    @Test fun rebootIsIndependentOfCivilTime() {
        val clock = FakeClock()
        clock.advance(1234)
        val civil = clock.now()
        clock.reboot()
        assertEquals(civil, clock.now())
        assertEquals(0L, clock.elapsedRealtime())
        assertEquals(2, clock.bootCount())
    }

    @Test fun advanceMovesBothClocksWithoutSleeping() {
        val clock = FakeClock()
        val initial = clock.now()
        clock.advance(1000)
        assertEquals(initial.plusSeconds(1), clock.now())
        assertEquals(1000L, clock.elapsedRealtime())
    }

    @Test fun deterministicIdsAreUniqueAndRepeatable() {
        val first = FakeIdGenerator()
        val second = FakeIdGenerator()
        assertEquals(first.next(), second.next())
        assertNotEquals(first.next(), first.next())
    }
}
