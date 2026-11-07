package com.verza.ui.signin

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SignInTest {
    @Test fun `Google and YouTube hosts, and Google's country domains, are allowed`() {
        listOf("accounts.google.com", "music.youtube.com", "youtube.com", "www.google.co.uk", "google.de", "i.ytimg.com", "lh3.googleusercontent.com")
            .forEach { assertTrue(it, allowedSignInHost(it)) }
    }

    @Test fun `look-alikes are not`() {
        listOf("youtube.evil.example", "notgoogle.com", "google.com.evil.com", "evil-youtube.com", "", null)
            .forEach { assertFalse(it.toString(), allowedSignInHost(it)) }
    }
}
