/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.presentation.screens.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import adb.captain.R
import adb.captain.domain.model.Macro
import adb.captain.domain.model.MacroStep
import adb.captain.domain.model.StepCondition
import adb.captain.domain.model.StepFailure
import adb.captain.ui.theme.ExpressiveProgress

/** One editable step, kept as strings so typing never fights the parser. */
private data class StepDraft(
    val command: String = "",
    val condition: StepCondition = StepCondition.ALWAYS,
    val conditionValue: String = "",
    val onError: StepFailure = StepFailure.STOP,
    val waitMs: String = "0"
) {
    fun toStep(): MacroStep = MacroStep(
        command = command.trim(),
        condition = condition,
        conditionValue = conditionValue.trim(),
        onError = onError,
        waitMs = waitMs.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
    )

    companion object {
        fun from(step: MacroStep) = StepDraft(
            command = step.command,
            condition = step.condition,
            conditionValue = step.conditionValue,
            onError = step.onError,
            waitMs = step.waitMs.toString()
        )
    }
}

@Composable
fun MacroRow(
    macro: Macro,
    running: Boolean,
    onRun: () -> Unit,
    onEdit: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(macro.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(R.string.debug_macros_steps, macro.steps.size) +
                    if (macro.variables.isNotEmpty()) {
                        "  ·  " + macro.variables.joinToString(", ") { "{{$it}}" }
                    } else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
        if (running) {
            ExpressiveProgress.indicator(size = 20.dp)
        } else {
            TextButton(onClick = onRun) { Text(stringResource(R.string.debug_macros_run)) }
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.debug_macros_edit))
        }
        IconButton(onClick = onExport) {
            Icon(Icons.Default.Share, contentDescription = stringResource(R.string.debug_macros_export))
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
        }
    }
}

/**
 * Creates or edits a macro. Every step has its own condition, wait time and
 * error policy, which is what makes a macro more than a command list.
 */
@Composable
fun MacroEditorDialog(
    initial: Macro?,
    onDismiss: () -> Unit,
    onSave: (name: String, steps: List<MacroStep>, id: Long) -> Unit
) {
    var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }
    val drafts = remember(initial) {
        mutableStateListOf<StepDraft>().apply {
            addAll(initial?.steps?.map { StepDraft.from(it) } ?: listOf(StepDraft()))
        }
    }
    val scroll = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (initial == null) R.string.debug_macros_add else R.string.debug_macros_edit
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.debug_macros_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(R.string.debug_macros_placeholders),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                drafts.forEachIndexed { index, draft ->
                    StepEditor(
                        index = index,
                        draft = draft,
                        onChange = { drafts[index] = it },
                        onRemove = { if (drafts.size > 1) drafts.removeAt(index) }
                    )
                }
                TextButton(
                    onClick = { drafts.add(StepDraft()) }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.debug_macros_add_step))
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && drafts.any { it.command.isNotBlank() },
                onClick = {
                    onSave(
                        name.trim(),
                        drafts.map { it.toStep() }.filter { it.command.isNotEmpty() },
                        initial?.id ?: 0L
                    )
                }
            ) { Text(stringResource(R.string.debug_macros_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

@Composable
private fun StepEditor(
    index: Int,
    draft: StepDraft,
    onChange: (StepDraft) -> Unit,
    onRemove: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.debug_macros_step, index + 1),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.debug_macros_remove_step)
                )
            }
        }
        OutlinedTextField(
            value = draft.command,
            onValueChange = { onChange(draft.copy(command = it)) },
            label = { Text(stringResource(R.string.debug_macros_command)) },
            minLines = 2,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false
            ),
            modifier = Modifier.fillMaxWidth()
        )
        EnumPicker(
            label = stringResource(R.string.debug_macros_condition),
            current = conditionLabel(draft.condition),
            options = StepCondition.entries.map { conditionLabel(it) to it },
            onSelect = { onChange(draft.copy(condition = it)) }
        )
        if (draft.condition == StepCondition.OUTPUT_CONTAINS ||
            draft.condition == StepCondition.OUTPUT_NOT_CONTAINS
        ) {
            OutlinedTextField(
                value = draft.conditionValue,
                onValueChange = { onChange(draft.copy(conditionValue = it)) },
                label = { Text(stringResource(R.string.debug_macros_condition_value)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        EnumPicker(
            label = stringResource(R.string.debug_macros_on_error),
            current = stringResource(
                if (draft.onError == StepFailure.STOP) {
                    R.string.debug_macros_on_error_stop
                } else {
                    R.string.debug_macros_on_error_continue
                }
            ),
            options = listOf(
                stringResource(R.string.debug_macros_on_error_stop) to StepFailure.STOP,
                stringResource(R.string.debug_macros_on_error_continue) to StepFailure.CONTINUE
            ),
            onSelect = { onChange(draft.copy(onError = it)) }
        )
        OutlinedTextField(
            value = draft.waitMs,
            onValueChange = { text ->
                if (text.all { it.isDigit() }) onChange(draft.copy(waitMs = text))
            },
            label = { Text(stringResource(R.string.debug_macros_wait)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun <T> EnumPicker(
    label: String,
    current: String,
    options: List<Pair<String, T>>,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(current)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (text, value) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        expanded = false
                        onSelect(value)
                    }
                )
            }
        }
    }
}

@Composable
private fun conditionLabel(condition: StepCondition): String = stringResource(
    when (condition) {
        StepCondition.ALWAYS -> R.string.debug_condition_always
        StepCondition.OUTPUT_CONTAINS -> R.string.debug_condition_contains
        StepCondition.OUTPUT_NOT_CONTAINS -> R.string.debug_condition_not_contains
        StepCondition.OUTPUT_EMPTY -> R.string.debug_condition_empty
        StepCondition.OUTPUT_NOT_EMPTY -> R.string.debug_condition_not_empty
    }
)

/** Asks for the `{{variables}}` of a macro right before it runs. */
@Composable
fun MacroVariablesDialog(
    macro: Macro,
    onDismiss: () -> Unit,
    onRun: (Map<String, String>) -> Unit
) {
    val names = remember(macro) { macro.variables }
    val values = remember(macro) { mutableStateMapOf<String, String>() }
    val scroll = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.debug_macros_variables_title, macro.name)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.debug_macros_serial_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                names.forEach { variable ->
                    OutlinedTextField(
                        value = values[variable].orEmpty(),
                        onValueChange = { values[variable] = it },
                        label = { Text("{{$variable}}") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = names.all { !values[it].isNullOrBlank() },
                onClick = { onRun(values.toMap()) }
            ) { Text(stringResource(R.string.debug_macros_run)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        }
    )
}
