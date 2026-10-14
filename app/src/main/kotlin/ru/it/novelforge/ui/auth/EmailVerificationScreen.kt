package ru.it.novelforge.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.ceil

@Composable
fun EmailVerificationScreen(
    state: AuthUiState,
    email: String,
    onVerify: (String, String) -> Unit,
    onResend: () -> Unit,
    onBack: () -> Unit
) {
    var code by
        remember(email) {
            mutableStateOf("")
        }

    var now by
        remember {
            mutableLongStateOf(
                System.currentTimeMillis()
            )
        }

    LaunchedEffect(
        state.resendAvailableAtMillis
    ) {
        while (
            System.currentTimeMillis() <
            state.resendAvailableAtMillis
        ) {
            now =
                System.currentTimeMillis()

            delay(1_000)
        }

        now =
            System.currentTimeMillis()
    }

    val remainingSeconds =
        ceil(
            (
                state.resendAvailableAtMillis -
                    now
                ).coerceAtLeast(0L) /
                1_000.0
        ).toInt()

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        contentAlignment =
            Alignment.Center
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .widthIn(
                        max = 520.dp
                    )
        ) {
            Column(
                modifier =
                    Modifier.padding(
                        24.dp
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(
                        14.dp
                    )
            ) {
                IconButton(
                    onClick = onBack,
                    enabled = !state.loading
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription =
                            "Назад"
                    )
                }

                Icon(
                    Icons.Default.MarkEmailRead,
                    contentDescription = null
                )

                Text(
                    text =
                        "Подтверждение email",
                    style =
                        MaterialTheme
                            .typography
                            .headlineSmall,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text =
                        "Мы отправили 6-значный код на\n$email",
                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium
                )

                OutlinedTextField(
                    value = code,
                    onValueChange = { value ->
                        code =
                            value
                                .filter {
                                    it.isDigit()
                                }
                                .take(6)
                    },
                    label = {
                        Text(
                            "Код из письма"
                        )
                    },
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType =
                                KeyboardType.NumberPassword
                        ),
                    singleLine = true,
                    modifier =
                        Modifier.fillMaxWidth()
                )

                state.info?.let { message ->
                    Text(
                        text = message,
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,
                        color =
                            MaterialTheme
                                .colorScheme
                                .primary
                    )
                }

                state.error?.let { message ->
                    Text(
                        text = message,
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,
                        color =
                            MaterialTheme
                                .colorScheme
                                .error
                    )
                }

                Button(
                    onClick = {
                        onVerify(
                            email,
                            code
                        )
                    },
                    enabled =
                        !state.loading &&
                            code.length == 6,
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    if (state.loading) {
                        CircularProgressIndicator(
                            modifier =
                                Modifier.height(
                                    20.dp
                                ),
                            strokeWidth =
                                2.dp
                        )
                    } else {
                        Text(
                            "Подтвердить"
                        )
                    }
                }

                TextButton(
                    onClick = onResend,
                    enabled =
                        !state.loading &&
                            remainingSeconds <= 0,
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (remainingSeconds > 0) {
                            "Отправить повторно через ${remainingSeconds} с"
                        } else {
                            "Отправить код повторно"
                        },
                        textAlign =
                            TextAlign.Center
                    )
                }

                Text(
                    text =
                        "Код действует 10 минут. " +
                            "Если письма нет, проверьте папку «Спам».",
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }
        }
    }
}
