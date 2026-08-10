package ru.it.novelforge.ui.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.it.novelforge.domain.model.CharacterProfile
import ru.it.novelforge.domain.model.NodeType
import ru.it.novelforge.domain.model.StoryConnection
import ru.it.novelforge.domain.model.StoryGraph
import ru.it.novelforge.domain.model.StoryNode
import ru.it.novelforge.domain.model.TextFormatMark
import ru.it.novelforge.domain.model.ValidationIssue
import ru.it.novelforge.domain.usecase.StoryUseCases

@HiltViewModel
class EditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val useCases: StoryUseCases
) : ViewModel() {

    val storyId: Long =
        checkNotNull(savedStateHandle["storyId"])

    val graph: StateFlow<StoryGraph?> =
        useCases
            .observeStoryGraph(storyId)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                null
            )

    val characters: StateFlow<List<CharacterProfile>> =
        useCases
            .observeCharacters(storyId)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    fun updateManuscript(text: String) {
        viewModelScope.launch {
            useCases.updateManuscript(
                storyId = storyId,
                manuscript = text
            )
        }
    }

    fun saveCharacter(character: CharacterProfile) {
        viewModelScope.launch {
            useCases.upsertCharacter(
                character.copy(
                    storyId = storyId,
                    name = character.name.trim().ifBlank { "Новый персонаж" },
                    role = character.role.trim(),
                    description = character.description.trim(),
                    personality = character.personality.trim(),
                    appearance = character.appearance.trim(),
                    notes = character.notes.trim()
                )
            )
        }
    }

    fun createCharacter(
        name: String,
        role: String,
        description: String,
        personality: String,
        appearance: String,
        notes: String
    ) {
        saveCharacter(
            CharacterProfile(
                storyId = storyId,
                name = name,
                role = role,
                description = description,
                personality = personality,
                appearance = appearance,
                notes = notes
            )
        )
    }

    fun deleteCharacter(characterId: Long) {
        viewModelScope.launch {
            useCases.deleteCharacter(characterId)
        }
    }

    fun saveNode(node: StoryNode) {
        viewModelScope.launch {
            if (node.type == NodeType.MODE_DIVIDER) {
                val currentGraph = graph.value
                currentGraph
                    ?.connections
                    ?.filter {
                        it.fromNodeId == node.id ||
                                it.toNodeId == node.id
                    }
                    ?.forEach { connection ->
                        useCases.deleteConnection(connection.id)
                    }
            }

            useCases.upsertNode(
                if (node.type == NodeType.MODE_DIVIDER) {
                    node.copy(
                        text = "",
                        formatting = emptyList()
                    )
                } else {
                    node
                }
            )
        }
    }

    fun createNode(
        type: NodeType,
        title: String,
        text: String,
        formatting: List<TextFormatMark>,
        x: Float,
        y: Float
    ) {
        viewModelScope.launch {
            useCases.upsertNode(
                StoryNode(
                    storyId = storyId,
                    title =
                        title
                            .trim()
                            .ifBlank {
                                type.displayName()
                            },
                    text =
                        if (type == NodeType.MODE_DIVIDER) {
                            ""
                        } else {
                            text
                        },
                    formatting =
                        if (type == NodeType.MODE_DIVIDER) {
                            emptyList()
                        } else {
                            formatting
                        },
                    type = type,
                    positionX = x,
                    positionY = y
                )
            )
        }
    }

    fun deleteNode(node: StoryNode) {
        if (node.type == NodeType.START) {
            return
        }

        viewModelScope.launch {
            useCases.deleteNode(node.id)
        }
    }

    fun moveNode(
        nodeId: Long,
        x: Float,
        y: Float
    ) {
        viewModelScope.launch {
            useCases.updateNodePosition(
                nodeId,
                x.coerceAtLeast(0f),
                y.coerceAtLeast(0f)
            )
        }
    }

    fun addConnection(
        fromNodeId: Long,
        toNodeId: Long,
        label: String
    ) {
        val currentGraph = graph.value ?: return
        val fromNode = currentGraph.nodeById(fromNodeId)
        val toNode = currentGraph.nodeById(toNodeId)

        if (
            fromNode?.type == NodeType.MODE_DIVIDER ||
            toNode?.type == NodeType.MODE_DIVIDER
        ) {
            return
        }

        val sortOrder =
            currentGraph
                .outgoing(fromNodeId)
                .size

        viewModelScope.launch {
            useCases.upsertConnection(
                StoryConnection(
                    storyId = storyId,
                    fromNodeId = fromNodeId,
                    toNodeId = toNodeId,
                    label = label.trim(),
                    sortOrder = sortOrder
                )
            )
        }
    }

    fun deleteConnection(connectionId: Long) {
        viewModelScope.launch {
            useCases.deleteConnection(connectionId)
        }
    }

    fun validate(): List<ValidationIssue> =
        graph.value
            ?.let(useCases.validateStory::invoke)
            .orEmpty()

    fun exportText(): String =
        graph.value
            ?.let(useCases.exportText::invoke)
            .orEmpty()

    fun exportJson(): String =
        graph.value
            ?.let(useCases.exportJson::invoke)
            .orEmpty()
}

fun NodeType.displayName(): String =
    when (this) {
        NodeType.START -> "Начало"
        NodeType.SCENE -> "Сцена"
        NodeType.DIALOGUE -> "Диалог"
        NodeType.ACTION -> "Действие"
        NodeType.POSITIVE_ACTION -> "Позитивное действие"
        NodeType.NEGATIVE_ACTION -> "Негативное действие"
        NodeType.CHOICE -> "Выбор"
        NodeType.INSPECTION -> "Осмотр"
        NodeType.ENDING -> "Концовка"
        NodeType.MODE_DIVIDER -> "Разделитель режима"
    }
