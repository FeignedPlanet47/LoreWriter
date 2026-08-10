package ru.it.novelforge.ui.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.it.novelforge.domain.model.NodeType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    onBack: () -> Unit,
    viewModel: PreviewViewModel = hiltViewModel()
) {
    val graph by viewModel.graph.collectAsStateWithLifecycle()
    val currentNodeId by viewModel.currentNodeId.collectAsStateWithLifecycle()
    val node = currentNodeId?.let { graph?.nodeById(it) }
    val outgoing = node?.let { graph?.outgoing(it.id) }.orEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(graph?.story?.title ?: "Прохождение") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!viewModel.back()) onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::restart) {
                        Icon(Icons.Default.Refresh, contentDescription = "Начать заново")
                    }
                }
            )
        }
    ) { padding ->
        if (node == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("История не готова к прохождению")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(node.type.name, style = MaterialTheme.typography.labelLarge)
                    Text(node.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            node.text.ifBlank { "Текст сцены не задан" },
                            modifier = Modifier.padding(20.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                when {
                    node.type == NodeType.ENDING -> item {
                        Button(onClick = viewModel::restart, modifier = Modifier.fillMaxWidth()) {
                            Text("Начать заново")
                        }
                    }

                    outgoing.isEmpty() -> item {
                        Text("У этой сцены нет продолжения", color = MaterialTheme.colorScheme.error)
                    }

                    outgoing.size == 1 && node.type != NodeType.CHOICE -> item {
                        Button(onClick = { viewModel.choose(outgoing.first()) }, modifier = Modifier.fillMaxWidth()) {
                            Text(outgoing.first().label.ifBlank { "Продолжить" })
                        }
                    }

                    else -> items(outgoing, key = { it.id }) { connection ->
                        OutlinedButton(
                            onClick = { viewModel.choose(connection) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(connection.label.ifBlank {
                                graph?.nodeById(connection.toNodeId)?.title ?: "Продолжить"
                            })
                        }
                    }
                }
            }
        }
    }
}
