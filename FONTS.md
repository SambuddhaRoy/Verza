# Bundled fonts

Verza ships its typefaces as font files in [`app/src/main/res/font`](app/src/main/res/font)
rather than fetching them at runtime from the Google Play Services *downloadable fonts*
provider. This keeps the app free of any proprietary Google Mobile Services dependency, which
is a requirement for distribution on F-Droid and IzzyOnDroid.

Both families are licensed under the **SIL Open Font License 1.1** (OFL), which permits
bundling and redistribution. Inter is the upstream variable font; IBM Plex Mono ships as two
static instances. Both are taken from the [google/fonts](https://github.com/google/fonts) repository.

| Family        | File(s)                                                  | Designer / Foundry | License |
|---------------|----------------------------------------------------------|--------------------|---------|
| Inter         | `inter_variable.ttf`                                     | Rasmus Andersson   | OFL 1.1 |
| IBM Plex Mono | `ibm_plex_mono_regular.ttf`, `ibm_plex_mono_medium.ttf`  | IBM / Bold Monday  | OFL 1.1 |

Inter sets all text (display, headline, title, body, label); IBM Plex Mono is used only for
numeric chrome (durations, indices, datelines). The full OFL text accompanies each family in the
upstream repository linked above.
