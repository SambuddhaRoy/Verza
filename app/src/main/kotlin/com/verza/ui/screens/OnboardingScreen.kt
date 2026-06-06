package com.verza.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.ui.platform.LocalContext
import com.verza.ui.theme.CaptionItalic
import com.verza.ui.theme.DynamicColorSupported
import com.verza.ui.theme.FontDisplay
import com.verza.ui.theme.FontSleeve
import com.verza.ui.theme.GlowColorPreset
import com.verza.ui.theme.LocalVerzaExtendedColors
import com.verza.ui.theme.VerzaTheme
import com.verza.ui.theme.resolveColor
import com.verza.ui.theme.toColorScheme

/**
 * First-launch onboarding. Four steps, button-driven, no swipe:
 *
 *  1. Welcome   — brand intro and a "Begin" button.
 *  2. Sign-in   — optional YouTube Music sign-in (navigates to LoginScreen, auto-advances on success).
 *  3. Theme     — quick Light/Dark choice (maps to Atelier light/dark; full picker stays in Settings).
 *  4. Done      — celebratory close-out + "Begin listening" button that marks completion.
 *
 * Completion writes `onboarding_completed = true` to DataStore, after which MainActivity
 * routes future cold-launches straight to Home.
 */
@Composable
fun OnboardingScreen(
    onSignIn: () -> Unit,
    onFinished: (takeTour: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val isSignedIn by viewModel.isSignedIn.collectAsStateWithLifecycle()

    var step by remember { mutableIntStateOf(0) }

    // If the user comes back from the LoginScreen with a fresh cookie while still on the
    // sign-in step, advance them automatically — no point making them tap "Continue" again.
    LaunchedEffect(isSignedIn, step) {
        if (step == 1 && isSignedIn) step = 2
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 28.dp, vertical = 32.dp),
        ) {
            // Progress dots — a quiet header signalling there are a few steps.
            StepDots(current = step, total = 6)
            Spacer(Modifier.height(40.dp))

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    fadeIn(animationSpec = tween(280)) togetherWith fadeOut(animationSpec = tween(180))
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                label = "onboarding-step",
            ) { current ->
                when (current) {
                    0 -> StepWelcome(onContinue = { step = 1 })
                    1 -> StepSignIn(
                        isSignedIn = isSignedIn,
                        onSignIn = onSignIn,
                        onSkip = { step = 2 },
                        onContinue = { step = 2 },
                    )
                    2 -> StepTheme(
                        dynamicSupported = DynamicColorSupported,
                        onPick = { theme ->
                            viewModel.setTheme(theme)
                            step = 3
                        },
                    )
                    3 -> StepAppearance(
                        onPick = { sleeve ->
                            viewModel.setSleeveMode(sleeve)
                            step = 4
                        },
                    )
                    4 -> StepGlow(
                        onPick = { enabled, preset, reactive ->
                            viewModel.setGlowEnabled(enabled)
                            if (preset != null) viewModel.setGlowColor(preset)
                            viewModel.setGlowReactive(reactive)
                            step = 5
                        },
                    )
                    else -> StepDone(
                        onFinish = { takeTour ->
                            viewModel.setOnboardingCompleted()
                            onFinished(takeTour)
                        },
                    )
                }
            }
        }
    }
}

// ── Step content ─────────────────────────────────────────────────────────────

@Composable
private fun StepWelcome(onContinue: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        // The accent rule signs the page — same idiom used in Settings/Home section headers.
        Box(
            Modifier
                .width(36.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(colors.primary),
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = "WELCOME",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.6.sp),
            color = colors.primary,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Verza",
            style = MaterialTheme.typography.displayLarge,
            color = colors.onBackground,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "A quieter way to listen.",
            style = CaptionItalic.copy(fontSize = 18.sp),
            color = ext.muted,
        )
        Spacer(Modifier.height(48.dp))
        Text(
            text = "A few quick choices and you're in.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onBackground,
        )
        Spacer(Modifier.weight(1f))
        PrimaryActionButton(text = "Begin", onClick = onContinue)
    }
}

@Composable
private fun StepSignIn(
    isSignedIn: Boolean,
    onSignIn: () -> Unit,
    onSkip: () -> Unit,
    onContinue: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    Column(modifier = Modifier.fillMaxSize()) {
        Eyebrow(text = "STEP 01")
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Sync your music.",
            style = MaterialTheme.typography.displaySmall,
            color = colors.onBackground,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Sign in with YouTube Music to bring your home feed, library and recommendations along. " +
                  "You can skip this and listen anonymously — your local likes and playlists still work.",
            style = MaterialTheme.typography.bodyLarge,
            color = ext.muted,
        )

        Spacer(Modifier.weight(1f))

        if (isSignedIn) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp),
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = colors.primary)
                Text(text = "Signed in.", style = CaptionItalic, color = colors.onBackground)
            }
            PrimaryActionButton(text = "Continue", onClick = onContinue)
        } else {
            PrimaryActionButton(text = "Sign in with YouTube Music", onClick = onSignIn)
            Spacer(Modifier.height(12.dp))
            TextActionButton(text = "Continue without signing in", onClick = onSkip)
        }
    }
}

