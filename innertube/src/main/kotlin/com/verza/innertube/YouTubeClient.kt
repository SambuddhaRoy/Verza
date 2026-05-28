package com.verza.innertube

/**
 * A YouTube InnerTube client identity. Different clients are served by different hosts/keys
 * and get different treatment from the player endpoint. As of 2024+, the ANDROID_MUSIC client
 * returns LOGIN_REQUIRED with no streamingData; a handful of *youtube.com* clients (notably
 * ANDROID_VR and IOS) still return directly-playable adaptive formats without a PoToken — but
 * only when hit on www.youtube.com with their own API key, not on music.youtube.com.
 */
data class YouTubeClient(
    val clientName: String,
    val clientVersion: String,
    val clientId: String,           // numeric X-YouTube-Client-Name header value
    val apiBaseUrl: String,
    val apiKey: String,
    val origin: String,
    val userAgent: String,
    val osName: String? = null,
    val osVersion: String? = null,
    val deviceMake: String? = null,
    val deviceModel: String? = null,
    val androidSdkVersion: Int? = null,
) {
    companion object {
        private const val MUSIC_BASE = "https://music.youtube.com/youtubei/v1"
        private const val WWW_BASE = "https://www.youtube.com/youtubei/v1"

        private const val KEY_MUSIC = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-KKVH2hiOg"
        private const val KEY_WEB = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"
        private const val KEY_ANDROID = "AIzaSyA8eiZmM1FaDVjRy-df2KTyQ_vz_yYM39w"
        private const val KEY_IOS = "AIzaSyB-63vPrdThhKuerbB2N_l7Kwwcxj6yUAc"

        val WEB_REMIX = YouTubeClient(
            clientName = "WEB_REMIX",
            clientVersion = "1.20240101.01.00",
            clientId = "67",
            apiBaseUrl = MUSIC_BASE,
            apiKey = KEY_MUSIC,
            origin = "https://music.youtube.com",
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        )

        // Oculus YouTube VR app — historically does not require a PoToken for streams.
        val ANDROID_VR = YouTubeClient(
            clientName = "ANDROID_VR",
            clientVersion = "1.60.19",
            clientId = "28",
            apiBaseUrl = WWW_BASE,
            apiKey = KEY_ANDROID,
            origin = "https://www.youtube.com",
            userAgent = "com.google.android.apps.youtube.vr.oculus/1.60.19 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip",
            osName = "Android",
            osVersion = "12L",
            deviceMake = "Oculus",
            deviceModel = "Quest 3",
            androidSdkVersion = 32,
        )

        // iOS YouTube app — another commonly-working no-PoToken fallback.
        val IOS = YouTubeClient(
            clientName = "IOS",
            clientVersion = "19.45.4",
            clientId = "5",
            apiBaseUrl = WWW_BASE,
            apiKey = KEY_IOS,
            origin = "https://www.youtube.com",
            userAgent = "com.google.ios.youtube/19.45.4 (iPhone16,2; U; CPU iOS 18_1_0 like Mac OS X)",
            osName = "iOS",
            osVersion = "18.1.0.22B83",
            deviceMake = "Apple",
            deviceModel = "iPhone16,2",
        )

        // Legacy music client — kept for diagnostics/comparison (currently LOGIN_REQUIRED).
        val ANDROID_MUSIC = YouTubeClient(
            clientName = "ANDROID_MUSIC",
            clientVersion = "6.29.59",
            clientId = "21",
            apiBaseUrl = MUSIC_BASE,
            apiKey = KEY_MUSIC,
            origin = "https://music.youtube.com",
            userAgent = "com.google.android.apps.youtube.music/6.29.59 (Linux; U; Android 11) gzip",
            osName = "Android",
            osVersion = "11",
            androidSdkVersion = 30,
        )

        // Embedded TV player — bypasses some restrictions for embeddable videos.
        val TVHTML5 = YouTubeClient(
            clientName = "TVHTML5_SIMPLY_EMBEDDED_PLAYER",
            clientVersion = "2.0",
            clientId = "85",
            apiBaseUrl = WWW_BASE,
            apiKey = KEY_WEB,
            origin = "https://www.youtube.com",
            userAgent = "Mozilla/5.0 (PlayStation; PlayStation 4/12.00) AppleWebKit/605.1.15 (KHTML, like Gecko)",
        )
    }
}
