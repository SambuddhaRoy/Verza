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
3. Commit + push to `main`.
   No `Co-Authored-By: Claude` trailer or any other AI attribution, anywhere: the owner had
   every one stripped from history in October 2026. Commit as SambuddhaRoy only.

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

## Design setting: Material or Glass (Settings > Design)

Two looks over the same app, switchable live. **Material** is the app as it was (rounded, M3
Expressive, the whole palette from the cover). **Glass** is the quiet one with floating glass (below). Stored
as `design_scheme`; **absent or unknown means Material**, so an update never restyles anyone
(tested). There was a third, **Poster** (square slabs, GASS Records), removed in 1.19.0; anyone on it
lands on Material, since "POSTER" is now an unknown name.

- **One switch, read in composition.** `DesignScheme` and `LocalDesign` live in `ui/theme/Design.kt`,
  provided by `VerzaTheme(design = ...)` from MainActivity. Shape tokens (`VerzaShape`, the `Shape*`
  scale, `CloudShape`, `CookieShape`) are `@Composable` getters through `designShape(material,
  glass)`. **A shape must be read in composition and passed in**: one picked inside a draw or layout
  lambda cannot see the setting change. `HeroDisplay`/`HeroTitle` are getters for the same reason.
- **Material's Now Playing hides its controls** after 4s untouched while playing (`CONTROLS_HIDE_MS`);
  paused they stay. Any touch brings them back (an Initial-pass pointer watcher, never consuming).
  Never hides with touch exploration on; otherwise honours the system's "time to take action". The
  layout has exactly two arrangements and `animateBoundsIn` springs between them; **do not drive the
  layout with an animated value**, or animateBoundsIn chases a moving target every frame. Hidden
  controls are un-placed, not parked off-screen: parked below the player they caught touches meant for
  the queue.
- **A different display face for every song in Material** (`TitleFonts.kt`, `pickTitleFace`): OFL faces
  in `res/font/display_*`, licence texts in `assets/font-licenses/`. Stable per song (hash of the
  track key), never the previous song's face, and a face that cannot set every character of the title
  is skipped. "Can set" means a glyph **and ink**: Solide Mirage and Sunday map `"` to an empty glyph,
  which `hasGlyph` reports as present. If no face fits (Devanagari) the app's type is used. `FitTitle`
  sizes it: 120sp down, max three lines, no word split, at most 22% of screen height. PicNic is out:
  it has moved to a licence with conditions. **The mobile repo is public, so only OFL-type licences
  can go in it.** `TitleFonts.legible` is the subset that reads at list size.
- **List rows** use `RowTitle`/`RowSubtitle` rather than their own `Text`: a face of its own per title
  in Material (`variedFace`), one face at reading size in Glass.
- **Release notes render as text** (`releaseNotesText`): handles `##`, `- `, `**bold**` and
  `[links](url)`, which is all the notes use.

**The emulator gets killed with the turn when started as a background task**; start it with
PowerShell `Start-Process` instead.

## Design setting: Glass

Black or white with the system's dark mode, one accent from the cover, Inter's display cut for
headings, continuous corners, glass chrome, and plain rows with hairlines instead of tiles
(`GlassListRow`). The Colour settings are hidden in it: the flavours have nothing to push there.

- **Colour, with restraint** (`glassColors(cover, dark)`): the accent is the cover's most vivid
  swatch, searched through value and then saturation until it clears 4.5:1 on the canvas; its ink is
  `readableOn`. The canvas and surfaces carry only a breath of the cover's hue. A colourless (or
  missing) cover gets Verza's red, because a grey accent reads as a disabled one. Many covers in a
  library are black and white, so red is common, not a bug. `ExpressiveColorsTest` sweeps the hue
  wheel through both modes.
- **Type:** Inter has an optical-size axis (`opsz` 14 to 32). `FontDisplayCut` is the 32 end, used by
  `GlassTypography`'s display, headline and large-title slots and by `HeroTitle`/`HeroDisplay`/
  `GlassSectionTitle` in Glass; text sizes stay on `FontBody` (14). That is what keeps a large title
  from looking like body text zoomed in.
