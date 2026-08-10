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
