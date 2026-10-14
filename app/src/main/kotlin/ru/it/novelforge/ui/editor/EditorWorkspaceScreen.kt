package ru.it.novelforge.ui.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

private enum class EditorSection {
    GRAPH,
    MANUSCRIPT,
    WORD_DOCUMENT,
    CHARACTERS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorWorkspaceScreen(
    onBack: () -> Unit,
    onPreview: (Long) -> Unit,
    viewModel: EditorViewModel = hiltViewModel()
) {
    val graph by viewModel.graph.collectAsStateWithLifecycle()

    val drawerState =
        rememberDrawerState(
            initialValue =
                DrawerValue.Closed
        )

    val scope =
        rememberCoroutineScope()

    var section by
        remember {
            mutableStateOf(
                EditorSection.GRAPH
            )
        }

    fun openDrawer() {
        scope.launch {
            drawerState.open()
        }
    }

    fun selectSection(
        newSection: EditorSection
    ) {
        section =
            newSection

        scope.launch {
            drawerState.close()
        }
    }

    ModalNavigationDrawer(
        drawerState =
            drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier =
                    Modifier.width(
                        310.dp
                    )
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxHeight()
                            .padding(
                                vertical =
                                    12.dp
                            )
                ) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal =
                                        20.dp,
                                    vertical =
                                        12.dp
                                )
                    ) {
                        Text(
                            text =
                                graph
                                    ?.story
                                    ?.title
                                    ?: "NovelForge",
                            style =
                                MaterialTheme
                                    .typography
                                    .titleLarge,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            Modifier.height(
                                4.dp
                            )
                        )

                        Text(
                            text =
                                "Рабочее пространство новеллы",
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

                    HorizontalDivider()

                    Spacer(
                        Modifier.height(
                            8.dp
                        )
                    )

                    NavigationDrawerItem(
                        label = {
                            Text(
                                "Схема истории"
                            )
                        },
                        selected =
                            section ==
                                EditorSection.GRAPH,
                        onClick = {
                            selectSection(
                                EditorSection.GRAPH
                            )
                        },
                        icon = {
                            Icon(
                                Icons.Default.AccountTree,
                                contentDescription =
                                    null
                            )
                        },
                        modifier =
                            Modifier.padding(
                                horizontal =
                                    12.dp
                            )
                    )

                    NavigationDrawerItem(
                        label = {
                            Text(
                                "Текст новеллы"
                            )
                        },
                        selected =
                            section ==
                                EditorSection.MANUSCRIPT,
                        onClick = {
                            selectSection(
                                EditorSection.MANUSCRIPT
                            )
                        },
                        icon = {
                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription =
                                    null
                            )
                        },
                        modifier =
                            Modifier.padding(
                                horizontal =
                                    12.dp
                            )
                    )

                    NavigationDrawerItem(
                        label = {
                            Text(
                                "Word-документ"
                            )
                        },
                        selected =
                            section ==
                                EditorSection.WORD_DOCUMENT,
                        onClick = {
                            selectSection(
                                EditorSection.WORD_DOCUMENT
                            )
                        },
                        icon = {
                            Icon(
                                Icons.Default.Description,
                                contentDescription =
                                    null
                            )
                        },
                        modifier =
                            Modifier.padding(
                                horizontal =
                                    12.dp
                            )
                    )

                    NavigationDrawerItem(
                        label = {
                            Text(
                                "Персонажи"
                            )
                        },
                        selected =
                            section ==
                                EditorSection.CHARACTERS,
                        onClick = {
                            selectSection(
                                EditorSection.CHARACTERS
                            )
                        },
                        icon = {
                            Icon(
                                Icons.Default.Groups,
                                contentDescription =
                                    null
                            )
                        },
                        modifier =
                            Modifier.padding(
                                horizontal =
                                    12.dp
                            )
                    )

                    Spacer(
                        Modifier.weight(
                            1f
                        )
                    )

                    HorizontalDivider()

                    Spacer(
                        Modifier.height(
                            8.dp
                        )
                    )

                    NavigationDrawerItem(
                        label = {
                            Text(
                                "К списку новелл"
                            )
                        },
                        selected =
                            false,
                        onClick = {
                            scope.launch {
                                drawerState.close()
                            }

                            onBack()
                        },
                        icon = {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription =
                                    null
                            )
                        },
                        modifier =
                            Modifier.padding(
                                horizontal =
                                    12.dp
                            )
                    )
                }
            }
        }
    ) {
        when (section) {
            EditorSection.GRAPH ->
                EditorScreen(
                    onOpenMenu =
                        ::openDrawer,
                    onPreview =
                        onPreview,
                    viewModel =
                        viewModel
                )

            EditorSection.MANUSCRIPT ->
                ManuscriptScreen(
                    onOpenMenu =
                        ::openDrawer,
                    viewModel =
                        viewModel
                )

            EditorSection.WORD_DOCUMENT ->
                WordDocumentScreen(
                    onOpenMenu =
                        ::openDrawer,
                    viewModel =
                        viewModel
                )

            EditorSection.CHARACTERS ->
                CharactersScreen(
                    onOpenMenu =
                        ::openDrawer,
                    viewModel =
                        viewModel
                )
        }
    }
}
