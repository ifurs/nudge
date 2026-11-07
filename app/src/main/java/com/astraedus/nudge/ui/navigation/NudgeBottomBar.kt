package com.astraedus.nudge.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Modifier
import com.astraedus.nudge.ui.components.qaTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

/**
 * The Material 3 bottom bar: Home, Apps, Stats, Settings. Stateless; [NudgeBottomBarScaffold] and the
 * screenshot gallery both draw it.
 *
 * A label is always shown with the icon (no icon-only items), on one line, and all four share one
 * font size: the largest at which the longest of them fits its item (see [fittingLabelSize]).
 * Ukrainian runs longer than English ("Налаштування"), and at a larger font size on a 360dp phone
 * the stock labels were cut to "Налашт…". Four labels at four sizes would look broken, so the
 * longest decides.
 */
@Composable
fun NudgeBottomBar(
    selected: TopLevelDestination?,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    // The labels follow the user's font size up to MAX_LABEL_FONT_SCALE and no further: each item
    // is a quarter of the screen, and past that even the smallest size that is still legible does
    // not hold "Налаштування" on a 360dp phone. Everything else in the app keeps scaling.
    val density = LocalDensity.current
    val capped = remember(density) {
        Density(density.density, density.fontScale.coerceAtMost(MAX_LABEL_FONT_SCALE))
    }
    CompositionLocalProvider(LocalDensity provides capped) {
        BottomBarItems(selected, onSelect, modifier)
    }
}

@Composable
private fun BottomBarItems(
    selected: TopLevelDestination?,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier
) {
    val labels = TopLevelDestination.entries.map { stringResource(it.labelRes) }
    val base = MaterialTheme.typography.labelMedium
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier) {
        val labelSize = remember(labels, base, maxWidth, density) {
            val itemWidth = with(density) { (maxWidth / TopLevelDestination.entries.size).roundToPx() }
            val available = itemWidth - with(density) { ITEM_LABEL_PADDING.roundToPx() }
            fittingLabelSize(labels, base.fontSize) { text, size ->
                measurer.measure(
                    text = text,
                    style = base.copy(fontSize = size),
                    maxLines = 1,
                    softWrap = false
                ).size.width <= available
            }
        }
        NavigationBar {
            TopLevelDestination.entries.forEachIndexed { index, destination ->
                val isSelected = destination == selected
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onSelect(destination) },
                    modifier = Modifier.qaTag(destination.qaTag),
                    icon = {
                        // The label below says it; a second description would be read twice.
                        Icon(
                            if (isSelected) destination.selectedIcon else destination.icon,
                            contentDescription = null
                        )
                    },
                    label = {
                        Text(
                            labels[index],
                            style = base.copy(fontSize = labelSize),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        }
    }
}

/**
 * The largest size, from [start] down in 5% steps to [MIN_LABEL_SIZE], at which every label
 * satisfies [fits]. Pure apart from [fits], so the stepping is testable without a screen.
 */
internal fun fittingLabelSize(
    labels: List<String>,
    start: TextUnit,
    fits: (String, TextUnit) -> Boolean
): TextUnit {
    var size = start
    while (size > MIN_LABEL_SIZE && !labels.all { fits(it, size) }) size *= 0.95f
    return size
}

/** What an item keeps for itself on both sides of its label. Measured on the stock item. */
private val ITEM_LABEL_PADDING = 20.dp

private val MIN_LABEL_SIZE = 8.sp

/** The largest font scale the bar's labels follow. */
private const val MAX_LABEL_FONT_SCALE = 1.15f

/**
 * Wraps the app's screens with the bottom bar, which shows only on the four top-level routes.
 *
 * The screens inside keep their own Scaffolds (and so their own top bars and status-bar insets), so
 * this Scaffold takes no insets of its own: the bar itself pads for the navigation bar, and the
 * content area it leaves is marked consumed, so a screen's Scaffold does not add the navigation-bar
 * inset a second time above the bar. With no bar (a detail screen) the padding is zero and nothing
 * is consumed: those screens lay out exactly as they did before.
 */
@Composable
fun NudgeBottomBarScaffold(
    currentRoute: String?,
    onSelect: (TopLevelDestination) -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (TopLevelDestination.showsBottomBar(currentRoute)) {
                NudgeBottomBar(
                    selected = TopLevelDestination.fromRoute(currentRoute),
                    onSelect = onSelect
                )
            }
        }
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .consumeWindowInsets(inner)
        ) {
            content()
        }
    }
}

/**
 * Go to a top-level destination the standard way: pop back to Home (saving the state of what is
 * popped), one copy on top (`launchSingleTop`), and restore the destination's saved state, so
 * switching tabs keeps each one's scroll position and a tab never stacks on itself.
 */
fun NavController.navigateTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
