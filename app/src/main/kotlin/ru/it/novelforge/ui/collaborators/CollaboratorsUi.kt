package ru.it.novelforge.ui.collaborators

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.it.novelforge.domain.model.BlockAccessLevel
import ru.it.novelforge.domain.model.CollaboratorPermissions
import ru.it.novelforge.domain.model.ExportAccessLevel
import ru.it.novelforge.domain.model.LineAccessLevel
import ru.it.novelforge.domain.model.MemberAccessLevel
import ru.it.novelforge.domain.model.ScriptAccessLevel
import ru.it.novelforge.domain.model.StoryAccessRole
import ru.it.novelforge.domain.model.StoryCollaborator
import ru.it.novelforge.domain.model.defaultPermissionsFor
import ru.it.novelforge.ui.editor.EditorViewModel

private val DeepOlive = Color(0xFF48513D)
private val Sage = Color(0xFF8FA37A)
private val Cream = Color(0xFFFBF7EF)
private val BurntOrange = Color(0xFFD8653D)
private val Border = Color(0xFFE2DED6)

@Composable
fun CollaboratorBubbles(
    members: List<StoryCollaborator>,
    loading: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(enabled = !loading, onClick = onOpen),
        shape = RoundedCornerShape(22.dp),
        color = Color.White.copy(alpha = 0.94f),
        shadowElevation = 5.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (loading && members.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = DeepOlive
                )
            } else {
                members.take(3).forEach { member ->
                    AvatarCircle(member = member, size = 32)
                }

                if (members.size > 3) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(BurntOrange, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+${members.size - 3}",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollaboratorsScreen(
    onBack: () -> Unit,
    viewModel: EditorViewModel
) {
    val graph by viewModel.graph.collectAsStateWithLifecycle()
    val state by viewModel.collaboratorsUiState.collectAsStateWithLifecycle()
    val actorRole = graph?.story?.accessRole ?: StoryAccessRole.READER

    var selected by remember { mutableStateOf<StoryCollaborator?>(null) }

    LaunchedEffect(Unit) {
        viewModel.refreshCollaborators()
    }

    Scaffold(
        containerColor = Cream,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Участники")
                        Text(
                            text = "Доступ есть у ${state.members.size} чел.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::refreshCollaborators,
                        enabled = !state.loading
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            state.error?.let { error ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            if (state.loading && state.members.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = DeepOlive)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { Spacer(Modifier.height(4.dp)) }

                    items(state.members, key = { it.userId }) { member ->
                        MemberRow(
                            member = member,
                            onClick = { selected = member }
                        )
                    }

                    item { Spacer(Modifier.height(18.dp)) }
                }
            }
        }
    }

    selected?.let { member ->
        MemberEditorDialog(
            member = member,
            actorRole = actorRole,
            loading = state.loading,
            onDismiss = { selected = null },
            onSave = { role, permissions ->
                viewModel.updateCollaborator(member.userId, role, permissions)
                selected = null
            },
            onRemove = {
                viewModel.removeCollaborator(member.userId)
                selected = null
            }
        )
    }
}

