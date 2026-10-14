package ru.it.novelforge.data.network

import javax.inject.Inject
import okhttp3.Interceptor
import okhttp3.Response
import ru.it.novelforge.data.auth.SecureSessionStore

class AuthInterceptor @Inject constructor(
    private val sessionStore: SecureSessionStore
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = sessionStore.accessToken()
        val request = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
        return chain.proceed(request)
    }
}
