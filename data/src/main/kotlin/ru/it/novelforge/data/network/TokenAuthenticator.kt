package ru.it.novelforge.data.network

import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.HttpException
import ru.it.novelforge.data.auth.SecureSessionStore
import ru.it.novelforge.domain.model.UserAccount

class TokenAuthenticator @Inject constructor(
    private val authApi: AuthApi,
    private val sessionStore: SecureSessionStore
) : Authenticator {

    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null

        return synchronized(lock) {
            val requestToken = response.request.header("Authorization")
                ?.removePrefix("Bearer ")
            val currentAccess = sessionStore.accessToken()

            if (!currentAccess.isNullOrBlank() && requestToken != currentAccess) {
                return@synchronized response.request.newBuilder()
                    .header("Authorization", "Bearer $currentAccess")
                    .build()
            }

            val refresh = sessionStore.refreshToken() ?: return@synchronized null
            val refreshed = try {
                runBlocking {
                    authApi.refresh(RefreshRequestDto(refresh))
                }
            } catch (error: HttpException) {
                if (error.code() == 401) {
                    sessionStore.clear()
                }
                return@synchronized null
            } catch (_: Exception) {
                // Сетевой сбой не должен разлогинивать пользователя: локальный
                // кэш продолжает работать, а refresh повторится позже.
                return@synchronized null
            }

            sessionStore.saveSession(
                accessToken = refreshed.accessToken,
                refreshToken = refreshed.refreshToken,
                user = UserAccount(
                    id = refreshed.user.id,
                    email = refreshed.user.email,
                    displayName = refreshed.user.displayName
                )
            )

            response.request.newBuilder()
                .header("Authorization", "Bearer ${refreshed.accessToken}")
                .build()
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
