package com.verza.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The sign-in WebView's navigation allowlist.
 *
 * This check is the only thing standing between a hijacked redirect and an attacker's page loading
 * inside a JavaScript-enabled WebView that is carrying live Google cookies. It was written as
 * `host.contains("youtube")`, which "youtube.evil.example" satisfies — so the guard was decorative.
 * Pinned here because the replacement is a regex, and a regex nobody tests is the same guard again.
 */
class LoginHostTest {

    @Test
    fun `the hosts the sign-in flow actually uses are allowed`() {
        val allowed = listOf(
            "accounts.google.com",
            "google.com",
            "www.google.com",
            "myaccount.google.com",
            "ssl.gstatic.com",
            "www.youtube.com",
            "m.youtube.com",
            "youtube.com",
            "yt3.ggpht.com",
            "i.ytimg.com",
            "lh3.googleusercontent.com",
            "www.googleapis.com",
            // Google routes sign-in through country domains.
            "accounts.google.co.uk",
            "google.de",
            "www.google.com.au",
        )
        for (host in allowed) assertTrue("should allow $host", isAllowedLoginHost(host))
    }

    @Test
    fun `lookalike domains are rejected`() {
        val rejected = listOf(
            // Every one of these passed the old substring check.
            "youtube.evil.example",
            "my-google-login.tk",
            "accounts-google.co",
            "notgstatic.io",
            "googleapis.attacker.net",
            "google.com.evil.com",
            "evil.com/google.com",
            "notgoogle.com",
            "xgoogle.com",
            "ytimg.co.uk.phish.net",
            // And the degenerate inputs.
            "",
            "   ",
            null,
        )
        for (host in rejected) assertFalse("should reject ${host ?: "null"}", isAllowedLoginHost(host))
    }

    @Test
    fun `case and a trailing root dot do not get past it`() {
        assertTrue(isAllowedLoginHost("Accounts.Google.COM"))
        assertTrue(isAllowedLoginHost("accounts.google.com."))
        assertFalse(isAllowedLoginHost("YouTube.Evil.Example"))
    }
}
