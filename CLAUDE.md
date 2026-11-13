# Verza (Android), the rewrite

The phone app rebuilt from scratch on branch `rewrite` of `SambuddhaRoy/Verza`, beside the old app on
`main`, which keeps shipping 1.20.x until this one matches it. The design is the Zune-like one in `ui/theme`; Verza Next on the desktop copies it. Same package
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

## Lossless downloads from Soulseek (2.1.0)

The welcome asks (after the YouTube sign-in, and once on an update from before: `music_set_up`), and
Settings > downloads offers **Regular** (YouTube's AAC, ~4MB) or **Lossless (FLAC)** (`lossless_downloads`,
off by default, turned on by signing in to Soulseek). With Lossless and an account, a download is a FLAC
from someone sharing the song, and YouTube's stream only when nobody does or every copy stalls
(`data/downloads/Downloads.kt`, `saveFlac`).
- **The Nicotine+ core runs inside the app**: pynicotine 3.3.11, unmodified, in `src/main/python`, on
  Chaquopy's Python 3.11 (the newest with a 32-bit build; 3.12+ would drop armeabi-v7a phones).
  `bridge.py` drives it headless; `tagger.py` (with vendored mutagen) fills in missing tags and embeds a
  cover from MusicBrainz and the Cover Art Archive, with the song's title, artist and album as hints.
  Both are copies from Nicotine+ for Android (`C:\Users\Sambu\NicotineAndroid`); keep `tagger.py` the same in both.
- `Soulseek.kt` starts the core on first use (never at launch), searches for ~8s, and tries the best
  three files `SoulseekMatch` picks: same title, no live/remix/edit unless the title says so, length
  within 3s, then free slot, short queue, CD quality over hi-res, speed. A file that stalls 60s is
  skipped, and the result must be a real FLAC within 10s of the song (`flacSeconds`). `SoulseekMatchTest`.
- **Fake FLACs and symbol titles** (found on the desktop, November 2026). A FLAC made from an MP3 is
  *smaller* than the real one, its top end already gone: "Introduction (instructions)" came in at
  247kbps where every honest CD copy was 575-670. `dropFakes` drops a file under 70% of the median kbps
  of the song's other copies in the same format (three or more to judge by). A title of symbols alone
  ("$$$") matches on its symbols; it had no words, so it always came from YouTube. The desktop also
  ranks hi-res first now; the phone keeps CD first for its size.
- **pynicotine stops on any exception in a callback**, and 2.1.0's core left out the `buddies` component
  that shares and uploads look every peer up in: the first peer to browse us stopped it. Fixed in
  bridge.py, and if the loop ever ends again `Soulseek.kt` says so in the status (Python can't restart
  it in the same process) instead of searching into nothing. **It also saves its transfer list**
  (`files/soulseek/downloads.json`) and retries it at start, into folders nobody waits on; `start`
  deletes it and the download folder first, and a stopped transfer's `.incomplete` partial is deleted.
- **A search row is often just "Coldplay"**: no album and no length. Without the length `SoulseekMatch`
  accepts any file, a preview included, and without the album the tagger finds no release and no cover.
  `YouTube.complete` fills both from `next` first, and with the length still unknown it's a regular download.
- **Every download lands in the cache first** (`cacheDir/downloading`, cleared at start), is tagged
  (`Tags.kt`: tagger.py with Verza's title/artist/album as hints, MusicBrainz for the rest, YouTube Music's
  cover when the Cover Art Archive has none), then copied to `Music/Verza/<album artist>/<album>/Artist -
  Title.ext`, the folder named from the file's own tags so the phone's grouping (`PhoneGrouping`) reads it
  back as that album. The music folder only ever holds whole, tagged songs. MusicBrainz's title and album
  win over YouTube's where it has the song (the tagger fills empty tags only, and a new download has none).
- **Downloads from before the folders are moved once** (`Downloads.sortEarlier`, `downloads_sorted`):
  copied out, tagged, placed, then the old file deleted, and its folder once empty. A MediaStore file an
  earlier install saved can't be deleted, so the new copy is dropped instead and the song stays where it was.
  It runs through the download queue, so the dock says "sorting your downloads" and the service keeps it alive.
- **Regular downloads go in 4MB `Range` pieces** (`CHUNK`). One request for the whole file was sent at
  about the song's own speed: 167s for a 4-minute song on the emulator, ~15s in pieces.
- The core can't restart inside a process: it's disconnected, never quit. A kick by the same account
  signing in elsewhere is reconnected on the next download, not before.
- The password lives in the core's config, `filesDir/soulseek/config/config` (backups are off); the
  DataStore only has the name (`soulseek_user`). Files download to app-specific storage and are moved
  into the library like any download.
