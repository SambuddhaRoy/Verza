package com.verza.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.verza.R
import com.verza.ui.expressive.BodyStrong
import com.verza.ui.expressive.BodyText
import com.verza.ui.expressive.ColorFlavour
import com.verza.ui.expressive.CoverWash
import com.verza.ui.expressive.ExpressiveMotion
import com.verza.ui.expressive.HeroDisplay
import com.verza.ui.expressive.HeroTitle
import com.verza.ui.expressive.LightStatusIcons
import com.verza.ui.expressive.LocalExpressiveColors
import com.verza.ui.expressive.PillShape
import com.verza.ui.expressive.ShapeLargeIncreased
import com.verza.ui.expressive.ShapeSmall
import com.verza.ui.expressive.SquircleShape
import com.verza.ui.expressive.expressiveColorsFrom
import com.verza.ui.theme.DesignScheme
import com.verza.ui.theme.LocalArtworkColors
import com.verza.ui.theme.offeredFlavours
import com.verza.ui.theme.seedColors

/**
 * First launch, button-driven, no swipe:
 *
 *  1. Welcome — the mark on the flowing background Now Playing stands on.
 *  2. Look    — Glass (chosen already) or Material, each shown as a little drawing of itself.
 *               Picking one restyles everything at once, this screen included.
 *  3. Colour  — Material only: how far to push the cover's colours. Glass has nothing to push.
 *  4. Sign in — optional; moves on by itself if a sign-in lands while it is showing.
 *  5. Done    — tour or straight in.
 *
 * A new install is already in Glass (see [DesignScheme.fromName]); completing onboarding writes the
 * design down. Steps travel like the app's pages: opaque, the new one sliding over the old.
 */
@Composable
fun OnboardingScreen(
    onSignIn: () -> Unit,
    onFinished: (takeTour: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val isSignedIn by viewModel.isSignedIn.collectAsStateWithLifecycle()
    val design by viewModel.designScheme.collectAsStateWithLifecycle()
    val colors = LocalExpressiveColors.current

    val steps = remember(design) {
        listOfNotNull(Step.WELCOME, Step.LOOK, Step.COLOUR.takeIf { design == DesignScheme.MATERIAL }, Step.SIGN_IN, Step.DONE)
    }
    // Saveable: signing in leaves this page for the sign-in one, and coming back must not start over.
    var step by rememberSaveable { mutableStateOf(Step.WELCOME) }
    var forward by remember { mutableStateOf(true) }
    fun go(to: Step) { forward = steps.indexOf(to) >= steps.indexOf(step); step = to }
    fun next() = go(steps[(steps.indexOf(step) + 1).coerceAtMost(steps.lastIndex)])

    LaunchedEffect(isSignedIn, step) {
        if (step == Step.SIGN_IN && isSignedIn) next()
    }
    BackHandler(enabled = step != Step.WELCOME) {
        go(steps[(steps.indexOf(step) - 1).coerceAtLeast(0)])
    }

    AnimatedContent(
        targetState = step,
        transitionSpec = { com.verza.ui.expressive.pushTransform(forward) },
        modifier = modifier.fillMaxSize(),
        label = "onboarding-step",
    ) { current ->
        if (current == Step.WELCOME) {
            StepWelcome(onContinue = ::next)
        } else {
            val shown = steps.filter { it != Step.WELCOME }
            StepPage(index = shown.indexOf(current), total = shown.size) {
                when (current) {
                    Step.LOOK -> StepLook(
                        selected = design,
                        onPick = viewModel::setDesignScheme,
                        onContinue = ::next,
                    )
                    Step.COLOUR -> StepColour(
                        selected = viewModel.colorFlavour.collectAsStateWithLifecycle().value,
                        onPick = viewModel::setColorFlavour,
                        onContinue = ::next,
                    )
                    Step.SIGN_IN -> StepSignIn(
                        isSignedIn = isSignedIn,
                        onSignIn = onSignIn,
                        onContinue = ::next,
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

private enum class Step { WELCOME, LOOK, COLOUR, SIGN_IN, DONE }

// ── steps ────────────────────────────────────────────────────────────────────

/** The mark on the flowing wash, in white whatever the theme: the wash is always dark. */
@Composable
private fun StepWelcome(onContinue: () -> Unit) {
    LightStatusIcons()
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, ExpressiveMotion.spatialSlow()) }
    Box(Modifier.fillMaxSize().background(Color(0xFF101012))) {
        CoverWash(R.drawable.onboarding_wash, motion = true, Modifier.fillMaxSize())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 28.dp, vertical = 28.dp),
        ) {
            Spacer(Modifier.weight(1f))
            Column(Modifier.offset(y = ((1f - enter.value) * 24).dp)) {
                Image(
                    painter = painterResource(id = R.drawable.ic_verza_glyph),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Color.White),
                    modifier = Modifier.size(88.dp).offset(x = (-14).dp),
                )
                Spacer(Modifier.height(8.dp))
                Text("Verza", style = HeroDisplay.copy(fontSize = 60.sp, lineHeight = 64.sp), color = Color.White)
                Spacer(Modifier.height(10.dp))
                Text(
                    "YouTube Music, coloured by every cover.",
                    style = BodyText.copy(fontSize = 18.sp, lineHeight = 26.sp),
                    color = Color.White.copy(alpha = 0.78f),
                )
            }
            Spacer(Modifier.height(40.dp))
            WideButton(text = "Get started", container = Color.White, content = Color.Black, onClick = onContinue)
        }
    }
}

/** Every step after the welcome: the app's own ground, the progress, then the step. */
@Composable
private fun StepPage(index: Int, total: Int, content: @Composable () -> Unit) {
    val colors = LocalExpressiveColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.container)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        // Centred and capped. Full-width buttons across a tablet are a pill the length of the screen
        // with one word in the middle of it.
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
        ) {
            StepDots(current = index, total = total)
            Spacer(Modifier.height(28.dp))
            Box(Modifier.weight(1f)) { content() }
        }
    }
}

