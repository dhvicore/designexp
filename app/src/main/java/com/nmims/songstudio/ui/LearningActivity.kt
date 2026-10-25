package com.nmims.songstudio.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nmims.songstudio.data.Topic
import java.util.Locale
import kotlin.random.Random

data class PracticeBlank(val prompt: String, val answer: String)
data class PracticeLesson(val blanks: List<PracticeBlank>, val orderedLines: List<String>, val explanation: String)

/** Ports the prototype's blanks, same-unit formula choices and line-order practice. */
object PracticeActivities {
    fun normalize(value: String) = value.lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{N}]"), "")
    fun accepts(blank: PracticeBlank, answer: String): Boolean {
        val alternatives = when (blank.answer) {
            "zero" -> setOf("zero", "0")
            "one" -> setOf("one", "1")
            "plus" -> setOf("plus", "+")
            else -> setOf(blank.answer)
        }
        return alternatives.any { it == answer.trim() || normalize(it).isNotEmpty() && normalize(it) == normalize(answer) }
    }
    fun lesson(topic: Topic): PracticeLesson = when (topic.id) {
        0 -> PracticeLesson(listOf(
            PracticeBlank("The function must be ___ on the closed interval [a,b].", "continuous"),
            PracticeBlank("It must be ___ at every point inside (a,b).", "differentiable"),
            PracticeBlank("At some interior c, the derivative is ___.", "zero")
        ), listOf("Check continuity on [a,b].", "Check differentiability on (a,b).", "Check that f(a) = f(b).", "Conclude: some c in (a,b) has f′(c) = 0."),
            "All three conditions are required. The conclusion guarantees at least one interior point with zero slope, not exactly one.")
        17 -> PracticeLesson(listOf(
            PracticeBlank("For real m,n > 0, the beta integral runs from zero to ___.", "one"),
            PracticeBlank("B(m,n) = B(n,m) expresses ___.", "symmetry"),
            PracticeBlank("B(m,n) = Γ(m)Γ(n) / Γ(m ___ n).", "plus")
        ), listOf("Take real m,n > 0.", "Form x^(m−1) × (1−x)^(n−1).", "Integrate with respect to x from 0 to 1.", "Obtain B(m,n) = Γ(m)Γ(n) / Γ(m+n)."),
            "For real positive m,n, B(m,n) is symmetric. Both powers contain minus one; the denominator is gamma of the sum, Γ(m+n).")
        else -> {
            val lines = topic.lyrics["en"].orEmpty().lines().filter { it.isNotBlank() && !it.startsWith("[") }
            val keywords = setOf("derivative", "integral", "zero", "slope", "gradient", "limit", "constant", "partial", "factorial", "gamma", "beta", "continuous", "divergence", "curl")
            val blanks = lines.mapNotNull { line ->
                val words = Regex("[A-Za-z]+").findAll(line).toList()
                val word = words.lastOrNull { it.value.lowercase() in keywords }
                    ?: words.lastOrNull { it.value.length >= 6 }
                word?.let { PracticeBlank(line.replaceRange(it.range, "___"), it.value) }
            }.take(3)
            PracticeLesson(blanks, lines.distinct().take(4), topic.concept)
        }
    }
    fun formulaOptions(topic: Topic, all: List<Topic>): List<String> {
        val others = all.filter { it.id != topic.id && it.formula != topic.formula }
        val wrong = (others.filter { it.unit == topic.unit } + others.filter { it.unit != topic.unit })
            .map { it.formula }.distinct().take(3)
        return (wrong + topic.formula).shuffled(Random(topic.id + 517))
    }
    fun correctOrder(topicId: Int, picked: List<String>, expected: List<String>): Boolean =
        if (topicId == 0) picked.size == expected.size && picked.lastOrNull() == expected.lastOrNull() && picked.dropLast(1).toSet() == expected.dropLast(1).toSet()
        else picked == expected

    fun mixedLines(lines: List<String>, seed: Int): List<String> {
        val shuffled = lines.shuffled(Random(seed + 7919))
        return if (shuffled == lines && lines.size > 1) lines.drop(1) + lines.take(1) else shuffled
    }
}

