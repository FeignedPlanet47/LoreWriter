package ru.it.novelforge.data.repository

import androidx.room.withTransaction
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.HttpException
import ru.it.novelforge.data.auth.SecureSessionStore
import ru.it.novelforge.data.local.CharacterEntity
import ru.it.novelforge.data.local.NovelDatabase
import ru.it.novelforge.data.local.StoryConnectionEntity
import ru.it.novelforge.data.local.StoryDao
import ru.it.novelforge.data.local.StoryEntity
import ru.it.novelforge.data.local.StoryNodeEntity
import ru.it.novelforge.data.mapper.toDomain
import ru.it.novelforge.data.mapper.toEntity
import ru.it.novelforge.data.network.CharacterDto
import ru.it.novelforge.data.network.StoryApi
import ru.it.novelforge.data.network.StoryConnectionDto
import ru.it.novelforge.data.network.StoryNodeDto
import ru.it.novelforge.data.network.StoryPayloadDto
import ru.it.novelforge.data.network.StoryResponseDto
import ru.it.novelforge.data.network.StoryUpsertRequestDto
import ru.it.novelforge.data.network.TextFormatMarkDto
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
    private val dao: StoryDao,
    private val storyApi: StoryApi,
    private val sessionStore: SecureSessionStore
) : StoryRepository {

    private val uploadLocks = ConcurrentHashMap<Long, Mutex>()

    override fun observeStories(): Flow<List<Story>> =
        sessionStore.currentUser.flatMapLatest { user ->
            if (user == null) {
                flowOf(emptyList())
            } else {
                dao.observeStories(user.id).map { entities ->
                    entities.map { it.toDomain() }
                }
            }
        }

    override fun observeStoryGraph(storyId: Long): Flow<StoryGraph?> =
        sessionStore.currentUser.flatMapLatest { user ->
            if (user == null) {
                flowOf(null)
            } else {
                combine(
                    dao.observeStory(storyId),
                    dao.observeNodes(storyId),
                    dao.observeConnections(storyId)
                ) { story, nodes, connections ->
                    story
                        ?.takeIf {
                            it.ownerUserId == user.id && !it.pendingDelete
                        }
                        ?.let { ownedStory ->
                            StoryGraph(
                                story = ownedStory.toDomain(),
                                nodes = nodes.map { node -> node.toDomain() },
                                connections = connections.map { connection -> connection.toDomain() }
                            )
                        }
                }
            }
        }

    override fun observeCharacters(storyId: Long): Flow<List<CharacterProfile>> =
        sessionStore.currentUser.flatMapLatest { user ->
            if (user == null) {
                flowOf(emptyList())
            } else {
                combine(
                    dao.observeStory(storyId),
                    dao.observeCharacters(storyId)
                ) { story, entities ->
                    if (story?.ownerUserId == user.id && !story.pendingDelete) {
                        entities.map { it.toDomain() }
                    } else {
                        emptyList()
                    }
                }
            }
        }

    override suspend fun getStoryGraph(storyId: Long): StoryGraph? {
        val ownerUserId = sessionStore.currentUser.value?.id ?: return null
        val story = dao.getStory(storyId)
            ?.takeIf { it.ownerUserId == ownerUserId && !it.pendingDelete }
            ?: return null

        return StoryGraph(
            story = story.toDomain(),
            nodes = dao.getNodes(storyId).map { it.toDomain() },
            connections = dao.getConnections(storyId).map { it.toDomain() }
        )
    }

    override suspend fun createStory(title: String, description: String): Long {
        val ownerUserId = checkNotNull(sessionStore.currentUser.value) {
            "Для создания истории необходимо войти в аккаунт"
        }.id

        val storyId = database.withTransaction {
            val now = System.currentTimeMillis()
            val id = dao.insertStory(
                StoryEntity(
                    title = title,
                    description = description,
                    manuscript = "",
                    wordDocument = "",
                    ownerUserId = ownerUserId,
                    cloudId = null,
                    cloudRevision = 0,
                    isDirty = true,
                    pendingDelete = false,
                    createdAt = now,
                    updatedAt = now
                )
            )

            dao.insertNode(
                StoryNodeEntity(
                    storyId = id,
                    title = "Начало",
                    text = "Здесь начинается история.",
                    type = NodeType.START.name,
                    positionX = 100f,
                    positionY = 100f
                )
            )
            id
        }

        tryPushStory(storyId)
        return storyId
    }

    override suspend fun updateStory(story: Story) {
        val existing = requireOwnedStory(story.id)
        dao.updateStory(
            story.copy(
                ownerUserId = existing.ownerUserId,
                cloudId = existing.cloudId,
                cloudRevision = existing.cloudRevision,
                pendingDelete = existing.pendingDelete,
                createdAt = existing.createdAt,
                updatedAt = System.currentTimeMillis(),
                isDirty = true
            ).toEntity()
        )
        tryPushStory(story.id)
    }

    override suspend fun updateManuscript(storyId: Long, manuscript: String) {
        requireOwnedStory(storyId)
        dao.updateManuscript(storyId, manuscript, System.currentTimeMillis())
        tryPushStory(storyId)
    }

    override suspend fun updateWordDocument(storyId: Long, wordDocument: String) {
        requireOwnedStory(storyId)
        dao.updateWordDocument(storyId, wordDocument, System.currentTimeMillis())
        tryPushStory(storyId)
    }

    override suspend fun deleteStory(storyId: Long) {
        val story = requireOwnedStory(storyId)
        dao.markStoryPendingDelete(storyId, System.currentTimeMillis())

        if (story.cloudId == null || sessionStore.currentUser.value == null) {
            dao.hardDeleteStory(storyId)
            return
        }

        runCatching {
            storyApi.deleteStory(story.cloudId)
            dao.hardDeleteStory(storyId)
        }
    }

    override suspend fun upsertCharacter(character: CharacterProfile): Long {
        requireOwnedStory(character.storyId)
        val id = database.withTransaction {
            val savedId = if (character.id == 0L) {
                dao.insertCharacter(character.toEntity())
            } else {
                dao.updateCharacter(character.toEntity())
                character.id
            }
            dao.touchStory(character.storyId, System.currentTimeMillis())
            savedId
        }
        tryPushStory(character.storyId)
        return id
    }

    override suspend fun deleteCharacter(characterId: Long) {
        val owningStoryId = dao.getCharacterStoryId(characterId) ?: return
        requireOwnedStory(owningStoryId)
        val storyId = database.withTransaction {
            val id = dao.getCharacterStoryId(characterId)
            dao.deleteCharacter(characterId)
            if (id != null) dao.touchStory(id, System.currentTimeMillis())
            id
        }
        storyId?.let { tryPushStory(it) }
    }

    override suspend fun upsertNode(node: StoryNode): Long {
        requireOwnedStory(node.storyId)
        val id = database.withTransaction {
            val savedId = if (node.id == 0L) {
                dao.insertNode(node.toEntity())
            } else {
                dao.updateNode(node.toEntity())
                node.id
            }
            dao.touchStory(node.storyId, System.currentTimeMillis())
            savedId
        }
        tryPushStory(node.storyId)
        return id
    }

    override suspend fun deleteNode(nodeId: Long) {
        val owningStoryId = dao.getNodeStoryId(nodeId) ?: return
        requireOwnedStory(owningStoryId)
        val storyId = database.withTransaction {
            val id = dao.getNodeStoryId(nodeId)
            dao.deleteNode(nodeId)
            if (id != null) dao.touchStory(id, System.currentTimeMillis())
            id
        }
        storyId?.let { tryPushStory(it) }
    }

    override suspend fun updateNodePosition(nodeId: Long, x: Float, y: Float) {
        val owningStoryId = dao.getNodeStoryId(nodeId) ?: return
        requireOwnedStory(owningStoryId)
        val storyId = database.withTransaction {
            val id = dao.getNodeStoryId(nodeId)
            dao.updateNodePosition(nodeId, x, y)
            if (id != null) dao.touchStory(id, System.currentTimeMillis())
            id
        }
        storyId?.let { tryPushStory(it) }
    }

    override suspend fun upsertConnection(connection: StoryConnection): Long {
        requireOwnedStory(connection.storyId)
        val id = database.withTransaction {
            val savedId = if (connection.id == 0L) {
                dao.insertConnection(connection.toEntity())
            } else {
                dao.updateConnection(connection.toEntity())
                connection.id
            }
            dao.touchStory(connection.storyId, System.currentTimeMillis())
            savedId
        }
        tryPushStory(connection.storyId)
        return id
    }

    override suspend fun deleteConnection(connectionId: Long) {
        val owningStoryId = dao.getConnectionStoryId(connectionId) ?: return
        requireOwnedStory(owningStoryId)
        val storyId = database.withTransaction {
            val id = dao.getConnectionStoryId(connectionId)
            dao.deleteConnection(connectionId)
            if (id != null) dao.touchStory(id, System.currentTimeMillis())
            id
        }
        storyId?.let { tryPushStory(it) }
    }

    override suspend fun syncAllStories() {
        val ownerUserId = sessionStore.currentUser.value?.id ?: return
        dao.claimLegacyStories(ownerUserId)

        val localBefore = dao.getAllStories(ownerUserId)
        localBefore.filter { it.pendingDelete }.forEach { story ->
            story.cloudId?.let { cloudId ->
                try {
                    storyApi.deleteStory(cloudId)
                } catch (error: HttpException) {
                    if (error.code() != 404) throw error
                }
            }
            dao.hardDeleteStory(story.id)
        }

        dao.getAllStories(ownerUserId)
            .filter { !it.pendingDelete && (it.isDirty || it.cloudId == null) }
            .forEach { pushStory(it.id) }

        val remoteStories = storyApi.listStories()
        val remoteIds = remoteStories.mapTo(mutableSetOf()) { it.id }

        remoteStories.forEach { remote ->
            val local = dao.getStoryByCloudId(remote.id, ownerUserId)
            when {
                local == null -> importRemoteStory(remote)
                !local.isDirty && remote.revision > local.cloudRevision ->
                    replaceFromRemote(local.id, remote)
            }
        }

        dao.getAllStories(ownerUserId)
            .filter {
                it.cloudId != null &&
                    !it.isDirty &&
                    !it.pendingDelete &&
                    it.cloudId !in remoteIds
            }
            .forEach { dao.hardDeleteStory(it.id) }
    }

    override suspend fun clearLocalStories() {
        val ownerUserId = sessionStore.currentUser.value?.id ?: return
        dao.deleteAllStories(ownerUserId)
    }

    private suspend fun tryPushStory(storyId: Long) {
        if (sessionStore.currentUser.value == null) return
        runCatching { pushStory(storyId) }
    }

    private suspend fun pushStory(storyId: Long) {
        val mutex = uploadLocks.getOrPut(storyId) { Mutex() }
        mutex.withLock {
            val story = dao.getStory(storyId) ?: return@withLock
            if (story.pendingDelete) return@withLock

            val snapshotUpdatedAt = story.updatedAt
            val request = StoryUpsertRequestDto(
                payload = buildPayload(storyId, story),
                baseRevision = story.cloudRevision.takeIf { story.cloudId != null }
            )
            val response = if (story.cloudId.isNullOrBlank()) {
                storyApi.createStory(request)
            } else {
                try {
                    storyApi.updateStory(story.cloudId, request)
                } catch (error: HttpException) {
                    if (error.code() == 404) {
                        storyApi.createStory(request.copy(baseRevision = null))
                    } else {
                        throw error
                    }
                }
            }

            dao.markCloudSynced(
                storyId = storyId,
                cloudId = response.id,
                revision = response.revision,
                expectedUpdatedAt = snapshotUpdatedAt
            )
        }
    }

    private suspend fun buildPayload(storyId: Long, story: StoryEntity): StoryPayloadDto {
        val nodes = dao.getNodes(storyId).map { entity ->
            val node = entity.toDomain()
            StoryNodeDto(
                clientId = node.id,
                title = node.title,
                text = node.text,
                formatting = node.formatting.map { mark ->
                    TextFormatMarkDto(mark.start, mark.end, mark.type.name, mark.value)
                },
                type = node.type.name,
                positionX = node.positionX,
                positionY = node.positionY
            )
        }

        val connections = dao.getConnections(storyId).map { connection ->
            StoryConnectionDto(
                clientId = connection.id,
                fromNodeClientId = connection.fromNodeId,
                toNodeClientId = connection.toNodeId,
                label = connection.label,
                sortOrder = connection.sortOrder
            )
        }

        val characters = dao.getCharacters(storyId).map { character ->
            CharacterDto(
                clientId = character.id,
                name = character.name,
                role = character.role,
                description = character.description,
                personality = character.personality,
                appearance = character.appearance,
                notes = character.notes
            )
        }

        return StoryPayloadDto(
            title = story.title,
            description = story.description,
            manuscript = story.manuscript,
            wordDocument = story.wordDocument,
            createdAt = story.createdAt,
            updatedAt = story.updatedAt,
            nodes = nodes,
            connections = connections,
            characters = characters
        )
    }

    private suspend fun importRemoteStory(remote: StoryResponseDto) {
        val ownerUserId = checkNotNull(sessionStore.currentUser.value).id
        database.withTransaction {
            val p = remote.payload
            val storyId = dao.insertStory(
                StoryEntity(
                    title = p.title,
                    description = p.description,
                    manuscript = p.manuscript,
                    wordDocument = p.wordDocument,
                    ownerUserId = ownerUserId,
                    cloudId = remote.id,
                    cloudRevision = remote.revision,
                    isDirty = false,
                    pendingDelete = false,
                    createdAt = p.createdAt,
                    updatedAt = p.updatedAt
                )
            )
            insertRemoteChildren(storyId, remote)
        }
    }

    private suspend fun replaceFromRemote(localStoryId: Long, remote: StoryResponseDto) {
        database.withTransaction {
            val p = remote.payload
            dao.deleteConnectionsForStory(localStoryId)
            dao.deleteNodesForStory(localStoryId)
            dao.deleteCharactersForStory(localStoryId)
            dao.updateStoryFromCloud(
                storyId = localStoryId,
                title = p.title,
                description = p.description,
                manuscript = p.manuscript,
                wordDocument = p.wordDocument,
                revision = remote.revision,
                createdAt = p.createdAt,
                updatedAt = p.updatedAt
            )
            insertRemoteChildren(localStoryId, remote)
        }
    }

    private suspend fun insertRemoteChildren(storyId: Long, remote: StoryResponseDto) {
        val nodeIdMap = mutableMapOf<Long, Long>()

        remote.payload.nodes.forEach { node ->
            val newId = dao.insertNode(
                StoryNodeEntity(
                    storyId = storyId,
                    title = node.title,
                    text = node.text,
                    formattingJson = formattingJson(node.formatting),
                    type = node.type,
                    positionX = node.positionX,
                    positionY = node.positionY
                )
            )
            nodeIdMap[node.clientId] = newId
        }

        remote.payload.connections.forEach { connection ->
            val from = nodeIdMap[connection.fromNodeClientId] ?: return@forEach
            val to = nodeIdMap[connection.toNodeClientId] ?: return@forEach
            dao.insertConnection(
                StoryConnectionEntity(
                    storyId = storyId,
                    fromNodeId = from,
                    toNodeId = to,
                    label = connection.label,
                    sortOrder = connection.sortOrder
                )
            )
        }

        remote.payload.characters.forEach { character ->
            dao.insertCharacter(
                CharacterEntity(
                    storyId = storyId,
                    name = character.name,
                    role = character.role,
                    description = character.description,
                    personality = character.personality,
                    appearance = character.appearance,
                    notes = character.notes
                )
            )
        }
    }

    private suspend fun requireOwnedStory(storyId: Long): StoryEntity {
        val ownerUserId = checkNotNull(sessionStore.currentUser.value) {
            "Необходимо войти в аккаунт"
        }.id

        return dao.getStory(storyId)
            ?.takeIf { it.ownerUserId == ownerUserId && !it.pendingDelete }
            ?: throw SecurityException("История недоступна текущему пользователю")
    }

    private fun formattingJson(marks: List<TextFormatMarkDto>): String {
        val array = JSONArray()
        marks.forEach { mark ->
            array.put(
                JSONObject().apply {
                    put("start", mark.start)
                    put("end", mark.end)
                    put("type", mark.type)
                    mark.value?.let { put("value", it) }
                }
            )
        }
        return array.toString()
    }
}
