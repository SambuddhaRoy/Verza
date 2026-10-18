package com.verza.player

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Whether there is a network to stream over at all.
 *
 * ponytail: a connected network is taken as online, so Wi-Fi with no internet behind it still counts.
 * Add NET_CAPABILITY_VALIDATED if that case matters; some emulators never validate, so it is left out.
 */
internal fun isOnline(context: Context): Boolean {
    val cm = context.getSystemService(ConnectivityManager::class.java) ?: return true
    val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
