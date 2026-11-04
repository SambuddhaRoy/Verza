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

## Streams come from NewPipeExtractor, and it is pinned to a commit

`newpipe` in `gradle/libs.versions.toml` is a commit on NewPipeExtractor's main (JitPack builds
any commit), not a tag. Releases up to 0.26.5 ask YouTube's Android and iOS clients for streams;
YouTube enforces SABR on those for some devices and networks, so extraction returns nothing and
every song toasts "Couldn't play this track" (MusicService's resolver, the only place that
string comes from). It came and went per device: the emulator still played on 0.26.1 while a real
phone failed on every song. Main uses the visionOS client alone and runs no JavaScript for it.
When playback breaks for users, check NewPipeExtractor's recent merges before anything else, and
move back to a tag once one carries the fix.

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
Expressive, the whole palette from the cover). **Glass** is the Zune with a little glass on it (below). Stored
as `design_scheme`. **Absent means Material once onboarding is done, Glass before it**
(`DesignScheme.fromName(name, onboarded)`): an update never restyles anyone, and a new install
starts in Glass. `setOnboardingCompleted` writes the design down in the same edit, or finishing
onboarding would flip a Glass newcomer to the onboarded default, Material. Unknown names mean
Material (tested, `DesignSchemeTest`). There was a third, **Poster** (square slabs, GASS Records), removed in 1.19.0; anyone on it
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
- **Expressive type is opt-in** (Settings > Appearance, `expressive_type`, default off, provided as
  `LocalExpressiveType`). Off is Inter throughout: Material's heads in the display cut instead of
  Instrument Serif, `variedFace` a no-op, and Now Playing's title in Inter in both designs. On
  brings back everything below.
- **A different display face for every song** (`TitleFonts.kt`, `pickTitleFace`): OFL faces
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

After the Microsoft Zune: true black or white with the system's dark mode, content before chrome,
one accent from the cover. Very large light type does the work a card's edge would, lowercase where
the app speaks for itself (`head()`) and never where it shows you a name; navigation is words; lists
are bare rows (`GlassListRow`). The Colour settings are hidden in it: the flavours have nothing to
push there. The Expressive type and Spectrum settings are hidden too, and capture must not run for a
spectrum Glass doesn't draw (`spectrumShown` in MainActivity).

- **Colour, with restraint** (`glassColors(cover, dark)`): the accent is the cover's most vivid
  swatch, searched through value and then saturation until it clears 4.5:1 on the canvas; its ink is
  `readableOn`. The canvas and surfaces carry only a breath of the cover's hue. A colourless (or
  missing) cover gets the Zune's magenta, because a grey accent reads as a disabled one. Many covers in a
  library are black and white, so magenta is common, not a bug. `ExpressiveColorsTest` sweeps the hue
  wheel through both modes.
- **Type:** Inter has an optical-size axis (`opsz` 14 to 32). `FontDisplayCut` is the 32 end, used by
  `GlassTypography`'s display, headline and large-title slots and by `HeroTitle`/`HeroDisplay`/
  `GlassSectionTitle` in Glass; text sizes stay on `FontBody` (14). That is what keeps a large title
  from looking like body text zoomed in.
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
- **The dock** (`GlassDock` in `GlassChrome.kt`): one pane of frosted glass, hardly bent, holding the
  player and the tabs, divided by a hairline that is also the song's progress. The tabs are lowercase
  words from the left, the current one lit. Scrolling a page down folds them away and leaves the
  player with the current tab's name at its end to bring them back (a NestedScrollConnection on the
  NavHost, `onPostScroll` so pulling at a list's top does nothing). The dock keeps its full height while
  folded so `LocalBottomChrome` never changes. Its type is capped at 1.3x, and it lays out in RTL.
- **Sideways between tabs** (`swipeTabs` in `VerzaNavigation.kt`), the Zune's panels: a drag across a
  page goes to the next tab. A shelf or the filter words consume the drag first, so they still scroll.
- **Mastheads** (`GlassMasthead`): the dateline small and quiet over the page's name, very large and
  light. Filters (`ExpressiveChip`) are words too.
