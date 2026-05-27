# LSTN

A hyper-minimal, elegant **YouTube Music** client for Android — built from scratch with Jetpack Compose, Media3/ExoPlayer, and NewPipeExtractor.

LSTN streams the full YouTube Music catalogue without ads, plays your real album art (not random video frames), supports offline downloads, lyrics, song radio, and seven themed palettes — including Material You dynamic colour on Android 12+.

---

## Screenshots

<!-- Drop screenshots into a `docs/` folder and reference them here. -->
<!-- e.g. ![Home](docs/home.png) ![Now Playing](docs/now_playing.png) -->

---

## Features

### Music
- **Full YouTube Music catalogue** via NewPipeExtractor — handles signature deciphering and the `n`-parameter rolling cipher so streams play on a clean install with no auth required.
- **Account sign-in** (optional) for personalised home, your saved playlists, followed artists, server-side liked songs, and like-push back to your account.
- **Offline downloads** — cached to app-private storage; the resolver prefers local files when available, so playback works without network once a track is downloaded.
- **Song radio** — start an endless mix from any track (uses YouTube's `RDAMVM<videoId>` watch playlist).
- **Synced lyrics** via the free [LRCLIB](https://lrclib.net) API — line-by-line auto-scroll for tracks with synced timestamps; falls back to plain lyrics.
- **Real album art** — songs in the home feed, queue, library, search, and Now Playing pull the actual cover from the iTunes Search API rather than YouTube's video-frame thumbnails.

### Home page
- **Personal-first composition** — Recently Played, Quick Picks, Your Daily Discover, Keep Listening, From Your Liked Songs, Your YouTube Playlists, Similar to *artist* sections; one consolidated *Browse charts and trending* row at the bottom for everything generic.
- **Spotify-style mixed sizes** — large featured carousels, standard carousels, compact card rows, and a 4×2 non-scrolling dense grid mix so the page reads with rhythm and fits more content per screen.

### Search
- Filter tabs (Songs / Albums / Artists / Playlists), as-you-type autocomplete suggestions, recent-search history chips.

### Library
- Recently played + locally liked (Room-backed) when offline.
- Server-side Liked Songs + your YouTube playlists when signed in.
- Followed artists tab.
- Local downloads tab.
- User-created playlists with create/rename/delete, "Add to playlist" sheet on any track.

### Now Playing
- Big artwork with soft shadow, shuffle/prev/play/next/repeat, scrubbable progress bar, like, song radio, lyrics, an in-place "Up next" queue toggle.
- Overflow menu: Share, Copy link, Lyrics, Start radio, Download / Remove download.
- Queue rows are tappable to jump and removable.
- Foreground media service with lock-screen / notification controls via Media3 `MediaLibrarySession`.

### Player
- Queue persistence across cold starts — restores paused at saved position so a process kill doesn't lose your session.
- Audio quality preference (Low / Medium / High) — preferred bitrate honoured by the resolver.
- Like/unlike pushes to your YouTube account when signed in.

### Theming
- Seven hand-tuned themes: **Bauhaus**, **Malibu**, **Concrete**, **Noir**, **Ember**, **Acid**, **Magenta**.
- **Material You** (Dynamic) theme on Android 12+ that derives a full M3 colour scheme from your wallpaper and follows system light/dark.
- Custom `LstnExtendedColors` palette (`muted`, `glass`, `glassHeavy`, `borderGlass`, etc.) for surfaces M3 doesn't cover.
- Soft-rounded **Muse** design language: Playfair Display serif for headlines, Inter for body, IBM Plex Mono reserved for timecodes.

---

## Tech stack

- **Language:** Kotlin 2.0
- **UI:** Jetpack Compose + Material 3
- **Playback:** Media3 / ExoPlayer with a custom `ResolvingDataSource` that swaps the `innertube://<videoId>` placeholder URI for a real stream URL just before ExoPlayer opens the connection
- **Stream extraction:** [NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor) (bundled Mozilla Rhino for signature deciphering)
- **HTTP:** Ktor for InnerTube requests, OkHttp shared across the app
- **DI:** Hilt
- **Storage:** Room for play history, likes, downloads, and local playlists; DataStore for preferences and queue persistence
- **Image loading:** Coil 3 (OkHttp network fetcher)
- **Serialization:** kotlinx.serialization
- **Async:** Kotlin Coroutines + StateFlow

---

## Architecture

LSTN is a three-module Android project:

```
:app          Compose UI, ViewModels, Hilt graph, navigation
:innertube    InnerTube API client, parsers (search/home/artist/etc.),
              and the NewPipe-backed stream resolver
:player       Media3 MediaLibraryService + PlayerConnection
              (MediaController wrapper exposing PlaybackState)
```

Key flow for playback:
1. UI calls `PlaybackViewModel.playSongs(...)` with a list of `MusicItem`s.
2. `PlayerConnection.setQueue(...)` builds `MediaItem`s with URIs of the form `innertube://<videoId>` and hands them to `MediaController`.
3. The session's `onAddMediaItems` callback rebuilds the URIs after Media3's controller→session IPC strips them.
4. When ExoPlayer opens an item, a `ResolvingDataSource` intercepts the placeholder URI:
   - first checks Room for a local downloaded file → plays from disk;
   - otherwise calls `InnerTube.resolveAudioStream(videoId)` → NewPipeExtractor → returns a deciphered googlevideo URL → ExoPlayer streams the bytes.

---

## Building

### Requirements

- **JDK 17**
- **Android SDK 35**
- An Android device or emulator running **API 26+** (Android 8.0 Oreo)

### Local development

```bash
# Clone
git clone https://github.com/<your-user>/lstn.git
cd lstn

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

LSTN is an unofficial client. It uses public InnerTube endpoints and NewPipeExtractor — there is no ad bypass for premium content. Use at your own risk; behaviour may break at any time if YouTube changes its API or stream-resolution mechanism.

This project is for educational and personal use. It is not affiliated with, sponsored by, or endorsed by Google or YouTube.

---

## Acknowledgments

LSTN stands on the shoulders of:

- [**NewPipeExtractor**](https://github.com/TeamNewPipe/NewPipeExtractor) — handles the YouTube stream extraction, signature deciphering, and `n`-parameter rolling cipher.
- [**InnerTune / OuterTune / SimpMusic**](https://github.com/z-huang/InnerTune) — Kotlin YouTube Music clients that pioneered much of the InnerTube-on-Android approach LSTN follows.
- [**LRCLIB**](https://lrclib.net) — free, no-auth synced lyrics provider.
- [**iTunes Search API**](https://developer.apple.com/library/archive/documentation/AudioVideo/Conceptual/iTuneSearchAPI/) — the source of real album art when YouTube serves a music-video frame.
- [**Material 3**](https://m3.material.io/) — design system and the typography / shape / colour primitives.

---

## License

This project is released under the **MIT License**. See [`LICENSE`](LICENSE) for the full text.

---

## Credits

Designed and built by **Sambuddha Roy**.
</content>
