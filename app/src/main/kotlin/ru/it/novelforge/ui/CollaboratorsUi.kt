package ru.it.novelforge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val Olive = Color(0xFF48513D)
private val Orange = Color(0xFFD8653D)
private val Cream = Color(0xFFFBF7EF)

data class MemberPermissions(
    val blocks: String = "view", val lines: String = "view", val script: String = "read",
    val members: String = "none", val export: String = "none"
)
data class StoryMember(
    val userId: String, val displayName: String, val email: String,
    val role: String, val permissions: MemberPermissions
)

fun defaultPermissions(role: String) = when (role) {
    "owner" -> MemberPermissions("full", "full", "full", "full", "share")
    "admin" -> MemberPermissions("full", "full", "full", "manage", "share")
    "editor" -> MemberPermissions("edit", "edit", "edit", "none", "export")
    else -> MemberPermissions()
}

@Composable
fun CollaboratorBubbles(members: List<StoryMember>, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.clickable(onClick = onOpen), verticalAlignment = Alignment.CenterVertically) {
        members.take(3).forEachIndexed { index, member ->
            Box(Modifier.padding(start = if (index == 0) 0.dp else 3.dp).size(34.dp)
                .background(if (member.role == "owner") Olive else Color(0xFF8FA37A), CircleShape),
                contentAlignment = Alignment.Center) {
                Text((member.displayName.ifBlank { member.email }).take(1).uppercase(), color = Color.White)
            }
        }
        if (members.size > 3) Text(" +${members.size - 3}", color = Olive)
    }
}

@Composable
fun CollaboratorsScreen(
    members: List<StoryMember>, currentUserRole: String,
    onBack: () -> Unit,
    onSave: (userId: String, role: String, permissions: MemberPermissions) -> Unit,
    onRemove: (userId: String) -> Unit
) {
    var selected by remember { mutableStateOf<StoryMember?>(null) }
    Column(Modifier.fillMaxSize().background(Cream).padding(20.dp)) {
        TextButton(onClick = onBack) { Text("← Назад", color = Olive) }
        Text("Участники · ${members.size}", style = MaterialTheme.typography.headlineMedium, color = Olive)
        Spacer(Modifier.height(18.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(members, key = { it.userId }) { member ->
                Surface(shape = RoundedCornerShape(16.dp), color = Color.White,
                    modifier = Modifier.fillMaxWidth().clickable { selected = member }) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).background(Olive, CircleShape), contentAlignment = Alignment.Center) {
                            Text(member.displayName.ifBlank { member.email }.take(1).uppercase(), color = Color.White)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(member.displayName.ifBlank { member.email }, fontWeight = FontWeight.Medium)
                            Text(member.email, style = MaterialTheme.typography.bodySmall)
                        }
                        Text(roleLabel(member.role), color = Olive)
                    }
                }
            }
        }
    }
    selected?.let { member ->
        val editable = member.role != "owner" && (currentUserRole == "owner" ||
            (currentUserRole == "admin" && member.role != "admin"))
        MemberEditorDialog(member, editable, currentUserRole == "owner",
            onDismiss = { selected = null }, onSave = { role, perms ->
                onSave(member.userId, role, perms); selected = null
            }, onRemove = { onRemove(member.userId); selected = null })
    }
}

private fun roleLabel(role: String) = when (role) {
    "owner" -> "Владелец"; "admin" -> "Админ"; "editor" -> "Редактор"; else -> "Читатель"
}

@Composable
private fun MemberEditorDialog(
    member: StoryMember, editable: Boolean, canAssignAdmin: Boolean,
    onDismiss: () -> Unit, onSave: (String, MemberPermissions) -> Unit, onRemove: () -> Unit
) {
    var role by remember(member.userId) { mutableStateOf(member.role) }
    var permissions by remember(member.userId) { mutableStateOf(member.permissions) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(member.displayName.ifBlank { member.email }) },
        text = {
            Column(Modifier.heightIn(max = 510.dp)) {
                Text("Роль", fontWeight = FontWeight.SemiBold)
                val roles = if (canAssignAdmin) listOf("reader", "editor", "admin") else listOf("reader", "editor")
                roles.forEach { option ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable(enabled = editable) {
                        role = option; permissions = defaultPermissions(option)
                    }) {
                        RadioButton(selected = role == option, onClick = { role = option; permissions = defaultPermissions(option) }, enabled = editable)
                        Text(roleLabel(option))
                    }
                }
                HorizontalDivider()
                val ceiling = defaultPermissions(role)
                PermissionPicker("Блоки", permissions.blocks, listOf("view", "move", "edit", "full"), ceiling.blocks, editable) { permissions = permissions.copy(blocks = it) }
                PermissionPicker("Линии", permissions.lines, listOf("view", "move", "edit", "full"), ceiling.lines, editable) { permissions = permissions.copy(lines = it) }
                PermissionPicker("Сценарий", permissions.script, listOf("read", "edit", "full"), ceiling.script, editable) { permissions = permissions.copy(script = it) }
                PermissionPicker("Участники", permissions.members, listOf("none", "invite", "manage", "full"), ceiling.members, editable) { permissions = permissions.copy(members = it) }
                PermissionPicker("Экспорт", permissions.export, listOf("none", "export", "share"), ceiling.export, editable) { permissions = permissions.copy(export = it) }
                if (editable) TextButton(onClick = onRemove) { Text("Удалить доступ", color = Orange) }
            }
        },
        confirmButton = { if (editable) Button(onClick = { onSave(role, permissions) }, colors = ButtonDefaults.buttonColors(containerColor = Orange)) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } }
    )
}

@Composable
private fun PermissionPicker(title: String, selected: String, options: List<String>, ceiling: String, enabled: Boolean, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val allowed = options.take(options.indexOf(ceiling) + 1)
    Column {
        Spacer(Modifier.height(8.dp))
        Text(title, fontWeight = FontWeight.Medium)
        Box {
            TextButton(onClick = { expanded = true }, enabled = enabled) { Text("${selected} ▾", color = Olive) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                allowed.forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = { onChange(option); expanded = false },
                        leadingIcon = { RadioButton(selected = selected == option, onClick = null) })
                }
            }
        }
    }
}
