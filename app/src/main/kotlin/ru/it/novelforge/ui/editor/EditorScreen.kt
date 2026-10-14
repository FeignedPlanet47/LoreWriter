package ru.it.novelforge.ui.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.IOException
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlinx.coroutines.launch
import ru.it.novelforge.domain.model.NodeType
import ru.it.novelforge.domain.model.StoryConnection
import ru.it.novelforge.domain.model.StoryGraph
import ru.it.novelforge.domain.model.StoryNode
import ru.it.novelforge.domain.model.TextFormatMark
import ru.it.novelforge.domain.model.TextMarkType
import ru.it.novelforge.domain.model.ValidationIssue
import ru.it.novelforge.domain.model.ValidationSeverity
import ru.it.novelforge.ui.common.RichText
import ru.it.novelforge.ui.common.SpellingIssue
import ru.it.novelforge.ui.common.adjustMarksAfterTextChange
import ru.it.novelforge.ui.common.applyTextColor
import ru.it.novelforge.ui.common.buildRichAnnotatedString
import ru.it.novelforge.ui.common.clearTextFormatting
import ru.it.novelforge.ui.common.parseHexColor
import ru.it.novelforge.ui.common.rememberSpellCheckState
import ru.it.novelforge.ui.common.toggleTextMark

private const val CANVAS_WIDTH_DP = 2600f
private const val CANVAS_HEIGHT_DP = 1900f
private const val NODE_WIDTH_DP = 240f
private const val NODE_HEIGHT_DP = 150f

private const val MODE_DIVIDER_LEFT_DP = 80f
private const val MODE_DIVIDER_WIDTH_DP = 2200f
private const val MODE_DIVIDER_HEIGHT_DP = 72f

private const val CONNECTION_STROKE_WIDTH = 9f
private const val CONNECTION_HEAD_LENGTH_DP = 30f
private const val CONNECTION_HEAD_WIDTH_DP = 17f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    onOpenMenu: () -> Unit,
    onPreview: (Long) -> Unit,
    viewModel: EditorViewModel = hiltViewModel()
) {

    val graph by viewModel.graph.collectAsStateWithLifecycle()
    val canEdit = graph?.story?.accessRole?.canEdit == true

    var editingNode by remember {
        mutableStateOf<StoryNode?>(null)
    }

    var showNewNode by remember {
        mutableStateOf(false)
    }

    var validationIssues by remember {
        mutableStateOf<List<ValidationIssue>?>(null)
    }

    var pendingText by remember {
        mutableStateOf("")
    }

    var pendingJson by remember {
        mutableStateOf("")
    }

    val context = LocalContext.current

    val snackbarHostState =
        remember {
            SnackbarHostState()
        }

    val scope =
        rememberCoroutineScope()

    fun write(
        uri: Uri?,
        content: String
    ) {

        if (uri == null) {
            return
        }

        try {

            val output =
                context
                    .contentResolver
                    .openOutputStream(uri)
                    ?: throw IOException(
                        "Не удалось открыть выбранный файл"
                    )

            output
                .bufferedWriter()
                .use {
                    it.write(content)
                }

            scope.launch {
                snackbarHostState.showSnackbar(
                    "Файл сохранён"
                )
            }

        } catch (error: IOException) {

            scope.launch {
                snackbarHostState.showSnackbar(
                    "Ошибка сохранения: ${error.message}"
                )
            }
        }
    }

    val textLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument(
                "text/plain"
            )
        ) { uri ->

            write(
                uri,
                pendingText
            )
        }

    val jsonLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument(
                "application/json"
            )
        ) { uri ->

            write(
                uri,
                pendingJson
            )
        }

    Scaffold(

        snackbarHost = {
            SnackbarHost(
                snackbarHostState
            )
        },

        topBar = {

            TopAppBar(

                title = {

                    Column {

                        Text(
                            graph
                                ?.story
                                ?.title
                                ?: "Редактор"
                        )

                        Text(
                            "${graph?.nodes?.size ?: 0} узлов · " +
                                    "${graph?.connections?.size ?: 0} переходов" +
                                    if (canEdit) "" else " · Только чтение",
                            style =
                                MaterialTheme
                                    .typography
                                    .labelSmall
                        )
                    }
                },

                navigationIcon = {

                    IconButton(
                        onClick = onOpenMenu
                    ) {

                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "Меню"
                        )
                    }
                },

                actions = {

                    IconButton(

                        onClick = {
                            validationIssues =
                                viewModel.validate()
                        },

                        enabled =
                            graph != null
                    ) {

                        Icon(
                            Icons.Default.Rule,
                            contentDescription =
                                "Проверить"
                        )
                    }

                    IconButton(

                        onClick = {
                            onPreview(
                                viewModel.storyId
                            )
                        },

                        enabled =
                            graph != null
                    ) {

                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription =
                                "Пройти"
                        )
                    }

                    IconButton(

                        onClick = {

                            pendingText =
                                viewModel.exportText()

                            textLauncher.launch(
                                "${safeFileName(graph?.story?.title)}.txt"
                            )
                        },

                        enabled =
                            graph != null
                    ) {

                        Icon(
                            Icons.Default.Download,
                            contentDescription =
                                "Экспорт TXT"
                        )
                    }

                    TextButton(

                        onClick = {

                            pendingJson =
                                viewModel.exportJson()

                            jsonLauncher.launch(
                                "${safeFileName(graph?.story?.title)}.json"
                            )
                        },

                        enabled =
                            graph != null
                    ) {

                        Text("JSON")
                    }
                }
            )
        },

        floatingActionButton = {
            if (canEdit) {
                FloatingActionButton(
                    onClick = {
                        showNewNode = true
                    }
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Добавить узел"
                    )
                }
            }
        }

    ) { padding ->

        val currentGraph =
            graph

        if (currentGraph == null) {

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    "Загрузка истории…"
                )
            }

        } else {

            StoryGraphCanvas(

                graph =
                    currentGraph,

                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding),

                onNodeClick = { node ->
                    if (canEdit) editingNode = node
                },

                onMoveNode =
                    if (canEdit) viewModel::moveNode else { _, _, _ -> }
            )
        }
    }

    if (showNewNode) {

        NodeEditorDialog(

            node = null,

            graph = graph,

            onDismiss = {
                showNewNode = false
            },

            onSave = { type,
                       title,
                       text,
                       formatting ->

                val count =
                    graph?.nodes?.size
                        ?: 0

                val x =
                    if (type == NodeType.MODE_DIVIDER) {
                        MODE_DIVIDER_LEFT_DP
                    } else {
                        120f +
                                (count % 4) *
                                300f
                    }

                val y =
                    120f +
                            (count / 4) *
                            220f

                viewModel.createNode(
                    type = type,
                    title = title,
                    text = text,
                    formatting = formatting,
                    x = x,
                    y = y
                )

                showNewNode =
                    false
            },

            onDelete = {},

            onAddConnection = { _,
                                _,
                                _ ->
            },

            onDeleteConnection = {}
        )
    }

    editingNode?.let { node ->

        NodeEditorDialog(

            node =
                node,

            graph =
                graph,

            onDismiss = {
                editingNode = null
            },

            onSave = { type,
                       title,
                       text,
                       formatting ->

                viewModel.saveNode(

                    node.copy(

                        type =
                            type,

                        title =
                            title
                                .trim()
                                .ifBlank {
                                    type.displayName()
                                },
                        text =
                            text,

                        formatting =
                            formatting
                    )
                )

                editingNode =
                    null
            },

            onDelete = {

                viewModel.deleteNode(
                    node
                )

                editingNode =
                    null
            },

            onAddConnection = { from,
                                to,
                                label ->

                viewModel.addConnection(
                    from,
                    to,
                    label
                )
            },

            onDeleteConnection =
                viewModel::deleteConnection
        )
    }

    validationIssues?.let { issues ->

        ValidationDialog(

            issues =
                issues,

            onDismiss = {
                validationIssues = null
            }
        )
    }
}

