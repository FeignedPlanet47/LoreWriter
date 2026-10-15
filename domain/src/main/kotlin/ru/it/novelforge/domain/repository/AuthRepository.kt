package ru.it.novelforge.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.it.novelforge.domain.model.RegistrationPending
import ru.it.novelforge.domain.model.UserAccount

interface AuthRepository {
    val currentUser: Flow<UserAccount?>

    suspend fun restoreSession(): UserAccount?

    suspend fun register(
        email: String,
        password: String,
        login: String
    ): RegistrationPending

    suspend fun verifyEmail(
        email: String,
        code: String
    ): UserAccount

    suspend fun resendVerification(
        email: String
    ): RegistrationPending

    suspend fun login(
        login: String,
        password: String
    ): UserAccount

    suspend fun logout()
}
