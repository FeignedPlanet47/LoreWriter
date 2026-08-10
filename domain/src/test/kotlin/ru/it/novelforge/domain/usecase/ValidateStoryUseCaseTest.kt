package ru.it.novelforge.domain.usecase

import kotlin.test.Test
import kotlin.test.assertTrue
import ru.it.novelforge.domain.model.NodeType
import ru.it.novelforge.domain.model.Story
import ru.it.novelforge.domain.model.StoryGraph
import ru.it.novelforge.domain.model.StoryNode
import ru.it.novelforge.domain.model.ValidationSeverity

class ValidateStoryUseCaseTest {
    @Test
    fun `choice without two exits is error`() {
        val graph = StoryGraph(
            story = Story(id = 1, title = "Test"),
            nodes = listOf(
                StoryNode(id = 1, storyId = 1, title = "Start", type = NodeType.START),
                StoryNode(id = 2, storyId = 1, title = "Choice", type = NodeType.CHOICE)
            ),
            connections = emptyList()
        )

        val issues = ValidateStoryUseCase()(graph)
        assertTrue(issues.any { it.severity == ValidationSeverity.ERROR })
    }
}
