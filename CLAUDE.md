# Verza — working notes

Unofficial YouTube Music **Android** client. Kotlin 2.0, Jetpack Compose, Material 3,
Media3/ExoPlayer, NewPipeExtractor. Modules: `:app`, `:innertube`, `:player`. Hilt, Room, DataStore.
minSdk 26, target/compile 35. A separate Electron desktop port lives at
`github.com/SambuddhaRoy/Verza-Desktop` (not this repo).

## Build / run
- JDK **17** required. On Linux: install a JDK 17 and `export JAVA_HOME=/path/to/jdk-17` (the Windows
  path in old commands won't exist).
- Debug: `./gradlew :app:assembleDebug`
- Release: `./gradlew :app:assembleRelease` — **needs `keystore.properties` at repo root** (gitignored,
  never committed; copy it over manually or the release build is unsigned and fails). APKs auto-name
  to `Verza-v<versionName>.apk` under `app/build/outputs/apk/<type>/`.
- Quick error check: `./gradlew :app:compileReleaseKotlin -q 2>&1 | grep -E "^e:|error:"`
- `gh` CLI is NOT assumed installed; use plain `git` + the GitHub web UI for releases.

## Release convention (every shipped change)
1. Bump `versionCode` (+1) and `versionName` in `app/build.gradle.kts`.
2. `assembleRelease`, confirm the APK.
3. Commit + push to `main`. Commit messages END with:
   `Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>`

## Security (do NOT relax)
- Author commits as **SambuddhaRoy <rsambuddha476@gmail.com>**.
- NEVER commit `keystore.properties`, `*.jks`, `*.keystore`, `local.properties`.
- NEVER log or leak the YouTube Music auth cookie. Library backups (export/import) AND shared
  session links must EXCLUDE the cookie.

## Current state (latest = v1.14.0 / versionCode 57)
- Branch `main` is the live app. `UI-Redesign` ("Verso" living-thread redesign) is parked on GitHub,
  NOT merged.
- Last published GitHub *release* is v1.0.0; everything since (mixes, sound suite, OS media
  integration, halftone glow, cover-flow, EQ presets, share-to-Verza) is on `main` only.

## Branch `feat/gass-design` — square, colourful, poster Now Playing

Off `main`, not merged, not released. Keeps main's cover-derived colours; the references are GASS
Records (flat slabs of colour) and Uncut's display type.

- **No rounded edges anywhere.** `VerzaShape`, the Expressive shape names, `PillShape`, `CloudShape`
  and `CookieShape` are all `RectangleShape` (names kept as aliases). Material's theme slots are a
  zero-radius `RoundedCornerShape` because they are typed `CornerBasedShape`. Material hardwires
  `Button`/`TextButton`/`OutlinedButton` to a full circle whatever the theme says, so every call
  site passes `shape = RectangleShape`; the equalizer's `Switch` and `Slider`s have no shape
  parameter and are replaced by `SquareSwitch`/`SquareSlider` in `EqualizerScreen.kt`. The ten widget
  mask drawables are all the same square. **A new Material button without an explicit shape comes
  back round.**
- **Now Playing hides its controls** after 4s untouched while playing (`CONTROLS_HIDE_MS`); paused
  they stay. Any touch brings them back (an Initial-pass pointer watcher on the player, never
  consuming). Never hides with touch exploration on; otherwise honours the system's "time to take
  action". The layout has exactly two arrangements and `animateBoundsIn` springs between them;
  **do not drive the layout with an animated value**, or animateBoundsIn chases a moving target
  every frame. Hidden controls are un-placed, not parked off-screen, because parked below the
  player they caught touches meant for the queue.
- **A different display face for every song** (`TitleFonts.kt`, `pickTitleFace`): twelve OFL faces
  from uncut.wtf in `res/font/display_*`, licence texts in `assets/font-licenses/`. Stable per song
  (hash of the track key), never the previous song's face, and a face missing any character of the
  title is skipped (`Paint.hasGlyph`), falling back to the app's type if none fit (Devanagari).
  `FitTitle` sizes it: 120sp down, max three lines, no word split, at most 22% of screen height.
  PicNic is not included: Uncut still lists it as OFL but it has moved to a licence with conditions.
  Five more OFL faces (Getai Grotesk Display, LC Mogi, Cakra, Queering, Slibinas) need a manual
  download from their foundries.

## Architecture pointers
- **Background glow** (app-wide, behind the NavHost in `MainActivity`): `ui/theme/Glow.kt`.
  `enum GlowStyle { FLUID, HALFTONE, COVER }` chosen via Settings → Background glow → Pattern.
  AGSL `RuntimeShader`, **API 33+ only** (gradient fallback below that). `chaos` = the "Movement"
  slider (0..1); `glowEnabled`, `glowColor`, `glowIntensity`, `glowReactive` prefs.
- **COVER style** = `ui/theme/CoverFlow.kt`: flowing, blurred, simplex-domain-warped wash of the
  current cover (ported from Verza-Desktop `renderer.js`). Pipeline must stay exact: 160px texture,
  cover drawn oversized (-16, 192px), ~9px Gaussian (3-pass box blur), then the AGSL warp shader;
  700ms linear crossfade on track change. Fed `artworkUrl` from MainActivity; falls back to FLUID
  when nothing's playing.
- **Sleeve mode** = the alternate editorial appearance (`ui/sleeve/`), opt-in; standard mode is default.
- **Media session / notification / lock-screen / AOD**: `:player/MusicService.kt`. Release builds need
  `-keep class androidx.media3.** { *; }` (in `app/proguard-rules.pro`) — R8 stripped it otherwise.
- **Curated mixes** (Daylist/Discover/Release Radar, on-device): `data/MixesRepository.kt`.
- **Equalizer + presets**: `audio/AudioEffectsController.kt`, `audio/EqPreset.kt`.
- **Prefs** flow through `data/PreferencesRepository.kt` → `SettingsViewModel` → screens; UI prefs that
  Now-Playing needs are threaded via `VerzaNavigation` (e.g. `albumArtMotion`, `sleeveMode`).

## Gotchas
- PowerShell here-strings mangle commit messages with quotes/em-dashes (Windows only) — write the
  message to a temp file and `git commit -F`. Not an issue on Linux.
- AGSL shaders only compile at runtime; bad shader → `runCatching` returns null and the glow falls
  back, so verify visuals on a real device/emulator (no JVM test for GPU code).