- **Library** is its categories as a list of big words, the Zune's main menu; each opens as a page
  (BackHandler returns).
- **Albums**: the cover, the name, then `play` and `shuffle` as words.
- **A cover has a hairline on a light page** (`coverEdge`), or a white one is a gap in the list.
- **Motion:** pages push in from the edge with the old one sliding a third behind (`PushSpring`,
  critically damped), tabs cross-fade, Now Playing rises over the page and drops back (NavHost
  transitions with `KeepUntilTransitionsFinished`); drag down to close.
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

  On a phone upright the cover fills the screen over that wash (`Poster`), fading at the top into it
  and at the foot to near black (`PosterGrade`). The black under the type is measured per cover
  (`typeScrim`, `coverBrightness`): a dark cover keeps its colour, white type still reads on a white
  one (`verza-shots/mob/_contrast.py`). From the top: the line that says it pulls down (a tap closes it too), the title in Inter's
  display cut, with no per-song face, then every artist and the album, each going to its page; under
  that `LosslessBadge`, only on a song that is (`audioFormat` is null otherwise), and a tap on it shows
  the format. Liner notes are in More. At the foot six glyphs (Lyrics, like, add, radio, download, more), a thin scrubber
  and a bare transport. No spectrum, no volume slider, and the output is in More. Landscape and tablets
  have the square cover on the plain wash. Removing a download asks first (`NowPlayingScreen`, for both
  designs). Lyrics sits on the same wash (`OnCoverWash`), and the NavHost leaves its top unpadded on that
  route.
  - **Settings > Appearance > Now Playing, Poster or Square** (`fullBleed`, phones upright only, since
    landscape and tablets are already a square cover on the wash). Square is the first Glass layout in
    this design's type: cover, title and names with like and more beside them, the times under the line
    with the mark between them, transport, four glyphs. The times' row is a fixed height so the cover
    doesn't change size between a lossless song and the next.
  - **The queue is below the player, on one list** (`queueRows`), with its heading just showing at the
    foot (`QUEUE_PEEK`). The player is the list's first item, a window high less the peek, so the page
    scrolls as one thing with nothing nested. Up next comes first and what has played after it, each
    numbered by its place in the queue. Back from the queue returns to the player, and the picture
    darkens as the list comes up (read in a `graphicsLayer`, so a scroll is a redraw). On a tablet the
    queue stays beside the player.
  - **Pull to close is a NestedScrollConnection** (`PullToClose`): it only takes the drag the list had no
    use for, so it can't start until the player is all in view. A column that doesn't scroll gets the
    same drag by being in a `verticalScroll` of exactly its own height (tablets).
  - **Where the links come from** (`InnerTube.trackLinks`): the byline runs of the song's line in
    `next`. A video has no album, so it searches songs and takes the same title by one of the same
    artists (`parseSongMatch`); anything looser hands a tribute its album. A video's uploader is a
    `USER_CHANNEL`, and `parseArtistPage` opens those too.
- Empty states are `GlassEmpty`: a large quiet symbol over one line saying what to do.

## Both designs

- **Standby** (`AmbientDisplay.kt`, Now Playing > More): three faces in a pager, the last one remembered.
  The clock's digits are cut out of the cover: `saveLayer` with a `DstIn` paint, then the text. A `DstIn`
  on the text itself only reaches the pixels under its glyphs, so the whole picture stays. Dark covers
  are lifted with white (`lift`) so the digits read on black.
  - It asks for `SCREEN_ORIENTATION_FULL_SENSOR`, which rotates even with rotation locked, and puts the old
    request back on the way out. To test a lock, `wm user-rotation lock 0`: `settings put system
    accelerometer_rotation 0` didn't take on the emulator, which reads as a lock while being none.
  - Hours on a charger: the clock face is true black, nothing animates by itself, the pager shifts a few dp
    each minute, it dims 22:00 to 07:00, and the nav graph under it is at alpha 0 so that Now Playing's
    wash isn't drawn. The position reaches only the two things using it, through a lambda.
  - A `wm size` change mid-run leaves the wash a stale rectangle. A real rotation (`adb emu rotate`) doesn't.
    The controls last 4s and a UI dump can take longer, so check them from a dump right after the tap.
