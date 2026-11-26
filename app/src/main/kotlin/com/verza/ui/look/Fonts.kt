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
    // Inter has an optical-size axis (14 to 32): titles use the 32 end, so a large title isn't body text zoomed in.
    val InterText = family(R.font.inter_variable, 400, 500, 600, 700) { arrayOf(FontVariation.Setting("opsz", 14f)) }
    val InterDisplay = family(R.font.inter_variable, 400, 500, 600, 700, 800, 900) { arrayOf(FontVariation.Setting("opsz", 32f)) }
    /** Names and headings: a narrow editorial serif, so the large type isn't Inter set big. */
    val Serif = FontFamily(Font(R.font.instrument_serif))
}