- **Continuous corners** (`SquircleShape`, Figma's corner smoothing at iOS's 0.6) for artwork, cards,
  buttons and the Now Playing cover; 6dp thumbnails stay plain (too small to tell). It is a path
  outline, so it clips by path; fine for these, avoid it for hundreds of tiny thumbnails. The Material
  theme's `Shapes` slots need `CornerBasedShape`, so `GlassShapes` stays plain rounded.
- **Real glass, recorded rather than read back** (`ui/theme/LiquidGlass.kt`). Compose has no backdrop
  filter. `glassSource(backdrop)` records a node into a `GraphicsLayer`; `liquidGlass(shape, ...)`
  redraws its own patch of that recording through a RenderEffect: blur + saturation (API 31+), then
  an AGSL lens that samples outward near the rim (API 33+), plus a lit rim. Below 31 it is a plain
  fill. The recording references the live display list, so scrolling updates the glass for free.
  The lens's colour fringe divides by each sample's own alpha: past the end of the recording (the
  foot of the screen) a sample is transparent, and taking its red channel raw left a cyan edge.
  **Bend, then blur, with a gentle profile** (offset grows with t², strength 0.45 x the rim).
  Blurring first with a circular bevel (vertical slope at the edge) drew a sharp mirrored copy of
  the outline just inside the rim, which read as the effect stopping short of the edge.
- **A glass pane must never be inside the source it shows** (it would draw a recording of itself).
  The NavHost is the source for the bottom chrome only (`LocalGlassBackdrop` is provided to the
  bottom bar, not the pages). Now Playing records its own cover wash for its buttons; Collection and
  Artist record their page for the floating header buttons. Anywhere else glass falls back to a fill.
- **Its own look, not Apple Music's.** The first version copied Apple's signatures one for one
  (the iOS tab capsule with a separate search circle, large titles, a red chevron list for Library,
  the cover shrinking on pause, the volume slider, "Playing Next"), and read as a rip-off. What
  replaced each is Verza's own; keep it that way rather than drifting back to the reference:
- **The dock** (`GlassDock` in `GlassChrome.kt`): one pane of glass holding the player and the tabs
  (Home, Search, Library), divided by a hairline that is also the song's progress. The selected tab
  sits in a soft pool of the accent's light that slides between tabs. Scrolling a page down folds
  the tabs away and leaves the player, with the current tab's icon at its end to bring them back
  (a NestedScrollConnection on the NavHost, `onPostScroll` so pulling at a list's top does
  nothing). The dock keeps its full height while folded so `LocalBottomChrome` never changes. Its
  margin is concentric with the display's corner (`Display.getRoundedCorner`, 12+), its type is
  capped at 1.3x, and it lays out in RTL.
- **Mastheads** (`GlassMasthead`): the Material masthead's dateline in small capitals in the accent,
  over the page's name ("For you", "Library", "Search").
