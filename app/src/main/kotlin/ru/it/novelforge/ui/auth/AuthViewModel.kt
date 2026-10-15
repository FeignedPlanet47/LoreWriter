package ru.it.novelforge.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import ru.it.novelforge.domain.model.EmailAlreadyRegisteredException
import ru.it.novelforge.domain.model.EmailDeliveryException
import ru.it.novelforge.domain.model.EmailVerificationRequiredException
import ru.it.novelforge.domain.model.InvalidVerificationCodeException
import ru.it.novelforge.domain.model.LoginAlreadyRegisteredException
import ru.it.novelforge.domain.model.UserAccount
import ru.it.novelforge.domain.model.VerificationAttemptsExceededException
import ru.it.novelforge.domain.model.VerificationCodeExpiredException
import ru.it.novelforge.domain.model.VerificationResendTooSoonException
import ru.it.novelforge.domain.model.VerificationSendLimitException
import ru.it.novelforge.domain.repository.AuthRepository
import ru.it.novelforge.domain.repository.StoryRepository


private val LOGIN_REGEX = Regex("^[\\p{L}\\p{N}._-]{3,32}$")

data class AuthUiState(
    val initializing: Boolean = true,
    val loading: Boolean = false,
    val user: UserAccount? = null,
    val verificationEmail: String? = null,
    val resendAvailableAtMillis: Long = 0L,
    val info: String? = null,
    val error: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val storyRepository: StoryRepository
) : ViewModel() {

    private val _state =
        MutableStateFlow(
            AuthUiState()
        )

    val state: StateFlow<AuthUiState> =
        _state.asStateFlow()

    init {
        viewModelScope.launch {
            val user =
                authRepository.restoreSession()

            _state.value =
                AuthUiState(
                    initializing = false,
                    user = user
                )

            if (user != null) {
                runCatching {
                    storyRepository.syncAllStories()
                }
            }

            authRepository.currentUser.collect { current ->
                _state.value =
                    _state.value.copy(
                        initializing = false,
                        user = current,
                        loading = false
                    )
            }
        }
    }

    fun login(
        login: String,
        password: String
    ) {
        if (_state.value.loading) {
            return
        }

        if (!LOGIN_REGEX.matches(login.trim())) {
            _state.value = _state.value.copy(
                error = "Логин должен содержать 3–32 символа: буквы, цифры, точку, _ или -"
            )
            return
        }

        viewModelScope.launch {
            _state.value =
                _state.value.copy(
                    loading = true,
                    error = null,
                    info = null
                )

            runCatching {
                authRepository.login(
                    login,
                    password
                )
            }.onSuccess { user ->
                _state.value =
                    _state.value.copy(
                        user = user,
                        verificationEmail = null,
                        loading = false,
                        error = null,
                        info = null
                    )

                runCatching {
                    storyRepository.syncAllStories()
                }
            }.onFailure { error ->
                if (
                    error is
                            EmailVerificationRequiredException
                ) {
                    _state.value =
                        _state.value.copy(
                            loading = false,
                            verificationEmail = error.email,
                            resendAvailableAtMillis = 0L,
                            error = null,
                            info =
                                "Email ещё не подтверждён. " +
                                        "Введите код из письма или отправьте новый."
                        )
                } else {
                    _state.value =
                        _state.value.copy(
                            loading = false,
                            error = error.toUserMessage()
                        )
                }
            }
        }
    }

    fun register(
        email: String,
        password: String,
        login: String
    ) {
        if (_state.value.loading) {
            return
        }

        if (!LOGIN_REGEX.matches(login.trim())) {
            _state.value = _state.value.copy(
                error = "Логин должен содержать 3–32 символа: буквы, цифры, точку, _ или -"
            )
            return
        }

        viewModelScope.launch {
            _state.value =
                _state.value.copy(
                    loading = true,
                    error = null,
                    info = null
                )

            runCatching {
                authRepository.register(
                    email,
                    password,
                    login
                )
            }.onSuccess { pending ->
                _state.value =
                    _state.value.copy(
                        loading = false,
                        verificationEmail = pending.email,
                        resendAvailableAtMillis =
                            System.currentTimeMillis() +
                                    pending.resendAfterSeconds * 1_000L,
                        error = null,
                        info =
                            "Код подтверждения отправлен на ${pending.email}"
                    )
            }.onFailure { error ->
                _state.value =
                    _state.value.copy(
                        loading = false,
                        error = error.toUserMessage()
                    )
            }
        }
    }

    fun verifyEmail(
        email: String,
        code: String
    ) {
        if (_state.value.loading) {
            return
        }

        viewModelScope.launch {
            _state.value =
                _state.value.copy(
                    loading = true,
                    error = null,
                    info = null
                )

            runCatching {
                authRepository.verifyEmail(
                    email,
                    code
                )
            }.onSuccess { user ->
                _state.value =
                    _state.value.copy(
                        user = user,
                        verificationEmail = null,
                        resendAvailableAtMillis = 0L,
                        loading = false,
                        error = null,
                        info = null
                    )

                runCatching {
                    storyRepository.syncAllStories()
                }
            }.onFailure { error ->
                _state.value =
                    _state.value.copy(
                        loading = false,
                        error = error.toUserMessage()
                    )
            }
        }
    }

    fun resendVerification() {
        if (_state.value.loading) {
            return
        }

        val email =
            _state.value.verificationEmail
                ?: return

        viewModelScope.launch {
            _state.value =
                _state.value.copy(
                    loading = true,
                    error = null,
                    info = null
                )

            runCatching {
                authRepository.resendVerification(
                    email
                )
            }.onSuccess { pending ->
                _state.value =
                    _state.value.copy(
                        loading = false,
                        resendAvailableAtMillis =
                            System.currentTimeMillis() +
                                    pending.resendAfterSeconds * 1_000L,
                        error = null,
                        info =
                            "Новый код отправлен на ${pending.email}"
                    )
            }.onFailure { error ->
                val retryAfter =
                    when (error) {
                        is VerificationResendTooSoonException ->
                            error.retryAfterSeconds

                        is VerificationSendLimitException ->
                            error.retryAfterSeconds

                        else ->
                            null
                    }

                _state.value =
                    _state.value.copy(
                        loading = false,
                        resendAvailableAtMillis =
                            if (retryAfter != null) {
                                System.currentTimeMillis() +
                                        retryAfter * 1_000L
                            } else {
                                _state.value.resendAvailableAtMillis
                            },
                        error = error.toUserMessage()
                    )
            }
        }
    }

    fun cancelVerification() {
        if (_state.value.loading) {
            return
        }

        _state.value =
            _state.value.copy(
                verificationEmail = null,
                resendAvailableAtMillis = 0L,
                error = null,
                info = null
            )
    }

    fun logout() {
        if (_state.value.loading) {
            return
        }

        viewModelScope.launch {
            _state.value =
                _state.value.copy(
                    loading = true,
                    error = null
                )

            runCatching {
                storyRepository.syncAllStories()
            }

            runCatching {
                authRepository.logout()
            }

            _state.value =
                AuthUiState(
                    initializing = false,
                    user = null
                )
        }
    }

    fun clearError() {
        _state.value =
            _state.value.copy(
                error = null,
                info = null
            )
    }
}

