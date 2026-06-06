# Bundled fonts

Verza ships its typefaces as font files in [`app/src/main/res/font`](app/src/main/res/font)
rather than fetching them at runtime from the Google Play Services *downloadable fonts*
provider. This keeps the app free of any proprietary Google Mobile Services dependency, which
is a requirement for distribution on F-Droid and IzzyOnDroid.

All four families are licensed under the **SIL Open Font License 1.1** (OFL), which permits
bundling and redistribution. Each is the upstream variable font (or, for IBM Plex Mono, two
static instances), taken from the [google/fonts](https://github.com/google/fonts) repository.

| Family             | File(s)                                                            | Designer / Foundry                | License |
|--------------------|-------------------------------------------------------------------|-----------------------------------|---------|
| Cormorant Garamond | `cormorant_garamond_variable.ttf`, `cormorant_garamond_italic_variable.ttf` | Christian Thalmann (Catharsis Fonts) | OFL 1.1 |
| Newsreader         | `newsreader_variable.ttf`, `newsreader_italic_variable.ttf`        | Production Type                   | OFL 1.1 |
| Inter              | `inter_variable.ttf`                                               | Rasmus Andersson                  | OFL 1.1 |
| IBM Plex Mono      | `ibm_plex_mono_regular.ttf`, `ibm_plex_mono_medium.ttf`           | IBM / Bold Monday                 | OFL 1.1 |

The full OFL text accompanies each family in the upstream repository linked above.
