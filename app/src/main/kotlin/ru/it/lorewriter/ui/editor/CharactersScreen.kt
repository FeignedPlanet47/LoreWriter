package ru.it.lorewriter.ui.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.ByteArrayOutputStream
import ru.it.lorewriter.domain.model.CharacterProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharactersScreen(
    onOpenMenu: () -> Unit,
    viewModel: EditorViewModel
) {
    val characters by viewModel.characters.collectAsStateWithLifecycle()
    val graph by viewModel.graph.collectAsStateWithLifecycle()

    val canEdit = graph?.story?.accessRole?.canEdit == true

    var editingCharacter by remember {
        mutableStateOf<CharacterProfile?>(null)
    }

    var showNewCharacter by remember {
        mutableStateOf(false)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Персонажи")

                        Text(
                            text = "${characters.size} персонажей" +
                                    if (canEdit) {
                                        ""
                                    } else {
                                        " · Только чтение"
                                    },
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onOpenMenu
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Меню"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (canEdit) {
                FloatingActionButton(
                    onClick = {
                        showNewCharacter = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Добавить персонажа"
                    )
                }
            }
        }
    ) { padding ->

        if (characters.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null
                    )

                    Text(
                        text = "Персонажей пока нет",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "Добавьте героя и храните здесь его роль, биографию, характер, внешность и заметки.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding =
                    androidx.compose.foundation.layout.PaddingValues(
                        16.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = characters,
                    key = {
                        it.id
                    }
                ) { character ->

                    CharacterCard(
                        character = character,
                        onClick = {
                            if (canEdit) {
                                editingCharacter = character
                            }
                        }
                    )
                }
            }
        }
    }

    if (showNewCharacter) {
        CharacterEditorDialog(
            character = null,
            onDismiss = {
                showNewCharacter = false
            },
            onSave = { character ->

                viewModel.createCharacter(
                    name = character.name,
                    role = character.role,
                    description = character.description,
                    personality = character.personality,
                    appearance = character.appearance,
                    notes = character.notes,
                    photoBase64 = character.photoBase64
                )

                showNewCharacter = false
            },
            onDelete = null
        )
    }

    editingCharacter?.let { character ->

        CharacterEditorDialog(
            character = character,
            onDismiss = {
                editingCharacter = null
            },
            onSave = { updated ->

                viewModel.saveCharacter(updated)

                editingCharacter = null
            },
            onDelete = {

                viewModel.deleteCharacter(
                    character.id
                )

                editingCharacter = null
            }
        )
    }
}

