package ru.it.novelforge.domain.model

enum class TextMarkType {
    BOLD,
    ITALIC,
    STRIKETHROUGH,
    COLOR
}

data class TextFormatMark(
    val start: Int,
    val end: Int,
    val type: TextMarkType,
    val value: String? = null
)


enum class StoryAccessRole {
    OWNER,
    ADMIN,
    EDITOR,
    READER;

    val canEdit: Boolean
        get() = this != READER

    val canShare: Boolean
        get() = this == OWNER
}

enum class BlockAccessLevel {
    VIEW,
    MOVE,
    EDIT,
    FULL
}

enum class LineAccessLevel {
    VIEW,
    MOVE,
    EDIT,
    FULL
}

enum class ScriptAccessLevel {
    READ,
    EDIT,
    FULL
}

enum class MemberAccessLevel {
    NONE,
    INVITE,
    MANAGE,
    FULL
}

enum class ExportAccessLevel {
    NONE,
    EXPORT,
    SHARE
}

data class CollaboratorPermissions(
    val blocks: BlockAccessLevel = BlockAccessLevel.VIEW,
    val lines: LineAccessLevel = LineAccessLevel.VIEW,
    val script: ScriptAccessLevel = ScriptAccessLevel.READ,
    val members: MemberAccessLevel = MemberAccessLevel.NONE,
    val export: ExportAccessLevel = ExportAccessLevel.NONE
)

data class StoryCollaborator(
    val userId: String,
    val email: String,
    val displayName: String = "",
    val role: StoryAccessRole,
    val permissions: CollaboratorPermissions
) {
    val visibleName: String
        get() = displayName.ifBlank { email.substringBefore("@").ifBlank { email } }
}

fun defaultPermissionsFor(role: StoryAccessRole): CollaboratorPermissions =
    when (role) {
        StoryAccessRole.OWNER -> CollaboratorPermissions(
            blocks = BlockAccessLevel.FULL,
            lines = LineAccessLevel.FULL,
            script = ScriptAccessLevel.FULL,
            members = MemberAccessLevel.FULL,
            export = ExportAccessLevel.SHARE
        )

        StoryAccessRole.ADMIN -> CollaboratorPermissions(
            blocks = BlockAccessLevel.FULL,
            lines = LineAccessLevel.FULL,
            script = ScriptAccessLevel.FULL,
            members = MemberAccessLevel.MANAGE,
            export = ExportAccessLevel.SHARE
        )

        StoryAccessRole.EDITOR -> CollaboratorPermissions(
            blocks = BlockAccessLevel.EDIT,
            lines = LineAccessLevel.EDIT,
            script = ScriptAccessLevel.EDIT,
            members = MemberAccessLevel.NONE,
            export = ExportAccessLevel.EXPORT
        )

        StoryAccessRole.READER -> CollaboratorPermissions()
    }


enum class NodeType {
    START,
    SCENE,
    DIALOGUE,
    ACTION,
    POSITIVE_ACTION,
    NEGATIVE_ACTION,
    CHOICE,
    INSPECTION,
    ENDING,
    MODE_DIVIDER
}

data class Story(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val manuscript: String = "",
    val wordDocument: String = "",
    val ownerUserId: String = "",
    val cloudId: String? = null,
    val cloudRevision: Long = 0,
    val isDirty: Boolean = true,
    val pendingDelete: Boolean = false,
    val accessRole: StoryAccessRole = StoryAccessRole.OWNER,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class CharacterProfile(
    val id: Long = 0,
    val storyId: Long,
    val name: String,
    val role: String = "",
    val description: String = "",
    val personality: String = "",
    val appearance: String = "",
    val notes: String = ""
)

data class StoryNode(
    val id: Long = 0,
    val storyId: Long,
    val title: String,
    val text: String = "",
    val formatting: List<TextFormatMark> = emptyList(),
    val type: NodeType,
    val positionX: Float = 80f,
    val positionY: Float = 80f
)

data class StoryConnection(
    val id: Long = 0,
    val storyId: Long,
    val fromNodeId: Long,
    val toNodeId: Long,
    val label: String = "",
    val sortOrder: Int = 0
)

data class StoryGraph(
    val story: Story,
    val nodes: List<StoryNode>,
    val connections: List<StoryConnection>
) {
    fun nodeById(id: Long): StoryNode? =
        nodes.firstOrNull { it.id == id }

    fun outgoing(nodeId: Long): List<StoryConnection> =
        connections
            .filter { it.fromNodeId == nodeId }
            .sortedBy { it.sortOrder }

    fun incoming(nodeId: Long): List<StoryConnection> =
        connections.filter { it.toNodeId == nodeId }

    fun startNode(): StoryNode? =
        nodes.firstOrNull { it.type == NodeType.START }
            ?: nodes.firstOrNull { it.type != NodeType.MODE_DIVIDER }
}

enum class ValidationSeverity {
    ERROR,
    WARNING
}

data class ValidationIssue(
    val severity: ValidationSeverity,
    val message: String,
    val nodeId: Long? = null
)
