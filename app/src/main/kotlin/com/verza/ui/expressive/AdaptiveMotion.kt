package com.verza.ui.expressive

import androidx.compose.animation.core.DeferredTargetAnimation
import androidx.compose.animation.core.ExperimentalAnimatableApi
import androidx.compose.animation.core.VectorConverter
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ApproachLayoutModifierNode
import androidx.compose.ui.layout.ApproachMeasureScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round

/**
 * Springs an element from where it was to where the new layout puts it.
 *
 * When a tablet rotates or a fold opens, the window changes size in a single frame and every layout
 * recomputes at once, so without this the whole screen simply cuts to its new arrangement. With it,
 * each tagged element keeps its old size and position for that frame and then glides to the new
 * ones: the cover drifts across to its pane and grows, the controls slide in beside it. The screen
 * rearranges itself in front of you, which is the effect that makes rotation on an iPad feel
 * physical rather than like a page reload.
 *
 * How it works: inside a [LookaheadScope], Compose first measures the destination layout without
 * showing it. This node reads each element's destination size and position from that pass and
 * animates towards them, and the visible layout uses the animated values until they arrive.
 *
 * Uses the slow spring. On a rotation the system cross-fades a screenshot of the old orientation
 * over the app, and the default spring had already settled underneath it, so nothing visibly moved.
 *
 * Only animates when the destination changes, so it costs nothing in a layout that is sitting still.
 * The scope has to be inside anything that scrolls; otherwise scrolling moves every destination and
 * the content trails behind your finger.
 */
fun Modifier.animateBoundsIn(scope: LookaheadScope): Modifier = this then AnimateBoundsElement(scope)

private data class AnimateBoundsElement(val scope: LookaheadScope) : ModifierNodeElement<AnimateBoundsNode>() {
    override fun create() = AnimateBoundsNode(scope)

    override fun update(node: AnimateBoundsNode) {
        node.lookahead = scope
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "animateBoundsIn"
    }
}

@OptIn(ExperimentalAnimatableApi::class, ExperimentalComposeUiApi::class)
private class AnimateBoundsNode(var lookahead: LookaheadScope) : ApproachLayoutModifierNode, Modifier.Node() {

    // The first target each receives is taken as-is, so an element appears where it belongs on first
    // layout and only animates on later changes.
    private val size = DeferredTargetAnimation(IntSize.VectorConverter)
    private val offset = DeferredTargetAnimation(IntOffset.VectorConverter)

    override fun isMeasurementApproachInProgress(lookaheadSize: IntSize): Boolean {
        size.updateTarget(lookaheadSize, coroutineScope, ExpressiveMotion.spatialSlow<IntSize>())
        return !size.isIdle
    }

    override fun Placeable.PlacementScope.isPlacementApproachInProgress(
        lookaheadCoordinates: LayoutCoordinates,
    ): Boolean {
        val target = with(lookahead) {
            lookaheadScopeCoordinates.localLookaheadPositionOf(lookaheadCoordinates).round()
        }
        offset.updateTarget(target, coroutineScope, ExpressiveMotion.spatialSlow<IntOffset>())
        return !offset.isIdle
    }

    override fun ApproachMeasureScope.approachMeasure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val animated = size.updateTarget(lookaheadSize, coroutineScope, ExpressiveMotion.spatialSlow<IntSize>())
        // A spring overshoots, and an overshoot below zero is not a size.
        val width = animated.width.coerceAtLeast(0)
        val height = animated.height.coerceAtLeast(0)
        val placeable = measurable.measure(Constraints.fixed(width, height))
        return layout(width, height) {
            val coordinates = coordinates
            if (coordinates == null) {
                placeable.place(0, 0)
                return@layout
            }
            val target = with(lookahead) {
                lookaheadScopeCoordinates.localLookaheadPositionOf(coordinates).round()
            }
            val current = offset.updateTarget(target, coroutineScope, ExpressiveMotion.spatialSlow<IntOffset>())
            val laidOutAt = with(lookahead) {
                lookaheadScopeCoordinates.localPositionOf(coordinates, Offset.Zero).round()
            }
            // Draw at the animated position by offsetting from wherever the real layout put us.
            placeable.place(current - laidOutAt)
        }
    }
}
