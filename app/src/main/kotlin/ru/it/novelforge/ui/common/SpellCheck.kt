package ru.it.novelforge.ui.common

import android.content.Context
import android.os.Build
import android.util.Log
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SpellCheckerSession
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import android.view.textservice.TextServicesManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.io.Closeable
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

data class SpellingIssue(
    val start: Int,
    val end: Int,
    val suggestions: List<String> = emptyList(),
    val isGrammarError: Boolean = false
)

data class SpellCheckState(
    val issues: List<SpellingIssue> = emptyList(),
    val isAvailable: Boolean = true,
    val isChecking: Boolean = false
)

@Suppress("DEPRECATION")
private class AndroidSpellChecker(
    context: Context
) : SpellCheckerSession.SpellCheckerSessionListener, Closeable {

    private val manager =
        context.getSystemService(
            Context.TEXT_SERVICES_MANAGER_SERVICE
        ) as TextServicesManager

    /*
     * Создаём ОДНУ session и даём Android время подключить Gboard.
     *
     * ВАЖНО:
     * isSessionDisconnected() НЕ используем для решения о пересоздании.
     * В Android он возвращает true и в состоянии WAIT_CONNECTION.
     */
    private val session: SpellCheckerSession? =
        runCatching {
            manager.newSpellCheckerSession(
                null,
                null,
                this,
                true
            )
        }.onFailure { error ->
            Log.e(
                TAG,
                "Ошибка создания SpellCheckerSession",
                error
            )
        }.getOrNull()

    private var pendingCallback:
        ((List<SpellingIssue>) -> Unit)? = null

    private var pendingChunks:
        List<TextChunk> = emptyList()

    private var requestWasSent =
        false

    val isAvailable: Boolean
        get() {
            if (session == null) {
                return false
            }

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.S
            ) {
                return manager.isSpellCheckerEnabled &&
                    manager.currentSpellCheckerInfo != null
            }

            return true
        }

    init {
        Log.d(
            TAG,
            "SpellCheckerSession создана: ${session != null}; " +
                "package=${session?.spellChecker?.packageName}; " +
                "isSessionDisconnected=${session?.isSessionDisconnected}"
        )

        Log.d(
            TAG,
            "ВАЖНО: isSessionDisconnected=true сразу после создания " +
                "может означать WAIT_CONNECTION. Session не пересоздаём."
        )
    }

    fun check(
        text: String,
        onResult: (List<SpellingIssue>) -> Unit
    ) {
        val activeSession =
            session

        if (
            activeSession == null ||
            text.isBlank()
        ) {
            onResult(emptyList())
            return
        }

        val chunks =
            splitIntoChunks(text)

        pendingChunks =
            chunks

        pendingCallback =
            onResult

        requestWasSent =
            true

        Log.d(
            TAG,
            "========== НОВАЯ ПРОВЕРКА =========="
        )

        Log.d(
            TAG,
            "Текст: «${text.replace("\n", "\\n")}»"
        )

        Log.d(
            TAG,
            "package=${activeSession.spellChecker.packageName}"
        )

        Log.d(
            TAG,
            "isSessionDisconnected перед запросом=" +
                activeSession.isSessionDisconnected +
                " (true может означать WAIT_CONNECTION)"
        )

        val textInfos =
            chunks.map { chunk ->
                TextInfo(chunk.text)
            }.toTypedArray()

        /*
         * Если соединение ещё устанавливается, Android framework
         * сам поставит этот запрос в очередь до onServiceConnected().
         */
        activeSession.getSentenceSuggestions(
            textInfos,
            MAX_SUGGESTIONS
        )

        Log.d(
            TAG,
            "getSentenceSuggestions() вызван; ждём callback"
        )
    }

    override fun onGetSentenceSuggestions(
        results: Array<out SentenceSuggestionsInfo>?
    ) {
        val callback =
            pendingCallback ?: return

        val chunks =
            pendingChunks

        pendingCallback = null
        pendingChunks = emptyList()
        requestWasSent = false

        val safeResults =
            results.orEmpty()

        Log.d(
            TAG,
            "✅ onGetSentenceSuggestions CALLBACK: results=${safeResults.size}"
        )

        if (safeResults.isEmpty()) {
            Log.d(
                TAG,
                "Gboard ответил, но ошибок/подсказок не вернул"
            )

            callback(emptyList())
            return
        }

        val issues =
            buildList {
                safeResults.forEachIndexed { resultIndex, sentence ->

                    val chunk =
                        chunks.getOrNull(resultIndex)
                            ?: return@forEachIndexed

                    Log.d(
                        TAG,
                        "Фрагмент #${resultIndex + 1}: " +
                            "suggestionsCount=${sentence.suggestionsCount}"
                    )

                    for (
                        index in
                        0 until sentence.suggestionsCount
                    ) {
                        val info =
                            sentence.getSuggestionsInfoAt(index)

                        val localStart =
                            sentence.getOffsetAt(index)

                        val length =
                            sentence.getLengthAt(index)

                        if (
                            localStart < 0 ||
                            length <= 0 ||
                            localStart >= chunk.text.length
                        ) {
                            continue
                        }

                        val localEnd =
                            (localStart + length)
                                .coerceAtMost(chunk.text.length)

                        val word =
                            chunk.text.substring(
                                localStart,
                                localEnd
                            )

                        val attributes =
                            info.suggestionsAttributes

                        val inDictionary =
                            attributes and
                                SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY != 0

                        val looksLikeTypo =
                            attributes and
                                SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO != 0

                        val grammarError =
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                                attributes and
                                SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_GRAMMAR_ERROR != 0

                        val suggestions =
                            buildList {
                                for (
                                    suggestionIndex in
                                    0 until info.suggestionsCount
                                ) {
                                    add(
                                        info.getSuggestionAt(
                                            suggestionIndex
                                        )
                                    )
                                }
                            }
                                .filter { it.isNotBlank() }
                                .distinct()

                        if (
                            looksLikeTypo ||
                            grammarError
                        ) {
                            Log.d(
                                TAG,
                                "❌ ОШИБКА: «$word» | " +
                                    "inDictionary=$inDictionary | " +
                                    "looksLikeTypo=$looksLikeTypo | " +
                                    "grammarError=$grammarError | " +
                                    "suggestions=" +
                                    suggestions.joinToString()
                            )

                            add(
                                SpellingIssue(
                                    start =
                                        chunk.start +
                                            localStart,
                                    end =
                                        chunk.start +
                                            localEnd,
                                    suggestions =
                                        suggestions,
                                    isGrammarError =
                                        grammarError
                                )
                            )
                        } else {
                            Log.d(
                                TAG,
                                "✅ ПРАВИЛЬНО/НЕ ОТМЕЧЕНО: «$word» | " +
                                    "inDictionary=$inDictionary | " +
                                    "looksLikeTypo=$looksLikeTypo | " +
                                    "grammarError=$grammarError"
                            )
                        }
                    }
                }
            }
                .distinctBy {
                    Triple(
                        it.start,
                        it.end,
                        it.isGrammarError
                    )
                }
                .sortedBy {
                    it.start
                }

        Log.d(
            TAG,
            "ИТОГ: ошибок=${issues.size}"
        )

        Log.d(
            TAG,
            "========== КОНЕЦ ПРОВЕРКИ =========="
        )

        callback(issues)
    }

    override fun onGetSuggestions(
        results: Array<out SuggestionsInfo>?
    ) = Unit

    fun cancelPending() {
        /*
         * Не вызываем cancel() просто при каждом старте LaunchedEffect.
         * Особенно важно не слать лишний CANCEL пока session ещё
         * находится в WAIT_CONNECTION.
         */
        if (!requestWasSent) {
            pendingCallback = null
            pendingChunks = emptyList()
            return
        }

        requestWasSent = false
        pendingCallback = null
        pendingChunks = emptyList()

        session?.cancel()

        Log.d(
            TAG,
            "Предыдущий spell-check запрос отменён"
        )
    }

    fun logState(
        prefix: String
    ) {
        Log.d(
            TAG,
            "$prefix | session=${session != null} | " +
                "isSessionDisconnected=${session?.isSessionDisconnected} | " +
                "enabled=" +
                if (
                    Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.S
                ) {
                    manager.isSpellCheckerEnabled
                } else {
                    "unknown"
                }
        )
    }

    override fun close() {
        pendingCallback = null
        pendingChunks = emptyList()
        requestWasSent = false

        session?.close()

        Log.d(
            TAG,
            "SpellCheckerSession закрыта вместе с экраном"
        )
    }

    private fun splitIntoChunks(
        text: String
    ): List<TextChunk> {
        if (
            text.length <=
            MAX_CHUNK_LENGTH
        ) {
            return listOf(
                TextChunk(
                    start = 0,
                    text = text
                )
            )
        }

        val result =
            mutableListOf<TextChunk>()

        var start = 0

        while (
            start <
            text.length
        ) {
            var end =
                minOf(
                    start +
                        MAX_CHUNK_LENGTH,
                    text.length
                )

            if (
                end <
                text.length
            ) {
                val minimumBreak =
                    start +
                        MAX_CHUNK_LENGTH /
                        2

                var breakAt =
                    end

                while (
                    breakAt >
                    minimumBreak &&
                    !text[
                        breakAt -
                            1
                    ].isWhitespace()
                ) {
                    breakAt -= 1
                }

                if (
                    breakAt >
                    minimumBreak
                ) {
                    end =
                        breakAt
                }
            }

            result +=
                TextChunk(
                    start =
                        start,
                    text =
                        text.substring(
                            start,
                            end
                        )
                )

            start =
                end
        }

        return result
    }

    private data class TextChunk(
        val start: Int,
        val text: String
    )

    private companion object {
        const val TAG =
            "NovelForgeSpellCheck"

        const val MAX_SUGGESTIONS =
            5

        const val MAX_CHUNK_LENGTH =
            1_200
    }
}

