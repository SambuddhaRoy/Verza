package com.verza.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.verza.ui.theme.EditorialEyebrow

/**
 * Editorial section header — replaces the older `SectionHeader` lozenge.
 *
 * Renders the section label in a small-caps-feeling style (uppercased + tracked),
 * with a short accent rule beneath in the theme's primary colour. Used across
 * Home, Library, Search, Settings, Now Playing, etc. so the section breaks read
 * like article pull-quotes rather than M3 list dividers.
 *
 * The label is intentionally rendered uppercase (not relying on a CSS-like
 * `text-transform`); Android typography APIs don't expose true smallcaps for
 * web fonts, and synthetic smallcaps look amateurish.
 */
@Composable
fun EditorialSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    accentWidth: Int = 24,
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier.padding(horizontal = 24.dp)) {
        Box(
            Modifier
                .width(accentWidth.dp)
                .height(1.dp)
                .clip(RoundedCornerShape(0.5.dp))
                .background(colors.primary),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = title.uppercase(),
            style = EditorialEyebrow,
            color = colors.onBackground,
        )
    }
}
