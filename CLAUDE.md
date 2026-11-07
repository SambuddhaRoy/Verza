# Verza (Android), the rewrite

The phone app rebuilt from scratch on branch `rewrite` of `SambuddhaRoy/Verza`, beside the old app on
`main`, which keeps shipping 1.20.x until this one matches it. Glass is the only design. Same package
(`com.verza`) and signing key, so it installs over the old app as an update and reads its library.

## Build

- JDK 17: `JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew :app:testReleaseUnitTest :app:assembleRelease`
- Release needs `keystore.properties` and `verza-release-key.jks` at the root, never committed.
- One module. One HTTP client (OkHttp, shared by the player, Coil and every lookup). No DI framework:
  `Graph` builds every long-lived object once, and screens get what they need from it.

## Rules the old app learned the hard way

- **NewPipeExtractor is pinned to a commit** (`libs.versions.toml`). Releases up to 0.26.5 ask YouTube's
  Android and iOS clients for streams, which YouTube refuses many devices, so nothing plays. When
  playback breaks for users, look at NewPipeExtractor's recent merges first.
- **Core library desugaring stays on.** NewPipe calls `URLDecoder.decode(String, Charset)`, API 33; below
  Android 13 every lookup threw and nothing played, for six releases. Test on the `VerzaAndroid11` AVD.
- **R8 must keep Media3, NewPipe and Rhino whole** (`proguard-rules.pro`). Each failure was silent.
- Never log or back up the YouTube cookie. Never commit signing files. No AI attribution in commits,
  tags or release notes; commit as SambuddhaRoy.

## Data carried over

The old app's storage is read as it is, so an update loses nothing and needs no conversion step:
- Room database `verza.db`, schema version 5. `data/library/Tables.kt` declares exactly the old tables,
  field for field and in the same order, because Room checks a hash of the schema and refuses to open a
  database whose hash differs. A schema change from here on is version 6 with a migration.
- DataStore `verza_settings`, same keys. Keys only the old designs used are ignored.
- The cookie, encrypted with the Keystore key `verza_account_cookie_key` (`data/Account.kt`).
- Files in `filesDir` keep their names (`listenbrainz-cache.json`, `local-covers.json`).

## Layout

- `data/` everything that isn't drawing: YouTube, the library, settings, sources, music on the phone,
  covers, lyrics, downloads, mixes. Each file owns one thing and says so at the top.
- `playback/` the service (queue, resolving where a song plays from, the media session) and the UI's
  handle on it.
- `ui/` one package per page, plus `theme` and the shared pieces in `ui/kit`.

## Performance rules

- **Paint from disk, then refresh.** Every page shows its last saved copy at once and updates behind
  it. The old Home waited on six network calls each launch: 6.15s median to its first shelf.
- **A song's YouTube Music cover is the right one** (lh*/yt* on googleusercontent/ggpht), fetched at the
  size drawn. Only a video's frame (i.ytimg.com) is looked up elsewhere.
- **Look ahead.** The next three songs' lookups run while one plays; a skip finds them done.

## Baseline (old app 1.20.11, emulator, median of 5)

`verza-shots/mob/_baseline.py <apk> com.verza/.MainActivity`: cold start 424ms, Home's first shelf
6.15s, search to first result 2.92s (includes ~0.7s of the tool's own polling).

## Where it stands

Done and checked on the emulator (`verza-shots/mob/_carryover.py`, `_rw_play.py`, `_rw_more.py`):
the old library opens as it is; search; playback with the radio after a searched song; look-ahead
(a skip 1.9s); the queue kept across launches; album, playlist and artist pages from a saved copy;
Home from a saved copy; Now Playing (Poster); Library (liked, recent, playlists, downloaded, on
device); lossless sources added in Settings, playing lossless; likes sent to the account (`Sync`).

Still to port: lyrics, Standby, equaliser, visualiser and haptics; downloads; making playlists and
the song menu (add to playlist, play next, download); sign-in, onboarding, backups, ListenBrainz,
mixes, Your Sound, shared sessions; updates (through PackageInstaller sessions); widgets, the tile,
Android Auto; the flowing wash; Android 11; and the cold start, still about 90ms behind (below).

Measured (same script and emulator, quiet, median of 5): cold start old 464ms, new 552ms; Home's
first shelf old 4.47s, new 3.54s, but ~2.4s of each is uiautomator's own dump, and the new Home is
already drawn in the first dump; search old 2.84s, new 2.77s.

- **Http to the phone itself needs `network_security_config`**: without it Android refuses
  `http://localhost`, and adding a source served from the phone (or an adb-reversed one in tests)
  says it can't be reached.
- **A Media3 controller outlives its connection.** When the service is destroyed and made again, the
  old controller still takes commands, changes its own idea of the queue and plays nothing. `Player`
  replaces a controller that isn't `isConnected`.
- **Don't connect the player before the first frame**: it starts the service on the main thread and
  held the first frame back. `MainActivity` connects after one.

## Porting order

Each step builds, runs, and is checked on the emulator against the old app before the next.

1. Shell: theme, dock, pages, reading the old library and settings (checked against a real old install).
2. Playback: service, resolver (download, cache, lossless sources, YouTube), look-ahead, notification.
3. Search, album and artist pages, Now Playing, mini player.
4. Home from cache, mixes.
5. Library: likes, history, playlists, downloads, on device, sources.
6. Lyrics, Standby, equaliser, visualiser, haptics.
7. Settings, onboarding, backups, ListenBrainz, Your Sound, shared sessions, updates.
8. Widgets, quick-settings tile, Android Auto.
9. Parity pass and timing against the baseline, then release as 2.0.0.
