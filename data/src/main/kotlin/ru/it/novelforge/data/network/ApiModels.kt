package ru.it.novelforge.data.network

data class UserDto(
    val id: String,
    val email: String,
    val displayName: String = "",
    val emailVerified: Boolean = true
)

data class RegisterRequestDto(
    val email: String,
    val password: String,
    val displayName: String
)

data class LoginRequestDto(
    val email: String,
    val password: String
)

data class VerifyEmailRequestDto(
    val email: String,
    val code: String
)

data class ResendVerificationRequestDto(
    val email: String
)

data class VerificationPendingDto(
    val email: String,
    val verificationRequired: Boolean = true,
    val resendAfterSeconds: Int = 60
)

data class RefreshRequestDto(
    val refreshToken: String
)

data class LogoutRequestDto(
    val refreshToken: String
)

data class AuthResponseDto(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val user: UserDto
)

data class TextFormatMarkDto(
    val start: Int,
    val end: Int,
    val type: String,
    val value: String? = null
)

data class StoryNodeDto(
    val clientId: Long,
    val title: String,
    val text: String,
    val formatting: List<TextFormatMarkDto>,
    val type: String,
    val positionX: Float,
    val positionY: Float
)

data class StoryConnectionDto(
    val clientId: Long,
    val fromNodeClientId: Long,
    val toNodeClientId: Long,
    val label: String,
    val sortOrder: Int
)

data class CharacterDto(
    val clientId: Long,
    val name: String,
    val role: String,
    val description: String,
    val personality: String,
    val appearance: String,
    val notes: String
)

data class StoryPayloadDto(
    val title: String,
    val description: String,
    val manuscript: String,
    val wordDocument: String,
    val createdAt: Long,
    val updatedAt: Long,
    val nodes: List<StoryNodeDto>,
    val connections: List<StoryConnectionDto>,
    val characters: List<CharacterDto>
)

data class StoryUpsertRequestDto(
    val payload: StoryPayloadDto,
    val baseRevision: Long? = null
)

data class StoryResponseDto(
    val id: String,
    val revision: Long,
    val payload: StoryPayloadDto,
    val updatedAt: String
)
