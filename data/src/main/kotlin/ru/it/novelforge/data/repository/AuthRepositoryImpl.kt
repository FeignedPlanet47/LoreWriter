package ru.it.novelforge.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException
import ru.it.novelforge.data.auth.SecureSessionStore
import ru.it.novelforge.data.network.AuthApi
import ru.it.novelforge.data.network.LoginRequestDto
import ru.it.novelforge.data.network.LogoutRequestDto
import ru.it.novelforge.data.network.RefreshRequestDto
import ru.it.novelforge.data.network.RegisterRequestDto
import ru.it.novelforge.data.network.ResendVerificationRequestDto
import ru.it.novelforge.data.network.VerifyEmailRequestDto
import ru.it.novelforge.domain.model.EmailAlreadyRegisteredException
import ru.it.novelforge.domain.model.EmailDeliveryException
import ru.it.novelforge.domain.model.EmailVerificationRequiredException
import ru.it.novelforge.domain.model.InvalidVerificationCodeException
import ru.it.novelforge.domain.model.RegistrationPending
import ru.it.novelforge.domain.model.UserAccount
import ru.it.novelforge.domain.model.VerificationAttemptsExceededException
import ru.it.novelforge.domain.model.VerificationCodeExpiredException
import ru.it.novelforge.domain.model.VerificationResendTooSoonException
import ru.it.novelforge.domain.model.VerificationSendLimitException
import ru.it.novelforge.domain.repository.AuthRepository

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApi,
    private val sessionStore: SecureSessionStore
) : AuthRepository {

    override val currentUser: Flow<UserAccount?> =
        sessionStore.currentUser

    override suspend fun restoreSession(): UserAccount? {
        val cachedUser =
            sessionStore.currentUser.value

        val refresh =
            sessionStore.refreshToken()
                ?: run {
                    sessionStore.clear()
                    return null
                }

        return try {
            val response =
                api.refresh(
                    RefreshRequestDto(refresh)
                )

            response
                .toAccount()
                .also { user ->
                    sessionStore.saveSession(
                        response.accessToken,
                        response.refreshToken,
                        user
                    )
                }
        } catch (error: HttpException) {
            if (error.code() == 401) {
                sessionStore.clear()
                null
            } else {
                cachedUser
            }
        } catch (_: Exception) {
            // Если сервер временно недоступен, пользователь может продолжить
            // работать с собственным локальным Room-кэшем.
            cachedUser
        }
    }

    override suspend fun register(
        email: String,
        password: String,
        displayName: String
    ): RegistrationPending {
        val normalizedEmail =
            email.trim()

        return try {
            val response =
                api.register(
                    RegisterRequestDto(
                        email = normalizedEmail,
                        password = password,
                        displayName = displayName.trim()
                    )
                )

            RegistrationPending(
                email = response.email,
                resendAfterSeconds = response.resendAfterSeconds
            )
        } catch (error: HttpException) {
            throw error.toAuthException(normalizedEmail)
        }
    }

    override suspend fun verifyEmail(
        email: String,
        code: String
    ): UserAccount {
        val normalizedEmail =
            email.trim()

        return try {
            val response =
                api.verifyEmail(
                    VerifyEmailRequestDto(
                        email = normalizedEmail,
                        code = code.trim()
                    )
                )

            val user =
                response.toAccount()

            sessionStore.saveSession(
                response.accessToken,
                response.refreshToken,
                user
            )

            user
        } catch (error: HttpException) {
            throw error.toAuthException(normalizedEmail)
        }
    }

    override suspend fun resendVerification(
        email: String
    ): RegistrationPending {
        val normalizedEmail =
            email.trim()

        return try {
            val response =
                api.resendVerification(
                    ResendVerificationRequestDto(
                        email = normalizedEmail
                    )
                )

            RegistrationPending(
                email = response.email,
                resendAfterSeconds = response.resendAfterSeconds
            )
        } catch (error: HttpException) {
            throw error.toAuthException(normalizedEmail)
        }
    }

    override suspend fun login(
        email: String,
        password: String
    ): UserAccount {
        val normalizedEmail =
            email.trim()

        return try {
            val response =
                api.login(
                    LoginRequestDto(
                        email = normalizedEmail,
                        password = password
                    )
                )

            val user =
                response.toAccount()

            sessionStore.saveSession(
                response.accessToken,
                response.refreshToken,
                user
            )

            user
        } catch (error: HttpException) {
            throw error.toAuthException(normalizedEmail)
        }
    }

    override suspend fun logout() {
        val refresh =
            sessionStore.refreshToken()

        if (!refresh.isNullOrBlank()) {
            runCatching {
                api.logout(
                    LogoutRequestDto(refresh)
                )
            }
        }

        sessionStore.clear()
    }
}

private fun ru.it.novelforge.data.network.AuthResponseDto.toAccount() =
    UserAccount(
        id = user.id,
        email = user.email,
        displayName = user.displayName
    )

private fun HttpException.toAuthException(
    email: String
): Throwable {
    val errorBody =
        response()
            ?.errorBody()
            ?.string()
            .orEmpty()

    val retryAfter =
        response()
            ?.headers()
            ?.get("Retry-After")
            ?.toIntOrNull()

    return when {
        errorBody.contains("EMAIL_NOT_VERIFIED") ->
            EmailVerificationRequiredException(email)

        errorBody.contains("EMAIL_ALREADY_REGISTERED") ->
            EmailAlreadyRegisteredException()

        errorBody.contains("VERIFICATION_CODE_EXPIRED") ->
            VerificationCodeExpiredException()

        errorBody.contains("TOO_MANY_VERIFICATION_ATTEMPTS") ->
            VerificationAttemptsExceededException()

        errorBody.contains("VERIFICATION_RESEND_TOO_SOON") ->
            VerificationResendTooSoonException(retryAfter)

        errorBody.contains("VERIFICATION_SEND_LIMIT_REACHED") ->
            VerificationSendLimitException(retryAfter)

        errorBody.contains("EMAIL_SEND_FAILED") ->
            EmailDeliveryException()

        errorBody.contains("INVALID_VERIFICATION_CODE") ->
            InvalidVerificationCodeException()

        else -> this
    }
}