@Composable
fun rememberSpellCheckState(
    text: String,
    enabled: Boolean = true,
    debounceMillis: Long = 650L
): SpellCheckState {

    val context =
        LocalContext.current.applicationContext

    val checker =
        remember(context) {
            AndroidSpellChecker(context)
        }

    var state by
        remember(checker) {
            mutableStateOf(
                SpellCheckState(
                    isAvailable =
                        checker.isAvailable
                )
            )
        }

    DisposableEffect(checker) {
        onDispose {
            checker.close()
        }
    }

    LaunchedEffect(
        text,
        enabled,
        debounceMillis,
        checker
    ) {
        if (
            !enabled ||
            text.isBlank()
        ) {
            checker.cancelPending()

            state =
                SpellCheckState(
                    issues =
                        emptyList(),
                    isAvailable =
                        checker.isAvailable,
                    isChecking =
                        false
                )

            return@LaunchedEffect
        }

        if (
            !checker.isAvailable
        ) {
            state =
                SpellCheckState(
                    issues =
                        emptyList(),
                    isAvailable =
                        false,
                    isChecking =
                        false
                )

            return@LaunchedEffect
        }

        /*
         * Сначала ждём окончания ввода.
         * Только ПОСЛЕ debounce отменяем старый реально отправленный запрос.
         */
        delay(
            debounceMillis
        )

        checker.cancelPending()

        checker.logState(
            "Перед отправкой"
        )

        state =
            SpellCheckState(
                issues =
                    emptyList(),
                isAvailable =
                    true,
                isChecking =
                    true
            )

        val issues =
            withTimeoutOrNull(
                SPELL_CHECK_TIMEOUT_MILLIS
            ) {
                suspendCancellableCoroutine<
                    List<SpellingIssue>
                > { continuation ->

                    checker.check(
                        text
                    ) { result ->
                        if (
                            continuation
                                .isActive
                        ) {
                            continuation.resume(
                                result
                            )
                        }
                    }

                    continuation
                        .invokeOnCancellation {
                            /*
                             * При отмене LaunchedEffect новый эффект
                             * после debounce сам отменит старый запрос.
                             */
                        }
                }
            }

        if (
            issues == null
        ) {
            checker.logState(
                "После таймаута"
            )

            Log.w(
                "NovelForgeSpellCheck",
                "Callback не пришёл за " +
                    "${SPELL_CHECK_TIMEOUT_MILLIS} мс"
            )

            state =
                SpellCheckState(
                    issues =
                        emptyList(),
                    isAvailable =
                        true,
                    isChecking =
                        false
                )

            return@LaunchedEffect
        }

        state =
            SpellCheckState(
                issues =
                    issues,
                isAvailable =
                    true,
                isChecking =
                    false
            )
    }

    return state
}

private const val SPELL_CHECK_TIMEOUT_MILLIS =
    8_000L
