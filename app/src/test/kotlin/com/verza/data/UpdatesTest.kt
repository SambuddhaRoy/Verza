package com.verza.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    @Test fun `a phone takes its own ABI's APK, else the universal one`() {
        val split = listOf("Verza-v2.9.0.apk", "Verza-v2.9.0-armeabi-v7a.apk", "Verza-v2.9.0-arm64-v8a.apk", "Verza-v2.9.0-x86_64.apk")
        assertEquals("Verza-v2.9.0-arm64-v8a.apk", pickApk(split, listOf("arm64-v8a", "armeabi-v7a", "armeabi")))
        assertEquals("Verza-v2.9.0-armeabi-v7a.apk", pickApk(split, listOf("armeabi-v7a", "armeabi")))
        assertEquals("Verza-v2.9.0.apk", pickApk(split, listOf("riscv64")))
        // A release from before the split: one APK for everyone.
        assertEquals("Verza-v2.8.0.apk", pickApk(listOf("Verza-v2.8.0.apk"), listOf("arm64-v8a")))
        assertNull(pickApk(listOf("source.zip"), listOf("arm64-v8a")))
        // Only ABI APKs and none of them this phone's: nothing to install.
        assertNull(pickApk(listOf("Verza-v2.9.0-x86_64.apk"), listOf("arm64-v8a")))
    }
}