@Composable
private fun StoryGraphCanvas(
    graph: StoryGraph,
    modifier: Modifier,
    onNodeClick: (StoryNode) -> Unit,
    onMoveNode: (
        Long,
        Float,
        Float
    ) -> Unit
) {

    val horizontalScroll =
        rememberScrollState()

    val verticalScroll =
        rememberScrollState()

    val positions =
        remember {
            mutableStateMapOf<
                    Long,
                    Offset
                    >()
        }

    val density =
        LocalDensity.current

    val connectionColor =
        MaterialTheme.colorScheme.primary

    LaunchedEffect(
        graph.nodes
    ) {

        val ids =
            graph.nodes.mapTo(
                mutableSetOf()
            ) {
                it.id
            }

        positions
            .keys
            .filterNot {
                it in ids
            }
            .toList()
            .forEach {
                positions.remove(it)
            }

        graph.nodes.forEach { node ->

            if (
                positions[node.id] ==
                null
            ) {

                positions[node.id] =
                    Offset(
                        node.positionX,
                        node.positionY
                    )
            }
        }
    }

    Box(

        modifier =
            modifier
                .background(
                    MaterialTheme
                        .colorScheme
                        .surfaceVariant
                        .copy(
                            alpha = 0.35f
                        )
                )
                .horizontalScroll(
                    horizontalScroll
                )
                .verticalScroll(
                    verticalScroll
                )

    ) {

        Box(
            modifier =
                Modifier.size(
                    CANVAS_WIDTH_DP.dp,
                    CANVAS_HEIGHT_DP.dp
                )
        ) {

            Canvas(
                modifier =
                    Modifier.fillMaxSize()
            ) {

                graph.connections.forEach { connection ->

                    val fromNode =
                        graph.nodeById(
                            connection.fromNodeId
                        )

                    val toNode =
                        graph.nodeById(
                            connection.toNodeId
                        )

                    if (
                        fromNode?.type == NodeType.MODE_DIVIDER ||
                        toNode?.type == NodeType.MODE_DIVIDER
                    ) {
                        return@forEach
                    }

                    val from =
                        positions[
                            connection.fromNodeId
                        ]
                            ?: return@forEach

                    val to =
                        positions[
                            connection.toNodeId
                        ]
                            ?: return@forEach

                    val start =
                        Offset(

                            x =
                                (
                                        from.x +
                                                NODE_WIDTH_DP
                                        ) *
                                        density.density,

                            y =
                                (
                                        from.y +
                                                NODE_HEIGHT_DP /
                                                2f
                                        ) *
                                        density.density
                        )

                    val end =
                        Offset(

                            x =
                                to.x *
                                        density.density,

                            y =
                                (
                                        to.y +
                                                NODE_HEIGHT_DP /
                                                2f
                                        ) *
                                        density.density
                        )

                    drawArrowConnection(
                        start = start,
                        end = end,
                        color = connectionColor,
                        strokeWidth = CONNECTION_STROKE_WIDTH,
                        arrowHeadLength = CONNECTION_HEAD_LENGTH_DP * density.density,
                        arrowHeadWidth = CONNECTION_HEAD_WIDTH_DP * density.density
                    )
                }
            }

            graph.nodes.forEach { node ->

                val position =
                    positions[node.id]
                        ?: Offset(
                            node.positionX,
                            node.positionY
                        )

                if (node.type == NodeType.MODE_DIVIDER) {

                    ModeDivider(
                        node = node,
                        position = position,
                        onPositionChange = { updated ->
                            positions[node.id] = updated
                        },
                        onPositionCommitted = { committed ->
                            onMoveNode(
                                node.id,
                                MODE_DIVIDER_LEFT_DP,
                                committed.y
                            )
                        },
                        onClick = {
                            onNodeClick(node)
                        }
                    )

                } else {

                    GraphNodeCard(

                        node =
                            node,

                        outgoingCount =
                            graph
                                .outgoing(
                                    node.id
                                )
                                .count { connection ->
                                    graph.nodeById(connection.toNodeId)?.type !=
                                            NodeType.MODE_DIVIDER
                                },

                        position =
                            position,

                        onPositionChange = {

                            positions[node.id] =
                                it
                        },

                        onPositionCommitted = { committed ->

                            onMoveNode(

                                node.id,

                                committed.x,

                                committed.y
                            )
                        },

                        onClick = {
                            onNodeClick(node)
                        }
                    )
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArrowConnection(
    start: Offset,
    end: Offset,
    color: Color,
    strokeWidth: Float,
    arrowHeadLength: Float,
    arrowHeadWidth: Float
) {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val distance = sqrt(dx * dx + dy * dy)

    if (distance <= 0.1f) return

    val directionX = dx / distance
    val directionY = dy / distance
    val perpendicularX = -directionY
    val perpendicularY = directionX

    val arrowBaseCenter = Offset(
        x = end.x - directionX * arrowHeadLength,
        y = end.y - directionY * arrowHeadLength
    )

    drawLine(
        color = color,
        start = start,
        end = arrowBaseCenter,
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )

    val leftPoint = Offset(
        x = arrowBaseCenter.x + perpendicularX * arrowHeadWidth,
        y = arrowBaseCenter.y + perpendicularY * arrowHeadWidth
    )

    val rightPoint = Offset(
        x = arrowBaseCenter.x - perpendicularX * arrowHeadWidth,
        y = arrowBaseCenter.y - perpendicularY * arrowHeadWidth
    )

    val arrowHead = Path().apply {
        moveTo(end.x, end.y)
        lineTo(leftPoint.x, leftPoint.y)
        lineTo(rightPoint.x, rightPoint.y)
        close()
    }

    drawPath(
        path = arrowHead,
        color = color
    )
}

@Composable
private fun ModeDivider(
    node: StoryNode,
    position: Offset,
    onPositionChange: (Offset) -> Unit,
    onPositionCommitted: (Offset) -> Unit,
    onClick: () -> Unit
) {

    val density =
        LocalDensity.current

    val latestPosition by
    rememberUpdatedState(
        position
    )

    Box(
        modifier =
            Modifier
                .offset {
                    IntOffset(
                        (MODE_DIVIDER_LEFT_DP * density.density)
                            .roundToInt(),
                        (position.y * density.density)
                            .roundToInt()
                    )
                }
                .size(
                    MODE_DIVIDER_WIDTH_DP.dp,
                    MODE_DIVIDER_HEIGHT_DP.dp
                )
                .pointerInput(node.id) {

                    var workingPosition =
                        Offset(
                            MODE_DIVIDER_LEFT_DP,
                            latestPosition.y
                        )

                    detectDragGestures(
                        onDragStart = {
                            workingPosition =
                                Offset(
                                    MODE_DIVIDER_LEFT_DP,
                                    latestPosition.y
                                )
                        },
                        onDragEnd = {
                            onPositionCommitted(
                                workingPosition
                            )
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()

                            workingPosition =
                                Offset(
                                    x = MODE_DIVIDER_LEFT_DP,
                                    y = (
                                            workingPosition.y +
                                                    dragAmount.y /
                                                    density.density
                                            ).coerceIn(
                                            0f,
                                            CANVAS_HEIGHT_DP -
                                                    MODE_DIVIDER_HEIGHT_DP
                                        )
                                )

                            onPositionChange(
                                workingPosition
                            )
                        }
                    )
                }
                .clickable(
                    onClick = onClick
                )
    ) {

        Column(
            modifier =
                Modifier.fillMaxSize(),
            verticalArrangement =
                Arrangement.Bottom
        ) {

            Surface(
                shape = RoundedCornerShape(10.dp),
                color =
                    MaterialTheme
                        .colorScheme
                        .primaryContainer,
                modifier =
                    Modifier.clickable(
                        onClick = onClick
                    )
            ) {
                Text(
                    text =
                        node.title
                            .ifBlank {
                                "Переход режима"
                            },
                    style =
                        MaterialTheme
                            .typography
                            .labelLarge,
                    fontWeight =
                        FontWeight.SemiBold,
                    modifier =
                        Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 7.dp
                        )
                )
            }

            Spacer(
                Modifier.height(
                    6.dp
                )
            )

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .primary,
                            shape =
                                RoundedCornerShape(3.dp)
                        )
            )
        }
    }
}

@Composable
private fun GraphNodeCard(
    node: StoryNode,
    outgoingCount: Int,
    position: Offset,
    onPositionChange: (Offset) -> Unit,
    onPositionCommitted: (Offset) -> Unit,
    onClick: () -> Unit
) {

    val density =
        LocalDensity.current

    val latestPosition by
    rememberUpdatedState(
        position
    )

    Card(

        modifier =
            Modifier
                .offset {

                    IntOffset(

                        (
                                position.x *
                                        density.density
                                )
                            .roundToInt(),

                        (
                                position.y *
                                        density.density
                                )
                            .roundToInt()
                    )
                }
                .size(
                    NODE_WIDTH_DP.dp,
                    NODE_HEIGHT_DP.dp
                )
                .pointerInput(
                    node.id
                ) {

                    var workingPosition =
                        latestPosition

                    detectDragGestures(

                        onDragStart = {

                            workingPosition =
                                latestPosition
                        },

                        onDragEnd = {

                            onPositionCommitted(
                                workingPosition
                            )
                        },

                        onDrag = { change,
                                   dragAmount ->

                            change.consume()

                            workingPosition =
                                Offset(

                                    x =
                                        (
                                                workingPosition.x +
                                                        dragAmount.x /
                                                        density.density
                                                )
                                            .coerceIn(
                                                0f,
                                                CANVAS_WIDTH_DP -
                                                        NODE_WIDTH_DP
                                            ),

                                    y =
                                        (
                                                workingPosition.y +
                                                        dragAmount.y /
                                                        density.density
                                                )
                                            .coerceIn(
                                                0f,
                                                CANVAS_HEIGHT_DP -
                                                        NODE_HEIGHT_DP
                                            )
                                )

                            onPositionChange(
                                workingPosition
                            )
                        }
                    )
                }
                .clickable(
                    onClick = onClick
                ),

        shape =
            RoundedCornerShape(
                16.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    nodeContainerColor(
                        node.type
                    )
            )

    ) {

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        14.dp
                    )

        ) {

            Text(

                text =
                    node.type
                        .displayName(),

                style =
                    MaterialTheme
                        .typography
                        .labelMedium,

                fontWeight =
                    FontWeight.Bold
            )

            Text(

                text =
                    node.title,

                style =
                    MaterialTheme
                        .typography
                        .titleMedium,

                fontWeight =
                    FontWeight.SemiBold,

                maxLines =
                    2,

                overflow =
                    TextOverflow.Ellipsis
            )

            Spacer(
                Modifier.height(
                    6.dp
                )
            )

            if (
                node.text.isBlank()
            ) {

                Text(

                    text =
                        "Текст не задан",

                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,

                    maxLines =
                        3,

                    overflow =
                        TextOverflow.Ellipsis,

                    modifier =
                        Modifier.weight(
                            1f
                        )
                )

            } else {

                RichText(

                    text =
                        node.text,

                    marks =
                        node.formatting,

                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,

                    maxLines =
                        3,

                    overflow =
                        TextOverflow.Ellipsis,

                    modifier =
                        Modifier.weight(
                            1f
                        )
                )
            }

            Text(

                text =
                    "Переходов: $outgoingCount",

                style =
                    MaterialTheme
                        .typography
                        .labelSmall
            )
        }
    }
}