- **Library** is tiles, each lit from a corner by the accent (the dock's light again); each opens
  as a page (BackHandler returns).
- **Albums**: the cover full width with the name on a plate of glass across its foot (the cover is
  its own glass source), then Play as the one accent-filled control, beside Shuffle.
- **Motion:** pages push in from the edge with the old one sliding a third behind (`PushSpring`,
  critically damped), tabs cross-fade, Now Playing rises over the page and drops back (NavHost
  transitions with `KeepUntilTransitionsFinished`); drag down to close.
- **Haptics** (`Haptics`, `rememberHaptics`), sparing on purpose: a tick for tab changes, like and
  the queue toggles; a click for play/pause; a bump when a drag crosses the point where letting go
  acts (closing Now Playing, swiping to the next song).
- **Now Playing** (`NowPlayingGlass.kt`) stands on the desktop's flowing cover (`FluidWash.kt`, a
  port of `COVER_FRAG` in the desktop's renderer.js): the cover blurred once into a 256px texture
  (sigma ~8% of it, matching the old 72dp wash), then domain-warped by simplex noise every frame,
  at ~30fps, drawn at half the screen's size (an offscreen layer scaled 2x). It is the glass
  panes' source too. It moves with the music: the bass swells the warp, and each kick brightens and
  zooms it for a fifth of a second (`BeatPulse`). Below 13 (no AGSL) it is `DriftWash`, two copies
  of the blurred cover turning against each other. With album-art motion off, one still frame.
  - **A beat needs linear power, not the bands.** `VisualizerSignal.bass` is log-scaled and
    smoothed until it sits between 0.74 and 0.81 through a whole song with a steady kick; the
    bands are log-scaled too, and the lowest ones are one or two FFT bins, noisier than the kick.
    `VisualizerSignal.kick` is the raw power under 150Hz (0.001 between kicks, 0.1 to 0.4 on one),
    and a beat is that passing twice its one-second average, once per burst. `FluidWashTest`.
  - The flowing wash measured smoother than the drift (61fps and no janky frames on the emulator,
    against 35fps and two thirds janky): the drift re-blurred two full-screen layers every frame.

  The title is set in the
  song's own display face (`TitleFonts`, sized by the shared `FitTitle`), the position is the
  spectrum (`VisualizerSeekBar`), the transport is one capsule of glass, and Lyrics, the output (by
  name) and "Up next" are labelled pills. Paused, the cover dims. No volume slider: the keys do it.
  The queue opens in place of the cover. Lyrics sits on the same wash (`OnCoverWash`), and the
  NavHost leaves its top unpadded on that route.
- Empty states are `GlassEmpty`: a large quiet symbol over one line saying what to do.

## Both designs

- **Mix covers** (`MixCover` in `MixScreen.kt`): a duotone of the mix's own two colours (a 2x2
  mosaic of four of its covers when it has four, else one) with the name on a flat band of the
  mix's colour. Replaced a single cover under a gradient scrim with a small kind label on it. The
  same cover is the mix page's banner, wide.
- **Tapping the tab you are on** bumps `LocalTabReselect`; pages react with `onTabReselected`
  (which ignores the starting value): Home scrolls to the top, Library leaves a category for its
  index, Search focuses its field. **Search hides the keyboard on submit.** On the emulator, adb's
  Enter goes through a hardware keyboard whose floating toolbar stays up; tap the IME's own search
  key to test it.
- **A card's long press goes through its own `combinedClickable`** (`ExpressiveCard(onLongPress)`).
  A second clickable wrapped outside it never saw the long press: the inner one took the press and
  played the song on release.
- **Flow test**: `verza-shots/mob/r11_flow.py` walks every main interaction in Glass and checks each
  by its effect (media session state, settings, what is on screen). 33 checks. Which keyboard the
  emulator shows (the full one or the floating toolbar) changes between boots; `ime_search` looks
  for the search key in both, and the first mix's name follows the time of day.
- **Pages on the cover overlap**: Now Playing is kept until Lyrics has slid in over it, so both set
  the status-bar icons white at once. `LightStatusIcons` counts them and the last to leave restores
  the icons; restoring per page put dark icons over Lyrics.

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
- **ListenBrainz mixes** (Weekly Exploration, Weekly Jams, Last month on repeat): `data/ListenBrainz.kt`,
  a port of the desktop's `src/main/listenbrainz.js`. It is how Spotify listening reaches Verza: Spotify's
  own API gives new apps no recommendations and forbids pairing its data with other audio. Kept in their
  own flow and DataStore key inside `MixesRepository` (`_lb`), so connect/disconnect never waits on or is
  undone by the on-device mixes regenerating. Songs are matched by a YouTube Music search, cached for
  good in `filesDir/listenbrainz-cache.json` (misses too; a failed search is not cached). Settings >
  Listening elsewhere. `ListenBrainzTest` covers which weekly playlist is picked.
  **Never gate on `/listen-count`**: it stays at 0 for a freshly linked Spotify, so a real account was
  refused; `check()` reads `/listens`. `range=month` is the previous calendar month and answers an
  empty 204 for a new account; `topPairs` falls back to `this_month` ("This month on repeat").
- **Equalizer + presets**: `audio/AudioEffectsController.kt`, `audio/EqPreset.kt`.
- **Prefs** flow through `data/PreferencesRepository.kt` → `SettingsViewModel` → screens; UI prefs that
  Now-Playing needs are threaded via `VerzaNavigation` (e.g. `albumArtMotion`, `sleeveMode`).

