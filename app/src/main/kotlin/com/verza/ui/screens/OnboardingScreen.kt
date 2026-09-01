package com.verza.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.verza.R
import com.verza.ui.expressive.BodyStrong
import com.verza.ui.expressive.BodyText
import com.verza.ui.expressive.ColorFlavour
import com.verza.ui.expressive.CookieShape
import com.verza.ui.expressive.ExpressiveMotion
import com.verza.ui.expressive.HeroDisplay
import com.verza.ui.expressive.HeroTitle
import com.verza.ui.expressive.LocalExpressiveColors
import com.verza.ui.expressive.MetaLabel
import com.verza.ui.expressive.PillShape
import com.verza.ui.expressive.ShapeExtraLarge
import com.verza.ui.expressive.expressiveColorsFrom
import com.verza.ui.theme.LocalArtworkColors

/**
 * First launch. Four steps, button-driven, no swipe:
 *
 *  1. Welcome  — the mark, the name, one line.
 *  2. Sign in  — optional; auto-advances if a cookie lands while you are still on it.
 *  3. Colour   — pick a flavour, previewed live.
 *  4. Done     — tour or straight in.
 *
 * Built on the expressive palette rather than the Material scheme, so it is the app you are about to
 * use rather than a differently-dressed lobby in front of it. Completion writes
 * `onboarding_completed` to DataStore, after which cold launches route straight to Home.
 */
@Composable
fun OnboardingScreen(
    onSignIn: () -> Unit,
    onFinished: (takeTour: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val isSignedIn by viewModel.isSignedIn.collectAsStateWithLifecycle()
    val colors = LocalExpressiveColors.current

    var step by remember { mutableIntStateOf(0) }
    // Which way the step transition travels. Onboarding only ever goes forward, but the sign-in
    // auto-advance can arrive at any moment and a slide with no direction reads as a glitch.
    var lastStep by remember { mutableIntStateOf(0) }
    val forward = step >= lastStep
    if (step != lastStep) lastStep = step

    LaunchedEffect(isSignedIn, step) {
        if (step == 1 && isSignedIn) step = 2
    }

    Box(modifier = modifier.fillMaxSize().background(colors.container)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 24.dp, vertical = 28.dp),
        ) {
            StepDots(current = step, total = 4)
            Spacer(Modifier.height(36.dp))

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    val dir = if (forward) 1 else -1
                    (slideInHorizontally(ExpressiveMotion.spatialDefault()) { w -> dir * w / 4 } +
                        fadeIn(ExpressiveMotion.effectsDefault())) togetherWith
                        (slideOutHorizontally(ExpressiveMotion.spatialDefault()) { w -> -dir * w / 4 } +
                            fadeOut(ExpressiveMotion.effectsFast()))
                },
                modifier = Modifier.weight(1f).fillMaxWidth(),
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
                    2 -> StepColour(
                        selected = viewModel.colorFlavour.collectAsStateWithLifecycle().value,
                        onPick = viewModel::setColorFlavour,
                        onContinue = { step = 3 },
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

// ── steps ────────────────────────────────────────────────────────────────────

@Composable
private fun StepWelcome(onContinue: () -> Unit) {
    val colors = LocalExpressiveColors.current
    val enter = remember { Animatable(0.7f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, ExpressiveMotion.spatialSlow()) }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .scale(enter.value)
                .clip(CookieShape)
                .background(colors.accent),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_verza_glyph),
                contentDescription = null,
                colorFilter = ColorFilter.tint(colors.onAccent),
                modifier = Modifier.size(112.dp),
            )
        }
        Spacer(Modifier.height(28.dp))
        Text("Verza", style = HeroDisplay, color = colors.onContainer)
        Spacer(Modifier.height(8.dp))
        Text("A quieter way to listen.", style = BodyText, color = colors.onContainerMuted)
        Spacer(Modifier.weight(1f))
        PrimaryAction(text = "Begin", onClick = onContinue)
    }
}

@Composable
private fun StepSignIn(
    isSignedIn: Boolean,
    onSignIn: () -> Unit,
    onSkip: () -> Unit,
    onContinue: () -> Unit,
) {
    val colors = LocalExpressiveColors.current
    Column(modifier = Modifier.fillMaxSize()) {
        Text("STEP 01", style = MetaLabel, color = colors.onContainerMuted)
        Spacer(Modifier.height(8.dp))
        Text("Sync your music.", style = HeroTitle, color = colors.onContainer)
        Spacer(Modifier.height(14.dp))
        Text(
            "Sign in with YouTube Music to bring your home feed, library and recommendations " +
                "along. You can skip this and listen anonymously — your local likes and playlists " +
                "still work.",
            style = BodyText,
            color = colors.onContainerMuted,
        )

        Spacer(Modifier.weight(1f))

        if (isSignedIn) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 14.dp),
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = colors.accent)
                Text("Signed in.", style = BodyStrong, color = colors.onContainer)
            }
            PrimaryAction(text = "Continue", onClick = onContinue)
        } else {
            PrimaryAction(text = "Sign in with YouTube Music", onClick = onSignIn)
            Spacer(Modifier.height(10.dp))
            QuietAction(text = "Continue without signing in", onClick = onSkip)
        }
    }
}

