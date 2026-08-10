package ru.it.novelforge.data.repository

import androidx.room.withTransaction
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import ru.it.novelforge.data.local.NovelDatabase
import ru.it.novelforge.data.local.StoryDao
import ru.it.novelforge.data.local.StoryEntity
import ru.it.novelforge.data.local.StoryNodeEntity
import ru.it.novelforge.data.mapper.toDomain
import ru.it.novelforge.data.mapper.toEntity
import ru.it.novelforge.domain.model.CharacterProfile
import ru.it.novelforge.domain.model.NodeType
import ru.it.novelforge.domain.model.Story
import ru.it.novelforge.domain.model.StoryConnection
import ru.it.novelforge.domain.model.StoryGraph
import ru.it.novelforge.domain.model.StoryNode
import ru.it.novelforge.domain.repository.StoryRepository

@Singleton
class StoryRepositoryImpl @Inject constructor(
    private val database: NovelDatabase,
    private val dao: StoryDao
) : StoryRepository {

    override fun observeStories(): Flow<List<Story>> =
        dao.observeStories().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun observeStoryGraph(storyId: Long): Flow<StoryGraph?> =
        combine(
            dao.observeStory(storyId),
            dao.observeNodes(storyId),
            dao.observeConnections(storyId)
        ) { story, nodes, connections ->
            story?.let {
                StoryGraph(
                    story = it.toDomain(),
                    nodes = nodes.map { node -> node.toDomain() },
                    connections = connections.map { connection -> connection.toDomain() }
                )
            }
        }

    override fun observeCharacters(storyId: Long): Flow<List<CharacterProfile>> =
        dao.observeCharacters(storyId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getStoryGraph(storyId: Long): StoryGraph? {
        val story = dao.getStory(storyId) ?: return null
        return StoryGraph(
            story = story.toDomain(),
            nodes = dao.getNodes(storyId).map { it.toDomain() },
            connections = dao.getConnections(storyId).map { it.toDomain() }
        )
    }

    override suspend fun createStory(title: String, description: String): Long =
        database.withTransaction {
            val now = System.currentTimeMillis()
            val storyId = dao.insertStory(
                StoryEntity(
                    title = title,
                    description = description,
                    manuscript = "",
                    createdAt = now,
                    updatedAt = now
                )
            )

            dao.insertNode(
                StoryNodeEntity(
                    storyId = storyId,
                    title = "Начало",
                    text = "Здесь начинается история.",
                    type = NodeType.START.name,
                    positionX = 100f,
                    positionY = 100f
                )
            )

            storyId
        }

    override suspend fun updateStory(story: Story) {
        dao.updateStory(
            story
                .copy(updatedAt = System.currentTimeMillis())
                .toEntity()
        )
    }

    override suspend fun updateManuscript(storyId: Long, manuscript: String) {
        dao.updateManuscript(
            storyId = storyId,
            manuscript = manuscript,
            timestamp = System.currentTimeMillis()
        )
    }

    override suspend fun deleteStory(storyId: Long) =
        dao.deleteStory(storyId)

    override suspend fun upsertCharacter(character: CharacterProfile): Long =
        database.withTransaction {
            val id = if (character.id == 0L) {
                dao.insertCharacter(character.toEntity())
            } else {
                dao.updateCharacter(character.toEntity())
                character.id
            }

            dao.touchStory(character.storyId, System.currentTimeMillis())
            id
        }

    override suspend fun deleteCharacter(characterId: Long) =
        database.withTransaction {
            val storyId = dao.getCharacterStoryId(characterId)
            dao.deleteCharacter(characterId)
            if (storyId != null) {
                dao.touchStory(storyId, System.currentTimeMillis())
            }
        }

    override suspend fun upsertNode(node: StoryNode): Long =
        database.withTransaction {
            val id = if (node.id == 0L) {
                dao.insertNode(node.toEntity())
            } else {
                dao.updateNode(node.toEntity())
                node.id
            }

            dao.touchStory(node.storyId, System.currentTimeMillis())
            id
        }

    override suspend fun deleteNode(nodeId: Long) =
        database.withTransaction {
            val storyId = dao.getNodeStoryId(nodeId)
            dao.deleteNode(nodeId)
            if (storyId != null) {
                dao.touchStory(storyId, System.currentTimeMillis())
            }
        }

    override suspend fun updateNodePosition(nodeId: Long, x: Float, y: Float) =
        database.withTransaction {
            val storyId = dao.getNodeStoryId(nodeId)
            dao.updateNodePosition(nodeId, x, y)
            if (storyId != null) {
                dao.touchStory(storyId, System.currentTimeMillis())
            }
        }

    override suspend fun upsertConnection(connection: StoryConnection): Long =
        database.withTransaction {
            val id = if (connection.id == 0L) {
                dao.insertConnection(connection.toEntity())
            } else {
                dao.updateConnection(connection.toEntity())
                connection.id
            }

            dao.touchStory(connection.storyId, System.currentTimeMillis())
            id
        }

    override suspend fun deleteConnection(connectionId: Long) =
        database.withTransaction {
            val storyId = dao.getConnectionStoryId(connectionId)
            dao.deleteConnection(connectionId)
            if (storyId != null) {
                dao.touchStory(storyId, System.currentTimeMillis())
            }
        }
}
