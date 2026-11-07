package com.astraedus.nudge.ui.backup

import com.astraedus.nudge.R
import com.astraedus.nudge.domain.text.UiText
import com.astraedus.nudge.domain.usecase.ImportOutcome
import com.astraedus.nudge.domain.usecase.ImportPreview

/**
 * Pure text builders for the import dialogs.
 *
 * They live outside the composable so the wording -- especially "we skipped N entries you had in
 * your backup", the one thing the user must not miss -- is JVM-testable rather than only verifiable
 * by eye on a device. They return [UiText] (resource ids and numbers), so a test pins both the
 * English wording and, through the same ids, the Ukrainian plural forms.
 *
 * The per-entry REASONS ("Rule 3: unknown block mode") are the importer's parse diagnostics and stay
 * as the importer wrote them; everything around them is translated.
 */

/** Cap on how many per-entry reasons a dialog lists before collapsing the rest into a count. */
private const val MAX_LISTED_REASONS = 3

private val PARAGRAPH = UiText.Raw("\n\n")

/**
 * Body text of the "Import backup" confirmation dialog, shown BEFORE anything is written.
 *
 * A file with no history reads EXACTLY as it did before history existed -- a rules-only backup from
 * an older Nudge must not sprout a line about a feature it knows nothing about.
 */
fun buildImportPreviewMessage(preview: ImportPreview): UiText {
    val result = preview.result
    val parts = mutableListOf<UiText>()
    fun paragraph(text: UiText) {
        if (parts.isNotEmpty()) parts += PARAGRAPH
        parts += text
    }

    paragraph(
        if (result.groups.isNotEmpty()) {
            UiText.of(R.string.backup_preview_rules_groups, result.rules.size, result.groups.size)
        } else {
            UiText.of(R.string.backup_preview_rules, result.rules.size)
        }
    )
    paragraph(UiText.of(R.string.backup_preview_duplicates))
    if (result.history.isNotEmpty()) {
        paragraph(UiText.of(R.string.backup_preview_history, result.history.size, preview.newHistoryCount))
    }
    if (result.invalidCount > 0) {
        paragraph(withReasons(R.plurals.backup_entries_unreadable, result.invalidCount, result.invalidReasons))
    }
    if (result.settings != null) {
        // Warned BEFORE anything is written, because unlike rules and history this REPLACES what
        // is already on the device: custom block messages, the content filter, Strict Mode.
        paragraph(UiText.of(R.string.backup_preview_settings))
    }
    if (result.invalidHistoryCount > 0) {
        paragraph(
            withReasons(R.plurals.backup_history_unreadable, result.invalidHistoryCount, result.invalidHistoryReasons)
        )
    }
    if (result.invalidSettingsCount > 0) {
        paragraph(
            withReasons(R.plurals.backup_settings_unreadable, result.invalidSettingsCount, result.invalidSettingsReasons)
        )
    }
    return UiText.Joined(parts, "")
}

/** Body text of the "Import Complete" dialog. */
fun buildImportOutcomeMessage(outcome: ImportOutcome): UiText {
    val lines = mutableListOf<UiText>()
    fun line(text: UiText, blankLineBefore: Boolean = false) {
        if (lines.isNotEmpty()) lines += UiText.Raw(if (blankLineBefore) "\n\n" else "\n")
        lines += text
    }

    line(UiText.of(R.string.backup_outcome_imported, outcome.importedCount))
    if (outcome.groupsCreated > 0) line(UiText.of(R.string.backup_outcome_groups, outcome.groupsCreated))
    if (outcome.duplicateCount > 0) line(UiText.of(R.string.backup_outcome_duplicates, outcome.duplicateCount))
    if (outcome.hasHistory) {
        line(UiText.of(R.string.backup_outcome_history_restored, outcome.historyImportedCount), blankLineBefore = true)
        if (outcome.historyDuplicateCount > 0) {
            line(UiText.of(R.string.backup_outcome_history_present, outcome.historyDuplicateCount))
        }
        if (outcome.historyInvalidCount > 0) {
            line(UiText.of(R.string.backup_outcome_history_unreadable, outcome.historyInvalidCount))
        }
    }
    if (outcome.settingsApplied) line(UiText.of(R.string.backup_outcome_settings_restored), blankLineBefore = true)
    if (outcome.settingsInvalidCount > 0) {
        line(UiText.of(R.string.backup_outcome_settings_unreadable, outcome.settingsInvalidCount))
    }
    if (outcome.invalidCount > 0) {
        line(UiText.of(R.string.backup_outcome_skipped_unreadable, outcome.invalidCount), blankLineBefore = true)
        lines += reasonLines(outcome.invalidReasons)
    }
    return UiText.Joined(lines, "")
}

/**
 * Whether the import touched history at all. A rules-only file stays silent about it: reporting
 * "History events restored: 0" on a v1 backup is noise that reads like a failure.
 */
private val ImportOutcome.hasHistory: Boolean
    get() = historyImportedCount > 0 || historyDuplicateCount > 0 || historyInvalidCount > 0

/** "<count> ... could not be read and will be left out:" and up to three reasons under it. */
private fun withReasons(plural: Int, count: Int, reasons: List<String>): UiText =
    UiText.Joined(listOf(UiText.plural(plural, count)) + reasonLines(reasons), "")

private fun reasonLines(reasons: List<String>): List<UiText> {
    val lines = reasons.take(MAX_LISTED_REASONS).map { UiText.Raw("\n• $it") }
    val extra = reasons.size - MAX_LISTED_REASONS
    return if (extra > 0) lines + UiText.Joined(listOf(UiText.Raw("\n• "), UiText.of(R.string.backup_reasons_more, extra)), "") else lines
}
