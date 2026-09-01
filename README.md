<div align="center">

# Verza

**A YouTube Music client for Android that takes its colours from whatever you are listening to.**

[![Download](https://img.shields.io/github/v/release/SambuddhaRoy/Verza?style=for-the-badge&label=Download%20APK&color=5B4BE0)](https://github.com/SambuddhaRoy/Verza/releases/latest)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Android 8+](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue?style=for-the-badge)](LICENSE)

<img src="docs/home.jpg" alt="Home, coloured by the album art" width="300"/>
&nbsp;&nbsp;
<img src="docs/now-playing.jpg" alt="Now Playing" width="300"/>

<sub>Two moments from the same app. Every colour on both screens was derived from the cover that was playing.</sub>

</div>

---

Verza plays the YouTube Music catalogue without ads or a subscription. Sign in and it brings your home feed, playlists, followed artists and liked songs with you; skip it and everything except your account library still works. There is no Verza server, no account, and no telemetry — the app talks to YouTube, and to LRCLIB, MusicBrainz and iTunes for lyrics, genres and artwork, and to nothing else.

It is a personal project, built in the open. It is not affiliated with or endorsed by Google or YouTube.

## Install

Download the APK from the [latest release](https://github.com/SambuddhaRoy/Verza/releases/latest) and open it.

Android will warn you about installing from an unknown source, because the APK is signed with my own key rather than by Google Play. You will need to allow your browser or file manager to install apps once. Verza checks GitHub for its own updates and can install them for you from Settings.

Android 8.0 or newer.

## The colour

The screenshots above are the same build. One is playing something with a deep red cover, the other an olive-green one, and everything you can see — canvas, cards, buttons, the accent on the nav pill — was computed from those covers when the track started.

The rule is that the artwork chooses the hues and never chooses the ink. A colour is sampled from the cover and held inside a window; the text and icon colours on top of it are then *measured*, and pushed until every pairing clears 4.5:1 against the surface it sits on. That is why the olive screen has near-black controls and the red one has cyan: a bright accent could not clear the contrast floor against olive, so the search moved the other way. Nothing here is a stored palette that happens to look nice against a test image.

There are five **flavours** in Settings, which change how far to push the artwork rather than replacing it: *Signature*, *Deep*, *Hushed*, *Vivid* and *Pastel*. They only move the windows the canvas is allowed to occupy, so none of them can produce something illegible — the contrast search still runs underneath. A [test](app/src/test/kotlin/com/verza/ui/expressive/ExpressiveColorsTest.kt) sweeps all five across 483 seed colours and asserts every text pair in the app clears the floor. It has caught real failures that would otherwise have shipped, including a container colour sitting at 3.87:1 and a secondary text colour at 2.51:1.

## What it does

**Playback.** The full catalogue, background playback, lock-screen and notification controls, Bluetooth and Android Auto, a queue you can pull up from Now Playing and remove from, song radio, shuffle and repeat, a sleep timer with a slow fade, and a graphic equalizer with bass boost and loudness.

**Offline.** Download a song, an album or a playlist into your Music folder as ordinary files named *Artist - Title*, one folder per playlist, playable by anything on the device. Verza asks YouTube for AAC-in-MP4 and saves `.m4a`; where only Opus or WebM is offered it keeps that container rather than mislabelling it, which would produce a file that looks playable and is not. You choose the folder; the default is `Music/Verza`.

**Your library.** Six tabs — recent, liked, on-device, downloaded, playlists and artists — so music already on the phone sits beside music that is streamed, and both play from the same queue.

**Lyrics.** Time-synced where LRCLIB has them, scrolling with the song, and shareable as a card.

**A home page that is actually yours.** Recently played and your liked songs come straight from the device and appear immediately. Around them: *More like your week*, built on-device from what you have really listened to, and curated mixes — a daylist that follows the time of day, a discovery mix, and a release radar — all generated locally from your own play history. Genre chips filter the whole page.

**Listening stats.** Top songs, artists and genres over time, plus focus sessions and an ambient full-screen display for when the phone is propped up.

**Sharing.** Send a song, a lyric card, or a listening session another Verza user can join.

## The design

Material 3 Expressive, taken literally rather than as a coat of paint.

The album art wears a shape, not a rectangle — a scalloped mask that morphs into a different silhouette when the track changes, or a plain rounded square if you would rather. Headings are a large serif italic; controls are pills; the selected tab expands to carry its label. Motion is springs rather than curves, so things overshoot slightly and settle. The seek bar can double as a live spectrum driven by the playback audio, and the same signal can drive a gentle vibration on the beat.

Everything that animates at audio rate is read in the draw phase rather than during composition. That sounds like an implementation detail and is really a design constraint: read one frame too high in the tree and the whole screen recomposes sixty times a second, which is what a stuttering app actually is.

## Building it

```bash
git clone https://github.com/SambuddhaRoy/Verza.git
cd Verza
./gradlew :app:assembleDebug          # app/build/outputs/apk/debug/
./gradlew :app:testDebugUnitTest      # unit tests
```

JDK 17 and the Android SDK (API 35). A release build needs a `keystore.properties` in the project root pointing at your own signing key; without one, `assembleRelease` produces an unsigned APK.

## How it fits together

Three Gradle modules:

| Module | What lives there |
|---|---|
| `:app` | UI, navigation, Room database, preferences, downloads, stats — everything the user touches |
| `:innertube` | The YouTube Music client: search, browse, home feed, artists, playlists, lyrics |
| `:player` | The Media3 session, the foreground service, and stream resolution |

`:app` depends on both; `:innertube` and `:player` do not know about each other. Streams are resolved through [NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor); browse and search go through InnerTube directly. Playback is ExoPlayer behind a `MediaLibrarySession`, which is what makes the notification, the lock screen, Bluetooth and Auto work without special cases.

Compose for the UI, Hilt for dependency injection, Room for the library, DataStore for settings, Coil for images. No Google Play Services — the app runs on a de-Googled device.

## Privacy

There is no backend. Nothing about how you use Verza reaches me, because there is nowhere for it to go.

Your sign-in cookie is encrypted with a key held in the Android Keystore, kept out of backups and device transfers, and sent only to YouTube. Listening history, stats and playlists live in a database on your phone and are used only to build your own home page and mixes. Lyrics lookups go to LRCLIB with a song title and artist; artwork and genre lookups go to MusicBrainz and iTunes the same way. None of those requests carry anything identifying you.

If the app crashes it writes the error to a file on your device. Nothing is sent unless you go to Settings and choose to share it.

The full detail is in [PRIVACY.md](PRIVACY.md).

## Notes

Verza is unofficial. It uses the same public endpoints the YouTube Music web player does, and it can break when those change — that is the nature of the thing, and when it happens it usually shows up as songs failing to start.

Contributions and bug reports are welcome. If you hit a crash, the report from Settings → Help is far more useful than a description.

## Licence

[Apache 2.0](LICENSE).
