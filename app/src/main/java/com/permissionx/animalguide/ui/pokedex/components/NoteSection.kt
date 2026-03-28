package com.permissionx.animalguide.ui.pokedex.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.permissionx.animalguide.data.local.entity.AnimalEntry

@Composable
fun NoteSection(
    animal: AnimalEntry,
    isEditing: Boolean,
    onStartEdit: () -> Unit,
    onSave: (String) -> Unit,
    onCancel: () -> Unit
) {
    var noteText by remember(animal.note) { mutableStateOf(animal.note) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "📝 我的备注",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (!isEditing) {
                    IconButton(
                        onClick = onStartEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "编辑备注",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (isEditing) {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("记录你的发现...") },
                    minLines = 3
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            noteText = animal.note
                            onCancel()
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("取消") }
                    Button(
                        onClick = { onSave(noteText) },
                        modifier = Modifier.weight(1f)
                    ) { Text("保存") }
                }
            } else {
                Text(
                    text = animal.note.ifEmpty { "点击右上角编辑添加备注" },
                    fontSize = 14.sp,
                    color = if (animal.note.isEmpty())
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    else
                        MaterialTheme.colorScheme.onSurface,
                    lineHeight = 22.sp
                )
            }
        }
    }
}