package ru.it.novelforge.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stories",
    indices = [
        Index(value = ["cloudId", "ownerUserId"], unique = true),
        Index(value = ["ownerUserId"])
    ]
)
data class StoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    @ColumnInfo(defaultValue = "''") val manuscript: String = "",
    @ColumnInfo(defaultValue = "''") val wordDocument: String = "",
    @ColumnInfo(defaultValue = "''") val ownerUserId: String = "",
    val cloudId: String? = null,
    @ColumnInfo(defaultValue = "0") val cloudRevision: Long = 0,
    @ColumnInfo(defaultValue = "1") val isDirty: Boolean = true,
    @ColumnInfo(defaultValue = "0") val pendingDelete: Boolean = false,
    @ColumnInfo(defaultValue = "'OWNER'") val accessRole: String = "OWNER",
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "characters",
    foreignKeys = [
        ForeignKey(
            entity = StoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["storyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("storyId")]
)
data class CharacterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val storyId: Long,
    val name: String,
    val role: String,
    val description: String,
    val personality: String,
    val appearance: String,
    val notes: String
)

@Entity(
    tableName = "story_nodes",
    foreignKeys = [
        ForeignKey(
            entity = StoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["storyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("storyId")]
)
data class StoryNodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val storyId: Long,
    val title: String,
    val text: String,
    val formattingJson: String = "[]",
    val type: String,
    val positionX: Float,
    val positionY: Float
)

@Entity(
    tableName = "story_connections",
    foreignKeys = [
        ForeignKey(
            entity = StoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["storyId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = StoryNodeEntity::class,
            parentColumns = ["id"],
            childColumns = ["fromNodeId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = StoryNodeEntity::class,
            parentColumns = ["id"],
            childColumns = ["toNodeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("storyId"), Index("fromNodeId"), Index("toNodeId")]
)
data class StoryConnectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val storyId: Long,
    val fromNodeId: Long,
    val toNodeId: Long,
    val label: String,
    val sortOrder: Int
)
