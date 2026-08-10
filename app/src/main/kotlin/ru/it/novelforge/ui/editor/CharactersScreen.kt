package ru.it.novelforge.ui.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.it.novelforge.domain.model.CharacterProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharactersScreen(
    onOpenMenu: () -> Unit,
    viewModel: EditorViewModel
) {
    val characters by viewModel.characters.collectAsStateWithLifecycle()
    var editingCharacter by remember { mutableStateOf<CharacterProfile?>(null) }
    var showNewCharacter by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Персонажи")
                        Text(
                            text = "${characters.size} персонажей",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(Icons.Default.Menu, contentDescription = "Меню")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showNewCharacter = true }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить персонажа")
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
                        Icons.Default.Person,
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
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = characters,
                    key = { it.id }
                ) { character ->
                    CharacterCard(
                        character = character,
                        onClick = { editingCharacter = character }
                    )
                }
            }
        }
    }

    if (showNewCharacter) {
        CharacterEditorDialog(
            character = null,
            onDismiss = { showNewCharacter = false },
            onSave = { character ->
                viewModel.createCharacter(
                    name = character.name,
                    role = character.role,
                    description = character.description,
                    personality = character.personality,
                    appearance = character.appearance,
                    notes = character.notes
                )
                showNewCharacter = false
            },
            onDelete = null
        )
    }

    editingCharacter?.let { character ->
        CharacterEditorDialog(
            character = character,
            onDismiss = { editingCharacter = null },
            onSave = { updated ->
                viewModel.saveCharacter(updated)
                editingCharacter = null
            },
            onDelete = {
                viewModel.deleteCharacter(character.id)
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
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Person, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
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

            val summary = character.description.ifBlank {
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
    var name by remember(character?.id) { mutableStateOf(character?.name.orEmpty()) }
    var role by remember(character?.id) { mutableStateOf(character?.role.orEmpty()) }
    var description by remember(character?.id) { mutableStateOf(character?.description.orEmpty()) }
    var personality by remember(character?.id) { mutableStateOf(character?.personality.orEmpty()) }
    var appearance by remember(character?.id) { mutableStateOf(character?.appearance.orEmpty()) }
    var notes by remember(character?.id) { mutableStateOf(character?.notes.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (character == null) "Новый персонаж" else "Персонаж")
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 620.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Имя персонажа") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = role,
                        onValueChange = { role = it },
                        label = { Text("Роль") },
                        placeholder = { Text("Главный герой, антагонист, союзник…") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Описание / биография") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = personality,
                        onValueChange = { personality = it },
                        label = { Text("Характер") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = appearance,
                        onValueChange = { appearance = it },
                        label = { Text("Внешность") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Заметки") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        CharacterProfile(
                            id = character?.id ?: 0L,
                            storyId = character?.storyId ?: 0L,
                            name = name,
                            role = role,
                            description = description,
                            personality = personality,
                            appearance = appearance,
                            notes = notes
                        )
                    )
                },
                enabled = name.isNotBlank()
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Удалить")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Отмена")
                }
            }
        }
    )
}
