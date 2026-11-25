package com.verza.ui.look

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.verza.R

/*
 * The faces the looks are set in. All are under the SIL Open Font License, from Google Fonts' own
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
    val InterDisplay = family(R.font.inter_variable, 400, 600, 700, 800, 900) { arrayOf(FontVariation.Setting("opsz", 32f)) }

    val ChakraPetch = FontFamily(Font(R.font.chakra_petch_medium, FontWeight.Medium), Font(R.font.chakra_petch_bold, FontWeight.Bold))
    val JetBrainsMono = family(R.font.jetbrains_mono, 400, 500, 700, 800)
    val Anton = FontFamily(Font(R.font.anton))
    val AlfaSlab = FontFamily(Font(R.font.alfa_slab_one))
    val Unbounded = family(R.font.unbounded, 300, 500, 700, 900)
    val Poppins = FontFamily(
        Font(R.font.poppins_regular, FontWeight.Normal), Font(R.font.poppins_medium, FontWeight.Medium),
        Font(R.font.poppins_semibold, FontWeight.SemiBold), Font(R.font.poppins_bold, FontWeight.Bold),
    )
    val Bricolage = family(R.font.bricolage, 400, 600, 800) { arrayOf(FontVariation.Setting("opsz", 96f), FontVariation.width(85f)) }
    val BricolageText = family(R.font.bricolage, 400, 500, 700) { arrayOf(FontVariation.Setting("opsz", 14f), FontVariation.width(100f)) }
    val Caveat = family(R.font.caveat, 500, 700)
    val Orbitron = family(R.font.orbitron, 500, 700, 900)
    val ShareTechMono = FontFamily(Font(R.font.share_tech_mono))
    val InterTight = family(R.font.inter_tight, 400, 500, 700, 800, 900)
    val SpaceGrotesk = family(R.font.space_grotesk, 300, 400, 500, 700)
    /** Archivo stretched to its widest, for Minimal's titles. */
    val ArchivoWide = family(R.font.archivo, 600, 800, 900) { arrayOf(FontVariation.width(125f)) }
    val Archivo = family(R.font.archivo, 400, 500, 700) { arrayOf(FontVariation.width(100f)) }
    val ArchivoNarrow = family(R.font.archivo, 500, 700, 900) { arrayOf(FontVariation.width(75f)) }
    val SpaceMono = FontFamily(Font(R.font.space_mono, FontWeight.Normal), Font(R.font.space_mono_bold, FontWeight.Bold))
    val Pixelify = family(R.font.pixelify_sans, 400, 500, 700)
    val PlusJakarta = family(R.font.plus_jakarta, 400, 500, 700, 800)
    val Baloo = family(R.font.baloo2, 500, 600, 700, 800)
    val Fraunces = family(R.font.fraunces, 400, 600, 900) { arrayOf(FontVariation.Setting("opsz", 72f), FontVariation.Setting("SOFT", 100f), FontVariation.Setting("WONK", 1f)) }

    /**
     * Roboto Flex at any width (25 to 151) and weight (100 to 1000): the variable-font look moves both
     * with the music. Quantise [width] and [weight] before calling: each pair is a typeface of its own.
     */
    @OptIn(ExperimentalTextApi::class)
    fun flex(weight: Int, width: Float = 100f, slant: Float = 0f): FontFamily = flexCache.getOrPut(Triple(weight, width, slant)) {
        FontFamily(
            Font(
                R.font.roboto_flex,
                weight = FontWeight(weight.coerceIn(100, 1000)),
                variationSettings = FontVariation.Settings(
                    FontVariation.weight(weight.coerceIn(100, 1000)), FontVariation.width(width.coerceIn(25f, 151f)),
                    FontVariation.slant(slant.coerceIn(-10f, 0f)), FontVariation.Setting("opsz", 72f),
                ),
            ),
        )
    }
    private val flexCache = HashMap<Triple<Int, Float, Float>, FontFamily>()
}
