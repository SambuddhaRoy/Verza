package com.verza.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Version comparison for the self-updater.
 *
 * Worth pinning because the failure mode is silent and delayed. A string comparison works fine for
 * the first nine patch releases and then quietly decides 1.10.0 is older than 1.9.0, at which point
 * the app stops offering updates and nothing anywhere reports an error.
 */
class UpdateVersionTest {

    private fun newer(a: String, b: String) = UpdateRepository.isNewer(a, b)

    @Test
    fun `a higher version is offered`() {
        assertTrue(newer("1.3.2", "1.3.1"))
        assertTrue(newer("1.4.0", "1.3.9"))
        assertTrue(newer("2.0.0", "1.9.9"))
    }

    @Test
    fun `the same version is not offered`() {
        assertFalse(newer("1.3.1", "1.3.1"))
        assertFalse(newer("1.3", "1.3.0"))
        assertFalse(newer("1.3.0", "1.3"))
    }

    @Test
    fun `an older version is never offered`() {
        assertFalse(newer("1.3.0", "1.3.1"))
        assertFalse(newer("0.9.9", "1.0.0"))
    }

    @Test
    fun `double-digit segments compare numerically`() {
        // The case a string compare gets wrong, and the reason this function exists.
        assertTrue(newer("1.10.0", "1.9.0"))
        assertFalse(newer("1.9.0", "1.10.0"))
        assertTrue(newer("1.3.10", "1.3.9"))
    }

    @Test
    fun `differing segment counts are handled`() {
        assertTrue(newer("1.4", "1.3.9"))
        assertFalse(newer("1.3", "1.3.1"))
    }

    @Test
    fun `suffixes and junk do not throw`() {
        // Tags in the wild carry things like "1.4.0-beta1"; the numeric prefix is what matters and a
        // malformed tag must never crash the check.
        assertTrue(newer("1.4.0-beta1", "1.3.1"))
        assertFalse(newer("", "1.3.1"))
        assertFalse(newer("not-a-version", "1.3.1"))
    }
}
