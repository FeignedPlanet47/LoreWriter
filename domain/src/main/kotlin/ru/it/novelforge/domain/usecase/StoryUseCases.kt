package ru.it.novelforge.domain.usecase

import kotlinx.coroutines.flow.Flow
import ru.it.novelforge.domain.model.CharacterProfile
import ru.it.novelforge.domain.model.NodeType
import ru.it.novelforge.domain.model.Story
import ru.it.novelforge.domain.model.StoryAccessRole
import ru.it.novelforge.domain.model.StoryConnection
import ru.it.novelforge.domain.model.StoryGraph
import ru.it.novelforge.domain.model.StoryNode
import ru.it.novelforge.domain.model.ValidationIssue
import ru.it.novelforge.domain.model.ValidationSeverity
import ru.it.novelforge.domain.repository.StoryRepository

class ObserveStoriesUseCase(
    private val repository: StoryRepository
) {
    operator fun invoke(): Flow<List<Story>> =
        repository.observeStories()
}

class CreateStoryUseCase(
    private val repository: StoryRepository
) {
    suspend operator fun invoke(
        title: String,
        description: String
    ): Long =
        repository.createStory(
            title.trim().ifBlank { "Новая новелла" },
            description.trim()
        )
}

class DeleteStoryUseCase(
    private val repository: StoryRepository
) {
    suspend operator fun invoke(storyId: Long) =
        repository.deleteStory(storyId)
}

class ObserveStoryGraphUseCase(
    private val repository: StoryRepository
) {
    operator fun invoke(storyId: Long): Flow<StoryGraph?> =
        repository.observeStoryGraph(storyId)
}

class GetStoryGraphUseCase(
    private val repository: StoryRepository
) {
    suspend operator fun invoke(storyId: Long): StoryGraph? =
        repository.getStoryGraph(storyId)
}


class ObserveCharactersUseCase(
    private val repository: StoryRepository
) {
    operator fun invoke(storyId: Long): Flow<List<CharacterProfile>> =
        repository.observeCharacters(storyId)
}

class UpdateManuscriptUseCase(
    private val repository: StoryRepository
) {
    suspend operator fun invoke(storyId: Long, manuscript: String) =
        repository.updateManuscript(storyId, manuscript)
}

class UpdateWordDocumentUseCase(
    private val repository: StoryRepository
) {
    suspend operator fun invoke(storyId: Long, wordDocument: String) =
        repository.updateWordDocument(storyId, wordDocument)
}

class UpsertCharacterUseCase(
    private val repository: StoryRepository
) {
    suspend operator fun invoke(character: CharacterProfile): Long =
        repository.upsertCharacter(character)
}

class DeleteCharacterUseCase(
    private val repository: StoryRepository
) {
    suspend operator fun invoke(characterId: Long) =
        repository.deleteCharacter(characterId)
}

class UpsertNodeUseCase(
    private val repository: StoryRepository
) {
    suspend operator fun invoke(node: StoryNode): Long =
        repository.upsertNode(node)
}

class DeleteNodeUseCase(
    private val repository: StoryRepository
) {
    suspend operator fun invoke(nodeId: Long) =
        repository.deleteNode(nodeId)
}

class UpdateNodePositionUseCase(
    private val repository: StoryRepository
) {
    suspend operator fun invoke(
        nodeId: Long,
        x: Float,
        y: Float
    ) = repository.updateNodePosition(nodeId, x, y)
}

class UpsertConnectionUseCase(
    private val repository: StoryRepository
) {
    suspend operator fun invoke(connection: StoryConnection): Long =
        repository.upsertConnection(connection)
}

class DeleteConnectionUseCase(
    private val repository: StoryRepository
) {
    suspend operator fun invoke(connectionId: Long) =
        repository.deleteConnection(connectionId)
}

class CreateStoryShareLinkUseCase(
    private val repository: StoryRepository
) {
    suspend operator fun invoke(storyId: Long, role: StoryAccessRole): String =
        repository.createShareLink(storyId, role)
}

class AcceptStoryShareLinkUseCase(
    private val repository: StoryRepository
) {
    suspend operator fun invoke(token: String): Long =
        repository.acceptShareLink(token)
}

class SyncStoriesUseCase(
    private val repository: StoryRepository
) {
    suspend operator fun invoke() =
        repository.syncAllStories()
}

class ValidateStoryUseCase {

