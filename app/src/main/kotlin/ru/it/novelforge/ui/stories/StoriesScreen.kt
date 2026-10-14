package ru.it.novelforge.ui.stories

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.flow.collect
import ru.it.novelforge.domain.model.Story
import ru.it.novelforge.domain.model.StoryAccessRole
import ru.it.novelforge.domain.model.UserAccount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoriesScreen(
    user: UserAccount,
    onLogout: () -> Unit,
    onOpenStory: (Long) -> Unit,
    pendingShareToken: String? = null,
    onShareTokenConsumed: () -> Unit = {},
    viewModel: StoriesViewModel = hiltViewModel()
) {
    val stories by viewModel.stories.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showCreateDialog by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var sharingStory by remember { mutableStateOf<Story?>(null) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.openedStory.collect(onOpenStory)
    }

    LaunchedEffect(Unit) {
        viewModel.shareEvents.collect { event ->
            when (event) {
                is ShareEvent.LinkReady -> {
                    val roleText = when (event.role) {
                        StoryAccessRole.READER -> "читатель"
                        StoryAccessRole.EDITOR -> "редактор"
                        StoryAccessRole.OWNER -> "владелец"
                    }
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "NovelForge: доступ к истории ($roleText)\n${event.link}\n\n" +
                                    "Ссылка открывается в приложении NovelForge."
                        )
                    }
                    context.startActivity(
                        Intent.createChooser(sendIntent, "Поделиться историей")
                    )
                }

                ShareEvent.AccessGranted -> {
                    snackbar.showSnackbar("Доступ к общей истории получен")
                }

                is ShareEvent.Error -> {
                    snackbar.showSnackbar(event.message)
                }
            }
        }
    }

    LaunchedEffect(pendingShareToken) {
        val token = pendingShareToken ?: return@LaunchedEffect
        onShareTokenConsumed()
        viewModel.acceptShareLink(token)
    }

    LaunchedEffect(syncState) {
        when (val state = syncState) {
            SyncState.Success -> {
                snackbar.showSnackbar("Истории синхронизированы")
                viewModel.resetSyncMessage()
            }
            is SyncState.Error -> {
                snackbar.showSnackbar("Синхронизация: ${state.message}")
                viewModel.resetSyncMessage()
            }
            else -> Unit
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("NovelForge", fontWeight = FontWeight.Bold)
                        Text(
                            user.displayName.ifBlank { user.email },
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                },
                actions = {
                    if (syncState is SyncState.Syncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(12.dp).height(22.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = viewModel::sync) {
                            Icon(Icons.Default.CloudSync, contentDescription = "Синхронизировать")
                        }
                    }

                    IconButton(onClick = { showAccountDialog = true }) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Аккаунт")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Создать историю")
            }
        }
    ) { padding ->
        if (stories.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.MenuBook, contentDescription = null)
                    Spacer(Modifier.height(12.dp))
                    Text("Историй пока нет", style = MaterialTheme.typography.titleLarge)
                    Text("Создайте первую новеллу кнопкой + или откройте ссылку доступа")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(stories, key = { it.id }) { story ->
                    StoryCard(
                        story = story,
                        onOpen = { onOpenStory(story.id) },
                        onShare = if (story.accessRole.canShare) {
                            { sharingStory = story }
                        } else {
                            null
                        },
                        onDelete = if (story.accessRole == StoryAccessRole.OWNER) {
                            { viewModel.deleteStory(story.id) }
                        } else {
                            null
                        }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateStoryDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title, description ->
                showCreateDialog = false
                viewModel.createStory(title, description)
            }
        )
    }

    sharingStory?.let { story ->
        ShareStoryDialog(
            story = story,
            onDismiss = { sharingStory = null },
            onCreateLink = { role ->
                sharingStory = null
                viewModel.createShareLink(story.id, role)
            }
        )
    }

    if (showAccountDialog) {
        AlertDialog(
            onDismissRequest = { showAccountDialog = false },
            title = { Text("Аккаунт") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (user.displayName.isNotBlank()) {
                        Text(user.displayName, fontWeight = FontWeight.SemiBold)
                    }
                    Text(user.email)
                    Text(
                        "Новеллы сохраняются локально и синхронизируются с сервером.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAccountDialog = false
                        viewModel.sync()
                    }
                ) {
                    Text("Синхронизировать")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAccountDialog = false
                        onLogout()
                    }
                ) {
                    Text("Выйти")
                }
            }
        )
    }
}

@Composable
private fun StoryCard(
    story: Story,
    onOpen: () -> Unit,
    onShare: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    story.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (story.description.isNotBlank()) {
                    Text(
                        story.description,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2
                    )
                }

                Text(
                    when (story.accessRole) {
                        StoryAccessRole.OWNER -> "Владелец"
                        StoryAccessRole.EDITOR -> "Общий доступ: редактор"
                        StoryAccessRole.READER -> "Общий доступ: только чтение"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (story.accessRole == StoryAccessRole.READER) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (!story.isDirty && story.cloudId != null) {
                        Icon(
                            Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Синхронизировано",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Ожидает синхронизации",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    "Изменено: ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(story.updatedAt))}",
                    style = MaterialTheme.typography.labelSmall
                )
            }

            if (onShare != null) {
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = "Поделиться")
                }
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить")
                }
            }
        }
    }
}

@Composable
private fun ShareStoryDialog(
    story: Story,
    onDismiss: () -> Unit,
    onCreateLink: (StoryAccessRole) -> Unit
) {
    var role by remember(story.id) { mutableStateOf(StoryAccessRole.READER) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Поделиться историей") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(story.title, fontWeight = FontWeight.SemiBold)
                Text(
                    "Выберите права пользователя, который откроет ссылку в NovelForge.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { role = StoryAccessRole.READER },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = role == StoryAccessRole.READER,
                        onClick = { role = StoryAccessRole.READER }
                    )
                    Column {
                        Text("Читатель", fontWeight = FontWeight.Medium)
                        Text(
                            "Может открыть и читать историю, но не изменять ее.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { role = StoryAccessRole.EDITOR },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = role == StoryAccessRole.EDITOR,
                        onClick = { role = StoryAccessRole.EDITOR }
                    )
                    Column {
                        Text("Редактор", fontWeight = FontWeight.Medium)
                        Text(
                            "Может редактировать и синхронизировать историю, но не удалять ее и не раздавать доступ.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreateLink(role) }) {
                Text("Создать ссылку")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
private fun CreateStoryDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая новелла") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Краткое описание") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(title, description) }) {
                Text("Создать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
