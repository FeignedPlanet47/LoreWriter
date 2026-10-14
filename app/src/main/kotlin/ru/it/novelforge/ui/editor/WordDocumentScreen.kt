package ru.it.novelforge.ui.editor

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.IOException
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val DOCX_MIME =
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordDocumentScreen(
    onOpenMenu: () -> Unit,
    viewModel: EditorViewModel
) {
    val graph by viewModel.graph.collectAsStateWithLifecycle()
    val story = graph?.story
    val canEdit = story?.accessRole?.canEdit == true

    var text by remember { mutableStateOf("") }
    var savedText by remember { mutableStateOf("") }
    var loadedStoryId by remember { mutableStateOf<Long?>(null) }
    var showRegenerateDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    /*
     * Первый вход во вкладку:
     * - если документ уже редактировали, открываем сохранённую версию;
     * - если он пустой, автоматически формируем его из текущего сценария.
     */
    LaunchedEffect(story?.id) {
        val currentStory = story ?: return@LaunchedEffect

        if (loadedStoryId != currentStory.id) {
            val initialText =
                currentStory.wordDocument.ifBlank {
                    viewModel.buildWordDocumentFromStory()
                }

            text = initialText
            savedText = initialText
            loadedStoryId = currentStory.id

            if (
                canEdit &&
                currentStory.wordDocument.isBlank() &&
                initialText.isNotBlank()
            ) {
                viewModel.updateWordDocument(initialText)
            }
        }
    }

    /*
     * Если Room прислал изменение документа извне, синхронизируем его,
     * но не перетираем текст, который пользователь прямо сейчас редактирует.
     */
    LaunchedEffect(story?.wordDocument) {
        val currentStory = story ?: return@LaunchedEffect
        if (
            loadedStoryId == currentStory.id &&
            text == savedText &&
            currentStory.wordDocument != savedText
        ) {
            text = currentStory.wordDocument
            savedText = currentStory.wordDocument
        }
    }

    LaunchedEffect(text, loadedStoryId, canEdit) {
        if (!canEdit || loadedStoryId == null || text == savedText) {
            return@LaunchedEffect
        }

        delay(600)

        viewModel.updateWordDocument(text)
        savedText = text
    }

    val latestText by rememberUpdatedState(text)
    val latestTitle by rememberUpdatedState(story?.title.orEmpty())
    val context =
        androidx.compose.ui.platform.LocalContext
            .current
            .applicationContext

    val docxLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument(DOCX_MIME)
        ) { uri ->
            if (uri == null) {
                return@rememberLauncherForActivityResult
            }

            scope.launch {
                val result =
                    runCatching {
                        withContext(Dispatchers.IO) {
                            writeDocx(
                                context = context,
                                uri = uri,
                                text = latestText,
                                title = latestTitle
                            )
                        }
                    }

                result.fold(
                    onSuccess = {
                        snackbarHostState.showSnackbar(
                            "Word-документ сохранён"
                        )
                    },
                    onFailure = { error ->
                        snackbarHostState.showSnackbar(
                            "Ошибка сохранения DOCX: ${error.message}"
                        )
                    }
                )
            }
        }

    val words =
        remember(text) {
            text
                .trim()
                .split(Regex("\\s+"))
                .count { it.isNotBlank() }
        }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Word-документ")
                        Text(
                            text =
                                if (!canEdit) {
                                    "Только чтение"
                                } else if (text == savedText) {
                                    "Сохранено автоматически"
                                } else {
                                    "Сохранение…"
                                },
                            style =
                                MaterialTheme.typography.labelSmall,
                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant
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
                            showRegenerateDialog = true
                        },
                        enabled = graph != null && canEdit
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription =
                                "Сформировать заново из сценария"
                        )
                    }

                    IconButton(
                        onClick = {
                            if (canEdit && text != savedText) {
                                viewModel.updateWordDocument(text)
                                savedText = text
                            }

                            docxLauncher.launch(
                                "${safeWordFileName(story?.title)}.docx"
                            )
                        },
                        enabled = story != null
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription =
                                "Скачать Word-документ"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(
                        horizontal = 16.dp,
                        vertical = 10.dp
                    ),
            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text =
                    "Это текст будущего DOCX. Его можно свободно " +
                            "дописывать и исправлять перед скачиванием.",
                style =
                    MaterialTheme.typography.bodySmall,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Слов: $words",
                    style =
                        MaterialTheme.typography.labelMedium,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Символов: ${text.length}",
                    style =
                        MaterialTheme.typography.labelMedium,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        showRegenerateDialog = true
                    },
                    enabled = graph != null && canEdit,
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null
                    )
                    Text(
                        text = "Из сценария",
                        modifier =
                            Modifier.padding(start = 8.dp)
                    )
                }

                Button(
                    onClick = {
                        if (canEdit && text != savedText) {
                            viewModel.updateWordDocument(text)
                            savedText = text
                        }

                        docxLauncher.launch(
                            "${safeWordFileName(story?.title)}.docx"
                        )
                    },
                    enabled = story != null,
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = null
                    )
                    Text(
                        text = "Скачать DOCX",
                        modifier =
                            Modifier.padding(start = 8.dp)
                    )
                }
            }

            TextField(
                value = text,
                onValueChange = {
                    text = it
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                placeholder = {
                    Text(
                        "Здесь можно отредактировать текст, " +
                                "который попадёт в Word-документ…"
                    )
                },
                readOnly = !canEdit,
                textStyle =
                    MaterialTheme.typography.bodyLarge,
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

    if (showRegenerateDialog && canEdit) {
        AlertDialog(
            onDismissRequest = {
                showRegenerateDialog = false
            },
            title = {
                Text("Сформировать документ заново?")
            },
            text = {
                Text(
                    "Текущий текст Word-документа будет заменён " +
                            "актуальным экспортом из схемы истории. " +
                            "Ручные правки в этой вкладке будут удалены."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val generated =
                            viewModel.buildWordDocumentFromStory()

                        text = generated
                        savedText = generated
                        viewModel.updateWordDocument(generated)

                        showRegenerateDialog = false

                        scope.launch {
                            snackbarHostState.showSnackbar(
                                "Документ сформирован из сценария"
                            )
                        }
                    }
                ) {
                    Text("Заменить")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRegenerateDialog = false
                    }
                ) {
                    Text("Отмена")
                }
            }
        )
    }
}

