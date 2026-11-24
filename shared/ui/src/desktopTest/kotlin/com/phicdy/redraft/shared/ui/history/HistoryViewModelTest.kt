package com.phicdy.redraft.shared.ui.history

import com.phicdy.redraft.shared.data.history.HistoryContext
import com.phicdy.redraft.shared.data.history.HistoryEntry
import com.phicdy.redraft.shared.data.history.HistoryPoliteness
import com.phicdy.redraft.shared.data.history.HistoryPurpose
import com.phicdy.redraft.shared.data.history.HistoryResult
import com.phicdy.redraft.shared.data.history.HistoryUsage
import com.phicdy.redraft.shared.model.ProofreadResult
import com.phicdy.redraft.shared.model.TranslationResult
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
class HistoryViewModelTest {
    private val dispatcher: TestDispatcher = StandardTestDispatcher()
    private val repository = FakeHistoryRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun save(input: String, result: HistoryResult): HistoryEntry = repository.save(
        input = input,
        context = HistoryContext(purpose = HistoryPurpose.Chat, politeness = HistoryPoliteness.Neutral),
        result = result,
        model = "test-model",
        usage = HistoryUsage(inputTokens = 1, outputTokens = 2),
    )

    @Test
    fun listsEntriesNewestFirst(): TestResult = runTest(dispatcher) {
        val older = save(JAPANESE, TRANSLATION)
        val newer = save(ENGLISH, PROOFREAD)
        val viewModel = HistoryViewModel(repository)
        assertTrue(viewModel.uiState.value.isLoading)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(listOf(newer.id, older.id), state.entries.map { it.id })
    }

    @Test
    fun listFollowsRepositoryChanges(): TestResult = runTest(dispatcher) {
        val viewModel = HistoryViewModel(repository)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.entries.isEmpty())

        val saved = save(ENGLISH, PROOFREAD)
        advanceUntilIdle()

        assertEquals(listOf(saved.id), viewModel.uiState.value.entries.map { it.id })
    }

    @Test
    fun searchQueriesTheRepository(): TestResult = runTest(dispatcher) {
        save(JAPANESE, TRANSLATION)
        val english = save(ENGLISH, PROOFREAD)
        val viewModel = HistoryViewModel(repository)
        advanceUntilIdle()

        viewModel.onQueryChange("look")
        // The text field shows the typed text immediately.
        assertEquals("look", viewModel.uiState.value.query)
        advanceUntilIdle()

        assertEquals(listOf(english.id), viewModel.uiState.value.entries.map { it.id })
        assertEquals<List<String?>>(listOf("", "look"), repository.queries)

        viewModel.onQueryChange("no such text")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.entries.isEmpty())
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun selectingAnEntryOpensItsDetail(): TestResult = runTest(dispatcher) {
        val entry = save(JAPANESE, TRANSLATION)
        val viewModel = HistoryViewModel(repository)
        advanceUntilIdle()

        viewModel.onEntrySelect(entry.id)
        assertEquals(HistoryDetailState.Loading, viewModel.uiState.value.detail)
        advanceUntilIdle()
        assertEquals(HistoryDetailState.Loaded(entry), viewModel.uiState.value.detail)

        viewModel.onDetailClose()
        assertNull(viewModel.uiState.value.detail)
    }

    @Test
    fun missingEntryIsNotFound(): TestResult = runTest(dispatcher) {
        val viewModel = HistoryViewModel(repository)
        viewModel.onEntrySelect("missing")
        advanceUntilIdle()

        assertEquals(HistoryDetailState.NotFound, viewModel.uiState.value.detail)
    }

    @Test
    fun deleteAsksForConfirmationFirst(): TestResult = runTest(dispatcher) {
        val entry = save(ENGLISH, PROOFREAD)
        val viewModel = HistoryViewModel(repository)
        viewModel.onEntrySelect(entry.id)
        advanceUntilIdle()

        viewModel.onDeleteRequest()
        assertTrue(assertIs<HistoryDetailState.Loaded>(viewModel.uiState.value.detail).isConfirmingDelete)

        viewModel.onDeleteDismiss()
        advanceUntilIdle()
        assertFalse(assertIs<HistoryDetailState.Loaded>(viewModel.uiState.value.detail).isConfirmingDelete)
        assertTrue(repository.deletedIds.isEmpty())
    }

    @Test
    fun confirmedDeleteClosesTheDetailAndUpdatesTheList(): TestResult = runTest(dispatcher) {
        val kept = save(JAPANESE, TRANSLATION)
        val deleted = save(ENGLISH, PROOFREAD)
        val viewModel = HistoryViewModel(repository)
        viewModel.onEntrySelect(deleted.id)
        advanceUntilIdle()

        viewModel.onDeleteRequest()
        viewModel.onDeleteConfirm()
        advanceUntilIdle()

        assertEquals(listOf(deleted.id), repository.deletedIds)
        assertNull(viewModel.uiState.value.detail)
        assertEquals(listOf(kept.id), viewModel.uiState.value.entries.map { it.id })
    }

    @Test
    fun failedDeleteKeepsTheDetail(): TestResult = runTest(dispatcher) {
        repository.deleteFailure = IllegalStateException("disk error")
        val entry = save(ENGLISH, PROOFREAD)
        val viewModel = HistoryViewModel(repository)
        viewModel.onEntrySelect(entry.id)
        advanceUntilIdle()

        viewModel.onDeleteRequest()
        viewModel.onDeleteConfirm()
        advanceUntilIdle()

        val detail = assertIs<HistoryDetailState.Loaded>(viewModel.uiState.value.detail)
        assertTrue(detail.deleteFailed)
        assertFalse(detail.isConfirmingDelete)
        assertEquals(entry, detail.entry)
    }

    private companion object {
        const val JAPANESE = "来週の会議を金曜日に変更できますか？"
        const val ENGLISH = "I look forward to see you."

        val TRANSLATION = HistoryResult.Translation(
            TranslationResult(
                translation = "Could we move the meeting to Friday?",
                alternatives = emptyList(),
                grammarPoints = emptyList(),
                expressions = emptyList(),
            ),
        )
        val PROOFREAD = HistoryResult.Proofread(
            ProofreadResult(
                correctedText = "I look forward to seeing you.",
                issues = emptyList(),
                naturalAlternatives = emptyList(),
                overallComment = "",
            ),
        )
    }
}