/**
 * The colour step.
 *
 * Every swatch is derived from whatever cover is loaded — nothing here is a stored sample — so the
 * choice is shown in the same terms it will be lived in.
 */
@Composable
private fun StepColour(
    selected: ColorFlavour,
    onPick: (ColorFlavour) -> Unit,
    onContinue: () -> Unit,
) {
    val colors = LocalExpressiveColors.current
    val cover = LocalArtworkColors.current
    Column(modifier = Modifier.fillMaxSize()) {
        Text("STEP 02", style = MetaLabel, color = colors.onContainerMuted)
        Spacer(Modifier.height(8.dp))
        Text("Pick a mood.", style = HeroTitle, color = colors.onContainer)
        Spacer(Modifier.height(14.dp))
        Text(
            "Verza takes its colours from whatever is playing. This decides how far to push them. " +
                "You can change it any time in Settings.",
            style = BodyText,
            color = colors.onContainerMuted,
        )
        Spacer(Modifier.height(22.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ColorFlavour.entries.forEach { flavour ->
                val preview = remember(cover, flavour) { expressiveColorsFrom(cover, flavour) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ShapeExtraLarge)
                        .border(
                            width = if (flavour == selected) 2.dp else 1.dp,
                            color = if (flavour == selected) colors.accent else colors.line,
                            shape = ShapeExtraLarge,
                        )
                        .selectable(
                            selected = flavour == selected,
                            onClick = { onPick(flavour) },
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(modifier = Modifier.clip(RoundedCornerShape(8.dp))) {
                        Box(Modifier.size(24.dp).background(preview.container))
                        Box(Modifier.size(24.dp).background(preview.surface))
                        Box(Modifier.size(24.dp).background(preview.accent))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(flavour.displayName, style = BodyStrong, color = colors.onContainer)
                        Text(flavour.blurb, style = BodyText, color = colors.onContainerMuted)
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        PrimaryAction(text = "Continue", onClick = onContinue)
    }
}

@Composable
private fun StepDone(onFinish: (takeTour: Boolean) -> Unit) {
    val colors = LocalExpressiveColors.current
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text("READY", style = MetaLabel, color = colors.onContainerMuted)
        Spacer(Modifier.height(8.dp))
        Text("You're set.", style = HeroDisplay, color = colors.onContainer)
        Spacer(Modifier.height(12.dp))
        Text(
            "There's a lot under the hood — want the quick tour?",
            style = BodyText,
            color = colors.onContainerMuted,
        )
        Spacer(Modifier.weight(1f))
        PrimaryAction(text = "Take the tour", onClick = { onFinish(true) })
        Spacer(Modifier.height(10.dp))
        QuietAction(text = "Jump straight in", onClick = { onFinish(false) })
    }
}

// ── shared ───────────────────────────────────────────────────────────────────

@Composable
private fun StepDots(current: Int, total: Int) {
    val colors = LocalExpressiveColors.current
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { i ->
            val active = i == current
            // The active dot stretches into a bar. Width is the only thing that moves, so the row
            // never reflows around it.
            Box(
                Modifier
                    .height(4.dp)
                    .width(if (active) 26.dp else 10.dp)
                    .clip(PillShape)
                    .background(if (active) colors.accent else colors.onContainerMuted.copy(alpha = 0.3f)),
            )
        }
    }
}

@Composable
private fun PrimaryAction(text: String, onClick: () -> Unit) {
    val colors = LocalExpressiveColors.current
    BouncyBar(
        text = text,
        container = colors.accent,
        content = colors.onAccent,
        onClick = onClick,
    )
}

@Composable
private fun QuietAction(text: String, onClick: () -> Unit) {
    val colors = LocalExpressiveColors.current
    BouncyBar(
        text = text,
        container = Color.Transparent,
        content = colors.onContainerMuted,
        onClick = onClick,
    )
}

/** A full-width pill that squashes on press — the app's standard button feedback. */
@Composable
private fun BouncyBar(
    text: String,
    container: Color,
    content: Color,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale = remember { Animatable(1f) }
    LaunchedEffect(pressed) {
        scale.animateTo(if (pressed) 0.96f else 1f, ExpressiveMotion.spatialFast())
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(scale.value)
            .clip(PillShape)
            .background(container)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .semantics { contentDescription = text },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = BodyStrong, color = content)
    }
}
