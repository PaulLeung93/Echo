package dev.echoapp.echo.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.echoapp.echo.R
import dev.echoapp.echo.domain.model.ReportReason

/**
 * A reason picker for reporting content. Calls [onSubmit] with the chosen reason.
 * Stateless about *what* is being reported — the caller maps the reason to a
 * Report and submits it.
 */
@Composable
fun ReportDialog(
    onDismiss: () -> Unit,
    onSubmit: (ReportReason) -> Unit
) {
    var selected by remember { mutableStateOf(ReportReason.SPAM) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.report_dialog_title)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.report_dialog_message),
                    style = MaterialTheme.typography.bodyMedium
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    ReportReason.entries.forEach { reason ->
                        Row(
                            reason = reason,
                            selected = selected == reason,
                            onSelect = { selected = reason }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(selected) }) {
                Text(stringResource(R.string.report_submit))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun Row(reason: ReportReason, selected: Boolean, onSelect: () -> Unit) {
    androidx.compose.foundation.layout.Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect)
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(reason.label, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Confirmation dialog before blocking [username]. */
@Composable
fun BlockUserDialog(
    username: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val handle = username.substringAfter("@", username).ifBlank { username }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.block_dialog_title, handle)) },
        text = { Text(stringResource(R.string.block_dialog_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.block_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
