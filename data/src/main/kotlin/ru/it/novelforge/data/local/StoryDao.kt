package ru.it.novelforge.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StoryDao {
    @Query("SELECT * FROM stories WHERE ownerUserId = :ownerUserId AND pendingDelete = 0 ORDER BY updatedAt DESC")
    fun observeStories(ownerUserId: String): Flow<List<StoryEntity>>

    @Query("SELECT * FROM stories WHERE ownerUserId = :ownerUserId ORDER BY updatedAt DESC")
    suspend fun getAllStories(ownerUserId: String): List<StoryEntity>

    @Query("UPDATE stories SET ownerUserId = :ownerUserId, isDirty = 1 WHERE ownerUserId = ''")
    suspend fun claimLegacyStories(ownerUserId: String)

    @Query("SELECT * FROM stories WHERE id = :storyId AND pendingDelete = 0")
    fun observeStory(storyId: Long): Flow<StoryEntity?>

    @Query("SELECT * FROM stories WHERE id = :storyId")
    suspend fun getStory(storyId: Long): StoryEntity?

    @Query("SELECT * FROM stories WHERE cloudId = :cloudId AND ownerUserId = :ownerUserId LIMIT 1")
    suspend fun getStoryByCloudId(cloudId: String, ownerUserId: String): StoryEntity?

    @Query("SELECT * FROM characters WHERE storyId = :storyId ORDER BY name COLLATE NOCASE, id")
    fun observeCharacters(storyId: Long): Flow<List<CharacterEntity>>

    @Query("SELECT * FROM characters WHERE storyId = :storyId ORDER BY id")
    suspend fun getCharacters(storyId: Long): List<CharacterEntity>

    @Query("SELECT * FROM story_nodes WHERE storyId = :storyId ORDER BY id")
    fun observeNodes(storyId: Long): Flow<List<StoryNodeEntity>>

    @Query("SELECT * FROM story_nodes WHERE storyId = :storyId ORDER BY id")
    suspend fun getNodes(storyId: Long): List<StoryNodeEntity>

    @Query("SELECT * FROM story_connections WHERE storyId = :storyId ORDER BY sortOrder, id")
    fun observeConnections(storyId: Long): Flow<List<StoryConnectionEntity>>

    @Query("SELECT * FROM story_connections WHERE storyId = :storyId ORDER BY sortOrder, id")
    suspend fun getConnections(storyId: Long): List<StoryConnectionEntity>

    @Insert
    suspend fun insertStory(entity: StoryEntity): Long

    @Update
    suspend fun updateStory(entity: StoryEntity)

    @Query("UPDATE stories SET manuscript = :manuscript, updatedAt = :timestamp, isDirty = 1 WHERE id = :storyId")
    suspend fun updateManuscript(storyId: Long, manuscript: String, timestamp: Long)

    @Query("UPDATE stories SET wordDocument = :wordDocument, updatedAt = :timestamp, isDirty = 1 WHERE id = :storyId")
    suspend fun updateWordDocument(storyId: Long, wordDocument: String, timestamp: Long)

    @Query("UPDATE stories SET pendingDelete = 1, isDirty = 1, updatedAt = :timestamp WHERE id = :storyId")
    suspend fun markStoryPendingDelete(storyId: Long, timestamp: Long)

    @Query("DELETE FROM stories WHERE id = :storyId")
    suspend fun hardDeleteStory(storyId: Long)

    @Query("DELETE FROM stories WHERE ownerUserId = :ownerUserId")
    suspend fun deleteAllStories(ownerUserId: String)

    @Query("UPDATE stories SET cloudId = :cloudId, cloudRevision = :revision, isDirty = CASE WHEN updatedAt = :expectedUpdatedAt THEN 0 ELSE isDirty END WHERE id = :storyId")
    suspend fun markCloudSynced(
        storyId: Long,
        cloudId: String,
        revision: Long,
        expectedUpdatedAt: Long
    )

    @Query("UPDATE stories SET title = :title, description = :description, manuscript = :manuscript, wordDocument = :wordDocument, cloudRevision = :revision, accessRole = :accessRole, isDirty = 0, pendingDelete = 0, createdAt = :createdAt, updatedAt = :updatedAt WHERE id = :storyId")
    suspend fun updateStoryFromCloud(
        storyId: Long,
        title: String,
        description: String,
        manuscript: String,
        wordDocument: String,
        revision: Long,
        accessRole: String,
        createdAt: Long,
        updatedAt: Long
    )

    @Query("UPDATE stories SET accessRole = :accessRole WHERE id = :storyId")
    suspend fun updateAccessRole(storyId: Long, accessRole: String)

    @Query("DELETE FROM story_connections WHERE storyId = :storyId")
    suspend fun deleteConnectionsForStory(storyId: Long)

    @Query("DELETE FROM story_nodes WHERE storyId = :storyId")
    suspend fun deleteNodesForStory(storyId: Long)

    @Query("DELETE FROM characters WHERE storyId = :storyId")
    suspend fun deleteCharactersForStory(storyId: Long)

    @Insert
    suspend fun insertCharacter(entity: CharacterEntity): Long

    @Update
    suspend fun updateCharacter(entity: CharacterEntity)

    @Query("DELETE FROM characters WHERE id = :characterId")
    suspend fun deleteCharacter(characterId: Long)

    @Query("SELECT storyId FROM characters WHERE id = :characterId")
    suspend fun getCharacterStoryId(characterId: Long): Long?

    @Insert
    suspend fun insertNode(entity: StoryNodeEntity): Long

    @Update
    suspend fun updateNode(entity: StoryNodeEntity)

    @Query("DELETE FROM story_nodes WHERE id = :nodeId")
    suspend fun deleteNode(nodeId: Long)

    @Query("UPDATE story_nodes SET positionX = :x, positionY = :y WHERE id = :nodeId")
    suspend fun updateNodePosition(nodeId: Long, x: Float, y: Float)

    @Query("SELECT storyId FROM story_nodes WHERE id = :nodeId")
    suspend fun getNodeStoryId(nodeId: Long): Long?

    @Insert
    suspend fun insertConnection(entity: StoryConnectionEntity): Long

    @Update
    suspend fun updateConnection(entity: StoryConnectionEntity)

    @Query("DELETE FROM story_connections WHERE id = :connectionId")
    suspend fun deleteConnection(connectionId: Long)

    @Query("SELECT storyId FROM story_connections WHERE id = :connectionId")
    suspend fun getConnectionStoryId(connectionId: Long): Long?

    @Query("UPDATE stories SET updatedAt = :timestamp, isDirty = 1 WHERE id = :storyId")
    suspend fun touchStory(storyId: Long, timestamp: Long)
}
