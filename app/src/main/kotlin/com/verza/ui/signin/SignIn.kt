package com.verza.ui.signin

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.verza.graph
import com.verza.ui.kit.Word
import com.verza.ui.theme.Corner
import com.verza.ui.theme.LocalPalette
import com.verza.ui.theme.Type
import kotlinx.coroutines.launch

private const val SIGN_IN = "https://accounts.google.com/ServiceLogin?ltmpl=music&service=youtube&passive=true&continue=https%3A%2F%2Fmusic.youtube.com%2F"

private val ALLOWED = listOf("google.com", "youtube.com", "youtu.be", "gstatic.com", "ggpht.com", "ytimg.com", "googleusercontent.com", "googleapis.com", "withgoogle.com")
/** Google's country domains (google.co.uk, google.de), which can't be listed but can be shaped. */
private val GOOGLE_COUNTRY = Regex("^(?:[a-z0-9-]+\\.)*google\\.[a-z]{2,3}(?:\\.[a-z]{2})?$")

/**
 * Whether the sign-in WebView may go to [host]. An exact domain or a subdomain of one, never a
 * substring: "youtube.evil.example" got through when this was `host.contains("youtube")`, into a
 * WebView that runs JavaScript and holds the account's cookies.
 */
internal fun allowedSignInHost(host: String?): Boolean {
    val h = host?.lowercase()?.trimEnd('.').orEmpty()
    if (h.isBlank()) return false
    return ALLOWED.any { h == it || h.endsWith(".$it") } || GOOGLE_COUNTRY.matches(h)
}

/**
 * Google's own sign-in page, in a WebView whose user agent drops the WebView markers Google refuses
 * ("this browser may not be secure"). Once music.youtube.com has a SAPISID cookie the account is
 * signed in, and the cookie is kept encrypted (Account). The WebView's session is wiped on the way out.
 * Pasting a cookie copied from a browser is the way round a block.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SignIn(onDone: () -> Unit) {
    val graph = LocalContext.current.graph
    val scope = rememberCoroutineScope()
    var pasting by remember { mutableStateOf(false) }
    var pasted by remember { mutableStateOf("") }
    var captured by remember { mutableStateOf(false) }
    val p = LocalPalette.current
    val keep: (String) -> Unit = { cookie -> scope.launch { graph.account.set(cookie); graph.sync.flush(); onDone() } }
    BackHandler(onBack = onDone)
    DisposableEffect(Unit) {
        onDispose {
            CookieManager.getInstance().apply { removeAllCookies(null); flush() }
            WebStorage.getInstance().deleteAllData()
        }
    }
    Column(Modifier.fillMaxSize().background(p.canvas)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDone) { Icon(Icons.Rounded.Close, "Cancel", tint = p.ink) }
            Text("Sign in", style = Type.Section, color = p.ink, modifier = Modifier.weight(1f))
            TextButton(onClick = { pasting = !pasting }) { Text(if (pasting) "Browser" else "Paste cookie") }
        }
        if (pasting) Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "If Google blocks signing in here, open music.youtube.com in a browser where you're signed in, copy the request's Cookie header, and paste it below.",
                style = Type.Body, color = p.inkMuted,
            )
            OutlinedTextField(pasted, { pasted = it }, modifier = Modifier.fillMaxWidth(), minLines = 4, shape = Corner, placeholder = { Text("SAPISID=…; …") })
            Word("use it", p.accent, enabled = "SAPISID" in pasted) { keep(pasted.trim()) }
        } else AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                val cookies = CookieManager.getInstance().apply { setAcceptCookie(true) }
                WebView(context).apply {
                    cookies.setAcceptThirdPartyCookies(this, true)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.userAgentString = settings.userAgentString.replace("; wv", "").replace(Regex("Version/[0-9.]+ "), "")
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?) = !allowedSignInHost(request?.url?.host)
                        override fun onPageFinished(view: WebView?, url: String?) {
                            val cookie = cookies.getCookie("https://music.youtube.com")
                            if (!captured && cookie != null && "SAPISID" in cookie) { captured = true; keep(cookie) }
                        }
                    }
                    loadUrl(SIGN_IN)
                }
            },
        )
    }
}
