<div align="center">

# Verza

### An editorial YouTube Music client for Android

*Stream the full YouTube Music catalogue — no ads, real album art, offline downloads, synced lyrics, a living sound-reactive background, and a typographic design language — built from scratch in Kotlin + Compose.*

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
Full YouTube Music catalogue · No ads · Offline downloads · Song radio · Background playback · Lock-screen controls · Sleep timer

</td>
<td width="33%" valign="top">

### Living background
A flowing, GPU-shaded **glow** that drifts behind the app, takes on each song's **cover colours**, and can **react to the music**

</td>
<td width="33%" valign="top">

### Identity
**Material You** by default · nine curated palettes incl. the **Atelier** editorial pair · Cormorant Garamond display type

</td>
</tr>
<tr>
<td valign="top">

### Insights
**Your Sound** — top tracks & artists by real listened time, totals, and a day streak, from a local play-event log

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
<sub><b>Home</b> — personal "For You" feed, mixed section sizes, Material You accent, soft glow behind</sub>
</td>
<td align="center">
<img src="docs/now-playing.jpg" alt="Now Playing" width="280"/>
<br/>
<sub><b>Now Playing</b> — real album art with the glow picking up the cover's colour</sub>
</td>
</tr>
</table>
</div>

---

## Install

<div align="center">

