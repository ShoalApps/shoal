package io.github.shoalapps.shoal

import io.github.shoalapps.shoal.data.eligibleForUpdate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateEligibilityTest {
    private val app = "io.github.shoalapps.squill"
    private val shoal = "io.github.shoalapps.shoal"

    @Test fun onlyAvailableUpdatesAreEligible() {
        assertTrue(eligibleForUpdate(app, true, emptySet()))
        assertFalse(eligibleForUpdate(app, false, emptySet()))
        assertFalse(eligibleForUpdate(app, false, setOf(app)))
    }

    @Test fun aHoldOnlyExcludesItsOwnPackage() {
        assertFalse(eligibleForUpdate(app, true, setOf(app)))
        assertTrue(eligibleForUpdate(shoal, true, setOf(app)))
    }

    @Test fun shoalCanBeHeldToo() {
        assertFalse(eligibleForUpdate(shoal, true, setOf(shoal)))
        assertTrue(eligibleForUpdate(app, true, setOf(shoal)))
    }

    @Test fun removingAHoldAllowsUpdatesAgain() {
        val held = setOf(app, shoal)
        assertTrue(eligibleForUpdate(app, true, held - app))
        assertFalse(eligibleForUpdate(shoal, true, held - app))
    }

    @Test fun unknownAndRemovedPackagesDoNotAffectOtherApps() {
        assertTrue(eligibleForUpdate(app, true, setOf("unknown.package", "io.github.shoalapps.clownfish")))
    }
}