private fun writeDocx(
    context: Context,
    uri: Uri,
    text: String,
    title: String
) {
    val output =
        context
            .contentResolver
            .openOutputStream(uri)
            ?: throw IOException(
                "Не удалось открыть выбранный файл"
            )

    output.use {
        createDocx(
            output = it,
            text = text,
            title = title
        )
    }
}

/**
 * Создаёт минимальный настоящий Office Open XML .docx без Apache POI.
 *
 * Это значительно легче для Android по памяти и не требует новых Gradle
 * зависимостей. Word, LibreOffice и Google Docs открывают такой документ
 * как обычный DOCX.
 */
private fun createDocx(
    output: OutputStream,
    text: String,
    title: String
) {
    ZipOutputStream(output.buffered()).use { zip ->

        zip.writeEntry(
            "[Content_Types].xml",
            contentTypesXml()
        )

        zip.writeEntry(
            "_rels/.rels",
            packageRelationshipsXml()
        )

        zip.writeEntry(
            "docProps/core.xml",
            corePropertiesXml(title)
        )

        zip.writeEntry(
            "docProps/app.xml",
            appPropertiesXml()
        )

        zip.writeEntry(
            "word/document.xml",
            documentXml(text)
        )
    }
}

private fun ZipOutputStream.writeEntry(
    name: String,
    content: String
) {
    putNextEntry(
        ZipEntry(name)
    )

    write(
        content.toByteArray(
            Charsets.UTF_8
        )
    )

    closeEntry()
}

private fun contentTypesXml(): String =
    """
    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
        <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
        <Default Extension="xml" ContentType="application/xml"/>
        <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
        <Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
        <Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>
    </Types>
    """.trimIndent()

private fun packageRelationshipsXml(): String =
    """
    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
        <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
        <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
        <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/>
    </Relationships>
    """.trimIndent()

private fun corePropertiesXml(
    title: String
): String =
    """
    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <cp:coreProperties
        xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties"
        xmlns:dc="http://purl.org/dc/elements/1.1/"
        xmlns:dcterms="http://purl.org/dc/terms/"
        xmlns:dcmitype="http://purl.org/dc/dcmitype/"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <dc:title>${xmlEscape(title)}</dc:title>
        <dc:creator>NovelForge</dc:creator>
        <cp:lastModifiedBy>NovelForge</cp:lastModifiedBy>
    </cp:coreProperties>
    """.trimIndent()

private fun appPropertiesXml(): String =
    """
    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <Properties
        xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties"
        xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes">
        <Application>NovelForge</Application>
    </Properties>
    """.trimIndent()

private fun documentXml(
    text: String
): String {
    val normalized =
        text
            .replace("\r\n", "\n")
            .replace('\r', '\n')

    val paragraphs =
        normalized
            .split('\n')
            .joinToString(
                separator = "\n"
            ) { line ->
                if (line.isEmpty()) {
                    "<w:p/>"
                } else {
                    val safeLine =
                        xmlEscape(
                            sanitizeXmlText(
                                line.replace(
                                    "\t",
                                    "    "
                                )
                            )
                        )

                    """
                    <w:p>
                        <w:r>
                            <w:t xml:space="preserve">$safeLine</w:t>
                        </w:r>
                    </w:p>
                    """.trimIndent()
                }
            }

    return """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <w:document
            xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
            <w:body>
                $paragraphs
                <w:sectPr>
                    <w:pgSz w:w="11906" w:h="16838"/>
                    <w:pgMar
                        w:top="1134"
                        w:right="1134"
                        w:bottom="1134"
                        w:left="1134"
                        w:header="708"
                        w:footer="708"
                        w:gutter="0"/>
                </w:sectPr>
            </w:body>
        </w:document>
    """.trimIndent()
}

private fun sanitizeXmlText(
    value: String
): String =
    buildString(
        value.length
    ) {
        value.forEach { char ->
            if (
                char == '\t' ||
                char.code >= 0x20
            ) {
                append(char)
            }
        }
    }

private fun xmlEscape(
    value: String
): String =
    buildString(
        value.length
    ) {
        value.forEach { char ->
            when (char) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> append(char)
            }
        }
    }

private fun safeWordFileName(
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
        .take(80)
