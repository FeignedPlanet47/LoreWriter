package ru.it.novelforge.ui.stories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.it.novelforge.domain.usecase.StoryUseCases

@HiltViewModel
class StoriesViewModel @Inject constructor(
    private val useCases: StoryUseCases
) : ViewModel() {
    val stories = useCases.observeStories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _openedStory = Channel<Long>(Channel.BUFFERED)
    val openedStory = _openedStory.receiveAsFlow()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    fun createStory(title: String, description: String) {
        viewModelScope.launch {
            val id = useCases.createStory(title, description)
            _openedStory.send(id)
        }
    }

    fun deleteStory(storyId: Long) {
        viewModelScope.launch {
            useCases.deleteStory(storyId)
        }
    }

    fun sync() {
        if (_syncState.value is SyncState.Syncing) return
        viewModelScope.launch {
            _syncState.value = SyncState.Syncing
            runCatching { useCases.syncStories() }
                .onSuccess {
                    _syncState.value = SyncState.Success
                }
                .onFailure { error ->
                    val message = error.message.orEmpty()
                    _syncState.value = SyncState.Error(
                        if (message.contains("409")) {
                            "История была изменена на другом устройстве. " +
                                "Локальная версия не перезаписана. Синхронизация остановлена."
                        } else {
                            message.ifBlank { "Ошибка синхронизации" }
                        }
                    )
                }
        }
    }

    fun resetSyncMessage() {
        _syncState.value = SyncState.Idle
    }
}

sealed interface SyncState {
    data object Idle : SyncState
    data object Syncing : SyncState
    data object Success : SyncState
    data class Error(val message: String) : SyncState
}