@Composable
private fun MemberRow(
    member: StoryCollaborator,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarCircle(member = member, size = 44)

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = member.visibleName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = member.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = roleColor(member.role).copy(alpha = 0.12f)
            ) {
                Text(
                    text = member.role.label(),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = roleColor(member.role),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun AvatarCircle(member: StoryCollaborator, size: Int) {
    val letter = member.visibleName.trim().take(1).uppercase().ifBlank { "?" }

    Box(
        modifier = Modifier
            .size(size.dp)
            .background(roleColor(member.role), CircleShape)
            .border(1.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MemberEditorDialog(
    member: StoryCollaborator,
    actorRole: StoryAccessRole,
    loading: Boolean,
    onDismiss: () -> Unit,
    onSave: (StoryAccessRole, CollaboratorPermissions) -> Unit,
    onRemove: () -> Unit
) {
    var role by remember(member.userId) { mutableStateOf(member.role) }
    var permissions by remember(member.userId) { mutableStateOf(member.permissions) }

    val editable = when (actorRole) {
        StoryAccessRole.OWNER -> member.role != StoryAccessRole.OWNER
        StoryAccessRole.ADMIN -> member.role == StoryAccessRole.EDITOR || member.role == StoryAccessRole.READER
        StoryAccessRole.EDITOR,
        StoryAccessRole.READER -> false
    }

    val removable = actorRole == StoryAccessRole.OWNER && member.role != StoryAccessRole.OWNER

    val availableRoles = when (actorRole) {
        StoryAccessRole.OWNER -> listOf(
            StoryAccessRole.READER,
            StoryAccessRole.EDITOR,
            StoryAccessRole.ADMIN
        )

        StoryAccessRole.ADMIN -> listOf(
            StoryAccessRole.READER,
            StoryAccessRole.EDITOR
        )

        StoryAccessRole.EDITOR,
        StoryAccessRole.READER -> emptyList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(member.visibleName)
                Text(
                    text = member.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Роль",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                if (member.role == StoryAccessRole.OWNER) {
                    RoleRadioRow(
                        role = StoryAccessRole.OWNER,
                        selected = true,
                        enabled = false,
                        onClick = {}
                    )
                } else {
                    availableRoles.forEach { option ->
                        RoleRadioRow(
                            role = option,
                            selected = role == option,
                            enabled = editable && !loading,
                            onClick = {
                                role = option
                                permissions = defaultPermissionsFor(option)
                            }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(10.dp))

                Text(
                    text = "Права",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Роль задает максимальный уровень. Отдельные права можно уменьшать вручную.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                PermissionGroup(
                    title = "Работа с блоками",
                    selected = permissions.blocks,
                    options = allowedOptions(
                        all = BlockAccessLevel.entries,
                        ceiling = defaultPermissionsFor(role).blocks
                    ),
                    enabled = editable && !loading,
                    label = { it.label() },
                    onSelected = { permissions = permissions.copy(blocks = it) }
                )

                PermissionGroup(
                    title = "Работа с линиями",
                    selected = permissions.lines,
                    options = allowedOptions(
                        all = LineAccessLevel.entries,
                        ceiling = defaultPermissionsFor(role).lines
                    ),
                    enabled = editable && !loading,
                    label = { it.label() },
                    onSelected = { permissions = permissions.copy(lines = it) }
                )

                PermissionGroup(
                    title = "Текст и сценарий",
                    selected = permissions.script,
                    options = allowedOptions(
                        all = ScriptAccessLevel.entries,
                        ceiling = defaultPermissionsFor(role).script
                    ),
                    enabled = editable && !loading,
                    label = { it.label() },
                    onSelected = { permissions = permissions.copy(script = it) }
                )

                PermissionGroup(
                    title = "Управление участниками",
                    selected = permissions.members,
                    options = allowedOptions(
                        all = MemberAccessLevel.entries,
                        ceiling = defaultPermissionsFor(role).members
                    ),
                    enabled = editable && !loading,
                    label = { it.label() },
                    onSelected = { permissions = permissions.copy(members = it) }
                )

                PermissionGroup(
                    title = "Экспорт",
                    selected = permissions.export,
                    options = allowedOptions(
                        all = ExportAccessLevel.entries,
                        ceiling = defaultPermissionsFor(role).export
                    ),
                    enabled = editable && !loading,
                    label = { it.label() },
                    onSelected = { permissions = permissions.copy(export = it) }
                )

                if (removable) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = onRemove,
                        enabled = !loading
                    ) {
                        Text("Удалить доступ", color = BurntOrange)
                    }
                }
            }
        },
        confirmButton = {
            if (editable) {
                Button(
                    onClick = { onSave(role, permissions) },
                    enabled = !loading,
                    colors = ButtonDefaults.buttonColors(containerColor = BurntOrange)
                ) {
                    Text("Сохранить")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}

@Composable
private fun RoleRadioRow(
    role: StoryAccessRole,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            enabled = enabled
        )
        Column {
            Text(role.label())
            Text(
                text = role.description(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun <T> PermissionGroup(
    title: String,
    selected: T,
    options: List<T>,
    enabled: Boolean,
    label: (T) -> String,
    onSelected: (T) -> Unit
) {
    Spacer(Modifier.height(14.dp))
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = DeepOlive
    )

    options.forEach { option ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { onSelected(option) }
                .padding(vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected == option,
                onClick = { onSelected(option) },
                enabled = enabled
            )
            Text(label(option))
        }
    }
}

private fun <T> allowedOptions(all: List<T>, ceiling: T): List<T> {
    val index = all.indexOf(ceiling)
    return if (index < 0) all else all.take(index + 1)
}

private fun StoryAccessRole.label(): String = when (this) {
    StoryAccessRole.OWNER -> "Владелец"
    StoryAccessRole.ADMIN -> "Админ"
    StoryAccessRole.EDITOR -> "Редактор"
    StoryAccessRole.READER -> "Читатель"
}

private fun StoryAccessRole.description(): String = when (this) {
    StoryAccessRole.OWNER -> "Полный контроль над сценарием и доступами"
    StoryAccessRole.ADMIN -> "Редактирование сценария и управление редакторами/читателями"
    StoryAccessRole.EDITOR -> "Работа со сценарием без управления доступами"
    StoryAccessRole.READER -> "Просмотр без изменений"
}

private fun roleColor(role: StoryAccessRole): Color = when (role) {
    StoryAccessRole.OWNER -> DeepOlive
    StoryAccessRole.ADMIN -> BurntOrange
    StoryAccessRole.EDITOR -> Sage
    StoryAccessRole.READER -> Color(0xFF8E9298)
}

private fun BlockAccessLevel.label(): String = when (this) {
    BlockAccessLevel.VIEW -> "Только просмотр"
    BlockAccessLevel.MOVE -> "Перемещать блоки"
    BlockAccessLevel.EDIT -> "Редактировать блоки"
    BlockAccessLevel.FULL -> "Полный доступ"
}

private fun LineAccessLevel.label(): String = when (this) {
    LineAccessLevel.VIEW -> "Только просмотр"
    LineAccessLevel.MOVE -> "Перемещать линии"
    LineAccessLevel.EDIT -> "Редактировать линии"
    LineAccessLevel.FULL -> "Полный доступ"
}

private fun ScriptAccessLevel.label(): String = when (this) {
    ScriptAccessLevel.READ -> "Только чтение"
    ScriptAccessLevel.EDIT -> "Редактировать текст"
    ScriptAccessLevel.FULL -> "Полный доступ"
}

private fun MemberAccessLevel.label(): String = when (this) {
    MemberAccessLevel.NONE -> "Нет доступа"
    MemberAccessLevel.INVITE -> "Приглашать"
    MemberAccessLevel.MANAGE -> "Управлять ролями"
    MemberAccessLevel.FULL -> "Полный доступ"
}

private fun ExportAccessLevel.label(): String = when (this) {
    ExportAccessLevel.NONE -> "Без экспорта"
    ExportAccessLevel.EXPORT -> "Экспортировать"
    ExportAccessLevel.SHARE -> "Экспортировать и делиться"
}
