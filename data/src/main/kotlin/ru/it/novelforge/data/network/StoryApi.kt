package ru.it.novelforge.data.network

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface StoryApi {
    @GET("stories")
    suspend fun listStories(): List<StoryResponseDto>

    @POST("stories")
    suspend fun createStory(@Body body: StoryUpsertRequestDto): StoryResponseDto

    @PUT("stories/{storyId}")
    suspend fun updateStory(
        @Path("storyId") storyId: String,
        @Body body: StoryUpsertRequestDto
    ): StoryResponseDto

    @DELETE("stories/{storyId}")
    suspend fun deleteStory(@Path("storyId") storyId: String)

    @POST("stories/{storyId}/share-links")
    suspend fun createShareLink(
        @Path("storyId") storyId: String,
        @Body body: CreateShareLinkRequestDto
    ): ShareLinkResponseDto

    @POST("share-links/{token}/accept")
    suspend fun acceptShareLink(
        @Path("token") token: String
    ): StoryResponseDto

    @GET("stories/{storyId}/collaborators")
    suspend fun listCollaborators(
        @Path("storyId") storyId: String
    ): List<CollaboratorResponseDto>

    @PATCH("stories/{storyId}/collaborators/{userId}")
    suspend fun updateCollaborator(
        @Path("storyId") storyId: String,
        @Path("userId") userId: String,
        @Body body: UpdateCollaboratorRequestDto
    ): CollaboratorResponseDto

    @DELETE("stories/{storyId}/collaborators/{userId}")
    suspend fun removeCollaborator(
        @Path("storyId") storyId: String,
        @Path("userId") userId: String
    )
}