@Composable
private fun nodeContainerColor(
    type: NodeType
): Color =
    when (type) {

        NodeType.START ->
            MaterialTheme.colorScheme.primaryContainer

        NodeType.CHOICE ->
            MaterialTheme.colorScheme.tertiaryContainer

        NodeType.POSITIVE_ACTION ->
            MaterialTheme.colorScheme.secondaryContainer

        NodeType.NEGATIVE_ACTION ->
            MaterialTheme.colorScheme.errorContainer

        NodeType.INSPECTION ->
            MaterialTheme.colorScheme.surfaceContainerHighest

        NodeType.ENDING ->
            MaterialTheme.colorScheme.primaryContainer

        NodeType.MODE_DIVIDER ->
            MaterialTheme.colorScheme.surface

        else ->
            MaterialTheme.colorScheme.surfaceContainerHigh
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NodeEditorDialog(
    node: StoryNode?,
    graph: StoryGraph?,
    onDismiss: () -> Unit,

    onSave: (
        NodeType,
        String,
        String,
        List<TextFormatMark>
    ) -> Unit,

    onDelete: () -> Unit,

    onAddConnection: (
        Long,
        Long,
        String
    ) -> Unit,

    onDeleteConnection: (
        Long
    ) -> Unit
) {

    var title by
    remember(
        node?.id
    ) {

        mutableStateOf(
            node
                ?.title
                .orEmpty()
        )
    }

    var textValue by
    remember(
        node?.id
    ) {

        mutableStateOf(
            TextFieldValue(
                node
                    ?.text
                    .orEmpty()
            )
        )
    }

    var formatting by
    remember(
        node?.id
    ) {

        mutableStateOf(
            node
                ?.formatting
                .orEmpty()
        )
    }

    var customColor by
    remember(
        node?.id
    ) {

        mutableStateOf(
            "#E53935"
        )
    }

    var type by
    remember(
        node?.id
    ) {

        mutableStateOf(
            node?.type
                ?: NodeType.SCENE
        )
    }

    var typeExpanded by
    remember {

        mutableStateOf(
            false
        )
    }

    var targetExpanded by
    remember {

        mutableStateOf(
            false
        )
    }

    var selectedTargetId by
    remember(
        node?.id,
        graph?.nodes
    ) {

        mutableStateOf(
            graph
                ?.nodes
                ?.firstOrNull { candidate ->
                    candidate.id != node?.id &&
                            candidate.type != NodeType.MODE_DIVIDER
                }
                ?.id
        )
    }

    var connectionLabel by
    remember {

        mutableStateOf(
            ""
        )
    }

    var showFullScreenEditor by
    remember(
        node?.id
    ) {

        mutableStateOf(
            false
        )
    }

    val outgoing =
        if (node?.type == NodeType.MODE_DIVIDER) {
            emptyList()
        } else {
            node
                ?.let {
                    graph?.outgoing(
                        it.id
                    )
                }
                .orEmpty()
        }

    val spellCheckState =
        rememberSpellCheckState(
            text = textValue.text,
            enabled = type != NodeType.MODE_DIVIDER
        )

    val styledTextValue =
        TextFieldValue(

            annotatedString =
                buildRichAnnotatedString(
                    text =
                        textValue.text,
                    marks =
                        formatting,
                    spellingIssues =
                        spellCheckState.issues
                ),

            selection =
                textValue.selection,

            composition =
                textValue.composition
        )

    AlertDialog(

        onDismissRequest =
            onDismiss,

        title = {

            Text(
                if (node == null) {
                    "Новый узел"
                } else {
                    "Редактирование узла"
                }
            )
        },

        text = {

            Column(

                modifier =
                    Modifier
                        .widthIn(
                            min = 320.dp,
                            max = 560.dp
                        )
                        .heightIn(
                            max = 650.dp
                        )
                        .verticalScroll(
                            rememberScrollState()
                        ),

                verticalArrangement =
                    Arrangement.spacedBy(
                        12.dp
                    )

            ) {

                ExposedDropdownMenuBox(

                    expanded =
                        typeExpanded,

                    onExpandedChange = {

                        if (
                            node?.type !=
                            NodeType.START
                        ) {

                            typeExpanded =
                                !typeExpanded
                        }
                    }

                ) {

                    OutlinedTextField(

                        value =
                            type.displayName(),

                        onValueChange = {},

                        readOnly =
                            true,

                        label = {
                            Text("Тип")
                        },

                        trailingIcon = {

                            ExposedDropdownMenuDefaults
                                .TrailingIcon(
                                    typeExpanded
                                )
                        },

                        modifier =
                            Modifier
                                .menuAnchor()
                                .fillMaxWidth(),

                        enabled =
                            node?.type !=
                                    NodeType.START
                    )

                    ExposedDropdownMenu(

                        expanded =
                            typeExpanded,

                        onDismissRequest = {
                            typeExpanded =
                                false
                        }

                    ) {

                        NodeType.entries

                            .filterNot {
                                it ==
                                        NodeType.START
                            }

                            .forEach { item ->

                                DropdownMenuItem(

                                    text = {
                                        Text(
                                            item.displayName()
                                        )
                                    },

                                    onClick = {

                                        type =
                                            item

                                        typeExpanded =
                                            false
                                    }
                                )
                            }
                    }
                }

                OutlinedTextField(

                    value =
                        title,

                    onValueChange = {
                        title = it
                    },

                    label = {
                        Text(
                            if (type == NodeType.MODE_DIVIDER) {
                                "Название перехода, например «Переход в 3D»"
                            } else {
                                "Название"
                            }
                        )
                    },

                    modifier =
                        Modifier.fillMaxWidth()
                )

                if (type != NodeType.MODE_DIVIDER) {

                    RichTextFormattingToolbar(

                        selectionStart =
                            textValue
                                .selection
                                .min,

                        selectionEnd =
                            textValue
                                .selection
                                .max,

                        formatting =
                            formatting,

                        customColor =
                            customColor,

                        onCustomColorChange = {
                            customColor = it
                        },

                        onToggle = { markType ->

                            formatting =
                                toggleTextMark(

                                    marks =
                                        formatting,

                                    start =
                                        textValue
                                            .selection
                                            .min,

                                    end =
                                        textValue
                                            .selection
                                            .max,

                                    type =
                                        markType
                                )
                        },

                        onColor = { colorHex ->

                            formatting =
                                applyTextColor(

                                    marks =
                                        formatting,

                                    start =
                                        textValue
                                            .selection
                                            .min,

                                    end =
                                        textValue
                                            .selection
                                            .max,

                                    colorHex =
                                        colorHex
                                )
                        },

                        onClear = {

                            formatting =
                                clearTextFormatting(

                                    marks =
                                        formatting,

                                    start =
                                        textValue
                                            .selection
                                            .min,

                                    end =
                                        textValue
                                            .selection
                                            .max
                                )
                        }
                    )

                    OutlinedTextField(

                        value =
                            styledTextValue,

                        onValueChange = { updated ->

                            formatting =
                                adjustMarksAfterTextChange(

                                    oldText =
                                        textValue.text,

                                    newText =
                                        updated.text,

                                    marks =
                                        formatting
                                )

                            textValue =
                                TextFieldValue(

                                    text =
                                        updated.text,

                                    selection =
                                        updated.selection,

                                    composition =
                                        updated.composition
                                )
                        },

                        label = {

                            Text(
                                "Текст сцены, реплика или описание действия"
                            )
                        },

                        minLines =
                            5,

                        modifier =
                            Modifier.fillMaxWidth()
                    )

                    when {
                        !spellCheckState.isAvailable ->
                            Text(
                                text = "Системная проверка орфографии выключена",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                        spellCheckState.isChecking ->
                            Text(
                                text = "Проверка орфографии…",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                        spellCheckState.issues.isNotEmpty() ->
                            Text(
                                text = "Возможных ошибок: ${spellCheckState.issues.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                    }

                    OutlinedButton(

                        onClick = {

                            showFullScreenEditor =
                                true
                        },

                        modifier =
                            Modifier.fillMaxWidth()

                    ) {

                        Text(
                            "Открыть редактор на весь экран"
                        )
                    }

                } else {

                    Text(
                        text =
                            "Разделитель не участвует в прохождении. " +
                                    "Название отображается над горизонтальной линией.",
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
                    )
                }

                if (
                    node != null &&
                    type != NodeType.MODE_DIVIDER
                ) {

                    Text(

                        text =
                            "Переходы",

                        style =
                            MaterialTheme
                                .typography
                                .titleMedium,

                        fontWeight =
                            FontWeight.SemiBold
                    )

                    if (
                        outgoing.isEmpty()
                    ) {

                        Text(

                            text =
                                "Переходов пока нет",

                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall
                        )
                    }

                    outgoing.forEach { connection ->

                        ConnectionRow(

                            connection =
                                connection,

                            targetTitle =
                                graph
                                    ?.nodeById(
                                        connection.toNodeId
                                    )
                                    ?.title
                                    ?: "Неизвестный узел",

                            onDelete = {

                                onDeleteConnection(
                                    connection.id
                                )
                            }
                        )
                    }

                    val targets =
                        graph
                            ?.nodes
                            .orEmpty()
                            .filter { target ->
                                target.id != node.id &&
                                        target.type != NodeType.MODE_DIVIDER
                            }

                    if (
                        targets.isNotEmpty()
                    ) {

                        ExposedDropdownMenuBox(

                            expanded =
                                targetExpanded,

                            onExpandedChange = {

                                targetExpanded =
                                    !targetExpanded
                            }

                        ) {

                            OutlinedTextField(

                                value =
                                    targets
                                        .firstOrNull {
                                            it.id ==
                                                    selectedTargetId
                                        }
                                        ?.title
                                        .orEmpty(),

                                onValueChange = {},

                                readOnly =
                                    true,

                                label = {

                                    Text(
                                        "Куда ведёт переход"
                                    )
                                },

                                trailingIcon = {

                                    ExposedDropdownMenuDefaults
                                        .TrailingIcon(
                                            targetExpanded
                                        )
                                },

                                modifier =
                                    Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                            )

                            ExposedDropdownMenu(

                                expanded =
                                    targetExpanded,

                                onDismissRequest = {

                                    targetExpanded =
                                        false
                                }

                            ) {

                                targets.forEach { target ->

                                    DropdownMenuItem(

                                        text = {

                                            Text(
                                                target.title
                                            )
                                        },

                                        onClick = {

                                            selectedTargetId =
                                                target.id

                                            targetExpanded =
                                                false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(

                            value =
                                connectionLabel,

                            onValueChange = {
                                connectionLabel = it
                            },

                            label = {

                                Text(

                                    if (
                                        type ==
                                        NodeType.CHOICE
                                    ) {

                                        "Текст варианта выбора"

                                    } else {

                                        "Подпись перехода"
                                    }
                                )
                            },

                            modifier =
                                Modifier.fillMaxWidth()
                        )

                        OutlinedButton(

                            onClick = {

                                selectedTargetId
                                    ?.let { targetId ->

                                        onAddConnection(

                                            node.id,

                                            targetId,

                                            connectionLabel
                                        )

                                        connectionLabel =
                                            ""
                                    }
                            },

                            modifier =
                                Modifier.fillMaxWidth()

                        ) {

                            Text(
                                "Добавить переход"
                            )
                        }
                    }
                }
            }
        },

        confirmButton = {

            Button(

                onClick = {

                    onSave(

                        type,

                        title,

                        if (type == NodeType.MODE_DIVIDER) {
                            ""
                        } else {
                            textValue.text
                        },

                        if (type == NodeType.MODE_DIVIDER) {
                            emptyList()
                        } else {
                            formatting
                        }
                    )
                }

            ) {

                Text(
                    "Сохранить"
                )
            }
        },

        dismissButton = {

            Row {

                if (
                    node != null &&
                    node.type !=
                    NodeType.START
                ) {

                    TextButton(
                        onClick =
                            onDelete
                    ) {

                        Icon(
                            Icons.Default.Delete,
                            contentDescription =
                                null
                        )

                        Spacer(
                            Modifier.width(
                                4.dp
                            )
                        )

                        Text(
                            "Удалить"
                        )
                    }
                }

                TextButton(
                    onClick =
                        onDismiss
                ) {

                    Text(
                        "Отмена"
                    )
                }
            }
        }
    )

    if (
        showFullScreenEditor &&
        type != NodeType.MODE_DIVIDER
    ) {

        FullScreenTextEditor(

            textValue =
                textValue,

            formatting =
                formatting,

            spellingIssues =
                spellCheckState.issues,

            nodeType =
                type,

            customColor =
                customColor,

            onCustomColorChange = {
                customColor = it
            },

            onTextValueChange = { newValue ->

                textValue =
                    newValue
            },

            onFormattingChange = { newFormatting ->

                formatting =
                    newFormatting
            },

            onClose = {

                showFullScreenEditor =
                    false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullScreenTextEditor(
    textValue: TextFieldValue,
    formatting: List<TextFormatMark>,
    spellingIssues: List<SpellingIssue>,
    nodeType: NodeType,
    customColor: String,
    onCustomColorChange: (String) -> Unit,
    onTextValueChange: (TextFieldValue) -> Unit,
    onFormattingChange: (List<TextFormatMark>) -> Unit,
    onClose: () -> Unit
) {

    val styledValue =
        TextFieldValue(

            annotatedString =
                buildRichAnnotatedString(
                    text =
                        textValue.text,
                    marks =
                        formatting,
                    spellingIssues =
                        spellingIssues
                ),

            selection =
                textValue.selection,

            composition =
                textValue.composition
        )

    Dialog(

        onDismissRequest =
            onClose,

        properties =
            DialogProperties(
                usePlatformDefaultWidth =
                    false
            )

    ) {

        Surface(

            modifier =
                Modifier.fillMaxSize(),

            color =
                MaterialTheme
                    .colorScheme
                    .background

        ) {

            Scaffold(

                topBar = {

                    TopAppBar(

                        title = {

                            Text(
                                "${nodeType.displayName()}: текст"
                            )
                        },

                        navigationIcon = {

                            IconButton(

                                onClick =
                                    onClose

                            ) {

                                Icon(

                                    imageVector =
                                        Icons.Default.Close,

                                    contentDescription =
                                        "Закрыть"
                                )
                            }
                        },

                        actions = {

                            TextButton(

                                onClick =
                                    onClose

                            ) {

                                Text(
                                    "Готово"
                                )
                            }
                        }
                    )
                }

            ) { paddingValues ->

                Column(

                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(
                                paddingValues
                            )
                            .padding(
                                horizontal = 16.dp,
                                vertical = 12.dp
                            ),

                    verticalArrangement =
                        Arrangement.spacedBy(
                            10.dp
                        )

                ) {

                    RichTextFormattingToolbar(

                        selectionStart =
                            textValue
                                .selection
                                .min,

                        selectionEnd =
                            textValue
                                .selection
                                .max,

                        formatting =
                            formatting,

                        customColor =
                            customColor,

                        onCustomColorChange =
                            onCustomColorChange,

                        onToggle = { markType ->

                            onFormattingChange(

                                toggleTextMark(

                                    marks =
                                        formatting,

                                    start =
                                        textValue
                                            .selection
                                            .min,

                                    end =
                                        textValue
                                            .selection
                                            .max,

                                    type =
                                        markType
                                )
                            )
                        },

                        onColor = { hex ->

                            onFormattingChange(

                                applyTextColor(

                                    marks =
                                        formatting,

                                    start =
                                        textValue
                                            .selection
                                            .min,

                                    end =
                                        textValue
                                            .selection
                                            .max,

                                    colorHex =
                                        hex
                                )
                            )
                        },

                        onClear = {

                            onFormattingChange(

                                clearTextFormatting(

                                    marks =
                                        formatting,

                                    start =
                                        textValue
                                            .selection
                                            .min,

                                    end =
                                        textValue
                                            .selection
                                            .max
                                )
                            )
                        }
                    )

                    TextField(

                        value =
                            styledValue,

                        onValueChange = { updated ->

                            val newFormatting =
                                adjustMarksAfterTextChange(

                                    oldText =
                                        textValue.text,

                                    newText =
                                        updated.text,

                                    marks =
                                        formatting
                                )

                            onFormattingChange(
                                newFormatting
                            )

                            onTextValueChange(

                                TextFieldValue(

                                    text =
                                        updated.text,

                                    selection =
                                        updated.selection,

                                    composition =
                                        updated.composition
                                )
                            )
                        },

                        placeholder = {

                            Text(
                                "Начните писать…"
                            )
                        },

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(
                                    1f
                                ),

                        textStyle =
                            MaterialTheme
                                .typography
                                .bodyLarge,

                        colors =
                            TextFieldDefaults.colors(

                                focusedContainerColor =
                                    Color.Transparent,

                                unfocusedContainerColor =
                                    Color.Transparent,

                                disabledContainerColor =
                                    Color.Transparent,

                                focusedIndicatorColor =
                                    Color.Transparent,

                                unfocusedIndicatorColor =
                                    Color.Transparent,

                                disabledIndicatorColor =
                                    Color.Transparent
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun RichTextFormattingToolbar(
    selectionStart: Int,
    selectionEnd: Int,
    formatting: List<TextFormatMark>,
    customColor: String,
    onCustomColorChange: (String) -> Unit,
    onToggle: (TextMarkType) -> Unit,
    onColor: (String?) -> Unit,
    onClear: () -> Unit
) {

    val hasSelection =
        selectionEnd >
                selectionStart

    val palette =
        listOf(
            "#E53935", // красный
            "#FB8C00", // оранжевый
            "#FDD835", // жёлтый
            "#43A047", // зелёный
            "#00ACC1", // голубой
            "#1E88E5", // синий
            "#8E24AA", // фиолетовый
            "#EC407A"  // розовый
        )

    Column(

        verticalArrangement =
            Arrangement.spacedBy(
                8.dp
            )

    ) {

        Text(

            text =
                if (hasSelection) {

                    "Форматирование выделенного текста"

                } else {

                    "Выделите текст для форматирования"
                },

            style =
                MaterialTheme
                    .typography
                    .labelMedium
        )


        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(
                        rememberScrollState()
                    ),

            horizontalArrangement =
                Arrangement.spacedBy(
                    8.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically

        ) {

            OutlinedButton(

                onClick = {
                    onToggle(
                        TextMarkType.BOLD
                    )
                },

                enabled =
                    hasSelection

            ) {

                Text(

                    text =
                        "B",

                    fontWeight =
                        FontWeight.Bold
                )
            }

            OutlinedButton(

                onClick = {
                    onToggle(
                        TextMarkType.ITALIC
                    )
                },

                enabled =
                    hasSelection

            ) {

                Text(

                    text =
                        "I",

                    fontStyle =
                        FontStyle.Italic
                )
            }

            OutlinedButton(

                onClick = {
                    onToggle(
                        TextMarkType.STRIKETHROUGH
                    )
                },

                enabled =
                    hasSelection

            ) {

                Text(

                    text =
                        "S",

                    textDecoration =
                        TextDecoration.LineThrough
                )
            }

            TextButton(

                onClick = {
                    onColor(null)
                },

                enabled =
                    hasSelection

            ) {

                Text(
                    "Сбросить цвет"
                )
            }

            TextButton(

                onClick =
                    onClear,

                enabled =
                    hasSelection &&
                            formatting.isNotEmpty()

            ) {

                Text(
                    "Очистить стили"
                )
            }
        }

        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(
                        rememberScrollState()
                    ),

            horizontalArrangement =
                Arrangement.spacedBy(
                    10.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically

        ) {

            palette.forEach { hex ->

                val color =
                    parseHexColor(
                        hex
                    )
                        ?: Color.Gray

                Box(

                    modifier =
                        Modifier
                            .size(
                                34.dp
                            )
                            .background(
                                color =
                                    color,
                                shape =
                                    CircleShape
                            )
                            .clickable(
                                enabled =
                                    hasSelection
                            ) {

                                onColor(
                                    hex
                                )
                            }
                )
            }
        }

        Row(

            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(
                    8.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically

        ) {

            OutlinedTextField(

                value =
                    customColor,

                onValueChange =
                    onCustomColorChange,

                label = {
                    Text(
                        "HEX"
                    )
                },

                placeholder = {
                    Text(
                        "#FF0000"
                    )
                },

                singleLine =
                    true,

                modifier =
                    Modifier.weight(
                        1f
                    )
            )

            Button(

                onClick = {

                    onColor(
                        customColor
                    )
                },

                enabled =
                    hasSelection &&
                            parseHexColor(
                                customColor
                            ) != null

            ) {

                Text(
                    "Применить"
                )
            }
        }
    }
}

@Composable
private fun ConnectionRow(
    connection: StoryConnection,
    targetTitle: String,
    onDelete: () -> Unit
) {

    Card(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 12.dp,
                        vertical = 8.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically

        ) {

            Column(
                modifier =
                    Modifier.weight(
                        1f
                    )
            ) {

                Text(

                    text =
                        connection
                            .label
                            .ifBlank {
                                "Продолжить"
                            },

                    fontWeight =
                        FontWeight.Medium
                )

                Text(

                    text =
                        "→ $targetTitle",

                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )
            }

            IconButton(
                onClick =
                    onDelete
            ) {

                Icon(

                    Icons.Default.Delete,

                    contentDescription =
                        "Удалить переход"
                )
            }
        }
    }
}

@Composable
private fun ValidationDialog(
    issues: List<ValidationIssue>,
    onDismiss: () -> Unit
) {

    AlertDialog(

        onDismissRequest =
            onDismiss,

        title = {

            Text(
                "Проверка сценария"
            )
        },

        text = {

            Column(

                modifier =
                    Modifier
                        .heightIn(
                            max = 520.dp
                        )
                        .verticalScroll(
                            rememberScrollState()
                        ),

                verticalArrangement =
                    Arrangement.spacedBy(
                        10.dp
                    )

            ) {

                if (
                    issues.isEmpty()
                ) {

                    Text(
                        "Ошибок не найдено"
                    )

                } else {

                    issues.forEach { issue ->

                        Card(
                            modifier =
                                Modifier.fillMaxWidth()
                        ) {

                            Column(
                                Modifier.padding(
                                    12.dp
                                )
                            ) {

                                Text(

                                    text =
                                        if (
                                            issue.severity ==
                                            ValidationSeverity.ERROR
                                        ) {

                                            "Ошибка"

                                        } else {

                                            "Предупреждение"
                                        },

                                    fontWeight =
                                        FontWeight.Bold
                                )

                                Text(
                                    issue.message
                                )
                            }
                        }
                    }
                }
            }
        },

        confirmButton = {

            TextButton(
                onClick =
                    onDismiss
            ) {

                Text(
                    "Закрыть"
                )
            }
        }
    )
}

private fun safeFileName(
    title: String?
): String =
    title
        .orEmpty()
        .trim()
        .ifBlank {
            "novel"
        }
        .replace(
            Regex(
                """[\\/:*?"<>|]"""
            ),
            "_"
        )
        .take(
            80
        )
