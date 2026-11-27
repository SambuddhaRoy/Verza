package com.verza.ui.look

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.verza.R

/*
 * The faces the looks are set in, all under the SIL Open Font License from Google Fonts' own
 * repository; their licences ship in assets/licenses. Variable fonts get one Font per weight used,
 * each pinned to its axes, since Compose picks a Font by weight.
 */

@OptIn(ExperimentalTextApi::class)
private fun variable(res: Int, weight: Int, vararg axes: FontVariation.Setting) = Font(
    res,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight), *axes),
)

private fun family(res: Int, vararg weights: Int, axes: () -> Array<FontVariation.Setting> = { emptyArray() }) =
    FontFamily(weights.map { variable(res, it, *axes()) })

object Fonts {
    // Inter has an optical-size axis (14 to 32): names and headings use the 32 end, bold, so a large
    // title is drawn for its size rather than body text zoomed in. (Until 2.8.0 they were serifs,
    // Instrument Serif and Newsreader; the listener asked for a bold sans.)
    val InterText = family(R.font.inter_variable, 400, 500, 600, 700) { arrayOf(FontVariation.Setting("opsz", 14f)) }
    val InterDisplay = family(R.font.inter_variable, 400, 500, 600, 700, 800, 900) { arrayOf(FontVariation.Setting("opsz", 32f)) }
}
