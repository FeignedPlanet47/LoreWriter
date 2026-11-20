package ru.it.lorewriter.ui

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ru.it.lorewriter.domain.model.UserAccount
import ru.it.lorewriter.ui.auth.AuthScreen
import ru.it.lorewriter.ui.auth.AuthViewModel
import ru.it.lorewriter.ui.auth.EmailVerificationScreen
import ru.it.lorewriter.ui.editor.EditorWorkspaceScreen
import ru.it.lorewriter.ui.preview.PreviewScreen
import ru.it.lorewriter.ui.stories.StoriesScreen

private object Routes {
    const val STORIES = "stories"
    const val EDITOR = "editor/{storyId}"
    const val PREVIEW = "preview/{storyId}"

    fun editor(storyId: Long) = "editor/$storyId"
    fun preview(storyId: Long) = "preview/$storyId"
}

@Composable
fun LoreWriterApp(
    deepLink: String? = null,
    onDeepLinkConsumed: () -> Unit = {},
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authState by authViewModel.state.collectAsStateWithLifecycle()

    val pendingShareToken = extractShareToken(deepLink)

    when {
        authState.initializing -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        authState.verificationEmail != null -> {
            EmailVerificationScreen(
                state = authState,
                email = checkNotNull(authState.verificationEmail),
                onVerify = authViewModel::verifyEmail,
                onResend = authViewModel::resendVerification,
                onBack = authViewModel::cancelVerification
            )
        }

        authState.user == null -> {
            AuthScreen(
                state = authState,
                onLogin = authViewModel::login,
                onRegister = authViewModel::register
            )
        }

        else -> {
            AuthenticatedLoreWriterApp(
                user = checkNotNull(authState.user),
                pendingShareToken = pendingShareToken,
                onShareTokenConsumed = onDeepLinkConsumed,
                onLogout = authViewModel::logout
            )
        }
    }
}

@Composable
private fun AuthenticatedLoreWriterApp(
    user: UserAccount,
    pendingShareToken: String?,
    onShareTokenConsumed: () -> Unit,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.STORIES
    ) {
        composable(Routes.STORIES) {
            StoriesScreen(
                user = user,
                onLogout = onLogout,
                onOpenStory = { storyId ->
                    navController.navigate(Routes.editor(storyId))
                },
                pendingShareToken = pendingShareToken,
                onShareTokenConsumed = onShareTokenConsumed
            )
        }

        composable(
            route = Routes.EDITOR,
            arguments = listOf(
                navArgument("storyId") {
                    type = NavType.LongType
                }
            )
        ) {
            EditorWorkspaceScreen(
                onBack = {
                    navController.popBackStack()
                },
                onPreview = { storyId ->
                    navController.navigate(Routes.preview(storyId))
                }
            )
        }

        composable(
            route = Routes.PREVIEW,
            arguments = listOf(
                navArgument("storyId") {
                    type = NavType.LongType
                }
            )
        ) {
            PreviewScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

private fun extractShareToken(deepLink: String?): String? {
    if (deepLink.isNullOrBlank()) {
        return null
    }

    val uri = runCatching {
        Uri.parse(deepLink)
    }.getOrNull() ?: return null

    return when {
        uri.scheme.equals("https", ignoreCase = true) &&
                uri.host.equals("lorewriter.net", ignoreCase = true) &&
                uri.pathSegments.firstOrNull() == "share" -> {

            uri.pathSegments
                .getOrNull(1)
                ?.trim()
                ?.takeIf { it.length >= 20 }
        }

        uri.scheme.equals("novelforge", ignoreCase = true) &&
                uri.host.equals("share", ignoreCase = true) -> {

            uri.pathSegments
                .firstOrNull()
                ?.trim()
                ?.takeIf { it.length >= 20 }
        }

        else -> null
    }
}