## Gotchas
- **Pages run on under the mini player and nav bar** (`LocalBottomChrome`, provided by
  `VerzaNavigation`). The NavHost is no longer padded at the bottom, so covers show through the
  gaps around the two bars. The price: **every scrolling list adds `LocalBottomChrome.current` to
  its bottom padding**, and anything centred on a page (spinners, empty and error states) pads its
  bottom by it too, or it sits half behind the bars. A new screen that forgets this hides its last row.
- **Corners: three radii and a pill, and nested ones concentric** (the rule is in `theme/Shapes.kt`).
  28dp sheets, dialogs, the Now Playing cover and the mini player; 20dp cards and card rows
  (`VerzaShape`, `ShapeLargeIncreased`); 12dp thumbnails in rows, text fields and press highlights
  (`ShapeMedium`); pills for buttons, chips and the nav bar. A thumbnail inside a row curves by the
  row's radius minus the inset. Before, older screens and sheets used 8/10/14/16/18/24dp, and
  side by side that was the "corners don't match" look. Use the tokens, not `RoundedCornerShape(n.dp)`.
- **Widgets: the card takes the launcher's corner** (`@dimen/widget_corner`, which is
  `system_app_widget_background_radius` on 12+: 28dp on Pixel). It was 36dp, a second curve inside
  every other widget's. The Poster widget and Strip have a second layout chosen by size on 12+
  (`bySize`): at the sizes a launcher actually picks (3x3 for the Poster widget, 3x2 for
  Strip on a Pixel 7) the cover pushed the controls out, and the strip's pill became an oval.
  Place them on the emulator with `input draganddrop` from the widget picker.
- **Offline, a failed song pauses instead of skipping** (`PlayerConnection.onPlayerError`, `isOnline`
  in Network.kt). Skipping walked the whole queue away while offline, and whichever song loaded when
  the network came back started by itself. Online, failures still skip (three in a row stop).
- **Text on fixed-size cards is capped at 1.2x the system font** (`MixCard`). At 2x, words longer than
  the card split mid-word ("Evenin/g"). Scale the text, not the card, only up to that point.
- `dumpsys media_session` lists other apps' sessions too: a `description=null` there was the Google
  Assistant's session, not Verza's. Read the block under `package=com.verza`.
- **Discovery radio never wipes its memory** (`discoveryServed`, oldest first, cap 5000). Clearing it
  whenever a seed ran dry brought the same forty songs straight back. `MusicRepository.discoveryRadio`
  digs into the seed artists' catalogue (search) before recycling the oldest-offered songs, and
  `DiscoveryRadio.rank` puts songs found in 4+ radios (famous ones) last in each tier.
- **The Liked tab is local likes plus the account's**, deduped. It used to show the account's list
  alone when signed in, so a like made here only appeared after a refetch.
- **Long titles use `TitleFonts.legible` only** (`poolFor`, over 22 characters). Outward fuses its
  letters, and "Get Lucky (feat. Pharrell Williams and Nile Rodgers)" came out as a block of ink on
  Now Playing. Every caller of `pickTitleFace` (Now Playing, widgets) picks from the pool.
- **A hand-drawn control needs its semantics spelled out.** The Settings switches were plain
  clickables, so TalkBack (and `uiautomator`) saw no on/off state. `RowSurface(switchState = …)`
  sets `Role.Switch` and `toggleableState`.
- **Testing on the emulator**: `verza-shots/mob/m.py` drives it through `uiautomator dump` (decode
  its output as UTF-8). Now Playing's controls fade ~4s after the last touch, so tap the screen and
  act inside that window. The Library chip row scrolls: Downloaded, Playlists and Artists start
  off-screen. In landscape the nav is a side rail, not a bottom bar.
- PowerShell here-strings mangle commit messages with quotes/em-dashes (Windows only) — write the
  message to a temp file and `git commit -F`. Not an issue on Linux.
- AGSL shaders only compile at runtime; bad shader → `runCatching` returns null and the glow falls
  back, so verify visuals on a real device/emulator (no JVM test for GPU code).
