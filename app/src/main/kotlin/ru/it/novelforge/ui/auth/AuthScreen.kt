package ru.it.novelforge.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DeepOlive = Color(0xFF48513D)
private val Sage = Color(0xFF8FA37A)
private val Cream = Color(0xFFF6E7C9)
private val SoftCream = Color(0xFFFBF7EF)
private val Sand = Color(0xFFE9C998)

private val White = Color(0xFFFFFFFF)
private val BurntOrange = Color(0xFFD8653D)

private val FieldBorder = Color(0xFFD9D9D9)
private val SecondaryText = Color(0xFF929292)
private val MainText = Color(0xFF404040)

@Composable
fun AuthScreen(
    state: AuthUiState,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String) -> Unit
) {
    var registration by remember { mutableStateOf(false) }

    var login by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val passwordsMatch =
        password == confirmPassword

    val registrationValid =
        login.isNotBlank() &&
                email.isNotBlank() &&
                password.length >= 10 &&
                passwordsMatch

    val loginValid =
        login.isNotBlank() &&
                password.isNotBlank()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp),
            horizontalAlignment = Alignment.Start
        ) {

            AppLogo()

            Text(
                text = if (registration) {
                    "Создать аккаунт"
                } else {
                    "Войти в аккаунт"
                },
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MainText,
                fontFamily = FontFamily.SansSerif,
                fontSize = 28.sp,
                fontWeight = FontWeight.Normal
            )

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            AuthTextField(
                value = login,
                onValueChange = {
                    login = it
                },
                label = "Логин",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null
                    )
                }
            )

            if (registration) {

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                AuthTextField(
                    value = email,
                    onValueChange = {
                        email = it
                    },
                    label = "Email",
                    keyboardType = KeyboardType.Email,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null
                        )
                    }
                )
            }

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            AuthTextField(
                value = password,
                onValueChange = {
                    password = it
                },
                label = "Пароль",
                keyboardType = KeyboardType.Password,
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            passwordVisible = !passwordVisible
                        }
                    ) {
                        Icon(
                            imageVector = if (passwordVisible) {
                                Icons.Default.VisibilityOff
                            } else {
                                Icons.Default.Visibility
                            },
                            contentDescription = null
                        )
                    }
                }
            )

            if (registration) {

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                AuthTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                    },
                    label = "Повторите пароль",
                    keyboardType = KeyboardType.Password,
                    visualTransformation = if (confirmPasswordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                confirmPasswordVisible =
                                    !confirmPasswordVisible
                            }
                        ) {
                            Icon(
                                imageVector =
                                    if (confirmPasswordVisible) {
                                        Icons.Default.VisibilityOff
                                    } else {
                                        Icons.Default.Visibility
                                    },
                                contentDescription = null
                            )
                        }
                    }
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = when {
                        password.isNotEmpty() &&
                                password.length < 10 ->
                            "Пароль должен содержать минимум 10 символов"

                        confirmPassword.isNotEmpty() &&
                                !passwordsMatch ->
                            "Пароли не совпадают"

                        else ->
                            "После регистрации мы отправим код подтверждения на Email"
                    },
                    color = when {
                        password.isNotEmpty() &&
                                password.length < 10 ->
                            MaterialTheme.colorScheme.error

                        confirmPassword.isNotEmpty() &&
                                !passwordsMatch ->
                            MaterialTheme.colorScheme.error

                        else ->
                            SecondaryText
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }

            state.error?.let {

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(
                modifier = Modifier.height(30.dp)
            )

            Button(
                onClick = {
                    if (registration) {

                        onRegister(
                            email.trim(),
                            password,
                            login.trim()
                        )

                    } else {

                        onLogin(
                            login.trim(),
                            password
                        )
                    }
                },
                enabled = !state.loading &&
                        if (registration) {
                            registrationValid
                        } else {
                            loginValid
                        },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BurntOrange,
                    contentColor = Color.White,
                    disabledContainerColor =
                        BurntOrange.copy(alpha = 0.35f)
                )
            ) {

                if (state.loading) {

                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )

                } else {

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {

                        Text(
                            text = if (registration) {
                                "Создать аккаунт"
                            } else {
                                "Войти"
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            HorizontalDivider(
                color = FieldBorder.copy(alpha = 0.65f)
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = if (registration) {
                        "Уже есть аккаунт?"
                    } else {
                        "Нет аккаунта?"
                    },
                    color = SecondaryText,
                    style = MaterialTheme.typography.bodyMedium
                )

                TextButton(
                    onClick = {

                        registration = !registration

                        password = ""
                        confirmPassword = ""
                    },
                    enabled = !state.loading
                ) {

                    Text(
                        text = if (registration) {
                            "Войти в аккаунт"
                        } else {
                            "Создать аккаунт"
                        },
                        color = BurntOrange,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun AppLogo() {

    Column {

        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            text = "LoreWriter",
            color = MainText,
            fontFamily = FontFamily.SansSerif,
            fontSize = 50.sp,
            fontWeight = FontWeight.Normal
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )
    }
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation =
        VisualTransformation.None,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp),
        placeholder = {
            Text(
                text = label
            )
        },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType
        ),
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = DeepOlive,
            unfocusedBorderColor = FieldBorder,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedTextColor = MainText,
            unfocusedTextColor = MainText,
            focusedLeadingIconColor = DeepOlive,
            unfocusedLeadingIconColor = SecondaryText,
            focusedTrailingIconColor = DeepOlive,
            unfocusedTrailingIconColor = SecondaryText,
            focusedPlaceholderColor = SecondaryText,
            unfocusedPlaceholderColor = SecondaryText
        )
    )
}

@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
private fun AuthScreenPreview() {

    AuthScreen(
        state = AuthUiState(),
        onLogin = { _, _ -> },
        onRegister = { _, _, _ -> }
    )
}