[![Latest Release](https://img.shields.io/github/v/release/SambuddhaRoy/Verza?style=for-the-badge&label=Download%20APK&color=7F52FF)](https://github.com/SambuddhaRoy/Verza/releases/latest)

</div>

1. Download the **latest `Verza-vX.Y.Z.apk`** from the [Releases](https://github.com/SambuddhaRoy/Verza/releases) page on your Android phone.
2. Open the downloaded file. Android will ask whether your browser is allowed to install apps — tap **Settings → Allow from this source**, then back out.
3. Tap the APK again. Android will offer to install — tap **Install**.
4. **A "Google Play Protect" warning will appear.** This is normal for any app not downloaded from the Play Store — keep reading.
5. Tap **Install anyway** (sometimes shown as **More details → Install anyway** depending on your Android version).
6. Done. Launch Verza from your app drawer — a short first-run setup lets you choose your theme and glow.

> **Minimum requirements:** Android 8.0 Oreo (API 26) or newer. ~10 MB of storage. The fluid shader background uses the GPU on Android 13+; older devices get a lighter gradient glow automatically.

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
- **Resilient stream resolver** — tries progressive HTTP audio, then DASH stream URLs, then a video-with-audio fallback, then the page-level DASH manifest, so playback survives YouTube's periodic format changes.
- **Account sign-in (optional)** unlocks your personalised home, your saved playlists, your followed artists, your server-side Liked Songs, and pushes likes back to your account.
- **Offline downloads** — cached to app-private storage; the resolver prefers local files when available, so playback works without network once a track is downloaded.
- **Song radio** — start an endless mix from any track.
- **Sleep timer** — 15 / 30 / 45 / 60 minutes or end-of-track, with a soft volume fade-out and a live countdown.
- **Skip silence**, **audio-quality** picker (Low / Medium / High), and **queue persistence** across cold starts.

### Living background glow
- A flowing, domain-warped **fluid field** rendered with a real **AGSL `RuntimeShader`** on Android 13+, with a multi-gradient fallback on older devices.
- **Album-art adaptive colours** — extracts a vibrant palette from the current cover (AndroidX Palette) and colours the glow with it.
- **Sound reactivity (optional)** — an FFT visualizer drives the glow's motion and brightness with the music's bass/mid/treble (requires the audio permission, asked only when enabled).
- Colour presets, three intensity stops, and a **de-monochrome** colour derivation that keeps even Material You's palette lively.

### Identity & motion
- **Material You (Dynamic)** is the default theme on Android 12+, colouring the whole app from your wallpaper; older devices fall back to **Atelier Dark**.
- Nine curated palettes: the new **Atelier** light/dark editorial pair plus **Bauhaus · Malibu · Concrete · Noir · Ember · Acid · Magenta**.
- **Cormorant Garamond** for display & headlines (bold, architectural), **Inter** for body/labels, **IBM Plex Mono** for numerals & timecodes — with hairline rules instead of heavy cards throughout.
- **First-run onboarding**, a cold-launch **boot animation**, and the **"Fold"** launcher icon (with an Android-13 themed-icon variant).
- Motion pass: directional page transitions, press-scale feedback, a spring-animated bottom nav, staggered home reveal, breathing album art, and a smoothly interpolated seek bar.

### Home page
- **Personal-first composition** — *Recently Played*, *Quick Picks*, *Your Daily Discover*, *Keep Listening*, *From Your Liked Songs*, *Your YouTube Playlists*, *Similar to <artist>* — with one consolidated *Browse charts and trending* row at the bottom.
- **Mixed section sizes** — large featured carousels, standard carousels, compact card rows, and a 4×2 dense grid so the page reads with rhythm.

### Search
- Filter tabs — **Songs · Albums · Artists · Playlists**.
- As-you-type autocomplete from YouTube Music's suggest endpoint.
- Recent-search history chips (toggleable + clearable in Settings).

### Library
- **Recently played** + **Liked** (Room-backed, fully offline).
- **Downloaded** tab for offline tracks, **Playlists** tab (local + saved YT playlists), and an **Artists** tab for followed channels.
- *"Add to playlist"* sheet on any track from anywhere via the row overflow menu.

### Now Playing
- Full-bleed artwork with a live glow behind it, editorial type, scrubbable progress, and the **Like · Radio · Lyrics · Queue** action row.
- Overflow ⋯ menu: Share, Copy link, Lyrics, Start radio, **Sleep timer**, Download / Remove download.
- Foreground media service with lock-screen / notification controls via Media3 `MediaLibrarySession`.

### Lyrics
- **Synced (LRC) lyrics** from [LRCLIB](https://lrclib.net) with line-by-line auto-scroll; plain-text fallback; cached per `(title, artist, duration)`.

### Your Sound (listening stats)
- An editorial insights page built from a local **play-event log**: total time listened, tracks played, a **day streak**, and your **top artists & tracks** ranked by *real* engaged listening time (paused gaps excluded).

### Settings
- **General** (start screen), **Playback** (resume-on-open, skip silence, album-art motion), **Audio quality**, **Theme**, **Background glow** (enable / colour / intensity / reactivity), **Search** (save & clear history), and **Data** (reset listening stats).

---

## Tech stack

| Layer | Tech |
|---|---|
| **Language** | Kotlin 2.0 |
| **UI** | Jetpack Compose · Material 3 · Coil 3 · AGSL `RuntimeShader` |
| **Playback** | Media3 / ExoPlayer · custom `ResolvingDataSource` |
| **Stream extraction** | [NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor) (with Mozilla Rhino for the signature cipher) |
| **Colour & audio FX** | AndroidX Palette (album colours) · `android.media.audiofx.Visualizer` (FFT reactivity) |
| **HTTP** | Ktor for InnerTube · OkHttp shared across the app |
| **DI** | Hilt |
| **Persistence** | Room (history / likes / downloads / local playlists / play events) · DataStore (preferences + queue) |
| **Serialization** | kotlinx.serialization |
| **Async** | Kotlin Coroutines + StateFlow |

---

## Architecture

Verza is a three-module Android project:

```
:app          Compose UI, ViewModels, Hilt graph, navigation, theming,
              glow shader, audio visualizer, listening stats
:innertube    InnerTube API client, parsers (search / home / artist / …),
              and the NewPipe-backed stream resolver
:player       Media3 MediaLibraryService + PlayerConnection
              (MediaController wrapper exposing PlaybackState)
```

Because `:player` can't depend on `:app`, two process-wide singletons bridge the gap: **`AudioSessionRegistry`** exposes the live ExoPlayer audio-session id (for the visualizer) and **`PlayerSettings`** carries playback options like skip-silence the other way.

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
                                          │ 2. NewPipe resolve      │ ──▶ progressive / DASH /
                                          │    (4-strategy)         │     video / manifest URL
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
git clone https://github.com/SambuddhaRoy/Verza.git
cd Verza

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

## Privacy

Verza has **no backend, no analytics, no tracking, and no ads** — nothing about your usage is ever sent to the developer. See [**`PRIVACY.md`**](PRIVACY.md) for the full policy. In short:

- **On-device only** — liked songs, playlists, history, stats, queue, and downloads stay on your phone. The optional sign-in cookie is **encrypted with a hardware-backed Android Keystore key** and excluded from backups.
- **Third-party requests are minimal and anonymous** — YouTube/Google for the catalogue (and your account cookie *only if you sign in*, *only* to Google); Apple iTunes Search receives a track's title/artist to fetch real cover art; LRCLIB receives title/artist/duration for lyrics. None of these carry a user identifier.
- **The microphone permission is optional and never records you.** It's requested only if you enable the *Sound reactivity* glow, and is used solely to read a frequency snapshot of the music Verza is already playing (via Android's `Visualizer` API) to animate the background. No audio is captured, stored, or transmitted. Android labels this capability "Microphone" because the API is gated by `RECORD_AUDIO`, even though no mic input is used.

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
