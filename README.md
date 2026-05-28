<div align="center">

# Verza

### A hyper-minimal YouTube Music client for Android

*Stream the full YouTube Music catalogue. Without ads, with real album art, with offline downloads, with synced lyrics, with eight themed palettes — built from scratch in Kotlin + Compose.*

<br/>

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-API%2026%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack-Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Material-3-6750A4?style=for-the-badge&logo=materialdesign&logoColor=white)](https://m3.material.io/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue?style=for-the-badge)](LICENSE)

<br/>

<img src="docs/home.jpg" alt="Verza home page" width="270"/>&nbsp;&nbsp;<img src="docs/now-playing.jpg" alt="Verza now playing" width="270"/>

</div>

---

## At a glance

<table>
<tr>
<td width="33%" valign="top">

### Music
Full YouTube Music catalogue · No ads · Offline downloads · Song radio · Background playback · Lock-screen controls

</td>
<td width="33%" valign="top">

### Discovery
Personalised home feed · "Similar to" radios from your recent artists · Charts and trending tucked at the bottom

</td>
<td width="33%" valign="top">

### Identity
Eight color themes including **Material You** · Editorial typography · Soft-rounded Muse design language

</td>
</tr>
<tr>
<td valign="top">

### Lyrics
Free synced lyrics from LRCLIB · Line-by-line auto-scroll · Plain-text fallback

</td>
<td valign="top">

### Library
Local + server-side liked songs · Your YT playlists · Followed artists · Downloads tab · User-made playlists

</td>
<td valign="top">

### Real album art
Songs throughout the app pull the actual cover from iTunes Search — no more random music-video frames

</td>
</tr>
</table>

---

## Screenshots

<div align="center">
<table>
<tr>
<td align="center">
<img src="docs/home.jpg" alt="Home" width="280"/>
<br/>
<sub><b>Home</b> — personal-first feed with mixed section sizes</sub>
</td>
<td align="center">
<img src="docs/now-playing.jpg" alt="Now Playing" width="280"/>
<br/>
<sub><b>Now Playing</b> — real album art, big accent play button, action row</sub>
</td>
</tr>
</table>
</div>

---

## Install

<div align="center">

[![Latest Release](https://img.shields.io/github/v/release/SambuddhaRoy/Verza-Music?style=for-the-badge&label=Download%20APK&color=7F52FF)](https://github.com/SambuddhaRoy/Verza-Music/releases/latest)

</div>

1. Download the **latest `Verza-vX.Y.Z.apk`** from the [Releases](https://github.com/SambuddhaRoy/Verza-Music/releases) page on your Android phone.
2. Open the downloaded file. Android will ask whether your browser is allowed to install apps — tap **Settings → Allow from this source**, then back out.
3. Tap the APK again. Android will offer to install — tap **Install**.
4. **A "Google Play Protect" warning will appear.** This is normal for any app not downloaded from the Play Store — keep reading.
5. Tap **Install anyway** (sometimes shown as **More details → Install anyway** depending on your Android version).
6. Done. Launch Verza from your app drawer.

> **Minimum requirements:** Android 8.0 Oreo (API 26) or newer. ~10 MB of storage. No special permissions other than internet and (optionally) notifications for the playback controls.

### Why does Android show a "Play Protect" warning?

Short answer: **because Verza is not on the Google Play Store, and that's the only signal Play Protect can use.** It is **not** a sign that the app contains malware.

Longer answer:

- Google's *Play Protect* service runs on every Android phone and scans every app it sees. When an app isn't from the Play Store, Play Protect has no Google-verified record of it, so it shows a precautionary warning — **the same warning it shows for every sideloaded app**, from open-source music players to dev-built work apps.
- Verza isn't on the Play Store because **Google won't allow it there.** It's an unofficial YouTube Music client and depends on public YouTube endpoints — anything that streams YouTube content outside the official YouTube apps violates the Play Store's developer policies regardless of how clean the code is. Apps like NewPipe, OuterTune, and InnerTune all live off-Play for exactly the same reason.
- **Every line of Verza's code is open and inspectable in this very repository.** If you want to verify what the app does before installing, browse the `:app`, `:innertube`, and `:player` source directories. There's no obfuscation, no closed binary blob, no telemetry, no ads — just Kotlin source you can read.
- Once you install Verza once and Android's seen the signing certificate, future updates from the same signer trigger a much milder prompt, and eventually none at all.

If you'd rather not see the warning at all on a phone you trust Verza on, you can disable Play Protect's scanning under **Settings → Security → Google Play Protect → ⚙ → Scan apps with Play Protect**. Most users leave it on and just tap *Install anyway* the one time.

---

## Features

### Playback
- **Full YouTube Music catalogue** via [NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor) — handles signature deciphering and the `n`-parameter rolling cipher, so streams play on a clean install with no auth required.
- **Account sign-in (optional)** unlocks your personalised home, your saved playlists, your followed artists, your server-side Liked Songs, and pushes likes back to your account.
- **Offline downloads** — cached to app-private storage; the resolver prefers local files when available, so playback works without network once a track is downloaded.
- **Song radio** — start an endless mix from any track (YouTube `RDAMVM<videoId>` watch playlists).
- **Audio quality** picker (Low / Medium / High) honoured by the stream resolver.
- **Queue persistence** across cold starts — restores paused at the saved position so a process kill doesn't lose your session.

### Home page
- **Personal-first composition** — *Recently Played*, *Quick Picks*, *Your Daily Discover*, *Keep Listening*, *From Your Liked Songs*, *Your YouTube Playlists*, *Similar to <artist>* — with one consolidated *Browse charts and trending* row at the bottom for everything generic.
- **Spotify-style mixed sizes** — large featured carousels, standard carousels, compact card rows, and a 4×2 non-scrolling dense grid mix so the page reads with rhythm and fits more content per screen.
- **Genre chip row** at the top for the mood/genre filter, like the major apps.

### Search
- Filter tabs — **Songs · Albums · Artists · Playlists**.
- As-you-type autocomplete suggestions powered by YouTube Music's suggest endpoint.
- Recent-search history chips with one-tap recall + Clear.

### Library
- **Recently played** + **Liked** (Room-backed, works fully offline).
- **Downloaded** tab for tracks cached for offline playback.
- **Playlists** tab — your local user-created playlists plus your saved YouTube Music playlists.
- **Artists** tab for followed channels (signed-in).
- *"Add to playlist"* sheet on any track from anywhere in the app via the row overflow menu.

### Now Playing
- Big artwork with soft shadow · shuffle · prev · big accent play button · next · repeat · scrubbable progress.
- Action row: **Like · Radio · Lyrics · Queue** (toggles an in-place Up Next list).
- Overflow ⋯ menu: Share, Copy link, Lyrics, Start radio, Download / Remove download.
- Foreground media service with lock-screen / notification controls via Media3 `MediaLibrarySession`.

### Lyrics
- **Synced (LRC) lyrics** with line-by-line auto-scrolling — the active line is bold and centred, others fade with `muted` alpha.
- Falls back to plain-text lyrics when only those are available.
- Caches per `(title, artist, duration)` so re-fetch doesn't fire while the playhead ticks.

### Theming
- Seven hand-tuned themes: **Bauhaus · Malibu · Concrete · Noir · Ember · Acid · Magenta**.
- **Material You** (Dynamic) theme on Android 12+ derives a full M3 colour scheme from your wallpaper and follows system light/dark.
- Custom `VerzaExtendedColors` palette (`muted`, `glass`, `glassHeavy`, `borderGlass`, …) for surfaces M3 doesn't cover.
- **Muse** design language — Playfair Display serif for headlines, Inter for body, IBM Plex Mono reserved for timecodes.

---

## Tech stack

| Layer | Tech |
|---|---|
| **Language** | Kotlin 2.0 |
| **UI** | Jetpack Compose · Material 3 · Coil 3 |
| **Playback** | Media3 / ExoPlayer · custom `ResolvingDataSource` |
| **Stream extraction** | [NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor) (with Mozilla Rhino for the signature cipher) |
| **HTTP** | Ktor for InnerTube · OkHttp shared across the app |
| **DI** | Hilt |
| **Persistence** | Room (history / likes / downloads / local playlists) · DataStore (preferences + queue) |
| **Serialization** | kotlinx.serialization |
| **Async** | Kotlin Coroutines + StateFlow |

---

## Architecture

Verza is a three-module Android project:

```
:app          Compose UI, ViewModels, Hilt graph, navigation
:innertube    InnerTube API client, parsers (search / home / artist / …),
              and the NewPipe-backed stream resolver
:player       Media3 MediaLibraryService + PlayerConnection
              (MediaController wrapper exposing PlaybackState)
```

### Playback flow

```
UI ──playSongs──▶ PlaybackViewModel ──setQueue──▶ MediaController
                                                       │
                                                       ▼
                                          ┌────────────────────────┐
                                          │ MediaLibrarySession     │
                                          │ onAddMediaItems()       │
                                          │ ──rebuilds URI──────▶   │  innertube://<videoId>
                                          └────────────────────────┘
                                                       │
                                                       ▼
                                          ┌────────────────────────┐
                                          │ ExoPlayer + Resolving   │
                                          │ DataSource              │
                                          │                         │
                                          │ 1. Local cached file?   │ ──▶ play from disk
                                          │ 2. NewPipe resolve      │ ──▶ deciphered URL
                                          └────────────────────────┘
                                                       │
                                                       ▼
                                              ExoPlayer streams bytes
```

The Room `SongEntity.downloadPath` is queried via a small `DownloadLookup` interface in `:player` (implemented in `:app` to avoid a circular dependency), so the service can fall back to local files **before** hitting the network.

---

## Building

### Requirements

- **JDK 17**
- **Android SDK 35**
- An Android device or emulator running **API 26+** (Android 8.0 Oreo)

### Local development

```bash
# Clone
git clone https://github.com/SambuddhaRoy/Verza-Music.git
cd Verza-Music

# Configure local SDK location
echo "sdk.dir=/path/to/Android/Sdk" > local.properties

# Build a debug APK
./gradlew assembleDebug

# Install on a connected device
./gradlew installDebug
```

The first build pulls down NewPipeExtractor, Media3, Compose, Hilt, Room, and Ktor — expect ~5–10 minutes on a fresh machine.

### Signing in (optional)

Sign-in is only required to get your personalised home, your saved YT Music playlists, your server-side Liked Songs, and your followed artists. Anonymous use works fully for search, browse, playback, downloads, lyrics, and local features.

The in-app login uses a WebView aimed at Google's standard sign-in flow. The WebView's user-agent is rewritten to strip the embedded-browser markers (`; wv`, `Version/4.0`) so Google's "this browser may not be secure" check rarely fires. If it ever does, the login screen has a **Paste cookie** fallback — copy the `Cookie` header for `music.youtube.com` from a desktop browser while signed in and paste it directly.

---

## Disclaimer

Verza is an unofficial client. It uses public InnerTube endpoints and NewPipeExtractor — there is no premium-tier bypass. Use at your own risk; behaviour may break at any time if YouTube changes its API or stream-resolution mechanism.

This project is for educational and personal use. It is not affiliated with, sponsored by, or endorsed by Google, YouTube, or Apple.

---

## Acknowledgments

Verza stands on the shoulders of:

- [**NewPipeExtractor**](https://github.com/TeamNewPipe/NewPipeExtractor) — YouTube stream extraction, signature deciphering, and the `n`-parameter rolling cipher.
- [**InnerTune · OuterTune · SimpMusic**](https://github.com/z-huang/InnerTune) — Kotlin YouTube Music clients that pioneered the InnerTube-on-Android approach Verza follows.
- [**LRCLIB**](https://lrclib.net) — free, no-auth synced-lyrics provider.
- [**iTunes Search API**](https://developer.apple.com/library/archive/documentation/AudioVideo/Conceptual/iTuneSearchAPI/) — the source of real album art when YouTube serves a music-video frame.
- [**Material 3**](https://m3.material.io/) — design system and the typography / shape / colour primitives.

---

## License

This project is released under the **Apache License 2.0**. See [`LICENSE`](LICENSE) for the full text.

---

<div align="center">

### Designed and built by [**Sambuddha Roy**](https://github.com/SambuddhaRoy)

<sub>If Verza made your music a little nicer, leaving a ⭐ on the repo means a lot.</sub>

</div>
</content>
