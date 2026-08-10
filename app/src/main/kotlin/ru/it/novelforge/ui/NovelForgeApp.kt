package ru.it.novelforge.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ru.it.novelforge.ui.editor.EditorWorkspaceScreen
import ru.it.novelforge.ui.preview.PreviewScreen
import ru.it.novelforge.ui.stories.StoriesScreen

private object Routes {
    const val STORIES = "stories"
    const val EDITOR = "editor/{storyId}"
    const val PREVIEW = "preview/{storyId}"

    fun editor(storyId: Long) = "editor/$storyId"
    fun preview(storyId: Long) = "preview/$storyId"
}

@Composable
fun NovelForgeApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.STORIES) {
        composable(Routes.STORIES) {
            StoriesScreen(onOpenStory = { navController.navigate(Routes.editor(it)) })
        }
        composable(
            route = Routes.EDITOR,
            arguments = listOf(navArgument("storyId") { type = NavType.LongType })
        ) {
            EditorWorkspaceScreen(
                onBack = { navController.popBackStack() },
                onPreview = { storyId -> navController.navigate(Routes.preview(storyId)) }
            )
        }
        composable(
            route = Routes.PREVIEW,
            arguments = listOf(navArgument("storyId") { type = NavType.LongType })
        ) {
            PreviewScreen(onBack = { navController.popBackStack() })
        }
    }
}
