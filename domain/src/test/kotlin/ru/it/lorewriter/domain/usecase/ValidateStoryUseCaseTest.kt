package ru.it.lorewriter.domain.usecase

import kotlin.test.Test
import kotlin.test.assertTrue
import ru.it.lorewriter.domain.model.NodeType
import ru.it.lorewriter.domain.model.Story
import ru.it.lorewriter.domain.model.StoryGraph
import ru.it.lorewriter.domain.model.StoryNode
import ru.it.lorewriter.domain.model.ValidationSeverity

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