- **Haptics are felt from above, not asked for** (`tapHaptics` in `GlassChrome.kt`, on the root Row of
  `VerzaNavigation` and each sheet and dialog through `TAP_HAPTICS`, those being windows of their own).
  It watches in the Main pass, which runs child to parent, so by then a clickable has consumed the lift;
  empty page and scrolling don't consume one. So nothing has to remember, and a new button is covered.
  `Haptics` keeps 60ms of memory so a button's own firmer haptic and this one are one feeling. Kinds:
  `tick` every tap, `click` play and pause and removing from the queue, `detent` notches under a drag,
  `threshold` a gesture passing the point where letting go acts, and long presses.
  - **The constants matter.** CLOCK_TICK and CONTEXT_CLICK are no-ops on plenty of phones, and
    TEXT_HANDLE_MOVE is dropped wherever the text handle's haptic is configured off, which is the
    default. Use KEYBOARD_TAP, VIRTUAL_KEY, and SEGMENT_TICK on 34+.
  - **Check it from the system's log, the emulator included:** `adb shell dumpsys vibrator_manager` lists
    each one as `com.verza ... performHapticFeedback(constant=N)`. `verza-shots/mob/_audit2.py haptics`
    counts them per interaction.
- **One corner radius, everywhere** (`VerzaCorner`, 12dp, `CornerShape` in `ui/theme/Shapes.kt`): covers,
  cards, buttons, chips, fields, sheets, dialogs, the dock, widgets. Nothing morphs on press. Something
  smaller than twice it comes out as a pill or a circle by itself, so bars and dots need no exception.
  Write a corner any other way and `CornerConsistencyTest` fails. A launcher's widget corner is ignored
  on purpose: the widgets match the app, not the launcher.
- **Every page paints the app's background itself** (`page()` in `VerzaNavigation.kt`, reading
  `LocalCanvas`, the root's animated canvas). Pages used to be see-through over one root fill, so
  during a push the page underneath (dimmed to 60%, a third of the way out) showed through the new
  one and vanished when the transition ended. Register destinations with `page(`, not
  `composable(`. Library's category pages and onboarding's steps push the same way.
- **Onboarding** (`OnboardingScreen.kt`): welcome on the flowing wash (`CoverWash` fed
  `R.drawable.onboarding_wash`), then the look (Glass preselected and recommended; picking restyles
  live), colour (Material only), sign in, done. Steps are `rememberSaveable`: signing in leaves the
  page. Test it on the `VerzaFresh` AVD (a clean Pixel 7), not by wiping VerzaPhone's library.
- **The tour** (`FeatureTourScreen.kt`) names real paths in Settings. It once sent people to a
  "Background glow" group and a "Sleeve" switch long gone; move a path there when a setting moves.
  The dock is hidden on it (it covered the tour's button).
- **Each reader of the visualizer checks its own switch** (`LocalSpectrumOn`, `LocalWashBeatOn` in
  `Glow.kt`). Glass ignores the spectrum switch. The visualizer runs when the spectrum, "Feel the beat" or Glass's "Move to the beat"
  wants it, and the seek bar used to draw the spectrum whenever it ran.
- **Glass with no cover is the Zune's magenta** on a neutral ground: MainActivity passes `null`, not
  `DefaultCoverColors`, whose amber made a fresh install's Glass beige and brown.

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
  **The session shows the cover Now Playing shows.** The app looks the album's own art up by title
  and artist (`ArtworkRepository`, iTunes) and publishes it through `NowPlayingBridge.publishCover`;
  the service's `applyCover` swaps it onto the playing item (metadata only, like `embedArtwork`).
  Before, the notification, lock screen and widget kept the YouTube thumbnail the song was queued
  with, sometimes a 120px one.
- **Updates download before they are offered** (`SettingsViewModel.prefetchUpdate`): at launch a
  newer release is fetched quietly (not under Data Saver), and only then offered, so Update opens
  Android's installer at once (0.6s, was ~11s of progress bar). `UpdateRepository.download` reuses a
  complete APK already in the cache. Test it with a build labelled lower than the latest release.
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
