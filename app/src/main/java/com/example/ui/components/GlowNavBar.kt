package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Item model for [GlowNavBar].
 */
data class GlowNavItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

// Palette inspired by "Navigation Tabs V2" (dark glass + blue/violet neon glow)
private val NavBgTop = Color(0xFF151933)
private val NavBgBottom = Color(0xFF0A0C18)
val GlowNavSelectedBlue = Color(0xFF4F7CFF)
private val NeonCyan = Color(0xFF38BDF8)
val GlowNavSelectedViolet = Color(0xFF8B5CF6)
private val InactiveTint = Color(0xFF8A90A8)

/**
 * Floating dark pill navigation bar with a sliding neon-glow indicator,
 * spring physics, press-scale and haptic micro-interactions (v8.9.7).
 */
@Composable
fun GlowNavBar(
    items: List<GlowNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val containerShape = RoundedCornerShape(28.dp)
    val indicatorShape = RoundedCornerShape(20.dp)
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .shadow(
                elevation = 20.dp,
                shape = containerShape,
                ambientColor = GlowNavSelectedViolet.copy(alpha = 0.55f),
                spotColor = GlowNavSelectedBlue.copy(alpha = 0.65f)
            )
            .background(
                brush = Brush.verticalGradient(listOf(NavBgTop, NavBgBottom)),
                shape = containerShape
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        GlowNavSelectedBlue.copy(alpha = 0.55f),
                        GlowNavSelectedViolet.copy(alpha = 0.65f),
                        GlowNavSelectedBlue.copy(alpha = 0.40f)
                    )
                ),
                shape = containerShape
            )
            .padding(6.dp)
            .testTag("bottom_nav_bar")
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
        ) {
            val count = items.size.coerceAtLeast(1)
            val itemWidth = maxWidth / count
            val safeIndex = selectedIndex.coerceIn(-1, count - 1)
            val visible = safeIndex >= 0

            val indicatorX by animateDpAsState(
                targetValue = itemWidth * safeIndex.coerceAtLeast(0),
                animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f),
                label = "navIndicatorX"
            )
            val indicatorAlpha by animateFloatAsState(
                targetValue = if (visible) 1f else 0f,
                label = "navIndicatorAlpha"
            )

            // Soft neon light leak under the active tab (drawn outside bounds on purpose)
            Box(
                modifier = Modifier
                    .offset(x = indicatorX)
                    .width(itemWidth)
                    .fillMaxHeight()
                    .graphicsLayer { alpha = indicatorAlpha }
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    GlowNavSelectedViolet.copy(alpha = 0.45f),
                                    GlowNavSelectedBlue.copy(alpha = 0.18f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width / 2f, size.height * 0.95f),
                                radius = size.width * 0.95f
                            ),
                            radius = size.width * 0.95f,
                            center = Offset(size.width / 2f, size.height * 0.95f)
                        )
                    }
            )

            // Sliding glass indicator with gradient neon border
            Box(
                modifier = Modifier
                    .offset(x = indicatorX)
                    .width(itemWidth)
                    .fillMaxHeight()
                    .padding(horizontal = 2.dp)
                    .graphicsLayer { alpha = indicatorAlpha }
                    .shadow(
                        elevation = 12.dp,
                        shape = indicatorShape,
                        ambientColor = GlowNavSelectedViolet,
                        spotColor = GlowNavSelectedBlue
                    )
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(
                                GlowNavSelectedBlue.copy(alpha = 0.38f),
                                GlowNavSelectedViolet.copy(alpha = 0.30f)
                            )
                        ),
                        shape = indicatorShape
                    )
                    .border(
                        width = 1.3.dp,
                        brush = Brush.linearGradient(listOf(NeonCyan, GlowNavSelectedBlue, GlowNavSelectedViolet)),
                        shape = indicatorShape
                    )
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    GlowNavCell(
                        item = item,
                        selected = index == safeIndex,
                        onClick = {
                            if (index != safeIndex) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            onSelect(index)
                        },
                        modifier = Modifier
                            .width(itemWidth)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
private fun GlowNavCell(
    item: GlowNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
        label = "navPressScale"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.14f else 1f,
        animationSpec = spring(dampingRatio = 0.42f, stiffness = 520f),
        label = "navIconScale"
    )
    val iconLift by animateDpAsState(
        targetValue = if (selected) (-1).dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "navIconLift"
    )
    val tint by animateColorAsState(
        targetValue = if (selected) Color.White else InactiveTint,
        label = "navTint"
    )

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .testTag(item.tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
            contentDescription = item.title,
            tint = tint,
            modifier = Modifier
                .offset(y = iconLift)
                .size(22.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                }
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = item.title,
            color = tint,
            fontSize = 10.5.sp,
            letterSpacing = (-0.2).sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}
