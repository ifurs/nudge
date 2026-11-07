package com.astraedus.nudge.ui.screens.stats

import com.astraedus.nudge.ui.text.asString
import androidx.compose.ui.res.stringResource
import com.astraedus.nudge.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.astraedus.nudge.ui.screens.stats.charts.BlockedTrendChart
import com.astraedus.nudge.ui.screens.stats.charts.HourlyHeatmap
import com.astraedus.nudge.ui.screens.stats.charts.WeeklyBarChart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    viewModel: AppDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val dateLabel = state.dateLabel.asString()
    val rangeLabel = state.weekRangeLabel.asString()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.appName.ifEmpty { stringResource(R.string.stats_app_details) }) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.components_action_back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                DayNavigationHeader(
                    dayLabel = dateLabel,
                    rangeLabel = rangeLabel,
                    canGoForward = state.canGoForward,
                    isToday = state.isToday,
                    onPreviousDay = viewModel::goToPreviousDay,
                    onNextDay = viewModel::goToNextDay,
                    onJumpToToday = viewModel::jumpToToday
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            stringResource(R.string.stats_screen_time_day, dateLabel),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            state.todayFormatted.asString(),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "${state.weekTotalFormatted.asString()} · $rangeLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            item {
                InsightSection(
                    title = stringResource(R.string.stats_section_screen_time),
                    subtitle = stringResource(R.string.stats_tap_bar_subtitle, rangeLabel)
                ) {
                    WeeklyBarChart(
                        days = state.weeklyData,
                        selectedIndex = state.selectedDayIndex,
                        onSelectDay = viewModel::selectDay,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            item {
                InsightSection(
                    title = stringResource(R.string.stats_section_hourly),
                    subtitle = dateLabel
                ) {
                    HourlyHeatmap(
                        hourlyMs = state.hourlyMs,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            item {
                InsightSection(
                    title = stringResource(R.string.stats_section_effectiveness),
                    subtitle = stringResource(R.string.stats_tap_bar_subtitle, rangeLabel)
                ) {
                    BlockedTrendChart(
                        days = state.trendData,
                        selectedIndex = state.selectedDayIndex,
                        onSelectDay = viewModel::selectDay,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            item {
                InsightSection(title = stringResource(R.string.stats_section_activity)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatItem(
                                label = stringResource(R.string.stats_blocked_day, dateLabel),
                                value = "${state.blockedCountToday}"
                            )
                            StatItem(
                                label = stringResource(R.string.stats_walked_away_day, dateLabel),
                                value = "${state.walkedAwayCountToday}"
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatItem(label = stringResource(R.string.stats_blocked_all_time), value = "${state.blockedCountTotal}")
                            StatItem(
                                label = stringResource(R.string.stats_walked_away_all_time),
                                value = "${state.walkedAwayCountTotal}"
                            )
                        }
                    }
                }
            }

            if (state.blockModeBreakdown.isNotEmpty()) {
                item {
                    InsightSection(
                        title = stringResource(R.string.stats_section_breakdown),
                        subtitle = stringResource(R.string.stats_all_time)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            state.blockModeBreakdown.forEach { (mode, count) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        formatBlockMode(mode),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "$count",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun formatBlockMode(mode: String): String = when (mode) {
    "HARD_BLOCK" -> stringResource(R.string.components_block_mode_hard_block)
    "DELAY" -> stringResource(R.string.components_block_mode_delay)
    "HOLD" -> stringResource(R.string.components_block_mode_hold)
    "BREATHING" -> stringResource(R.string.components_block_mode_breathing)
    else -> mode.lowercase().replaceFirstChar { it.uppercase() }
}
