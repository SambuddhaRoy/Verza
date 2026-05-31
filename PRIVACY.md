# Privacy Policy

_Last updated: 2026-05-30_

Verza is an open-source, unofficial YouTube Music client for Android. It has **no backend of its own** — there are no Verza servers, no Verza accounts, **no analytics, no tracking, no advertising, and no crash/usage telemetry**. Nothing about how you use the app is ever sent to the developer.

This document explains exactly what data the app handles, where it goes, and what stays on your device.

## What leaves your device, and to whom

Verza only talks to the third parties below, and only to deliver the feature you're using. None of these requests include an identifier that ties the request to *you* as a Verza user.

| Destination | When | What is sent |
|---|---|---|
| **YouTube / Google** (InnerTube API + NewPipeExtractor) | Search, browse, playback, and — only if you sign in — your personalised feed/library | Your search terms and the IDs of content you open. **If (and only if) you sign in**, requests to Google also carry your Google account cookie, so Google can return your personalised data and register your likes. This is sent **only to Google**. |
| **Apple — iTunes Search API** | To fetch real cover art for a track | The track's **title and artist**, as an anonymous search query. No account, device, or user identifier. |
| **LRCLIB** (`lrclib.net`) | To fetch synced/plain lyrics | The track's **title, artist, and duration**, anonymously. |
| **Google Fonts** | First launch, to load the app's typefaces | A standard font request (no personal data). |

> Because album-art and lyrics lookups send a track's title/artist to Apple and LRCLIB respectively, those two services can observe *that some anonymous client requested metadata for a given song*. They receive no account, no device ID, and nothing linking the request to you. If you prefer to avoid even this, you can play without lyrics and the artwork will fall back to YouTube's own thumbnail.

All network traffic uses HTTPS.

## What stays on your device

The following never leaves your phone (except via your own Android backup, subject to the exclusions below):

- **Liked songs, playlists, play history, and listening stats** — stored in a local Room database.
- **Preferences and the saved playback queue** — stored in DataStore.
- **Downloads** — saved in the app's private storage.

### Your account credential

If you choose to sign in, the YouTube Music session cookie is:

- **Encrypted at rest** with an AES-256/GCM key held in the device's **Android Keystore** (the key is non-exportable and hardware-backed where supported);
- **Excluded from cloud backup and device-to-device transfer**, so it can't be lifted from a backup;
- **Sent only to Google**, never to any other host;
- **Removed** when you sign out.

Sign-in is entirely optional — search, browse, playback, downloads, lyrics, and all local features work without an account.

## Microphone permission (`RECORD_AUDIO`)

Verza requests the microphone permission **only if you turn on the optional "Sound reactivity" glow** in Settings → Background glow. When enabled, the app uses Android's `Visualizer` API to read a low-resolution frequency snapshot of **the music it is already playing** — purely to animate the background glow.

- It does **not** record from the microphone.
- It does **not** capture, store, or transmit any audio.
- The permission is requested only at the moment you enable the feature; decline it and everything else in the app works normally. You can revoke it anytime in Android system settings.

Android surfaces this capability as a "Microphone" permission because the `Visualizer` API is governed by `RECORD_AUDIO`, even though no microphone input is used.

## Children's privacy

Verza collects no personal data from anyone, including children.

## Changes

Any changes to this policy will be committed to this file in the repository, so the history is publicly auditable.

## Contact

Questions or concerns: open an issue at <https://github.com/SambuddhaRoy/Verza/issues>.
