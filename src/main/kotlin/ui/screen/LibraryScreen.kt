package com.notely.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notely.domain.Folder
import com.notely.service.FolderService
import com.notely.ui.component.CreateFolderDialog
import com.notely.ui.component.FolderCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LibraryScreen(
    folderService: FolderService,
    onOpenFolder: (Folder) -> Unit
) {
    val folders = remember { mutableStateListOf<Folder>() }
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(folderService) {
        isLoading = true
        try {
            val loaded = withContext(Dispatchers.IO) { folderService.getFolders() }
            folders.clear()
            folders.addAll(loaded)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            loadError = "Не удалось загрузить библиотеку"
        } finally {
            isLoading = false
        }
    }

    var showCreateDialog by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEDF4FC))
    ) {
        Column(
            modifier = Modifier
                .width(84.dp)
                .fillMaxHeight()
                .background(Color(0xFFDCEAF8))
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "🎵",
                fontSize = 30.sp
            )

            Button(
                onClick = {
                    errorMessage = null
                    showCreateDialog = true
                },
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFFBDD7F3),
                    contentColor = Color(0xFF304C70)
                ),
                contentPadding = PaddingValues(0.dp),
                elevation = ButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 1.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Создать папку",
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        ) {
            Text(
                text = "Моя библиотека",
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF304C70)
            )

            Spacer(Modifier.height(28.dp))

            if (isLoading) {
                CircularProgressIndicator(color = Color(0xFF719FCF))
            } else if (folders.isEmpty()) {
                Text(
                    text = "Пока нет папок. Нажмите +, чтобы создать первую.",
                    color = Color(0xFF6583A4),
                    fontSize = 16.sp
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(160.dp),
                    horizontalArrangement =
                        Arrangement.spacedBy(20.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(24.dp)
                ) {
                    items(
                        items = folders,
                        key = { it.id }
                    ) { folder ->
                        FolderCard(
                            folder = folder,
                            onClick = {
                                onOpenFolder(folder)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateFolderDialog(
            onDismiss = {
                showCreateDialog = false
            },
            errorMessage = errorMessage,
            onCreate = { name, emoji ->
                if (!isSaving) {
                    scope.launch {
                        isSaving = true
                        try {
                            val folder = withContext(Dispatchers.IO) {
                                folderService.createFolder(name, emoji)
                            }
                            folders.add(folder)
                            showCreateDialog = false
                            errorMessage = null
                        } catch (exception: CancellationException) {
                            throw exception
                        } catch (exception: Exception) {
                            errorMessage = exception.message ?: "Не удалось создать папку"
                        } finally {
                            isSaving = false
                        }
                    }
                }
            }
        )
    }
}
