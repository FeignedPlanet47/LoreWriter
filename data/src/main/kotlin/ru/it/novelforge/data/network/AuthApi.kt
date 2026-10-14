package ru.it.novelforge.data.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/register")
    suspend fun register(
        @Body body: RegisterRequestDto
    ): VerificationPendingDto

    @POST("auth/verify-email")
    suspend fun verifyEmail(
        @Body body: VerifyEmailRequestDto
    ): AuthResponseDto

    @POST("auth/resend-verification")
    suspend fun resendVerification(
        @Body body: ResendVerificationRequestDto
    ): VerificationPendingDto

    @POST("auth/login")
    suspend fun login(
        @Body body: LoginRequestDto
    ): AuthResponseDto

    @POST("auth/refresh")
    suspend fun refresh(
        @Body body: RefreshRequestDto
    ): AuthResponseDto

    @POST("auth/logout")
    suspend fun logout(
        @Body body: LogoutRequestDto
    )

    @GET("auth/me")
    suspend fun me(): UserDto
}
