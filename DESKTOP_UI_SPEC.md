# Desktop-consistent UI redesign — spec for the Verza **mobile** app

Goal: re-skin the mobile app's **standard (default) appearance** to match the Verza **Desktop**
"liquid-glass over a flowing album-art wash" language. This is a **presentation-layer pass** — colours,
shapes, typography, component chrome, motion. Do **not** rewrite features, navigation, data, playback,
media-session, mixes, EQ, or lyrics logic. Self-contained: everything the desktop uses is inlined here
(source lives at `github.com/SambuddhaRoy/Verza-Desktop`, `src/renderer/index.html` + `renderer.js`, if
you want pixel-exact values).

---

## 0. The one-paragraph concept

Verza is **neutral liquid glass with no inherent colour**. Translucent panels float over a
**flowing, blurred, domain-warped wash of the current album art** — that wash is the *only* source of
colour. A single **green** accent is reserved strictly for interactive elements (play button, active
nav, progress fill, likes). One **14 px** corner radius on everything. Inter typeface. Motion is a
Material-3 "bouncy overshoot spring + emphasized ease".

The mobile app **already has the hard part**: the cover-flow wash is `GlowStyle.COVER`
(`ui/theme/CoverFlow.kt`) — the same shader, ported from desktop. The redesign is mostly: make Cover
the default background, retheme surfaces to glass, and reshape a handful of components.

---

## 1. Design tokens (source of truth)

### Accent (green) — the ONLY non-neutral colour, interactive elements only
| token | dark | light |
|---|---|---|
| accent | `#52b788` | `#2d6a4f` |
| accent-dark | `#2d6a4f` | `#1b4332` |
| accent-pale | `#95d5b2` | `#52b788` |
| accent-glow | `rgba(82,183,136,.35)` | `rgba(45,106,79,.25)` |

Desktop lets the user pick the accent (a few palettes) + an adaptive-from-cover option. Mobile already
has glow-colour presets + adaptive — wire the accent to those.

### Glass (translucent panels)
| | dark | light |
|---|---|---|
| panel base RGB | `0,0,0` | `255,255,255` |
| panel alpha (default) | ~**0.55**, but desktop default tint reads **near-solid** (~0.97 opaque; the frost is a slider) | ~0.42 |
| blur | 16 px | 18 px |
| saturate / brightness | 220% / 140% | 120% / 110% |
| border | `rgba(255,255,255,.14)` | `rgba(255,255,255,.35)` |
| shadow | multi-layer: `inset 0 1px 0 rgba(255,255,255,.06)` + `0 30px 80px rgba(0,0,0,.55)` + mid + tight | lighter version |

> **Mobile translation of "glass":** do **not** attempt a real-time backdrop-blur of the animated
> wash — Compose `Modifier.blur` on a moving layer is too costly. Instead: **translucent panel colour
> over the cover-flow** (the wash supplies colour + motion) + hairline border + the layered shadow.
> That reads as glass. Default panels fairly **opaque** (tint ~0.97) so text stays legible; the frost
> can be an optional slider later.

### Text — an opacity ladder over one ink colour
- ink RGB: dark `255,255,255` / light `10,20,14`.
- primary `.85`, secondary `.45`, tertiary `.25`, faint labels `.15`. (Ladder rungs: .88 .85 .80 .75
  .70 .60 .55 .45 .40 .30 .25 .20 .16 .15 .12 .10 …)

### Shape
- **14 px** radius on **everything** (cards, panels, buttons, chips, thumbnails, sheets).
  Mobile currently uses 16 px (`VerzaCorner` in `ui/theme/Shapes.kt`) → change to **14**.

### Typography — Inter (mobile `Type.kt` already uses Inter ✓)
| role | weight | size |
|---|---|---|
| greeting | 700 | 24 |
| section heading | 600 | 15 |
| song title (now playing) | 600 | 18 |
| song artist | 400 | 13 |
| nav item | 500 | 13 |
| list track title | 500 | 12 |
| list track artist | 400 | 10 |
| badge / label | 500–600 | 10–11 |

### Motion
- spring (bouncy overshoot): `cubic-bezier(0.34, 1.56, 0.64, 1)` → Compose `spring(dampingRatio ≈ 0.5–0.6, stiffness Medium)`.
- emphasized ease: `cubic-bezier(0.2, 0, 0, 1)` → Compose `tween(…, easing = CubicBezierEasing(0.2f,0f,0f,1f))`.
- interaction scales: press ~0.90–0.97, hover/active ~1.10, entrances overshoot in. Mobile already has
  `pressableScale` (bouncy spring) — reuse it everywhere.

---