@Composable
private fun StepLook(selected: DesignScheme, onPick: (DesignScheme) -> Unit, onContinue: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        StepHeader(
            eyebrow = "Your look",
            title = "Pick a look.",
            body = "Same features either way. Change it any time in Settings.",
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LookCard(
                name = "Glass",
                blurb = "Clean and quiet, with glass controls",
                recommended = true,
                selected = selected == DesignScheme.GLASS,
                onClick = { onPick(DesignScheme.GLASS) },
                modifier = Modifier.weight(1f),
            ) { GlassSketch() }
            LookCard(
                name = "Material",
                blurb = "Bold colour from every cover",
                recommended = false,
                selected = selected == DesignScheme.MATERIAL,
                onClick = { onPick(DesignScheme.MATERIAL) },
                modifier = Modifier.weight(1f),
            ) { MaterialSketch() }
        }
        Spacer(Modifier.weight(1f))
        PrimaryAction(text = "Continue", onClick = onContinue)
    }
}

@Composable
private fun LookCard(
    name: String,
    blurb: String,
    recommended: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sketch: @Composable BoxScope.() -> Unit,
) {
    val colors = LocalExpressiveColors.current
    val shape = SquircleShape(22.dp)
    // Not clipped as a whole: the card's corner cut the first letter off the lines below it.
    Column(
        modifier = modifier
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        // No label of its own: the card's lines are merged into it, and a label repeating them was
        // read out twice.
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.62f)
                .clip(shape)
                .border(if (selected) 3.dp else 1.dp, if (selected) colors.accent else colors.line, shape),
        ) {
            sketch()
            if (selected) Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .size(26.dp)
                    .clip(PillShape)
                    .background(colors.accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = colors.onAccent, modifier = Modifier.size(17.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
            Text(name, style = BodyStrong.copy(fontSize = 17.sp), color = colors.onContainer)
            if (recommended) {
                Spacer(Modifier.width(8.dp))
                Text(
                    "RECOMMENDED",
                    style = BodyStrong.copy(fontSize = 10.sp, letterSpacing = 0.8.sp),
                    color = colors.accent,
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(blurb, style = BodyText.copy(fontSize = 14.sp), color = colors.onContainerMuted, modifier = Modifier.padding(horizontal = 4.dp))
    }
}

/**
 * A drawing of Glass's Now Playing: a dark flowing wash, a cover, two lines of type and the dock
 * floating as glass. Fixed colours, so it shows Glass whichever design is on.
 */
@Composable
private fun BoxScope.GlassSketch() {
    Box(
        Modifier
            .matchParentSize()
            .background(Color(0xFF15111B))
            .drawBehind {
                drawCircle(
                    Brush.radialGradient(listOf(Color(0xCCE5233F), Color.Transparent), Offset(size.width * 0.15f, size.height * 0.25f), size.width * 0.9f),
                    radius = size.width * 0.9f, center = Offset(size.width * 0.15f, size.height * 0.25f),
                )
                drawCircle(
                    Brush.radialGradient(listOf(Color(0xB35B3FD6), Color.Transparent), Offset(size.width * 0.95f, size.height * 0.7f), size.width),
                    radius = size.width, center = Offset(size.width * 0.95f, size.height * 0.7f),
                )
            },
    )
    Column(Modifier.matchParentSize().padding(14.dp)) {
        Spacer(Modifier.height(18.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(SquircleShape(12.dp))
                .background(Brush.linearGradient(listOf(Color(0xFFF29D38), Color(0xFFE5233F), Color(0xFF8E1F5C)))),
        )
        Spacer(Modifier.height(12.dp))
        Line(0.7f, 8.dp, Color.White)
        Spacer(Modifier.height(6.dp))
        Line(0.45f, 6.dp, Color.White.copy(alpha = 0.55f))
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .fillMaxWidth()
                .height(26.dp)
                .clip(PillShape)
                .background(Color.White.copy(alpha = 0.16f))
                .border(1.dp, Color.White.copy(alpha = 0.28f), PillShape),
            contentAlignment = Alignment.Center,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                repeat(3) { i -> Box(Modifier.size(if (i == 1) 9.dp else 7.dp).clip(PillShape).background(Color.White.copy(alpha = if (i == 1) 1f else 0.6f))) }
            }
        }
    }
}

/** A drawing of Material: the cover's colour everywhere, round tiles, a filled pill in the nav. */
@Composable
private fun BoxScope.MaterialSketch() {
    Box(Modifier.matchParentSize().background(Color(0xFFF7E6D8)))
    Column(Modifier.matchParentSize().padding(12.dp)) {
        Spacer(Modifier.height(14.dp))
        Line(0.55f, 10.dp, Color(0xFF4A2511))
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().aspectRatio(1.35f).clip(RoundedCornerShape(20.dp)).background(Color(0xFFE8833A)))
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(16.dp)).background(Color(0xFF2BB8A6)))
            Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(16.dp)).background(Color(0xFF7B5CD6)))
        }
        Spacer(Modifier.weight(1f))
        Box(
            Modifier.fillMaxWidth().height(26.dp).clip(PillShape).background(Color(0xFFEBCDB6)),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(Modifier.padding(start = 6.dp).width(30.dp).height(16.dp).clip(PillShape).background(Color(0xFF4A2511)))
        }
    }
}

