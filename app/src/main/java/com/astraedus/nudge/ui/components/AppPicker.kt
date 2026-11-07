package com.astraedus.nudge.ui.components

import com.astraedus.nudge.ui.text.asString
import com.astraedus.nudge.domain.text.UiText
import androidx.compose.ui.res.stringResource
import com.astraedus.nudge.R
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.astraedus.nudge.data.repository.InstalledAppsRepository
import com.astraedus.nudge.ui.components.rule.RuleSummary

private typealias PickerApp = InstalledAppsRepository.AppInfo

/** A group the picker offers, with its member count (spec 3.6, All apps editor only). */
@Immutable
data class PickerGroup(val id: Long, val name: String, val memberCount: Int)

/** What Done hands back: the picked apps and the picked groups. */
data class PickerSelection(val packages: Set<String>, val groupIds: Set<Long>)

private val SelectionSaver = Saver<Set<String>, ArrayList<String>>(
    save = { ArrayList(it) },
    restore = { it.toSet() }
)

private val GroupSelectionSaver = Saver<Set<Long>, LongArray>(
    save = { it.toLongArray() },
    restore = { it.toCollection(LinkedHashSet()) }
)

/**
 * Full-screen multi-select app picker (spec 3.6). Selection is local until Done; the caller
 * passes apps already stripped of essentials and sorted by name. Apps only: what the group
 * editor uses, since a group holds apps, never other groups.
 */
@Composable
fun AppPicker(
    apps: List<InstalledAppsRepository.AppInfo>,
    initiallySelected: Set<String>,
    onDone: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) = AppPicker(
    apps = apps,
    groups = emptyList(),
    initiallySelected = initiallySelected,
    initiallySelectedGroups = emptySet(),
    onDone = { onDone(it.packages) },
    onDismiss = onDismiss
)

/**
 * The picker with a **Groups** section above **Apps** (the All apps editor's "Add apps or
 * groups"). Search filters both sections; with no [groups] there is no Groups section at all, so
 * this reads exactly like the apps-only picker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPicker(
    apps: List<InstalledAppsRepository.AppInfo>,
    groups: List<PickerGroup>,
    initiallySelected: Set<String>,
    initiallySelectedGroups: Set<Long>,
    onDone: (PickerSelection) -> Unit,
    onDismiss: () -> Unit
) {
    var selection by rememberSaveable(stateSaver = SelectionSaver) { mutableStateOf(initiallySelected) }
    var groupSelection by rememberSaveable(stateSaver = GroupSelectionSaver) {
        mutableStateOf(initiallySelectedGroups)
    }
    var query by rememberSaveable { mutableStateOf("") }
    val visible = remember(apps, query) { filterPickerApps(apps, query) }
    val visibleGroups = remember(groups, query) { filterPickerGroups(groups, query) }
    val offersGroups = groups.isNotEmpty()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(pickerTitle(selection.size + groupSelection.size, offersGroups).asString()) },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.components_action_cancel))
                            }
                        },
                        actions = {
                            TextButton(onClick = { onDone(PickerSelection(selection, groupSelection)) }) {
                                Text(stringResource(R.string.components_action_done))
                            }
                        }
                    )
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = {
                            Text(
                                stringResource(
                                    if (offersGroups) R.string.components_picker_search_apps_groups else R.string.components_picker_search_apps
                                )
                            )
                        },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.components_picker_clear_search))
                                }
                            }
                        },
                        singleLine = true
                    )

                    if (visible.isEmpty() && visibleGroups.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = noMatchText(query, offersGroups).asString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            if (visibleGroups.isNotEmpty()) {
                                item(key = "groups_header") { PickerSubheader(stringResource(R.string.components_picker_groups)) }
                                items(visibleGroups, key = { "group_${it.id}" }) { group ->
                                    val checked = group.id in groupSelection
                                    ListItem(
                                        headlineContent = {
                                            Text(
                                                RuleSummary.groupTitle(group.name, group.memberCount).asString(),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        leadingContent = { TonalIcon(Icons.Default.Folder) },
                                        trailingContent = {
                                            Checkbox(checked = checked, onCheckedChange = null)
                                        },
                                        modifier = Modifier.toggleable(
                                            value = checked,
                                            role = Role.Checkbox,
                                            onValueChange = { groupSelection = toggled(groupSelection, group.id) }
                                        )
                                    )
                                }
                            }
                            // The Apps heading only earns its place under a Groups section.
                            if (offersGroups && visible.isNotEmpty()) {
                                item(key = "apps_header") { PickerSubheader(stringResource(R.string.components_picker_apps)) }
                            }
                            items(visible, key = { it.packageName }) { app ->
                                val checked = app.packageName in selection
                                AppListItem(
                                    appName = app.appName,
                                    icon = app.icon,
                                    modifier = Modifier.toggleable(
                                        value = checked,
                                        role = Role.Checkbox,
                                        onValueChange = { selection = toggled(selection, app.packageName) }
                                    ),
                                    trailingContent = {
                                        Checkbox(checked = checked, onCheckedChange = null)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** An M3 list subheader: small, primary-coloured, on the list's 16dp edge (as on Manage Apps). */
@Composable
private fun PickerSubheader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

/** Trimmed, case-insensitive match on the app name; a blank query keeps every app in order. */
internal fun filterPickerApps(apps: List<PickerApp>, query: String): List<PickerApp> {
    val q = query.trim()
    if (q.isEmpty()) return apps
    return apps.filter { it.appName.contains(q, ignoreCase = true) }
}

/** The same match as [filterPickerApps], on the group's name. */
internal fun filterPickerGroups(groups: List<PickerGroup>, query: String): List<PickerGroup> {
    val q = query.trim()
    if (q.isEmpty()) return groups
    return groups.filter { it.name.contains(q, ignoreCase = true) }
}

internal fun <T> toggled(selection: Set<T>, item: T): Set<T> =
    if (item in selection) selection - item else selection + item

/**
 * The top bar: "N selected", counting apps and groups together, or what the picker adds when
 * nothing is picked yet ("Add apps", or "Add apps or groups" when it offers groups).
 */
internal fun pickerTitle(count: Int, offersGroups: Boolean = false): UiText = when {
    count > 0 -> UiText.of(R.string.components_picker_selected, count)
    offersGroups -> UiText.of(R.string.components_picker_title_add_apps_groups)
    else -> UiText.of(R.string.components_picker_title_add_apps)
}

internal fun noMatchText(query: String, offersGroups: Boolean): UiText =
    UiText.of(
        if (offersGroups) R.string.components_picker_no_match_apps_groups else R.string.components_picker_no_match_apps,
        query.trim()
    )