@Composable
private fun StepTheme(
    dynamicSupported: Boolean,
    onPick: (VerzaTheme) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    Column(modifier = Modifier.fillMaxSize()) {
        Eyebrow(text = "STEP 02")
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Choose your look",
            style = MaterialTheme.typography.displaySmall,
            color = colors.onBackground,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Pick the mood you want to read in. You can change this anytime — and there are " +
                  "more palettes in Settings.",
            style = MaterialTheme.typography.bodyLarge,
            color = ext.muted,
        )
        Spacer(Modifier.height(24.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Material You leads (and is the default) on Android 12+; hidden where unsupported.
            if (dynamicSupported) {
                ThemeOptionRow(
                    theme = VerzaTheme.DYNAMIC,
                    label = "Material You",
                    subtitle = "Colours from your wallpaper",
                    onClick = { onPick(VerzaTheme.DYNAMIC) },
                )
            }
            ThemeOptionRow(
                theme = VerzaTheme.ATELIER_DARK,
                label = "Atelier Dark",
                subtitle = "Warm ink on coffee-black",
                onClick = { onPick(VerzaTheme.ATELIER_DARK) },
            )
            ThemeOptionRow(
                theme = VerzaTheme.ATELIER_LIGHT,
                label = "Atelier Light",
                subtitle = "Ink on warm bone",
                onClick = { onPick(VerzaTheme.ATELIER_LIGHT) },
            )
        }
    }
}

@Composable
private fun StepAppearance(onPick: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    Column(modifier = Modifier.fillMaxSize()) {
        Eyebrow(text = "STEP 03")
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Pick a layout",
            style = MaterialTheme.typography.displaySmall,
            color = colors.onBackground,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Standard is a clean Material layout. Sleeve is an editorial mode that recolours " +
                  "the whole app from the cover art, sets it in a serif, and turns Now Playing into " +
                  "a poster. You can switch anytime in Settings.",
            style = MaterialTheme.typography.bodyLarge,
            color = ext.muted,
        )
        Spacer(Modifier.height(24.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppearanceOptionRow(
                label = "Standard",
                subtitle = "Clean Material cards",
                serif = false,
                onClick = { onPick(false) },
            )
            AppearanceOptionRow(
                label = "Sleeve",
                subtitle = "Editorial · cover-driven · poster Now Playing",
                serif = true,
                onClick = { onPick(true) },
            )
        }
    }
}

@Composable
private fun StepGlow(onPick: (Boolean, GlowColorPreset?, Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    var reactive by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize()) {
        Eyebrow(text = "STEP 04")
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Set the mood",
            style = MaterialTheme.typography.displaySmall,
            color = colors.onBackground,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "A soft glow drifts behind the app and can take on each song's colours. " +
                  "It appears on dark themes; tweak it anytime in Settings.",
            style = MaterialTheme.typography.bodyLarge,
            color = ext.muted,
        )
        Spacer(Modifier.height(20.dp))

        // Sound-reactivity toggle — captured and applied when a glow option is chosen below.
        ReactiveToggleRow(checked = reactive, onCheckedChange = { reactive = it })
        Spacer(Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GlowOptionRow(
                label = "Album colours",
                subtitle = "Glow adapts to the cover art",
                swatch = GlowSwatch.Album,
                onClick = { onPick(true, GlowColorPreset.ALBUM_ART, reactive) },
            )
            GlowOptionRow(
                label = "Warm amber",
                subtitle = "A fixed, warm ambient glow",
                swatch = GlowSwatch.Solid(GlowColorPreset.WARM_AMBER.resolveColor()),
                onClick = { onPick(true, GlowColorPreset.WARM_AMBER, reactive) },
            )
            GlowOptionRow(
                label = "Off",
                subtitle = "No background glow",
                swatch = GlowSwatch.None,
                onClick = { onPick(false, null, false) },
            )
        }
    }
}

@Composable
private fun StepDone(onFinish: (takeTour: Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Box(
            Modifier
                .width(36.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(colors.primary),
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = "READY",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.6.sp),
            color = colors.primary,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "You're set.",
            style = MaterialTheme.typography.displayMedium,
            color = colors.onBackground,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "There's a lot under the hood — want the quick tour?",
            style = CaptionItalic.copy(fontSize = 18.sp),
            color = ext.muted,
        )
        Spacer(Modifier.weight(1f))
        PrimaryActionButton(text = "Take the tour", onClick = { onFinish(true) })
        Spacer(Modifier.height(12.dp))
        TextActionButton(text = "Jump straight in", onClick = { onFinish(false) })
    }
}

