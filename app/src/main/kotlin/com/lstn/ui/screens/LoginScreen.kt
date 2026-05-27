package com.lstn.ui.screens

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.lstn.ui.theme.LocalLstnExtendedColors

private const val LOGIN_URL =
    "https://accounts.google.com/ServiceLogin?ltmpl=music&service=youtube&passive=true&continue=https%3A%2F%2Fmusic.youtube.com%2F"

/**
 * Turns the device's real WebView user-agent into a believable mobile-Chrome one by removing the
 * embedded-WebView markers ("; wv" and "Version/4.0"). Google's "this browser may not be secure"
 * block keys off those markers, so stripping them is what lets the sign-in flow through.
 */
private fun WebView.deWebViewUserAgent(): String =
    settings.userAgentString
        .replace("; wv", "")
        .replace(Regex("Version/[0-9.]+ "), "")

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoginScreen(
    onSignedIn: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalLstnExtendedColors.current
    var captured by remember { mutableStateOf(false) }
    var showManual by remember { mutableStateOf(false) }
    var manualCookie by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize()) {
        // ── Header ─────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.Close, contentDescription = "Cancel", tint = colors.onBackground)
            }
            Spacer(Modifier.width(4.dp))
            Text(
                "Sign in to YouTube Music",
                style = MaterialTheme.typography.titleLarge,
                color = colors.onBackground,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            TextButton(
                onClick = { showManual = !showManual },
                shape = CircleShape,
            ) {
                Text(if (showManual) "Browser" else "Paste cookie")
            }
        }

        if (showManual) {
            ManualCookieEntry(
                value = manualCookie,
                onValueChange = { manualCookie = it },
                onSubmit = {
                    val c = manualCookie.trim()
                    if (c.contains("SAPISID")) onSignedIn(c)
                },
            )
        } else {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    val cookieManager = CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)

                    WebView(context).apply {
                        cookieManager.setAcceptThirdPartyCookies(this, true)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.userAgentString = deWebViewUserAgent()

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                if (captured) return
                                val cookie = cookieManager.getCookie("https://music.youtube.com")
                                if (cookie != null && cookie.contains("SAPISID")) {
                                    captured = true
                                    onSignedIn(cookie)
                                }
                            }
                        }
                        loadUrl(LOGIN_URL)
                    }
                },
            )
        }
    }
}

@Composable
private fun ManualCookieEntry(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalLstnExtendedColors.current
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            "If Google blocks the in-app sign-in, open music.youtube.com in your browser while " +
                "signed in, copy the request 'Cookie' header, and paste it below.",
            style = MaterialTheme.typography.bodyMedium,
            color = ext.muted,
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    "VISITOR_INFO1_LIVE=…; SAPISID=…; …",
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.muted,
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            minLines = 4,
            textStyle = MaterialTheme.typography.bodySmall,
        )
        Button(
            onClick = onSubmit,
            shape = CircleShape,
            enabled = value.contains("SAPISID"),
        ) { Text("Use this cookie") }
    }
}
