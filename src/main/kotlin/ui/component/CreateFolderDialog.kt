package com.notely.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val TextColor = Color(0xFF304C70)
private val AccentColor = Color(0xFFBDD7F3)
private val BackgroundColor = Color(0xFFEDF4FC)

private val musicalEmojis = listOf(
    "🎻", "🎹", "🎸", "🎺",
    "🎷", "🥁", "🪈", "🪕",
    "🪘", "🎤", "🎼", "🎶",
    "🎵", "🎧", "🪗", "🔔"
)

@Composable
fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit,
    errorMessage: String?
) {
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("🎻") }
    var emojiMenuExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(18.dp),
        backgroundColor = Color.White,
        title = {
            Text(
                text = "Новая папка",
                color = TextColor,
                fontSize = 22.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = AccentColor,
                        focusedLabelColor = TextColor,
                        cursorColor = TextColor
                    )
                )

                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = emoji,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Эмодзи") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            Text(
                                text = "⌄",
                                color = TextColor,
                                fontSize = 22.sp
                            )
                        },
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = AccentColor,
                            focusedLabelColor = TextColor
                        )
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable {
                                emojiMenuExpanded = true
                            }
                    )

                    DropdownMenu(
                        expanded = emojiMenuExpanded,
                        onDismissRequest = {
                            emojiMenuExpanded = false
                        },
                        modifier = Modifier
                            .background(BackgroundColor)
                            .width(280.dp)
                    ) {
                        musicalEmojis.chunked(4).forEach { row ->
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = 8.dp,
                                    vertical = 4.dp
                                ),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { option ->
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .background(
                                                color = if (option == emoji) {
                                                    AccentColor
                                                } else {
                                                    Color.White
                                                },
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                emoji = option
                                                emojiMenuExpanded = false
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = option,
                                            fontSize = 26.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                errorMessage?.let {
                    Text(
                        text = it,
                        color = Color(0xFFB65B66),
                        fontSize = 13.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreate(name, emoji)
                },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = AccentColor,
                    contentColor = TextColor
                ),
                elevation = null
            ) {
                Text("Создать")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = TextColor
                )
            ) {
                Text("Отмена")
            }
        }
    )
}
