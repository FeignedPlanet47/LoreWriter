package ru.it.novelforge.ui.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.it.novelforge.domain.model.CharacterProfile
import ru.it.novelforge.domain.model.CollaboratorPermissions
import ru.it.novelforge.domain.model.NodeType
import ru.it.novelforge.domain.model.StoryAccessRole
import ru.it.novelforge.domain.model.StoryCollaborator
import ru.it.novelforge.domain.model.StoryConnection
import ru.it.novelforge.domain.model.StoryGraph
import ru.it.novelforge.domain.model.StoryNode
import ru.it.novelforge.domain.model.TextFormatMark
import ru.it.novelforge.domain.model.ValidationIssue
import ru.it.novelforge.domain.usecase.StoryUseCases

data class CollaboratorsUiState(
    val loading: Boolean = false,
    val members: List<StoryCollaborator> = emptyList(),
    val error: String? = null
)

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

    private val _collaboratorsUiState = MutableStateFlow(CollaboratorsUiState())
    val collaboratorsUiState: StateFlow<CollaboratorsUiState> =
        _collaboratorsUiState.asStateFlow()

    init {
        viewModelScope.launch {
            graph
                .filterNotNull()
                .map { it.story.cloudId }
                .distinctUntilChanged()
                .collect { cloudId ->
                    if (cloudId != null) {
                        refreshCollaborators()
                    } else {
                        _collaboratorsUiState.value = CollaboratorsUiState()
                    }
                }
        }
    }

    val characters: StateFlow<List<CharacterProfile>> =
        useCases
            .observeCharacters(storyId)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    fun refreshCollaborators() {
        if (graph.value?.story?.cloudId == null) return

        viewModelScope.launch {
            _collaboratorsUiState.value =
                _collaboratorsUiState.value.copy(loading = true, error = null)

            runCatching {
                useCases.getCollaborators(storyId)
            }.onSuccess { members ->
                _collaboratorsUiState.value = CollaboratorsUiState(
                    loading = false,
                    members = members
                )
            }.onFailure { error ->
                _collaboratorsUiState.value =
                    _collaboratorsUiState.value.copy(
                        loading = false,
                        error = error.message ?: "Не удалось загрузить участников"
                    )
            }
        }
    }

    fun updateCollaborator(
        userId: String,
        role: StoryAccessRole,
        permissions: CollaboratorPermissions
    ) {
        viewModelScope.launch {
            _collaboratorsUiState.value =
                _collaboratorsUiState.value.copy(loading = true, error = null)

            runCatching {
                useCases.updateCollaborator(
                    storyId = storyId,
                    userId = userId,
                    role = role,
                    permissions = permissions
                )
            }.onSuccess { updated ->
                val members = _collaboratorsUiState.value.members.map { member ->
                    if (member.userId == updated.userId) updated else member
                }
                _collaboratorsUiState.value = CollaboratorsUiState(
                    loading = false,
                    members = members
                )
            }.onFailure { error ->
                _collaboratorsUiState.value =
                    _collaboratorsUiState.value.copy(
                        loading = false,
                        error = error.message ?: "Не удалось изменить доступ"
                    )
            }
        }
    }

    fun removeCollaborator(userId: String) {
        viewModelScope.launch {
            _collaboratorsUiState.value =
                _collaboratorsUiState.value.copy(loading = true, error = null)

            runCatching {
                useCases.removeCollaborator(storyId, userId)
            }.onSuccess {
                _collaboratorsUiState.value = CollaboratorsUiState(
                    loading = false,
                    members = _collaboratorsUiState.value.members.filterNot { it.userId == userId }
                )
            }.onFailure { error ->
                _collaboratorsUiState.value =
                    _collaboratorsUiState.value.copy(
                        loading = false,
                        error = error.message ?: "Не удалось удалить доступ"
                    )
            }
        }
    }

    fun updateManuscript(text: String) {
        if (!canEditCurrentStory()) return
        viewModelScope.launch {
            useCases.updateManuscript(
                storyId = storyId,
                manuscript = text
            )
        }
    }

    fun updateWordDocument(text: String) {
        if (!canEditCurrentStory()) return
        viewModelScope.launch {
            useCases.updateWordDocument(
                storyId = storyId,
                wordDocument = text
            )
        }
    }

    fun buildWordDocumentFromStory(): String =
        exportText()

    fun saveCharacter(character: CharacterProfile) {
        if (!canEditCurrentStory()) return
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
        if (!canEditCurrentStory()) return
        viewModelScope.launch {
            useCases.deleteCharacter(characterId)
        }
    }

    fun saveNode(node: StoryNode) {
        if (!canEditCurrentStory()) return
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
        if (!canEditCurrentStory()) return
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
        if (!canEditCurrentStory()) return
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
        if (!canEditCurrentStory()) return
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
        if (!canEditCurrentStory()) return
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
        if (!canEditCurrentStory()) return
        viewModelScope.launch {
            useCases.deleteConnection(connectionId)
        }
    }

    private fun canEditCurrentStory(): Boolean =
        graph.value?.story?.accessRole?.canEdit == true

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