- `DownloadService` (dataSync) keeps the process in the foreground while anything downloads. It's
  stopped with an intent, never `stopService`, which crashes if it lands before `startForeground`.
- **`org.gradle.parallel=false`**: in parallel, KSP reads Chaquopy's file dependency from another
  thread and fails with "Current thread does not hold the state lock".
- **`buildPython=` in `local.properties`** names a Python 3.11 (here uv's,
  `%APPDATA%\uv\python\cpython-3.11.15-windows-x86_64-none\python.exe`; `py -3.11` doesn't find it), and
  Chaquopy compiles the app's Python at build time: `app.imy` holds `.pyc` only. Without it the build still
  works, ships `.py`, and each phone compiles them the first time Soulseek starts.
- The APK went from 5.0MB to 53.7MB: Python and its native modules for three ABIs.
- **Testing without an account** (never sign in to the real network from a test): `tools/fake_server.py
  song.flac` in NicotineAndroid is a fake server and peer that offers and sends that file for any search;
  make it as long as the song (`ffmpeg -f lavfi -i sine=duration=262 -ac 2 song.flac`). Point the app at it
  with `server = ('10.0.2.2', 2242)` under `[server]` in `files/soulseek/config/config`, written through
  `run-as` on a debug build. The release APK isn't debuggable: `adb root` on the emulator, copy the file into
  `/data/data/com.verza/files/soulseek/config/`, then `chown` it to the app's uid and `chcon` it to the context of `files/`.
  `tools/verza_bridge_test.py` runs this bridge against it on the desktop.
  Checked on the emulator: sign-in, a FLAC tagged from MusicBrainz with its cover (24s), a wrong-length
  copy refused and YouTube used instead, the service up during a download and gone after.

## Verza Connect and one-switch lossless (November 2026, unreleased)

- **`data/Connect.kt`, `ui/kit/Devices.kt`**: Verza Next on a computer shows here what it's playing,
  takes play, pause and skip from here, and a queue is handed over either way ("play here", "play
  there"). Settings > devices, and Devices in Now Playing's menu. The other end and the protocol are
  `src/main/connect.js` in the desktop repo; its CLAUDE.md has the detail.
- **Plain sockets, not OkHttp**: it's http to an address on the local network, which
  `network_security_config` refuses for every HTTP client and should go on refusing.
- Computers are watched only between `onStart` and `onStop`. A blocked socket read ignores
  cancellation, so the watcher's socket is closed when its coroutine ends.
- A computer is found by its UDP broadcast (port 47127, with a multicast lock) or added by address.
  **Broadcasts don't reach an emulator**, so discovery is unchecked; the rest was checked on
  `VerzaFresh` against the desktop build (`verza-shots/mob/_connect.py`).
- **Lossless is one switch** (`Lossless()` in SettingsPage.kt, used by the welcome too): with no
  Soulseek account, turning it on makes one with a random name and password. Compiles and draws;
  **the sign-in it makes was not run**, against the fake server or the real one.

## A song on the phone is a song (local files and downloads)

- **Where a song plays from is decided by `PhoneMusic.copyOf`** (Resolver, before any lookup): its own
  download, a download of the same song under another id (a song's video and album track are two ids), or a
  file in the last scan of the phone's music, matched by `SameSong` (title and first artist, normalised;
  lengths within 3s when both are known, so a live take or the video's longer cut isn't taken for it). So a
  radio, playlist or mix plays a song you have from its file, and a FLAC plays as Lossless. The phone's
  music is scanned after the first frame when the permission is held, so this works without opening On device.
- **A file on the phone borrows its YouTube song** (`YouTube.twin`: same title, artist and length among the
  first five search results, kept for the process) for its radio, Discovery radio, artist, album and Share.
  Without one those say so; play, queue, like and playlists never needed one.
- **Removing a download** (a song's menu) deletes the file, its album and artist folders once empty, and
  the row's path, so the song streams again. A file an earlier install saved can't be deleted by this one
  (MediaStore ownership), and the menu says so rather than forgetting a file that's still there.
- **A playlist made in Verza** is renamed or deleted from the ⋯ over its page, and a song taken out from
  that song's menu there. Each is queued for the account like a like (`Sync`): removal looks up the
  entry's `playlistSetVideoId` first, since YouTube removes entries, not songs. Deleting a playlist that
  never reached the account drops its queued ops instead. **Not checked against a real account.**
- `Library.likedIds` / `downloadedIds` are shared `StateFlow`s: every row's menu reads them, and as
  `Flow`s each row ran its own Room query on every change to `songs`. `copyOf` indexes downloads by
  `SameSong.key` (rebuilt when `downloadedIds` changes) instead of reading every row on each open.
- Checked on the emulator: an update from 2.1.0 sorted 11 downloads into Coldplay/Parachutes with MusicBrainz
  track numbers and removed the old folder; a YouTube "The Scientist" played the phone's FLAC (and a cover
  by someone else didn't); a radio from that file; a downloaded song from a radio with the network off; a
  phone file in a playlist; a lossless download (27s) and a wrong-length one falling back to regular.

## Performance rules

- **Paint from disk, then refresh.** Every page shows its last saved copy at once and updates behind
  it. The old Home waited on six network calls each launch: 6.15s median to its first shelf.
- **A song's YouTube Music cover is the right one** (lh*/yt* on googleusercontent/ggpht), fetched at the
  size drawn. Only a video's frame (i.ytimg.com) is looked up elsewhere.
- **Home is only what you listen to** (`data/mixes/HomeShelves.kt`, `ui/home/HomePage.kt`). YouTube's home,
  explore and charts feeds were dropped in 2.0.1: signed out, and for most accounts signed in, they are
  charts and trending. Don't bring them back.
- **Look ahead.** The next three songs' lookups run while one plays; a skip finds them done.

## Baseline (old app 1.20.11, emulator, median of 5)

`verza-shots/mob/_baseline.py <apk> com.verza/.MainActivity`: cold start 424ms, Home's first shelf
6.15s, search to first result 2.92s (includes ~0.7s of the tool's own polling).

## Where it stands

Everything the old app did is here except the visualiser and what rode on it (haptics on the beat,
the wash pulsing with the bass) and the moving wash behind Now Playing, which is a still blur of the
cover. The visualiser needs the microphone permission (`RECORD_AUDIO`), which the rewrite doesn't ask
for. New: Android Auto lists the library, and Play on headphones or the system's media card picks up
the saved queue with the app closed.

Checked on the emulator, scripts in `verza-shots/mob/`:

| Script | What |
|---|---|
| `_carryover.py old.apk new.apk` | an old install's like, history and playlist survive the update (also on `VerzaAndroid11`) |
| `_rw_play.py new.apk` | search, play, radio, skip, pause, queue kept across launches (also on Android 11) |
| `_rw_more.py new.apk` | a lossless source (PC server on 8790, adb reverse), album page, Now Playing, on device |
| `_wplace.py`, `_rw_widget.py`, `_w5.py`, `_w6.py` | place a widget; it follows playback; Play with the process gone resumes; the tile |
| `_rw_links.py`, `_rw_sleep2.py`, `_rw_standby.py`, `_rw_disc.py`, `_rw_tabs.py` | links in, the menu, sleep timer, Standby, Discovery, tabs |
| `_baseline.py <apk> com.verza/.MainActivity` | timings |

Timings (same emulator, nothing else running, median of 5), old 1.20.11 against the rewrite: cold start
519ms / 524ms (the old one ranged 414-650ms), search 2.83s / 2.70s, Home's first shelf 4.13s / 3.29s.
~2.4s of each Home and search figure is uiautomator's own dump. The APK is 5.0MB, the old one 7.4MB.

Not checked anywhere: Android Auto in a car or the head-unit emulator (not installed here).

## Gotchas found while porting

- **Http to the phone itself needs `network_security_config`**: without it Android refuses
  `http://localhost`, and adding a source served from the phone (or an adb-reversed one in tests)
  says it can't be reached.
- **A Media3 controller outlives its connection.** When the service is destroyed and made again, the
  old controller still takes commands, changes its own idea of the queue and plays nothing. `Player`
  replaces a controller that isn't `isConnected`.
- **Don't connect the player before the first frame**: it starts the service on the main thread and
  held the first frame back. `MainActivity` connects after one.
- **Anything drawn over the pages needs `takesTouches()`** (`ui/kit/Kit.kt`). A background lets taps
  through: a tap between the tabs, or anywhere on the first-launch welcome, played a song from the
  page underneath. Tests that tapped rows behind the dock passed for the same wrong reason.
- **The widget and tile classes keep their old names** (`com.verza.widget.*Widget`,
  `com.verza.tile.PlayPauseTileService`). A renamed provider and every widget on a home screen
  disappears on update.
- **Resuming with the app closed needs `MediaButtonReceiver` in the manifest** as well as
  `onPlaybackResumption`. Without it Media3 refuses the system ("No root for client
  com.android.systemui") and headphone Play does nothing. A widget's Play holds its controller until
  playback starts, or the service stops before the queue has loaded.
- **A force-stop greys out an app's widgets** until it runs again; test "app closed" with `am kill`.
- **On Android 11, uiautomator reports the dock's tabs with no size** (the old app's too).
  `m.py`'s `tab()` falls back to where they're drawn.
- `adb emu rotate` in the Standby test can leave the emulator's user rotation locked sideways; reset
  with `cmd window user-rotation lock 0`.