## 2. Component treatments (desktop → mobile)

- **Background:** the cover-flow wash behind *every* screen. Mobile = `GlowStyle.COVER`. **Make it the
  default** (default `glowStyle = COVER`, default movement/`glowChaos` = **1.0** — desktop runs it at
  max). Halftone is **retired on desktop** (looked unfinished); on mobile, demote it — keep Fluid +
  Cover, hide/drop Halftone from the picker unless you want to keep it as an extra.
- **Surfaces / panels:** a shared `glassSurface` treatment (translucent panel colour + border +
  shadow, 14 px). Apply to nav, mini-player, cards, sheets, now-playing panels.
- **Navigation:** desktop sidebar → keep the mobile **bottom bar** (4 tabs). Make it a glass strip;
  active tab = accent indicator + accent-tinted label (the existing bar is close — glass-ify it).
- **Home cards:** square, cover/gradient fill, dark bottom overlay, title + subtitle bottom-left, a
  play affordance that appears on press; press-scale 0.97, entrance stagger with overshoot.
- **Now Playing (the showpiece):**
  - **Vinyl album art** — circular, spinning while playing (paused when not), radial groove texture,
    small centre label, soft glow behind. (Replaces the square art in standard mode.)
  - title / artist / a year + quality badge row.
  - **Progress "thread"** — thin track, accent-gradient fill, thumb with accent glow, expands on press.
  - transport row with a **liquid-glass "droplet" play button**: a translucent refractive accent
    circle — subtle outline + refraction, **no glow/specular**. Mobile: approximate with a frosted
    translucent-accent circle; a true refraction (an AGSL shader sampling the wash behind the button)
    is a *nice-to-have, optional*.
  - extra controls: like / shuffle / repeat + a volume control; an "up next" list of glass rows.
- **Mini-player:** translucent strip (`rgba(0,0,0,.15)` dark), 40 px cover (14 px radius), title +
  artist, compact progress line, play button.

---

## 3. Translation rules — DROP / RE-MAP / KEEP

- **DROP (desktop-only, no mobile analog):** window chrome (traffic lights, drag header), the fake
  desktop-background image, collapsible sidebar, output-device picker, tray, global hotkeys, Discord
  presence, updater UI.
- **RE-MAP:** sidebar → bottom nav; GSAP eases → Compose spring/tween (§1 Motion); `backdrop-filter`
  glass → translucent Compose panels over the cover-flow (§1, do **not** real-time-blur the wash).
- **KEEP — mobile already matches, don't redo:** the cover-flow wash (`ui/theme/CoverFlow.kt`), the
  green-accent-only rule, adaptive/album-art colouring, and **all** navigation / data / playback /
  media-session / mixes / EQ / lyrics logic.

---

## 4. Where the mobile levers are (change tokens first, reskin many screens for free)

Central theme files — retheme these and most of the UI follows via `MaterialTheme`:
- `ui/theme/Shapes.kt` — `VerzaCorner` 16 → **14**.
- `ui/theme/Color.kt` + `ui/theme/Theme.kt` — the Material3 colour schemes (glass surfaces, accent as
  primary). `GlowColors.kt` for cover/artwork colours; `LocalVerzaExtendedColors` / `LocalCoverColors`
  / `LocalArtworkColors` for the extras.
- `ui/theme/Type.kt` — already Inter; adjust the scale in §1 if needed.
- `ui/theme/Glow.kt` + `CoverFlow.kt` — default `GlowStyle.COVER`, default movement 1.0.

Hand-touch only the distinctive components:
- `ui/components/BottomBar.kt`, `MiniPlayer.kt`, `SectionRow.kt` (cards), `PressScale.kt` (motion).
- `ui/screens/NowPlayingScreen.kt` (vinyl + thread progress + droplet button + up-next).
- `ui/sleeve/` — **decision:** the desktop glass look effectively replaces "standard". Recommend
  making the new glass look the standard/default appearance and **retiring or leaving Sleeve as-is**;
  don't build a third mode. Confirm with the user before deleting Sleeve.

Settings prefs already exist for glow style / movement / colour — reuse them; don't add a parallel
system.

---

## 5. Guardrails
- Kotlin + Jetpack Compose + Material 3. minSdk 26 (AGSL cover-flow is API 33+ with a fluid fallback —
  keep the guard).
- This is a **skin**, not a feature rewrite. Smallest diff that lands the look; prefer changing central
  tokens over editing every composable. Deletion over addition.
- Follow the existing repo conventions in `CLAUDE.md` (build/release/security, commit trailer, never
  leak the auth cookie). Verify the build and check the look on a real device/emulator.
