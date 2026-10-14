package ru.it.novelforge.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import ru.it.novelforge.domain.model.TextFormatMark
import ru.it.novelforge.domain.model.TextMarkType

fun buildRichAnnotatedString(
    text: String,
    marks: List<TextFormatMark>,
    spellingIssues: List<SpellingIssue> = emptyList()
): AnnotatedString {
    if (text.isEmpty()) return AnnotatedString(text)

    val ranges = marks.mapNotNull { mark ->
        val start = mark.start.coerceIn(0, text.length)
        val end = mark.end.coerceIn(0, text.length)
        if (end <= start) return@mapNotNull null

        val style = when (mark.type) {
            TextMarkType.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
            TextMarkType.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
            TextMarkType.STRIKETHROUGH -> SpanStyle(textDecoration = TextDecoration.LineThrough)
            TextMarkType.COLOR -> {
                val color = mark.value?.let(::parseHexColor) ?: return@mapNotNull null
                SpanStyle(color = color)
            }
        }
        AnnotatedString.Range(style, start, end)
    }

    val spellingRanges = spellingIssues.mapNotNull { issue ->
        val start = issue.start.coerceIn(0, text.length)
        val end = issue.end.coerceIn(0, text.length)
        if (end <= start) return@mapNotNull null

        AnnotatedString.Range(
            item = SpanStyle(
                color = SPELLING_ERROR_COLOR,
                textDecoration = TextDecoration.Underline
            ),
            start = start,
            end = end
        )
    }

    return AnnotatedString(
        text = text,
        spanStyles = ranges + spellingRanges
    )
}

@Composable
fun RichText(
    text: String,
    marks: List<TextFormatMark>,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    Text(
        text = buildRichAnnotatedString(text, marks),
        modifier = modifier,
        style = style,
        maxLines = maxLines,
        overflow = overflow
    )
}

private val SPELLING_ERROR_COLOR = Color(0xFFD32F2F)

fun parseHexColor(value: String): Color? = runCatching {
    val normalized = value.trim().let {
        when {
            it.matches(Regex("#[0-9a-fA-F]{6}")) -> it
            it.matches(Regex("[0-9a-fA-F]{6}")) -> "#$it"
            it.matches(Regex("#[0-9a-fA-F]{8}")) -> it
            it.matches(Regex("[0-9a-fA-F]{8}")) -> "#$it"
            else -> error("Invalid color")
        }
    }
    Color(android.graphics.Color.parseColor(normalized))
}.getOrNull()

fun toggleTextMark(
    marks: List<TextFormatMark>,
    start: Int,
    end: Int,
    type: TextMarkType
): List<TextFormatMark> {
    if (end <= start || type == TextMarkType.COLOR) return marks
    return if (isFullyCovered(marks, start, end, type)) {
        removeTextMark(marks, start, end, type)
    } else {
        normalizeMarks(marks + TextFormatMark(start, end, type))
    }
}

fun applyTextColor(
    marks: List<TextFormatMark>,
    start: Int,
    end: Int,
    colorHex: String?
): List<TextFormatMark> {
    if (end <= start) return marks
    val withoutOldColor = removeTextMark(marks, start, end, TextMarkType.COLOR)
    if (colorHex == null) return normalizeMarks(withoutOldColor)
    return normalizeMarks(withoutOldColor + TextFormatMark(start, end, TextMarkType.COLOR, colorHex.uppercase()))
}

fun clearTextFormatting(
    marks: List<TextFormatMark>,
    start: Int,
    end: Int
): List<TextFormatMark> {
    if (end <= start) return marks
    return TextMarkType.entries.fold(marks) { result, type ->
        removeTextMark(result, start, end, type)
    }.let(::normalizeMarks)
}

fun adjustMarksAfterTextChange(
    oldText: String,
    newText: String,
    marks: List<TextFormatMark>
): List<TextFormatMark> {
    if (oldText == newText || marks.isEmpty()) return marks

    var prefix = 0
    val prefixLimit = minOf(oldText.length, newText.length)
    while (prefix < prefixLimit && oldText[prefix] == newText[prefix]) prefix++

    var suffix = 0
    while (
        suffix < oldText.length - prefix &&
        suffix < newText.length - prefix &&
        oldText[oldText.lastIndex - suffix] == newText[newText.lastIndex - suffix]
    ) {
        suffix++
    }

    val oldEnd = oldText.length - suffix
    val newEnd = newText.length - suffix
    val delta = newText.length - oldText.length
    val insertionOnly = prefix == oldEnd

    val adjusted = buildList {
        marks.forEach { mark ->
            val updated = when {
                insertionOnly && mark.start < prefix && mark.end > prefix ->
                    mark.copy(end = mark.end + delta)

                insertionOnly && mark.start >= prefix ->
                    mark.copy(start = mark.start + delta, end = mark.end + delta)

                mark.end <= prefix -> mark
                mark.start >= oldEnd -> mark.copy(start = mark.start + delta, end = mark.end + delta)
                else -> {
                    val newStart = if (mark.start < prefix) mark.start else prefix
                    val newMarkEnd = if (mark.end > oldEnd) mark.end + delta else newEnd
                    mark.copy(start = newStart, end = newMarkEnd)
                }
            }

            val safeStart = updated.start.coerceIn(0, newText.length)
            val safeEnd = updated.end.coerceIn(0, newText.length)
            if (safeEnd > safeStart) add(updated.copy(start = safeStart, end = safeEnd))
        }
    }

    return normalizeMarks(adjusted)
}

private fun removeTextMark(
    marks: List<TextFormatMark>,
    start: Int,
    end: Int,
    type: TextMarkType
): List<TextFormatMark> = buildList {
    marks.forEach { mark ->
        if (mark.type != type || mark.end <= start || mark.start >= end) {
            add(mark)
            return@forEach
        }
        if (mark.start < start) add(mark.copy(end = start))
        if (mark.end > end) add(mark.copy(start = end))
    }
}

private fun isFullyCovered(
    marks: List<TextFormatMark>,
    start: Int,
    end: Int,
    type: TextMarkType
): Boolean {
    val ranges = marks
        .asSequence()
        .filter { it.type == type && it.end > start && it.start < end }
        .map { maxOf(it.start, start) to minOf(it.end, end) }
        .sortedBy { it.first }
        .toList()

    var coveredUntil = start
    for ((rangeStart, rangeEnd) in ranges) {
        if (rangeStart > coveredUntil) return false
        coveredUntil = maxOf(coveredUntil, rangeEnd)
        if (coveredUntil >= end) return true
    }
    return false
}

private fun normalizeMarks(marks: List<TextFormatMark>): List<TextFormatMark> {
    if (marks.isEmpty()) return emptyList()
    val grouped = marks
        .filter { it.end > it.start }
        .groupBy { it.type to it.value }

    return grouped.values.flatMap { group ->
        val sorted = group.sortedBy { it.start }
        if (sorted.isEmpty()) return@flatMap emptyList()
        val merged = mutableListOf<TextFormatMark>()
        var current = sorted.first()
        for (next in sorted.drop(1)) {
            if (next.start <= current.end) {
                current = current.copy(end = maxOf(current.end, next.end))
            } else {
                merged += current
                current = next
            }
        }
        merged += current
        merged
    }.sortedWith(compareBy<TextFormatMark> { it.start }.thenBy { it.end }.thenBy { it.type.name })
}
