package com.phicdy.redraft.shared.ui.report

import com.phicdy.redraft.shared.data.report.ReportWeek
import com.phicdy.redraft.shared.llm.LlmError
import com.phicdy.redraft.shared.ui.history.FakeHistoryRepository
import com.phicdy.redraft.shared.ui.main.FakeLlmClient
import com.phicdy.redraft.shared.ui.main.UNEXPECTED_ERROR_MESSAGE
import com.phicdy.redraft.shared.ui.main.toUserMessage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ReportViewModelTest {
    private val dispatcher: TestDispatcher = StandardTestDispatcher()
    private val history = FakeHistoryRepository()
    private val reports = FakeWeeklyReportRepository()
    private val llm = FakeLlmClient()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(): ReportViewModel = ReportViewModel(reports, generator(history, reports, llm))

    private fun hasEntries(week: ReportWeek) {
        history.aggregations[week.startInstant(TOKYO)] = aggregation(translate = 1, proofread = 2, articleErrors = 3)
    }

    @Test
    fun startsWithLastWeekAndListsReports(): TestResult = runTest(dispatcher) {
        hasEntries(LAST_WEEK)
        val existing = generator(history, reports, llm).generate(ReportTarget.LastWeek)
        val viewModel = viewModel()
        assertTrue(viewModel.uiState.value.isLoading)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(ReportTarget.LastWeek, state.target)
        assertEquals(LAST_WEEK, state.lastWeek)
        assertEquals(THIS_WEEK, state.thisWeek)
        assertEquals(listOf((existing as GenerationResult.Generated).report), state.reports)
    }

    @Test
    fun generatingShowsProgressThenOpensTheReport(): TestResult = runTest(dispatcher) {
        hasEntries(LAST_WEEK)
        val gate = CompletableDeferred<Unit>()
        llm.gate = gate
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onGenerate()
        advanceUntilIdle()
        assertEquals(GenerationState.Running(LAST_WEEK), viewModel.uiState.value.generation)

        // A second tap while running is ignored.
        viewModel.onGenerate()
        gate.complete(Unit)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(GenerationState.Idle, state.generation)
        val detail = assertIs<ReportDetailState.Loaded>(state.detail)
        assertEquals(LAST_WEEK, detail.report.week)
        assertEquals(listOf(detail.report), state.reports)
        assertEquals(1, llm.requests.size)
    }

    @Test
    fun thisWeekCanBeChosen(): TestResult = runTest(dispatcher) {
        hasEntries(THIS_WEEK)
        val viewModel = viewModel()

        viewModel.onTargetSelect(ReportTarget.ThisWeek)
        viewModel.onGenerate()
        advanceUntilIdle()

        val detail = assertIs<ReportDetailState.Loaded>(viewModel.uiState.value.detail)
        assertEquals(THIS_WEEK, detail.report.week)
    }

    @Test
    fun emptyWeekShowsAMessageWithoutCallingTheLlm(): TestResult = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onGenerate()
        advanceUntilIdle()

        assertEquals(GenerationState.NoEntries(LAST_WEEK), viewModel.uiState.value.generation)
        assertNull(viewModel.uiState.value.detail)
        assertTrue(llm.requests.isEmpty())

        viewModel.onTargetSelect(ReportTarget.ThisWeek)
        assertEquals(GenerationState.Idle, viewModel.uiState.value.generation)
    }

    @Test
    fun llmErrorIsShownInJapanese(): TestResult = runTest(dispatcher) {
        hasEntries(LAST_WEEK)
        llm.llmError = LlmError.MissingApiKey
        val viewModel = viewModel()

        viewModel.onGenerate()
        advanceUntilIdle()

        assertEquals(GenerationState.Failed(LlmError.MissingApiKey.toUserMessage()), viewModel.uiState.value.generation)
        assertTrue(reports.reports.value.isEmpty())
    }

    @Test
    fun unexpectedErrorDoesNotCrash(): TestResult = runTest(dispatcher) {
        hasEntries(LAST_WEEK)
        reports.saveFailure = IllegalStateException("disk full")
        val viewModel = viewModel()

        viewModel.onGenerate()
        advanceUntilIdle()

        assertEquals(GenerationState.Failed(UNEXPECTED_ERROR_MESSAGE), viewModel.uiState.value.generation)
    }

    @Test
    fun opensAndClosesASavedReport(): TestResult = runTest(dispatcher) {
        hasEntries(LAST_WEEK)
        val saved = (generator(history, reports, llm).generate(ReportTarget.LastWeek) as GenerationResult.Generated).report
        val viewModel = viewModel()

        viewModel.onReportSelect(saved.id)
        assertEquals(ReportDetailState.Loading, viewModel.uiState.value.detail)
        advanceUntilIdle()
        assertEquals(ReportDetailState.Loaded(saved), viewModel.uiState.value.detail)

        viewModel.onDetailClose()
        assertNull(viewModel.uiState.value.detail)

        viewModel.onReportSelect("missing")
        advanceUntilIdle()
        assertEquals(ReportDetailState.NotFound, viewModel.uiState.value.detail)
    }
}