@Composable
fun LearningActivity(topic: Topic, all: List<Topic>) {
    val lesson = remember(topic.id) { PracticeActivities.lesson(topic) }
    val options = remember(topic.id) { PracticeActivities.formulaOptions(topic, all) }
    var attempt by rememberSaveable(topic.id) { mutableIntStateOf(0) }
    var answers by rememberSaveable(topic.id, attempt) { mutableStateOf(List(lesson.blanks.size) { "" }) }
    var formula by rememberSaveable(topic.id, attempt) { mutableStateOf("") }
    var selectedOrder by rememberSaveable(topic.id, attempt) { mutableStateOf(emptyList<String>()) }
    var checked by rememberSaveable(topic.id, attempt) { mutableStateOf(false) }
    val mixed = remember(topic.id, attempt) { PracticeActivities.mixedLines(lesson.orderedLines, topic.id + attempt) }
    val blankMarks = answers.zip(lesson.blanks).map { (a,b) -> PracticeActivities.accepts(b, a) }
    val blanksCorrect = blankMarks.isNotEmpty() && blankMarks.all { it }
    val formulaCorrect = formula == topic.formula
    val orderCorrect = PracticeActivities.correctOrder(topic.id, selectedOrder, lesson.orderedLines)
    val score = listOf(blanksCorrect, formulaCorrect, orderCorrect).count { it }
    Text("Listen. Recall.\nTry it yourself.", style = MaterialTheme.typography.headlineLarge)
    Text("${topic.unit} / ${topic.title}")
    Text("Practice in English · adapted from your prototype’s Activity section.", style = MaterialTheme.typography.bodySmall)
    Panel(Lavender) {
        Text("1 / Fill in the blanks", style = MaterialTheme.typography.titleLarge)
        lesson.blanks.forEachIndexed { index, blank ->
            Text(blank.prompt)
            OutlinedTextField(value = answers[index], onValueChange = { value ->
                answers = answers.toMutableList().also { it[index] = value }
            }, enabled = !checked, singleLine = true, label = { Text("Answer ${index + 1}") }, modifier = Modifier.fillMaxWidth())
            if (checked) Text(if (blankMarks[index]) "Correct" else "Answer: ${blank.answer}",
                color = if (blankMarks[index]) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
        }
    }
    Panel(Lavender) {
        Text(if (topic.subject == "Chemistry") "2 / Choose the key terms" else "2 / Choose the formula", style = MaterialTheme.typography.titleLarge)
        Text(if (topic.subject == "Chemistry") "Which key terms belong to ${topic.title}?" else "Which formula belongs to ${topic.title}?")
        options.forEach { option ->
            OutlinedButton(onClick = { formula = option }, enabled = !checked, modifier = Modifier.fillMaxWidth()) {
                Text("${if (formula == option) "●" else "○"} $option")
            }
        }
        if (checked) Text(if (formulaCorrect) "Correct!" else "The answer is: ${topic.formula}")
    }
    Panel(Lavender) {
        Text("3 / Rebuild the lesson", style = MaterialTheme.typography.titleLarge)
        Text(if (topic.id == 0) "Put all three condition checks before the conclusion. The checks can be in any sequence." else "Tap the statements in the expected lesson order.")
        mixed.filter { it !in selectedOrder }.forEach { line ->
            OutlinedButton(onClick = { selectedOrder = selectedOrder + line }, enabled = !checked, modifier = Modifier.fillMaxWidth()) { Text(line) }
        }
        selectedOrder.forEachIndexed { i, line -> Text("${i + 1}. $line") }
        TextButton(onClick = { selectedOrder = emptyList() }, enabled = !checked && selectedOrder.isNotEmpty()) { Text("Clear order") }
        if (checked) {
            Text(if (orderCorrect) "Correct order!" else "Expected order:")
            if (!orderCorrect) lesson.orderedLines.forEachIndexed { i, line -> Text("${i + 1}. $line") }
            if (topic.id == 0) Text("The three conditions may be checked in any sequence; the conclusion follows after all three.", style = MaterialTheme.typography.bodySmall)
        }
    }
    Button(onClick = { checked = true }, enabled = !checked && answers.all { it.isNotBlank() } && formula.isNotBlank() && selectedOrder.size == lesson.orderedLines.size,
        modifier = Modifier.fillMaxWidth()) { Text("Check my answers") }
    if (checked) {
        Panel(Lime) {
            Text("$score / 3 activities correct", style = MaterialTheme.typography.titleLarge)
            Text(lesson.explanation)
            Text(if (score == 3) "Well done. Return later and try recalling it again." else "Review the feedback, listen again, then retry.")
        }
        OutlinedButton(onClick = { attempt++ }, modifier = Modifier.fillMaxWidth()) { Text("Try again") }
    }
}
