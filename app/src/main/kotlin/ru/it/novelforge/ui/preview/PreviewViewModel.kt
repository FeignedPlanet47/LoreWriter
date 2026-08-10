package ru.it.novelforge.ui.preview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.it.novelforge.domain.model.StoryConnection
import ru.it.novelforge.domain.model.StoryGraph
import ru.it.novelforge.domain.usecase.StoryUseCases

@HiltViewModel
class PreviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    useCases: StoryUseCases
) : ViewModel() {
    private val storyId: Long = checkNotNull(savedStateHandle["storyId"])

    val graph: StateFlow<StoryGraph?> = useCases.observeStoryGraph(storyId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _currentNodeId = MutableStateFlow<Long?>(null)
    val currentNodeId: StateFlow<Long?> = _currentNodeId

    private val history = ArrayDeque<Long>()

    init {
        viewModelScope.launch {
            graph.filterNotNull().collect { loaded ->
                val current = _currentNodeId.value
                if (current == null || loaded.nodeById(current) == null) {
                    _currentNodeId.value = loaded.startNode()?.id
                    history.clear()
                }
            }
        }
    }

    fun choose(connection: StoryConnection) {
        _currentNodeId.value?.let { history.addLast(it) }
        _currentNodeId.value = connection.toNodeId
    }

    fun back(): Boolean {
        if (history.isEmpty()) return false
        _currentNodeId.value = history.removeLast()
        return true
    }

    fun restart() {
        history.clear()
        _currentNodeId.value = graph.value?.startNode()?.id
    }
}
