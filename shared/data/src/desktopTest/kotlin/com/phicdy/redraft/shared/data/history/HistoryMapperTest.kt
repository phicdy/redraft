package com.phicdy.redraft.shared.data.history

import com.phicdy.redraft.shared.data.history.db.AggregationRows
import com.phicdy.redraft.shared.data.history.db.FindingCountRow
import com.phicdy.redraft.shared.data.history.db.FindingKind
import com.phicdy.redraft.shared.data.history.db.FindingSeverity
import com.phicdy.redraft.shared.data.history.db.ModeCountRow
import com.phicdy.redraft.shared.model.ErrorCategory
import com.phicdy.redraft.shared.model.InputMode
import com.phicdy.redraft.shared.model.Severity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class HistoryMapperTest {
    private fun entry(result: HistoryResult): HistoryEntry = HistoryEntry(
        id = "entry-1",
        deviceId = "device-1",
        createdAt = Instant.fromEpochMilliseconds(1_700_000_000_000),
        updatedAt = Instant.fromEpochMilliseconds(1_700_000_000_000),
        input = "input",
        context = HistorySamples.context,
        model = "test-model",
        usage = HistorySamples.usage,
        result = result,
    )

    @Test
    fun storedValuesAreStable() {
        // These strings are in users' databases; changing one breaks existing history.
        assertEquals("translate", HistoryMapper.modeValue(InputMode.Translate))
        assertEquals("proofread", HistoryMapper.modeValue(InputMode.Proofread))
        assertEquals(FindingSeverity.ERROR, HistoryMapper.severityValue(Severity.Error))
        assertEquals(FindingSeverity.IMPROVEMENT, HistoryMapper.severityValue(Severity.Improvement))
        assertEquals("spelling_punctuation", HistoryMapper.categoryValue(ErrorCategory.SpellingPunctuation))
        assertEquals(listOf("email", "chat", "document"), HistoryPurpose.entries.map { it.storageValue })
        assertEquals(listOf("casual", "neutral", "formal"), HistoryPoliteness.entries.map { it.storageValue })
    }

    @Test
    fun entityRoundTripKeepsEverything() {
        for (result in listOf(HistorySamples.translation, HistorySamples.proofread)) {
            val original = entry(result)
            val entity = HistoryMapper.toEntity(original)

            assertEquals(HistoryMapper.RESULT_SCHEMA_VERSION, entity.resultSchemaVersion)
            assertNull(entity.deletedAt)
            assertEquals(original, HistoryMapper.toEntry(entity))
        }
    }

    @Test
    fun resultTextIsTheMainOutput() {
        assertEquals("Could we move the meeting to Friday?", HistoryMapper.toEntity(entry(HistorySamples.translation)).resultText)
        assertEquals("I look forward to seeing you.", HistoryMapper.toEntity(entry(HistorySamples.proofread)).resultText)
    }

    @Test
    fun proofreadIssuesBecomeFindings() {
        var next = 0
        val findings = HistoryMapper.toFindings(entry(HistorySamples.proofread)) { "f${next++}" }

        assertEquals(listOf("f0", "f1"), findings.map { it.id })
        val first = findings[0]
        assertEquals(FindingKind.ISSUE, first.kind)
        assertEquals("entry-1", first.entryId)
        assertEquals(0, first.position)
        assertEquals("verb_form", first.category)
        assertEquals("error", first.severity)
        assertEquals("to see", first.text)
        assertEquals("to seeing", first.detail)
        assertNull(first.expressionType)
        assertEquals(1_700_000_000_000, first.createdAt)
        assertEquals(listOf("naturalness", "improvement", "I'm looking"), findings[1].let { listOf(it.category, it.severity, it.detail) })
    }

    @Test
    fun translationGrammarPointsAndExpressionsBecomeFindings() {
        var next = 0
        val findings = HistoryMapper.toFindings(entry(HistorySamples.translation)) { "f${next++}" }

        assertEquals(
            listOf(FindingKind.GRAMMAR_POINT, FindingKind.GRAMMAR_POINT, FindingKind.EXPRESSION),
            findings.map { it.kind },
        )
        assertEquals(listOf("tense", "preposition", null), findings.map { it.category })
        assertEquals(listOf(0, 1, 0), findings.map { it.position })
        assertEquals(listOf("未来の予定", "曜日の前置詞", "move the meeting"), findings.map { it.text })
        val expression = findings[2]
        assertEquals("phrase", expression.expressionType)
        assertEquals("会議を移動する", expression.detail)
        assertNull(expression.severity)
    }

    @Test
    fun unreadableEntriesAreSkipped() {
        val entity = HistoryMapper.toEntity(entry(HistorySamples.translation))

        assertNull(HistoryMapper.toEntry(entity.copy(mode = "summarize")))
        assertNull(HistoryMapper.toEntry(entity.copy(resultJson = "{\"translation\": 1}")))
    }

    @Test
    fun unknownContextValuesFallBackToDefaults() {
        val entity = HistoryMapper.toEntity(entry(HistorySamples.proofread)).copy(purpose = "tweet", politeness = "rude")

        assertEquals(
            HistoryContext(purpose = HistoryPurpose.Chat, politeness = HistoryPoliteness.Neutral),
            HistoryMapper.toEntry(entity)?.context,
        )
    }

    @Test
    fun containsPatternEscapesWildcards() {
        assertNull(HistoryMapper.containsPattern(null))
        assertNull(HistoryMapper.containsPattern("  "))
        assertEquals("%meeting%", HistoryMapper.containsPattern(" meeting "))
        assertEquals("%100\\%\\_a\\\\b%", HistoryMapper.containsPattern("100%_a\\b"))
    }

    @Test
    fun aggregationSkipsValuesUnknownToThisVersion() {
        val rows = AggregationRows(
            modeCounts = listOf(ModeCountRow(mode = "proofread", count = 2), ModeCountRow(mode = "summarize", count = 5)),
            issueCounts = listOf(
                FindingCountRow(category = "tense", severity = "error", count = 1),
                FindingCountRow(category = "future_category", severity = "error", count = 4),
                FindingCountRow(category = "tense", severity = "critical", count = 3),
            ),
            grammarPointCounts = listOf(FindingCountRow(category = "future_category", severity = null, count = 1)),
            expressions = emptyList(),
        )

        val aggregation = HistoryMapper.toAggregation(rows)

        assertEquals(mapOf(InputMode.Translate to 0, InputMode.Proofread to 2), aggregation.entryCounts)
        assertEquals(listOf(IssueCount(ErrorCategory.Tense, Severity.Error, 1)), aggregation.issueCounts)
        assertEquals(emptyList(), aggregation.grammarPointCounts)
    }
}
