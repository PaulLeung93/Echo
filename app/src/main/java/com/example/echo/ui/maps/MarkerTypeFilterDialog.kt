package com.example.echo.ui.maps

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MarkerTypeFilterDialog(
    selectedTypes: Set<String>,
    onDismiss: () -> Unit,
    onApply: (Set<String>) -> Unit
) {
    // Always use lowercase for internal values
    val allTypes = listOf("user posts", "landmark", "park", "college")
    val selected = remember { mutableStateListOf<String>().apply { addAll(selectedTypes) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter by Marker Type") },
        text = {
            Column {
                // Select/Clear All Buttons
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = {
                        selected.clear()
                        selected.addAll(allTypes)
                    }) {
                        Text("Select All")
                    }
                    TextButton(onClick = {
                        selected.clear()
                    }) {
                        Text("Clear All")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                allTypes.forEach { type ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = selected.contains(type),
                            onCheckedChange = { isChecked ->
                                if (isChecked) selected.add(type)
                                else selected.remove(type)
                            }
                        )
                        Text(type.replaceFirstChar { it.uppercase() })

                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onApply(selected.toSet()) // still returns lowercase values
                onDismiss()
            }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
