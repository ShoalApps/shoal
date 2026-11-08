package io.github.shoalapps.shoal

import io.github.shoalapps.shoal.data.Attempt
import io.github.shoalapps.shoal.data.Catalog
import io.github.shoalapps.shoal.data.HistoryEntry.Kind
import io.github.shoalapps.shoal.data.UpdateHistory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateHistoryTest {
    private val app = Catalog.reminders
    private val day = UpdateHistory.ATTEMPT_EXPIRY_MILLIS

    @Test fun installingOverABuildIsAnUpdate() {
        val e = UpdateHistory.success(app, Attempt(41, 42, 0, background = true), 42, 10)
        assertEquals(Kind.UPDATED, e.kind)
        assertEquals(41, e.fromBuild)
        assertEquals(42, e.toBuild)
        assertEquals(true, e.background)
    }

    @Test fun aFirstInstallIsAnInstall() {
        assertEquals(Kind.INSTALLED, UpdateHistory.success(app, Attempt(null, 7, 0), 7, 10).kind)
        assertEquals(Kind.INSTALLED, UpdateHistory.success(app, null, 7, 10).kind)
    }

    @Test fun failuresKeepTheirReason() {
        val e = UpdateHistory.failure(app, Attempt(41, 42, 0), "Cancelled", 10)
        assertEquals(Kind.FAILED, e.kind)
        assertEquals("Cancelled", e.message)
        assertEquals(42, e.toBuild)
    }

    @Test fun newestFirstAndCapped() {
        var list = emptyList<io.github.shoalapps.shoal.data.HistoryEntry>()
        for (b in 1..5) list = UpdateHistory.add(list, UpdateHistory.success(app, Attempt(b - 1, b, 0), b, b.toLong()), limit = 3)
        assertEquals(listOf(5, 4, 3), list.map { it.toBuild })
    }

    @Test fun theSameSuccessIsOnlyRecordedOnce() {
        val first = UpdateHistory.add(emptyList(), UpdateHistory.success(app, Attempt(41, 42, 0), 42, 10))
        val again = UpdateHistory.add(first, UpdateHistory.success(app, null, 42, 20))
        assertEquals(1, again.size)
        val otherApp = UpdateHistory.add(again, UpdateHistory.success(Catalog.video, null, 42, 20))
        assertEquals(2, otherApp.size)
    }

    @Test fun repeatedFailuresAreAllKept() {
        val f = UpdateHistory.failure(app, Attempt(41, 42, 0), "Install failed", 10)
        assertEquals(2, UpdateHistory.add(UpdateHistory.add(emptyList(), f), f).size)
    }

    @Test fun anInstallFinishedWhileShoalWasStoppedIsRecorded() {
        val e = UpdateHistory.settle(Catalog.manager, Attempt(41, 42, 0), 42, 10)
        assertEquals(Kind.UPDATED, e?.kind)
    }

    @Test fun anUnfinishedInstallWaitsADayThenFails() {
        assertNull(UpdateHistory.settle(app, Attempt(41, 42, 0), 41, day - 1))
        val e = UpdateHistory.settle(app, Attempt(41, 42, 0), 41, day + 1)
        assertEquals(Kind.FAILED, e?.kind)
        assertEquals("The install didn't finish", e?.message)
    }
}
