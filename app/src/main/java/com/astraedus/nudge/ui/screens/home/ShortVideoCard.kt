package com.astraedus.nudge.ui.screens.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.astraedus.nudge.R
import com.astraedus.nudge.ui.components.qaTag
import com.astraedus.nudge.ui.testtags.UiTags
import com.astraedus.nudge.ui.theme.NudgeTheme

/**
 * "Block short videos": the first card on Home, directly under the header. It answers the question
 * most people install Nudge for ("keep Instagram, lose Reels") on the first screen, with one switch
 * per feature, instead of four screens down inside an app's config.
 */
@Composable
fun ShortVideoCard(
    state: ShortVideoState,
    onToggle: (ShortVideoRow, Boolean) -> Unit,
    onMoreOptions: (packageName: String) -> Unit,
    onEnableAccessibility: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .qaTag(UiTags.SHORT_VIDEO_CARD),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.home_short_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                stringResource(R.string.home_short_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
            )
            ShortVideoBlockRows(
                state = state,
                onToggle = onToggle,
                onMoreOptions = onMoreOptions,
                onEnableAccessibility = onEnableAccessibility
            )
        }
    }
}

@Preview(name = "Block short videos", widthDp = 360)
@Composable
private fun ShortVideoCardPreview() {
    NudgeTheme(dynamicColor = false) {
        ShortVideoCard(
            state = ShortVideoState(
                loaded = true,
                rows = ShortVideoBlocks.rowsFor(
                    labels = mapOf("com.instagram.android" to "Instagram", "com.google.android.youtube" to "YouTube"),
                    rules = emptyList()
                ),
                accessibilityWorking = false
            ),
            onToggle = { _, _ -> },
            onMoreOptions = {},
            onEnableAccessibility = {}
        )
    }
}