@Composable
private fun CharacterCard(
    character: CharacterProfile,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                CharacterPhoto(
                    photoBase64 = character.photoBase64,
                    size = 56
                )

                Spacer(
                    modifier = Modifier.width(10.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = character.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (character.role.isNotBlank()) {
                        Text(
                            text = character.role,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            val summary =
                character.description.ifBlank {
                    character.personality.ifBlank {
                        character.notes
                    }
                }

            if (summary.isNotBlank()) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = "Информация пока не заполнена",
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CharacterEditorDialog(
    character: CharacterProfile?,
    onDismiss: () -> Unit,
    onSave: (CharacterProfile) -> Unit,
    onDelete: (() -> Unit)?
) {
    var name by remember(character?.id) {
        mutableStateOf(
            character?.name.orEmpty()
        )
    }

    var role by remember(character?.id) {
        mutableStateOf(
            character?.role.orEmpty()
        )
    }

    var description by remember(character?.id) {
        mutableStateOf(
            character?.description.orEmpty()
        )
    }

    var personality by remember(character?.id) {
        mutableStateOf(
            character?.personality.orEmpty()
        )
    }

    var appearance by remember(character?.id) {
        mutableStateOf(
            character?.appearance.orEmpty()
        )
    }

    var notes by remember(character?.id) {
        mutableStateOf(
            character?.notes.orEmpty()
        )
    }

    var photoBase64 by remember(character?.id) {
        mutableStateOf(
            character?.photoBase64.orEmpty()
        )
    }

    val context = LocalContext.current

    val photoPicker =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.GetContent()
        ) { uri ->

            if (uri != null) {
                photoBase64 =
                    imageUriToBase64(
                        context = context,
                        uri = uri
                    )
            }
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (character == null) {
                    "Новый персонаж"
                } else {
                    "Персонаж"
                }
            )
        },
        text = {
            LazyColumn(
                modifier =
                    Modifier.heightIn(
                        max = 620.dp
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(
                        12.dp
                    )
            ) {

                item {
                    Column(
                        modifier =
                            Modifier.fillMaxWidth(),
                        horizontalAlignment =
                            Alignment.CenterHorizontally,
                        verticalArrangement =
                            Arrangement.spacedBy(
                                8.dp
                            )
                    ) {

                        CharacterPhoto(
                            photoBase64 =
                                photoBase64,
                            size = 120
                        )

                        Button(
                            onClick = {
                                photoPicker.launch(
                                    "image/*"
                                )
                            }
                        ) {
                            Text(
                                if (
                                    photoBase64.isBlank()
                                ) {
                                    "Добавить фото"
                                } else {
                                    "Изменить фото"
                                }
                            )
                        }

                        if (
                            photoBase64.isNotBlank()
                        ) {
                            TextButton(
                                onClick = {
                                    photoBase64 = ""
                                }
                            ) {
                                Text(
                                    "Удалить фото"
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                        },
                        label = {
                            Text(
                                "Имя персонажа"
                            )
                        },
                        singleLine = true,
                        modifier =
                            Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = role,
                        onValueChange = {
                            role = it
                        },
                        label = {
                            Text("Роль")
                        },
                        placeholder = {
                            Text(
                                "Главный герой, антагонист, союзник…"
                            )
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = {
                            description = it
                        },
                        label = {
                            Text(
                                "Описание / биография"
                            )
                        },
                        minLines = 3,
                        modifier =
                            Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = personality,
                        onValueChange = {
                            personality = it
                        },
                        label = {
                            Text("Характер")
                        },
                        minLines = 2,
                        modifier =
                            Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = appearance,
                        onValueChange = {
                            appearance = it
                        },
                        label = {
                            Text("Внешность")
                        },
                        minLines = 2,
                        modifier =
                            Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = {
                            notes = it
                        },
                        label = {
                            Text("Заметки")
                        },
                        minLines = 3,
                        modifier =
                            Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        CharacterProfile(
                            id =
                                character?.id
                                    ?: 0L,
                            storyId =
                                character?.storyId
                                    ?: 0L,
                            name = name,
                            role = role,
                            description =
                                description,
                            personality =
                                personality,
                            appearance =
                                appearance,
                            notes = notes,
                            photoBase64 =
                                photoBase64
                        )
                    )
                },
                enabled =
                    name.isNotBlank()
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            Row {

                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete
                    ) {
                        Icon(
                            imageVector =
                                Icons.Default.Delete,
                            contentDescription =
                                null
                        )

                        Spacer(
                            modifier =
                                Modifier.width(
                                    4.dp
                                )
                        )

                        Text("Удалить")
                    }
                }

                TextButton(
                    onClick = onDismiss
                ) {
                    Text("Отмена")
                }
            }
        }
    )
}

@Composable
private fun CharacterPhoto(
    photoBase64: String,
    size: Int
) {
    val bitmap =
        remember(photoBase64) {
            base64ToBitmap(
                photoBase64
            )
        }

    if (bitmap != null) {

        Image(
            bitmap =
                bitmap.asImageBitmap(),
            contentDescription =
                "Фото персонажа",
            modifier =
                Modifier
                    .size(size.dp)
                    .clip(CircleShape),
            contentScale =
                ContentScale.Crop
        )
    } else {

        Box(
            modifier =
                Modifier
                    .size(size.dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme
                            .colorScheme
                            .surfaceVariant
                    ),
            contentAlignment =
                Alignment.Center
        ) {

            Icon(
                imageVector =
                    Icons.Default.Person,
                contentDescription = null,
                modifier =
                    Modifier.size(
                        (size / 2).dp
                    )
            )
        }
    }
}

private fun imageUriToBase64(
    context: Context,
    uri: Uri
): String {
    return runCatching {

        val original =
            context
                .contentResolver
                .openInputStream(uri)
                ?.use { input ->
                    BitmapFactory.decodeStream(
                        input
                    )
                }
                ?: return ""

        if (
            original.width <= 0 ||
            original.height <= 0
        ) {
            return ""
        }

        val maxDimension = 800

        val scale =
            minOf(
                maxDimension.toFloat() /
                        original.width,
                maxDimension.toFloat() /
                        original.height,
                1f
            )

        val newWidth =
            (original.width * scale)
                .toInt()
                .coerceAtLeast(1)

        val newHeight =
            (original.height * scale)
                .toInt()
                .coerceAtLeast(1)

        val resized =
            if (
                newWidth !=
                original.width ||
                newHeight !=
                original.height
            ) {
                Bitmap.createScaledBitmap(
                    original,
                    newWidth,
                    newHeight,
                    true
                )
            } else {
                original
            }

        val output =
            ByteArrayOutputStream()

        resized.compress(
            Bitmap.CompressFormat.JPEG,
            80,
            output
        )

        if (resized !== original) {
            resized.recycle()
        }

        original.recycle()

        Base64.encodeToString(
            output.toByteArray(),
            Base64.NO_WRAP
        )
    }.getOrDefault("")
}

private fun base64ToBitmap(
    photoBase64: String
): Bitmap? {
    if (
        photoBase64.isBlank()
    ) {
        return null
    }

    return runCatching {

        val bytes =
            Base64.decode(
                photoBase64,
                Base64.DEFAULT
            )

        BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size
        )
    }.getOrNull()
}