    operator fun invoke(
        graph: StoryGraph
    ): List<ValidationIssue> {

        val issues =
            mutableListOf<ValidationIssue>()

        val storyNodes =
            graph.nodes.filterNot {
                it.type == NodeType.MODE_DIVIDER
            }

        val starts =
            storyNodes.filter {
                it.type == NodeType.START
            }

        if (storyNodes.isEmpty()) {
            return listOf(
                ValidationIssue(
                    ValidationSeverity.ERROR,
                    "В истории нет ни одного сюжетного узла"
                )
            )
        }

        if (starts.isEmpty()) {
            issues +=
                ValidationIssue(
                    ValidationSeverity.ERROR,
                    "Не найден стартовый узел"
                )
        }

        if (starts.size > 1) {
            issues +=
                ValidationIssue(
                    ValidationSeverity.ERROR,
                    "В истории должно быть только одно начало"
                )
        }

        if (
            storyNodes.none {
                it.type == NodeType.ENDING
            }
        ) {
            issues +=
                ValidationIssue(
                    ValidationSeverity.WARNING,
                    "В истории нет ни одной концовки"
                )
        }

        storyNodes.forEach { node ->

            val outgoing =
                graph
                    .outgoing(node.id)
                    .filter { connection ->
                        graph
                            .nodeById(connection.toNodeId)
                            ?.type != NodeType.MODE_DIVIDER
                    }

            when {
                node.type == NodeType.ENDING &&
                        outgoing.isNotEmpty() ->
                    issues +=
                        ValidationIssue(
                            ValidationSeverity.WARNING,
                            "У концовки «${node.title}» есть исходящий переход",
                            node.id
                        )

                node.type != NodeType.ENDING &&
                        outgoing.isEmpty() ->
                    issues +=
                        ValidationIssue(
                            ValidationSeverity.ERROR,
                            "Узел «${node.title}» не имеет продолжения",
                            node.id
                        )
            }

            if (
                node.type == NodeType.CHOICE &&
                outgoing.size < 2
            ) {
                issues +=
                    ValidationIssue(
                        ValidationSeverity.ERROR,
                        "Выбор «${node.title}» должен иметь минимум два варианта",
                        node.id
                    )
            }

            if (
                node.type == NodeType.CHOICE &&
                outgoing.any {
                    it.label.isBlank()
                }
            ) {
                issues +=
                    ValidationIssue(
                        ValidationSeverity.WARNING,
                        "У одного из вариантов выбора «${node.title}» нет подписи",
                        node.id
                    )
            }

            if (
                outgoing.any {
                    it.toNodeId == node.id
                }
            ) {
                issues +=
                    ValidationIssue(
                        ValidationSeverity.WARNING,
                        "Узел «${node.title}» ссылается сам на себя",
                        node.id
                    )
            }
        }

        val start =
            graph.startNode()

        if (start != null) {
            val reachable =
                mutableSetOf<Long>()

            val stack =
                ArrayDeque<Long>()

            stack.add(start.id)

            while (stack.isNotEmpty()) {
                val id =
                    stack.removeLast()

                if (!reachable.add(id)) {
                    continue
                }

                graph
                    .outgoing(id)
                    .filter { connection ->
                        graph
                            .nodeById(connection.toNodeId)
                            ?.type != NodeType.MODE_DIVIDER
                    }
                    .forEach {
                        stack.add(it.toNodeId)
                    }
            }

            storyNodes
                .filterNot {
                    it.id in reachable
                }
                .forEach { node ->
                    issues +=
                        ValidationIssue(
                            ValidationSeverity.WARNING,
                            "Узел «${node.title}» недостижим из начала истории",
                            node.id
                        )
                }
        }

        detectCycles(graph)
            .forEach { nodeId ->
                val node =
                    graph.nodeById(nodeId)

                issues +=
                    ValidationIssue(
                        ValidationSeverity.WARNING,
                        "Ветка с узлом «${node?.title ?: nodeId}» содержит цикл",
                        nodeId
                    )
            }

        return issues.distinctBy {
            Triple(
                it.severity,
                it.message,
                it.nodeId
            )
        }
    }

