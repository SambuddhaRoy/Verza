package com.verza.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * The YouTube Music sign-in: the account's cookie, encrypted at rest with an AES-256/GCM key that
 * never leaves the Android Keystore. Never logged, never in a backup. The key alias and the setting
 * are the old app's, so an update stays signed in.
 */
class Account(context: Context, scope: CoroutineScope) {
    private val encrypted = Setting(context, stringPreferencesKey("account_cookie_enc"), "")

    /** The cookie, or null when signed out. Held in memory, since every request reads it. */
    val cookie: StateFlow<String?> = encrypted.flow
        .map { if (it.isEmpty()) null else decrypt(it) }
        .stateIn(scope, SharingStarted.Eagerly, null)

    val signedIn: Boolean get() = !cookie.value.isNullOrBlank()

    suspend fun set(cookie: String?) = encrypted.set(cookie?.takeIf { it.isNotBlank() }?.let(::encrypt).orEmpty())

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(
                KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
        }.generateKey()
    }

    /** Base64(iv ‖ ciphertext), the old app's format. */
    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORM).apply { init(Cipher.ENCRYPT_MODE, key()) }
        return Base64.encodeToString(cipher.iv + cipher.doFinal(plain.toByteArray()), Base64.NO_WRAP)
    }

    private fun decrypt(stored: String): String? = runCatching {
        val bytes = Base64.decode(stored, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORM).apply {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        }
        String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)))
    }.getOrNull()

    private companion object {
        const val ALIAS = "verza_account_cookie_key"
        const val TRANSFORM = "AES/GCM/NoPadding"
    }
}
