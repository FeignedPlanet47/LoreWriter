package ru.it.novelforge.data.mapper

import org.json.JSONArray
import org.json.JSONObject
import ru.it.novelforge.data.local.CharacterEntity
import ru.it.novelforge.data.local.StoryConnectionEntity
import ru.it.novelforge.data.local.StoryEntity
import ru.it.novelforge.data.local.StoryNodeEntity
import ru.it.novelforge.domain.model.CharacterProfile
import ru.it.novelforge.domain.model.NodeType
import ru.it.novelforge.domain.model.Story
import ru.it.novelforge.domain.model.StoryConnection
import ru.it.novelforge.domain.model.StoryNode
import ru.it.novelforge.domain.model.TextFormatMark
import ru.it.novelforge.domain.model.TextMarkType

fun StoryEntity.toDomain() = Story(
    id = id,
    title = title,
    description = description,
    manuscript = manuscript,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Story.toEntity() = StoryEntity(
    id = id,
    title = title,
    description = description,
    manuscript = manuscript,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun CharacterEntity.toDomain() = CharacterProfile(
    id = id,
    storyId = storyId,
    name = name,
    role = role,
    description = description,
    personality = personality,
    appearance = appearance,
    notes = notes
)

fun CharacterProfile.toEntity() = CharacterEntity(
    id = id,
    storyId = storyId,
    name = name,
    role = role,
    description = description,
    personality = personality,
    appearance = appearance,
    notes = notes
)

fun StoryNodeEntity.toDomain() = StoryNode(
    id = id,
    storyId = storyId,
    title = title,
    text = text,
    formatting = decodeFormatting(formattingJson, text.length),
    type = runCatching { NodeType.valueOf(type) }.getOrDefault(NodeType.SCENE),
    positionX = positionX,
    positionY = positionY
)

fun StoryNode.toEntity() = StoryNodeEntity(
    id = id,
    storyId = storyId,
    title = title,
    text = text,
    formattingJson = encodeFormatting(formatting),
    type = type.name,
    positionX = positionX,
    positionY = positionY
)

fun StoryConnectionEntity.toDomain() = StoryConnection(
    id = id,
    storyId = storyId,
    fromNodeId = fromNodeId,
    toNodeId = toNodeId,
    label = label,
    sortOrder = sortOrder
)

fun StoryConnection.toEntity() = StoryConnectionEntity(
    id = id,
    storyId = storyId,
    fromNodeId = fromNodeId,
    toNodeId = toNodeId,
    label = label,
    sortOrder = sortOrder
)

private fun encodeFormatting(marks: List<TextFormatMark>): String {
    val array = JSONArray()
    marks.forEach { mark ->
        array.put(
            JSONObject().apply {
                put("start", mark.start)
                put("end", mark.end)
                put("type", mark.type.name)
                if (mark.value != null) put("value", mark.value)
            }
        )
    }
    return array.toString()
}

private fun decodeFormatting(json: String, textLength: Int): List<TextFormatMark> = runCatching {
    val array = JSONArray(json)
    buildList {
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            val start = item.optInt("start", 0).coerceIn(0, textLength)
            val end = item.optInt("end", 0).coerceIn(0, textLength)
            val type = runCatching {
                TextMarkType.valueOf(item.getString("type"))
            }.getOrNull() ?: continue
            if (end <= start) continue
            add(
                TextFormatMark(
                    start = start,
                    end = end,
                    type = type,
                    value = item.optString("value").takeIf { it.isNotBlank() }
                )
            )
        }
    }
}.getOrDefault(emptyList())
