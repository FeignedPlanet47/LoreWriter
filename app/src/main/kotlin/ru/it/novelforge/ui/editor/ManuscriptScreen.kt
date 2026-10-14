package ru.it.novelforge.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import ru.it.novelforge.ui.common.buildRichAnnotatedString
import ru.it.novelforge.ui.common.rememberSpellCheckState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManuscriptScreen(
    onOpenMenu: () -> Unit,
    viewModel: EditorViewModel
) {
    val graph by viewModel.graph.collectAsStateWithLifecycle()
    val story = graph?.story

    if (story == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Текст новеллы") },
                    navigationIcon = {
                        IconButton(onClick = onOpenMenu) {
                            Icon(Icons.Default.Menu, contentDescription = "Меню")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                Text("Загрузка…")
            }
        }
        return
    }

    var textValue by remember(story.id) {
        mutableStateOf(
            TextFieldValue(
                text = story.manuscript
            )
        )
    }

    var savedText by remember(story.id) {
        mutableStateOf(story.manuscript)
    }

    val text = textValue.text
    val latestText by rememberUpdatedState(text)
    val latestSavedText by rememberUpdatedState(savedText)

    val spellCheckState = rememberSpellCheckState(
        text = text,
        enabled = true
    )

    val styledTextValue = TextFieldValue(
        annotatedString = buildRichAnnotatedString(
            text = text,
            marks = emptyList(),
            spellingIssues = spellCheckState.issues
        ),
        selection = textValue.selection,
        composition = textValue.composition
    )

    DisposableEffect(story.id) {
        onDispose {
            if (latestText != latestSavedText) {
                viewModel.updateManuscript(latestText)
            }
        }
    }

    LaunchedEffect(text) {
        if (text == savedText) return@LaunchedEffect
        delay(600)
        viewModel.updateManuscript(text)
        savedText = text
    }

    val words = remember(text) {
        text
            .trim()
            .split(Regex("\\s+"))
            .count { it.isNotBlank() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Текст новеллы")
                        Text(
                            text = if (text == savedText) {
                                "Сохранено автоматически"
                            } else {
                                "Сохранение…"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(Icons.Default.Menu, contentDescription = "Меню")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Слов: $words",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Символов: ${text.length}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            when {
                !spellCheckState.isAvailable -> {
                    Text(
                        text = "Проверка орфографии недоступна. Включите системную проверку текста в настройках Android.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                spellCheckState.isChecking -> {
                    Text(
                        text = "Проверка орфографии…",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                spellCheckState.issues.isNotEmpty() -> {
                    Text(
                        text = "Возможных ошибок: ${spellCheckState.issues.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            TextField(
                value = styledTextValue,
                onValueChange = { updated ->
                    textValue = TextFieldValue(
                        text = updated.text,
                        selection = updated.selection,
                        composition = updated.composition
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                placeholder = {
                    Text(
                        "Пишите новеллу обычным текстом. Здесь можно хранить линейную версию истории, черновик или готовый литературный текст…"
                    )
                },
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )
        }
    }
}
