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
import com.verza.ui.theme.CaptionItalic
import com.verza.ui.theme.FontDisplay
import com.verza.ui.theme.LocalVerzaExtendedColors
import com.verza.ui.theme.VerzaTheme
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
    onFinished: () -> Unit,
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
            StepDots(current = step, total = 4)
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
                        onPickLight = {
                            viewModel.setTheme(VerzaTheme.ATELIER_LIGHT)
                            step = 3
                        },
                        onPickDark = {
                            viewModel.setTheme(VerzaTheme.ATELIER_DARK)
                            step = 3
                        },
                    )
                    else -> StepDone(
                        onFinish = {
                            viewModel.setOnboardingCompleted()
                            onFinished()
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
            text = "Two short questions and you're in.",
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
    onPickLight: () -> Unit,
    onPickDark: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    Column(modifier = Modifier.fillMaxSize()) {
        Eyebrow(text = "STEP 02")
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Light or dark?",
            style = MaterialTheme.typography.displaySmall,
            color = colors.onBackground,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Pick the mood you want to read in. You can change this anytime — and there are " +
                  "six more palettes in Settings if you want to dig in.",
            style = MaterialTheme.typography.bodyLarge,
            color = ext.muted,
        )
        Spacer(Modifier.height(28.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ThemePreviewCard(
                theme = VerzaTheme.ATELIER_LIGHT,
                label = "Light",
                modifier = Modifier.weight(1f),
                onClick = onPickLight,
            )
            ThemePreviewCard(
                theme = VerzaTheme.ATELIER_DARK,
                label = "Dark",
                modifier = Modifier.weight(1f),
                onClick = onPickDark,
            )
        }
    }
}

@Composable
private fun StepDone(onFinish: () -> Unit) {
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
            text = "Welcome to Verza.",
            style = CaptionItalic.copy(fontSize = 18.sp),
            color = ext.muted,
        )
        Spacer(Modifier.weight(1f))
        PrimaryActionButton(text = "Begin listening", onClick = onFinish)
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

@Composable
private fun ThemePreviewCard(
    theme: VerzaTheme,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    val scheme = remember(theme) { theme.toColorScheme() }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .border(width = 1.dp, color = ext.borderGlass, shape = RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
    ) {
        // Stylised page rather than a screenshot: a colored panel + accent rule + miniature type.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(scheme.background)
                .padding(16.dp),
        ) {
            Box(
                Modifier
                    .width(24.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(scheme.primary),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Verza",
                style = TextStyle(
                    fontFamily = FontDisplay,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                ),
                color = scheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "A quieter way to listen.",
                style = CaptionItalic.copy(fontSize = 12.sp),
                color = scheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(16.dp).clip(CircleShape).background(scheme.primary))
                Box(Modifier.size(16.dp).clip(CircleShape).background(scheme.secondary))
                Box(Modifier.size(16.dp).clip(CircleShape).background(scheme.tertiary))
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, style = MaterialTheme.typography.titleMedium, color = colors.onBackground)
            Text(text = "Tap", style = CaptionItalic, color = ext.muted)
        }
    }
}
