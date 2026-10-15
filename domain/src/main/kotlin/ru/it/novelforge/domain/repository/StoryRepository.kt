package ru.it.novelforge.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.it.novelforge.domain.model.CharacterProfile
import ru.it.novelforge.domain.model.Story
import ru.it.novelforge.domain.model.StoryConnection
import ru.it.novelforge.domain.model.StoryGraph
import ru.it.novelforge.domain.model.StoryNode
import ru.it.novelforge.domain.model.CollaboratorPermissions
import ru.it.novelforge.domain.model.StoryAccessRole
import ru.it.novelforge.domain.model.StoryCollaborator

interface StoryRepository {
    fun observeStories(): Flow<List<Story>>
    fun observeStoryGraph(storyId: Long): Flow<StoryGraph?>
    fun observeCharacters(storyId: Long): Flow<List<CharacterProfile>>

    suspend fun getStoryGraph(storyId: Long): StoryGraph?
    suspend fun createStory(title: String, description: String): Long
    suspend fun updateStory(story: Story)
    suspend fun updateManuscript(storyId: Long, manuscript: String)
    suspend fun updateWordDocument(storyId: Long, wordDocument: String)
    suspend fun deleteStory(storyId: Long)

    suspend fun upsertCharacter(character: CharacterProfile): Long
    suspend fun deleteCharacter(characterId: Long)

    suspend fun upsertNode(node: StoryNode): Long
    suspend fun deleteNode(nodeId: Long)
    suspend fun updateNodePosition(nodeId: Long, x: Float, y: Float)

    suspend fun upsertConnection(connection: StoryConnection): Long
    suspend fun deleteConnection(connectionId: Long)

    suspend fun createShareLink(storyId: Long, role: StoryAccessRole): String
    suspend fun acceptShareLink(token: String): Long

    suspend fun getCollaborators(storyId: Long): List<StoryCollaborator>
    suspend fun updateCollaborator(
        storyId: Long,
        userId: String,
        role: StoryAccessRole,
        permissions: CollaboratorPermissions
    ): StoryCollaborator
    suspend fun removeCollaborator(storyId: Long, userId: String)

    suspend fun syncAllStories()
    suspend fun clearLocalStories()
}
