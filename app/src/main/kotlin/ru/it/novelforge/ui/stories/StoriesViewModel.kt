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
import ru.it.novelforge.domain.model.StoryAccessRole
import ru.it.novelforge.domain.usecase.StoryUseCases

@HiltViewModel
class StoriesViewModel @Inject constructor(
    private val useCases: StoryUseCases
) : ViewModel() {
    val stories = useCases.observeStories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _openedStory = Channel<Long>(Channel.BUFFERED)
    val openedStory = _openedStory.receiveAsFlow()

    private val _shareEvents = Channel<ShareEvent>(Channel.BUFFERED)
    val shareEvents = _shareEvents.receiveAsFlow()

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
            try {
                useCases.deleteStory(storyId)
            } catch (error: Throwable) {
                _shareEvents.send(
                    ShareEvent.Error(error.message.ifBlankFallback("Не удалось удалить историю"))
                )
            }
        }
    }

    fun createShareLink(storyId: Long, role: StoryAccessRole) {
        viewModelScope.launch {
            try {
                val link = useCases.createShareLink(storyId, role)
                _shareEvents.send(ShareEvent.LinkReady(link, role))
            } catch (error: Throwable) {
                _shareEvents.send(
                    ShareEvent.Error(error.message.ifBlankFallback("Не удалось создать ссылку"))
                )
            }
        }
    }

    fun acceptShareLink(token: String) {
        viewModelScope.launch {
            try {
                val storyId = useCases.acceptShareLink(token)
                _shareEvents.send(ShareEvent.AccessGranted)
                _openedStory.send(storyId)
            } catch (error: Throwable) {
                _shareEvents.send(
                    ShareEvent.Error(
                        error.message.ifBlankFallback(
                            "Ссылка недействительна или доступ был отозван"
                        )
                    )
                )
            }
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

private fun String?.ifBlankFallback(fallback: String): String =
    this?.takeIf { it.isNotBlank() } ?: fallback

sealed interface ShareEvent {
    data class LinkReady(
        val link: String,
        val role: StoryAccessRole
    ) : ShareEvent

    data object AccessGranted : ShareEvent
    data class Error(val message: String) : ShareEvent
}

sealed interface SyncState {
    data object Idle : SyncState
    data object Syncing : SyncState
    data object Success : SyncState
    data class Error(val message: String) : SyncState
}
