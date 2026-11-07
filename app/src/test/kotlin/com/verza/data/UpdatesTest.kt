package com.verza.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdatesTest {
    @Test fun `versions compare by number, not as text`() {
        assertTrue(Updates.isNewer("1.10.0", "1.9.0"))
        assertTrue(Updates.isNewer("2.0.0", "1.20.11"))
        assertTrue(Updates.isNewer("2.0.1", "2.0"))
        assertFalse(Updates.isNewer("2.0.0", "2.0.0"))
        assertFalse(Updates.isNewer("1.20.11", "2.0.0"))
    }
}