    private fun detectCycles(
        graph: StoryGraph
    ): Set<Long> {

        val state =
            mutableMapOf<Long, Int>()

        val cycleNodes =
            mutableSetOf<Long>()

        val storyNodeIds =
            graph.nodes
                .filterNot {
                    it.type == NodeType.MODE_DIVIDER
                }
                .mapTo(mutableSetOf()) {
                    it.id
                }

        fun visit(nodeId: Long) {
            state[nodeId] = 1

            graph
                .outgoing(nodeId)
                .filter {
                    it.toNodeId in storyNodeIds
                }
                .forEach { edge ->
                    when (
                        state[edge.toNodeId]
                            ?: 0
                    ) {
                        0 ->
                            visit(edge.toNodeId)

                        1 -> {
                            cycleNodes += nodeId
                            cycleNodes += edge.toNodeId
                        }
                    }
                }

            state[nodeId] = 2
        }

        storyNodeIds.forEach { nodeId ->
            if (
                (state[nodeId] ?: 0) == 0
            ) {
                visit(nodeId)
            }
        }

        return cycleNodes
    }
}

class ExportStoryTextUseCase {

    operator fun invoke(
        graph: StoryGraph
    ): String {

        val builder =
            StringBuilder()

        val storyNodes =
            graph.nodes.filterNot {
                it.type == NodeType.MODE_DIVIDER
            }

        val dividers =
            graph.nodes
                .filter {
                    it.type == NodeType.MODE_DIVIDER
                }
                .sortedBy {
                    it.positionY
                }

        builder.appendLine(
            graph.story.title.uppercase()
        )

        if (
            graph.story.description.isNotBlank()
        ) {
            builder.appendLine(
                graph.story.description
            )
        }

        builder.appendLine()
        builder.appendLine(
            "Сюжетных узлов: ${storyNodes.size}; " +
                    "переходов: ${graph.connections.size}; " +
                    "разделителей режимов: ${dividers.size}"
        )
        builder.appendLine(
            "=".repeat(64)
        )
        builder.appendLine()

        val visited =
            linkedSetOf<Long>()

        val start =
            graph.startNode()

        if (start != null) {
            appendNode(
                graph,
                start.id,
                visited,
                builder
            )
        }

        val unvisited =
            storyNodes.filterNot {
                it.id in visited
            }

        if (unvisited.isNotEmpty()) {
            builder.appendLine()
            builder.appendLine(
                "НЕДОСТИЖИМЫЕ ИЛИ ОТДЕЛЬНЫЕ СЦЕНЫ"
            )
            builder.appendLine(
                "=".repeat(64)
            )

            unvisited.forEach {
                appendNode(
                    graph,
                    it.id,
                    visited,
                    builder
                )
            }
        }

        if (dividers.isNotEmpty()) {
            builder.appendLine()
            builder.appendLine(
                "ПЕРЕХОДЫ МЕЖДУ РЕЖИМАМИ"
            )
            builder.appendLine(
                "=".repeat(64)
            )

            dividers.forEachIndexed { index, divider ->
                builder.appendLine(
                    "${index + 1}. ${divider.title.ifBlank { "Переход режима" }}"
                )
            }
        }

        return builder
            .toString()
            .trimEnd() + "\n"
    }

    private fun appendNode(
        graph: StoryGraph,
        nodeId: Long,
        visited: MutableSet<Long>,
        builder: StringBuilder
    ) {
        val node =
            graph.nodeById(nodeId)
                ?: return

        if (
            node.type == NodeType.MODE_DIVIDER ||
            !visited.add(nodeId)
        ) {
            return
        }

        builder.appendLine(
            "[${node.type}] ${node.title}  (ID: ${node.id})"
        )
        builder.appendLine(
            "-".repeat(64)
        )

        if (node.text.isNotBlank()) {
            builder.appendLine(
                node.text
            )
        }

        val outgoing =
            graph
                .outgoing(node.id)
                .filter { connection ->
                    graph
                        .nodeById(connection.toNodeId)
                        ?.type != NodeType.MODE_DIVIDER
                }

        if (outgoing.isEmpty()) {
            builder.appendLine(
                "→ Конец ветки"
            )
        } else {
            builder.appendLine()
            builder.appendLine(
                "ПЕРЕХОДЫ:"
            )

            outgoing.forEachIndexed { index, connection ->
                val target =
                    graph.nodeById(
                        connection.toNodeId
                    )

                val label =
                    connection.label.ifBlank {
                        "Продолжить"
                    }

                builder.appendLine(
                    "${index + 1}. $label → " +
                            "${target?.title ?: "Удалённый узел"} " +
                            "(ID: ${connection.toNodeId})"
                )
            }
        }

        builder.appendLine()
        builder.appendLine(
            "=".repeat(64)
        )
        builder.appendLine()

        outgoing.forEach {
            appendNode(
                graph,
                it.toNodeId,
                visited,
                builder
            )
        }
    }
}