@Composable
private fun Line(fraction: Float, height: Dp, color: Color) {
    Box(Modifier.fillMaxWidth(fraction).height(height).clip(PillShape).background(color))
}

/**
 * The colour step, Material only.
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
        StepHeader(
            eyebrow = "Colour",
            title = "How much colour?",
            body = "Material takes its colours from the cover of what's playing, or from your phone. Choose how strong.",
        )
        Spacer(Modifier.height(22.dp))

        val context = LocalContext.current
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            offeredFlavours.forEach { flavour ->
                val preview = remember(cover, flavour) { expressiveColorsFrom(seedColors(context, flavour, cover), flavour) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ShapeLargeIncreased)
                        .border(
                            width = if (flavour == selected) 2.dp else 1.dp,
                            color = if (flavour == selected) colors.accent else colors.line,
                            shape = ShapeLargeIncreased,
                        )
                        .selectable(
                            selected = flavour == selected,
                            onClick = { onPick(flavour) },
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(modifier = Modifier.clip(ShapeSmall)) {
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
private fun StepSignIn(
    isSignedIn: Boolean,
    onSignIn: () -> Unit,
    onContinue: () -> Unit,
) {
    val colors = LocalExpressiveColors.current
    Column(modifier = Modifier.fillMaxSize()) {
        StepHeader(
            eyebrow = "Your music",
            title = "Bring your library.",
            body = "Sign in with YouTube Music to sync your feed, playlists and likes. " +
                "Likes, playlists and downloads also work without an account.",
        )
        Spacer(Modifier.height(28.dp))
        Points(
            Point(Icons.Rounded.Home, "Your home feed", "Quick picks and mixes"),
            Point(Icons.Rounded.LibraryMusic, "Your library", "Playlists, albums and liked songs"),
            Point(Icons.Rounded.Favorite, "Likes that sync", "Likes in Verza show up on YouTube Music"),
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
            QuietAction(text = "Not now", onClick = onContinue)
        }
    }
}

@Composable
private fun StepDone(onFinish: (takeTour: Boolean) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        StepHeader(
            eyebrow = "Ready",
            title = "You're set.",
            body = "The tour shows where everything is. It takes a minute.",
        )
        Spacer(Modifier.height(28.dp))
        Points(
            Point(Icons.Rounded.AutoAwesome, "Mixes from your listening", "Updated as you listen"),
            Point(Icons.Rounded.Lyrics, "Synced lyrics", "Highlighted as the song plays"),
            Point(Icons.Rounded.GraphicEq, "Equaliser and downloads", "Tune the sound, save songs for offline"),
        )
        Spacer(Modifier.weight(1f))
        PrimaryAction(text = "Take the tour", onClick = { onFinish(true) })
        Spacer(Modifier.height(10.dp))
        QuietAction(text = "Start listening", onClick = { onFinish(false) })
    }
}

// ── shared ───────────────────────────────────────────────────────────────────

private class Point(val icon: androidx.compose.ui.graphics.vector.ImageVector, val title: String, val line: String)

/** A short list of what a step offers: an icon on a tint of the accent, a title, one line. */
@Composable
private fun Points(vararg points: Point) {
    val colors = LocalExpressiveColors.current
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        points.forEach { p ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(SquircleShape(13.dp)).background(colors.accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(p.icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(p.title, style = BodyStrong, color = colors.onContainer)
                    Text(p.line, style = BodyText.copy(fontSize = 14.sp), color = colors.onContainerMuted)
                }
            }
        }
    }
}