private fun Throwable.toUserMessage(): String =
    when (this) {
        is EmailAlreadyRegisteredException ->
            "Аккаунт с таким email уже существует"

        is LoginAlreadyRegisteredException ->
            "Такой логин уже занят"

        is InvalidVerificationCodeException ->
            "Неверный код подтверждения"

        is VerificationCodeExpiredException ->
            "Код истёк. Отправьте новый код"

        is VerificationAttemptsExceededException ->
            "Слишком много неверных попыток. Отправьте новый код"

        is VerificationResendTooSoonException ->
            "Новый код можно запросить немного позже"

        is VerificationSendLimitException ->
            "Слишком много писем за последний час. Попробуйте позже"

        is EmailDeliveryException ->
            "Не удалось отправить письмо. Проверьте SMTP-настройки сервера"

        else -> {
            val raw =
                message.orEmpty()

            when {
                raw.contains("401") ->
                    "Неверный логин или пароль"

                raw.contains(
                    "Unable to resolve host",
                    ignoreCase = true
                ) ->
                    "Сервер недоступен. Проверьте интернет и адрес API"

                raw.contains(
                    "Failed to connect",
                    ignoreCase = true
                ) ->
                    "Сервер недоступен. Проверьте интернет и адрес API"

                else ->
                    raw.ifBlank {
                        "Не удалось выполнить запрос"
                    }
            }
        }
    }
