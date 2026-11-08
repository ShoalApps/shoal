package io.github.shoalapps.shoal

import io.github.shoalapps.shoal.data.Channel
import io.github.shoalapps.shoal.data.Channels
import io.github.shoalapps.shoal.data.PublishedBuild
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChannelTest {
    private val day = 24 * 60 * 60 * 1000L
    private val now = 100 * day
    /** Builds published [ages] days ago, newest build first. */
    private fun builds(vararg ages: Double, pre: Set<Int> = emptySet()) = ages.mapIndexed { i, age ->
        val build = ages.size - i
        PublishedBuild(build, now - (age * day).toLong(), build in pre, null, "page-$build")
    }

    @Test fun nightlyTakesTheNewestBuild() {
        assertEquals(5, Channels.pick(builds(0.1, 1.0, 3.0, 8.0, 9.0), Channel.NIGHTLY, now, false)?.build)
    }

    @Test fun betaWaitsTwoDaysAndStableAWeek() {
        val list = builds(0.1, 1.0, 3.0, 8.0, 9.0)
        assertEquals(3, Channels.pick(list, Channel.BETA, now, false)?.build)
        assertEquals(2, Channels.pick(list, Channel.STABLE, now, false)?.build)
    }

    @Test fun preReleasesAreKeptOffBetaAndStable() {
        val list = builds(0.1, 3.0, 8.0, pre = setOf(2, 3))
        assertEquals(3, Channels.pick(list, Channel.NIGHTLY, now, false)?.build)
        assertEquals(1, Channels.pick(list, Channel.BETA, now, false)?.build)
    }

    @Test fun noBuildIsOldEnoughYet() {
        assertNull(Channels.pick(builds(0.5, 1.0), Channel.STABLE, now, listIsFull = false))
    }

    @Test fun aBusyWeekStillOffersTheOldestListedBuild() {
        assertEquals(1, Channels.pick(builds(0.5, 1.0, 2.0), Channel.STABLE, now, listIsFull = true)?.build)
    }

    @Test fun unknownSettingsFallBackToNightly() {
        assertEquals(Channel.NIGHTLY, Channel.of("canary"))
        assertEquals(Channel.STABLE, Channel.of("stable"))
    }
}
