package one.volkov.apps.tacita.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import one.volkov.apps.tacita.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CollapsibleHeader(
    enabled: Boolean,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
) {
    val state = scrollBehavior.state
    Surface(
        modifier = modifier.draggable(
            orientation = Orientation.Vertical,
            state = rememberDraggableState { state.heightOffset += it },
            onDragStopped = { velocity ->
                // Reuse Material's fling and snap behavior for drags on the header too.
                scrollBehavior.nestedScrollConnection.onPostFling(Velocity.Zero, Velocity(0f, velocity))
            },
        ),
        color = lerp(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.surfaceContainer,
            state.collapsedFraction,
        ),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Layout(
            modifier = Modifier.fillMaxWidth()
                .windowInsetsPadding(TopAppBarDefaults.windowInsets)
                .clipToBounds().padding(horizontal = 16.dp),
            content = {
                Image(painterResource(R.drawable.tacita_header_icon), contentDescription = null)
                Text(
                    "Tacita",
                    modifier = Modifier.semantics {
                        heading()
                        stateDescription = if (enabled) "Active" else "Disabled"
                    },
                    fontSize = 32.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    "Lower media volume when your screen turns off.",
                    modifier = if (state.collapsedFraction >= 0.5f) Modifier.clearAndSetSemantics {} else Modifier,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                )
                StatusChip(
                    enabled = enabled,
                    modifier = if (state.collapsedFraction >= 0.5f) Modifier.clearAndSetSemantics {} else Modifier,
                )
                // One dot moves from the status chip to the title throughout collapse.
                Box(
                    Modifier.size(8.dp).testTag("statusDot").background(
                        if (enabled) Color(0xFF2E7D32) else lerp(
                            MaterialTheme.colorScheme.onSurface,
                            MaterialTheme.colorScheme.onSurfaceVariant,
                            1f - state.collapsedFraction,
                        ),
                        CircleShape,
                    ),
                )
            },
        ) { measurables, constraints ->
            val expandedIconSize = 76.dp.roundToPx()
            val collapsedIconSize = 48.dp.roundToPx()
            val gap = 16.dp.roundToPx()
            val detailsGap = 8.dp.roundToPx()
            val expandedPadding = 16.dp.roundToPx()
            val collapsedPadding = 8.dp.roundToPx()
            val collapsedTitleScale = 22f / 32f

            // Measure at the expanded width so wrapping and the scroll range stay stable in motion.
            val dotTitleSpace = 16.dp.roundToPx()
            val textConstraints = Constraints(maxWidth = (constraints.maxWidth - expandedIconSize - gap - dotTitleSpace).coerceAtLeast(0))
            val title = measurables[1].measure(textConstraints)
            val subtitle = measurables[2].measure(textConstraints)
            val status = measurables[3].measure(textConstraints)
            val dot = measurables[4].measure(Constraints.fixed(8.dp.roundToPx(), 8.dp.roundToPx()))
            val statusGap = 12.dp.roundToPx()
            val detailsHeight = subtitle.height + statusGap + status.height
            val collapsedTitleHeight = (title.height * collapsedTitleScale).toInt()
            val collapsedHeight = constraints.constrainHeight(
                maxOf(64.dp.roundToPx(), maxOf(collapsedIconSize, collapsedTitleHeight) + collapsedPadding * 2),
            )
            val expandedHeight = constraints.constrainHeight(
                maxOf(expandedIconSize, title.height + detailsGap + detailsHeight) + expandedPadding * 2,
            ).coerceAtLeast(collapsedHeight)
            val heightOffsetLimit = (collapsedHeight - expandedHeight).toFloat()
            if (state.heightOffsetLimit != heightOffsetLimit) {
                val collapsedFraction = state.collapsedFraction
                state.heightOffsetLimit = heightOffsetLimit
                state.heightOffset = heightOffsetLimit * collapsedFraction
            }

            val expandedFraction = 1f - state.collapsedFraction
            val height = lerp(collapsedHeight, expandedHeight, expandedFraction)
            val iconSize = lerp(collapsedIconSize, expandedIconSize, expandedFraction)
            val icon = measurables[0].measure(Constraints.fixed(iconSize, iconSize))
            val titleScale = lerp(collapsedTitleScale, 1f, expandedFraction)
            val titleY = lerp((collapsedHeight - collapsedTitleHeight) / 2, expandedPadding, expandedFraction)
            val textX = iconSize + gap
            val titleOrigin = TransformOrigin(if (layoutDirection == LayoutDirection.Ltr) 0f else 1f, 0f)
            val subtitleY = titleY + (title.height * titleScale).toInt() + detailsGap
            val statusY = subtitleY + subtitle.height + statusGap

            layout(constraints.maxWidth, height) {
                icon.placeRelative(0, lerp((collapsedHeight - collapsedIconSize) / 2, expandedPadding, expandedFraction))
                title.placeRelativeWithLayer(textX, titleY) {
                    scaleX = titleScale
                    scaleY = titleScale
                    transformOrigin = titleOrigin
                }
                // Expanded details never determine the compact toolbar's height or remain accessible.
                if (expandedFraction > 0.5f) {
                    subtitle.placeRelativeWithLayer(textX, subtitleY) {
                        alpha = (expandedFraction - 0.5f) * 2f
                    }
                    status.placeRelativeWithLayer(textX, statusY) {
                        alpha = (expandedFraction - 0.5f) * 2f
                    }
                }
                dot.placeRelative(
                    lerp(textX + (title.width * titleScale).toInt() + 8.dp.roundToPx(), textX + 10.dp.roundToPx(), expandedFraction),
                    lerp(titleY + ((title.height * titleScale - dot.height) / 2).toInt(), statusY + (status.height - dot.height) / 2, expandedFraction),
                )
            }
        }
    }
}

@Composable
private fun StatusChip(enabled: Boolean, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp)) {
        Text(
            if (enabled) "Tacita is active" else "Disabled",
            modifier = Modifier.padding(start = 26.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}
