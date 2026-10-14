package ru.it.novelforge.data.network

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
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
}