/** The app's masthead, as on Home, Library and Search: a line in the accent over the title. */
@Composable
private fun StepHeader(eyebrow: String, title: String, body: String) {
    val colors = LocalExpressiveColors.current
    GlassMasthead(eyebrow, title, titleLines = 3)
    Spacer(Modifier.height(12.dp))
    Text(body, style = BodyText.copy(fontSize = 16.sp, lineHeight = 23.sp), color = colors.onContainerMuted)
}

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
                    .background(if (i <= current) colors.accent else colors.onContainerMuted.copy(alpha = 0.3f)),
            )
        }
    }
}

@Composable
private fun PrimaryAction(text: String, onClick: () -> Unit) {
    val colors = LocalExpressiveColors.current
    WideButton(text = text, container = colors.accent, content = colors.onAccent, onClick = onClick)
}

@Composable
private fun QuietAction(text: String, onClick: () -> Unit) {
    val colors = LocalExpressiveColors.current
    WideButton(text = text, container = Color.Transparent, content = colors.onContainerMuted, onClick = onClick)
}

/** A full-width pill that squashes on press — the app's standard button feedback. The tour uses it too. */
@Composable
internal fun WideButton(
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
            // The text inside is the label; one set here as well was read out twice.
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = BodyStrong, color = content)
    }
}