class ExportStoryJsonUseCase {

    operator fun invoke(
        graph: StoryGraph
    ): String =
        buildString {
            appendLine("{")
            appendLine("  \"story\": {")
            appendLine("    \"id\": ${graph.story.id},")
            appendLine("    \"title\": \"${escape(graph.story.title)}\",")
            appendLine("    \"description\": \"${escape(graph.story.description)}\",")
            appendLine("    \"manuscript\": \"${escape(graph.story.manuscript)}\",")
            appendLine("    \"wordDocument\": \"${escape(graph.story.wordDocument)}\",")
            appendLine("    \"createdAt\": ${graph.story.createdAt},")
            appendLine("    \"updatedAt\": ${graph.story.updatedAt}")
            appendLine("  },")
            appendLine("  \"nodes\": [")

            graph.nodes.forEachIndexed { index, node ->
                appendLine("    {")
                appendLine("      \"id\": ${node.id},")
                appendLine("      \"storyId\": ${node.storyId},")
                appendLine("      \"title\": \"${escape(node.title)}\",")
                appendLine("      \"text\": \"${escape(node.text)}\",")
                appendLine("      \"formatting\": [")

                node.formatting.forEachIndexed { markIndex, mark ->
                    append(
                        "        {\"start\": ${mark.start}, " +
                                "\"end\": ${mark.end}, " +
                                "\"type\": \"${mark.type}\""
                    )

                    mark.value?.let {
                        append(
                            ", \"value\": \"${escape(it)}\""
                        )
                    }

                    append("}")
                    appendLine(
                        if (
                            markIndex < node.formatting.lastIndex
                        ) {
                            ","
                        } else {
                            ""
                        }
                    )
                }

                appendLine("      ],")
                appendLine("      \"type\": \"${node.type}\",")
                appendLine("      \"positionX\": ${node.positionX},")
                appendLine("      \"positionY\": ${node.positionY}")
                append("    }")
                appendLine(
                    if (index < graph.nodes.lastIndex) {
                        ","
                    } else {
                        ""
                    }
                )
            }

            appendLine("  ],")
            appendLine("  \"connections\": [")

            graph.connections.forEachIndexed { index, connection ->
                appendLine("    {")
                appendLine("      \"id\": ${connection.id},")
                appendLine("      \"storyId\": ${connection.storyId},")
                appendLine("      \"fromNodeId\": ${connection.fromNodeId},")
                appendLine("      \"toNodeId\": ${connection.toNodeId},")
                appendLine("      \"label\": \"${escape(connection.label)}\",")
                appendLine("      \"sortOrder\": ${connection.sortOrder}")
                append("    }")
                appendLine(
                    if (
                        index < graph.connections.lastIndex
                    ) {
                        ","
                    } else {
                        ""
                    }
                )
            }

            appendLine("  ]")
            appendLine("}")
        }

    private fun escape(
        value: String
    ): String =
        buildString(value.length) {
            value.forEach { char ->
                when (char) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(char)
                }
            }
        }
}

data class StoryUseCases(
    val observeStories: ObserveStoriesUseCase,
    val createStory: CreateStoryUseCase,
    val deleteStory: DeleteStoryUseCase,
    val observeStoryGraph: ObserveStoryGraphUseCase,
    val getStoryGraph: GetStoryGraphUseCase,
    val observeCharacters: ObserveCharactersUseCase,
    val updateManuscript: UpdateManuscriptUseCase,
    val updateWordDocument: UpdateWordDocumentUseCase,
    val upsertCharacter: UpsertCharacterUseCase,
    val deleteCharacter: DeleteCharacterUseCase,
    val upsertNode: UpsertNodeUseCase,
    val deleteNode: DeleteNodeUseCase,
    val updateNodePosition: UpdateNodePositionUseCase,
    val upsertConnection: UpsertConnectionUseCase,
    val deleteConnection: DeleteConnectionUseCase,
    val createShareLink: CreateStoryShareLinkUseCase,
    val acceptShareLink: AcceptStoryShareLinkUseCase,
    val syncStories: SyncStoriesUseCase,
    val validateStory: ValidateStoryUseCase,
    val exportText: ExportStoryTextUseCase,
    val exportJson: ExportStoryJsonUseCase
)
