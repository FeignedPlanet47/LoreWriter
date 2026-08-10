package ru.it.novelforge.ui.stories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
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

    fun createStory(title: String, description: String) {
        viewModelScope.launch {
            val id = useCases.createStory(title, description)
            _openedStory.send(id)
        }
    }

    fun deleteStory(storyId: Long) {
        viewModelScope.launch { useCases.deleteStory(storyId) }
    }
}