// ── Shared pieces ────────────────────────────────────────────────────────────

@Composable
private fun StepDots(current: Int, total: Int) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { i ->
            val active = i == current
            Box(
                Modifier
                    .height(2.dp)
                    .width(if (active) 24.dp else 12.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (active) colors.primary else ext.muted.copy(alpha = 0.35f)),
            )
        }
    }
}

@Composable
private fun Eyebrow(text: String) {
    val colors = MaterialTheme.colorScheme
    Column {
        Box(
            Modifier
                .width(24.dp)
                .height(1.dp)
                .clip(RoundedCornerShape(0.5.dp))
                .background(colors.primary),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.6.sp),
            color = colors.primary,
        )
    }
}

@Composable
private fun PrimaryActionButton(text: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

@Composable
private fun TextActionButton(text: String, onClick: () -> Unit) {
    val ext = LocalVerzaExtendedColors.current
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
    ) {
        Text(text = text, style = CaptionItalic, color = ext.muted)
    }
}

/** Compact, full-width theme option: a swatch quartet + label/subtitle, tappable. */
@Composable
private fun ThemeOptionRow(
    theme: VerzaTheme,
    label: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    val context = LocalContext.current
    // For Material You, show the actual wallpaper-derived colours; otherwise the theme's own scheme.
    val scheme = remember(theme) {
        if (theme == VerzaTheme.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            dynamicDarkColorScheme(context)
        else
            theme.toColorScheme()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(width = 1.dp, color = ext.borderGlass, shape = RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(modifier = Modifier.clip(RoundedCornerShape(8.dp))) {
            Box(Modifier.size(28.dp).background(scheme.background))
            Box(Modifier.size(28.dp).background(scheme.primary))
            Box(Modifier.size(28.dp).background(scheme.secondary))
            Box(Modifier.size(28.dp).background(scheme.tertiary))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleMedium, color = colors.onBackground)
            Text(subtitle, style = CaptionItalic, color = ext.muted)
        }
    }
}

/** Full-width appearance option: a small preview chip + label/subtitle, tappable. */
@Composable
private fun AppearanceOptionRow(
    label: String,
    subtitle: String,
    serif: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(width = 1.dp, color = ext.borderGlass, shape = RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (serif) androidx.compose.ui.graphics.Color(0xFF0B0705) else colors.primary),
            contentAlignment = Alignment.Center,
        ) {
            if (serif) {
                // A serif "Aa" on a dark chip hints at the editorial Sleeve look.
                Text(
                    text = "Aa",
                    style = TextStyle(fontFamily = FontSleeve, fontWeight = FontWeight.Normal, fontSize = 20.sp),
                    color = androidx.compose.ui.graphics.Color(0xFFF2E9DD),
                )
            } else {
                Text(
                    text = "Aa",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onPrimary,
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleMedium, color = colors.onBackground)
            Text(subtitle, style = CaptionItalic, color = ext.muted)
        }
    }
}

/** Full-width toggle row for the optional sound-reactive glow. */
@Composable
private fun ReactiveToggleRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(width = 1.dp, color = ext.borderGlass, shape = RoundedCornerShape(16.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("React to the music", style = MaterialTheme.typography.titleMedium, color = colors.onBackground)
            Text(
                "Glow pulses with the beat — reads playback only, never records",
                style = CaptionItalic,
                color = ext.muted,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** Visual hint for a glow option row. */
private sealed interface GlowSwatch {
    data object Album : GlowSwatch
    data object None : GlowSwatch
    data class Solid(val color: androidx.compose.ui.graphics.Color) : GlowSwatch
}

/** Compact, full-width glow option: a representative swatch + label/subtitle, tappable. */
@Composable
private fun GlowOptionRow(
    label: String,
    subtitle: String,
    swatch: GlowSwatch,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(width = 1.dp, color = ext.borderGlass, shape = RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        val swatchModifier = when (swatch) {
            is GlowSwatch.Album -> Modifier.background(
                androidx.compose.ui.graphics.Brush.sweepGradient(
                    listOf(
                        androidx.compose.ui.graphics.Color(0xFFE0556E),
                        androidx.compose.ui.graphics.Color(0xFFE8B14A),
                        androidx.compose.ui.graphics.Color(0xFF5A8068),
                        androidx.compose.ui.graphics.Color(0xFF6B8BA8),
                        androidx.compose.ui.graphics.Color(0xFFE0556E),
                    )
                )
            )
            is GlowSwatch.Solid -> Modifier.background(swatch.color)
            is GlowSwatch.None -> Modifier.background(ext.muted.copy(alpha = 0.25f))
        }
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .then(swatchModifier)
                .border(width = 1.dp, color = ext.borderGlass, shape = CircleShape),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleMedium, color = colors.onBackground)
            Text(subtitle, style = CaptionItalic, color = ext.muted)
        }
    }
}
