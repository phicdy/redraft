package com.phicdy.redraft.shared.ui.main

import com.phicdy.redraft.shared.llm.LlmError
import com.phicdy.redraft.shared.llm.ProviderConfig
import com.phicdy.redraft.shared.llm.ProviderKind
import com.phicdy.redraft.shared.llm.WritingAssistant
import com.phicdy.redraft.shared.llm.prompt.Politeness
import com.phicdy.redraft.shared.llm.prompt.Prompts
import com.phicdy.redraft.shared.llm.prompt.WritingContext
import com.phicdy.redraft.shared.llm.prompt.WritingPurpose
import com.phicdy.redraft.shared.model.InputMode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    private val dispatcher: TestDispatcher = StandardTestDispatcher()
    private val client = FakeLlmClient()
    private val config = ProviderConfig(
        id = "fake",
        kind = ProviderKind.OpenAiResponses,
        baseUrl = "https://example.invalid",
        defaultModel = "fake-model",
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): MainViewModel = MainViewModel(WritingAssistant(client = client, config = config))

    @Test
    fun japaneseInputIsTranslated(): TestResult = runTest(dispatcher) {
        val viewModel = createViewModel()
        viewModel.onInputChange(JAPANESE)
        viewModel.submit()

        assertEquals(ResultState.Loading(InputMode.Translate), viewModel.uiState.value.result)
        assertTrue(viewModel.uiState.value.isRunning)
        assertFalse(viewModel.uiState.value.canRun)

        advanceUntilIdle()

        val result = assertIs<ResultState.Translated>(viewModel.uiState.value.result)
        assertEquals(JAPANESE, result.input)
        assertEquals(client.translation, result.result)
        assertEquals(client.usage, result.usage)
        assertEquals(listOf(FakeLlmClient.TRANSLATION), client.requestedOutputs)
        assertEquals(JAPANESE, client.requests.single().input)
    }

    @Test
    fun englishInputIsProofread(): TestResult = runTest(dispatcher) {
        val viewModel = createViewModel()
        viewModel.onInputChange(ENGLISH)
        viewModel.submit()
        advanceUntilIdle()

        val result = assertIs<ResultState.Proofread>(viewModel.uiState.value.result)
        assertEquals(ENGLISH, result.input)
        assertEquals(client.proofread, result.result)
        assertEquals(listOf(FakeLlmClient.PROOFREAD), client.requestedOutputs)
    }

    @Test
    fun manualModeWinsOverDetectedMode(): TestResult = runTest(dispatcher) {
        val viewModel = createViewModel()
        viewModel.onInputChange(JAPANESE)
        viewModel.onModeSelect(InputMode.Proofread)
        assertEquals(InputMode.Proofread, viewModel.uiState.value.mode)

        viewModel.submit()
        advanceUntilIdle()

        assertIs<ResultState.Proofread>(viewModel.uiState.value.result)
        assertEquals(listOf(FakeLlmClient.PROOFREAD), client.requestedOutputs)
    }

    @Test
    fun manualModeAllowsUndecidedInput(): TestResult = runTest(dispatcher) {
        val viewModel = createViewModel()
        viewModel.onInputChange("2026")
        assertFalse(viewModel.uiState.value.canRun)

        viewModel.onModeSelect(InputMode.Translate)
        assertTrue(viewModel.uiState.value.canRun)
    }

    @Test
    fun writingContextIsSentWithRequest(): TestResult = runTest(dispatcher) {
        val viewModel = createViewModel()
        viewModel.onPurposeSelect(WritingPurpose.Email)
        viewModel.onPolitenessSelect(Politeness.Formal)
        assertEquals(
            WritingContext(purpose = WritingPurpose.Email, politeness = Politeness.Formal),
            viewModel.uiState.value.writingContext,
        )

        viewModel.onInputChange(ENGLISH)
        viewModel.submit()
        advanceUntilIdle()

        val expected = Prompts.proofread(
            input = ENGLISH,
            context = WritingContext(purpose = WritingPurpose.Email, politeness = Politeness.Formal),
            model = config.defaultModel,
        )
        assertEquals(expected, client.requests.single())
    }

    @Test
    fun blankInputIsNotSent(): TestResult = runTest(dispatcher) {
        val viewModel = createViewModel()
        viewModel.onInputChange("   \n")
        viewModel.onModeSelect(InputMode.Translate)
        assertFalse(viewModel.uiState.value.canRun)

        viewModel.submit()
        advanceUntilIdle()

        assertEquals(ResultState.Idle, viewModel.uiState.value.result)
        assertTrue(client.requests.isEmpty())
    }

    @Test
    fun undecidedModeIsNotSent(): TestResult = runTest(dispatcher) {
        val viewModel = createViewModel()
        viewModel.onInputChange("123 !?")
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(ResultState.Idle, viewModel.uiState.value.result)
        assertTrue(client.requests.isEmpty())
    }

    @Test
    fun llmErrorIsShownAndInputIsKept(): TestResult = runTest(dispatcher) {
        client.llmError = LlmError.MissingApiKey
        val viewModel = createViewModel()
        viewModel.onInputChange(JAPANESE)
        viewModel.submit()
        advanceUntilIdle()

        val result = assertIs<ResultState.Failed>(viewModel.uiState.value.result)
        assertEquals("APIキーが設定されていません。設定画面から登録してください", result.message)
        assertEquals(JAPANESE, viewModel.uiState.value.input)
        assertTrue(viewModel.uiState.value.canRun)
    }

    @Test
    fun rateLimitMessageIncludesRetryAfter(): TestResult = runTest(dispatcher) {
        client.llmError = LlmError.RateLimited(retryAfterSeconds = 30, message = null)
        val viewModel = createViewModel()
        viewModel.onInputChange(ENGLISH)
        viewModel.submit()
        advanceUntilIdle()

        val result = assertIs<ResultState.Failed>(viewModel.uiState.value.result)
        assertTrue("30秒" in result.message, result.message)
    }

    @Test
    fun unexpectedExceptionIsShownAsError(): TestResult = runTest(dispatcher) {
        client.unexpectedFailure = IllegalStateException("boom")
        val viewModel = createViewModel()
        viewModel.onInputChange(ENGLISH)
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(ResultState.Failed(UNEXPECTED_ERROR_MESSAGE), viewModel.uiState.value.result)
    }

    @Test
    fun submitWhileRunningIsIgnored(): TestResult = runTest(dispatcher) {
        client.gate = CompletableDeferred()
        val viewModel = createViewModel()
        viewModel.onInputChange(JAPANESE)
        viewModel.submit()
        runCurrent()
        viewModel.submit()
        runCurrent()

        assertEquals(1, client.requests.size)
        client.gate?.complete(Unit)
        advanceUntilIdle()
        assertIs<ResultState.Translated>(viewModel.uiState.value.result)
    }

    @Test
    fun cancelReturnsToIdleAndDropsLateResult(): TestResult = runTest(dispatcher) {
        client.gate = CompletableDeferred()
        val viewModel = createViewModel()
        viewModel.onInputChange(JAPANESE)
        viewModel.submit()
        runCurrent()

        viewModel.cancel()
        assertEquals(ResultState.Idle, viewModel.uiState.value.result)

        client.gate?.complete(Unit)
        advanceUntilIdle()
        assertEquals(ResultState.Idle, viewModel.uiState.value.result)
        assertEquals(JAPANESE, viewModel.uiState.value.input)
    }

    @Test
    fun editingInputWhileRunningCancelsRequest(): TestResult = runTest(dispatcher) {
        client.gate = CompletableDeferred()
        val viewModel = createViewModel()
        viewModel.onInputChange(JAPANESE)
        viewModel.submit()
        runCurrent()

        viewModel.onInputChange("$JAPANESE よろしくお願いします。")
        assertEquals(ResultState.Idle, viewModel.uiState.value.result)

        client.gate?.complete(Unit)
        advanceUntilIdle()
        assertEquals(ResultState.Idle, viewModel.uiState.value.result)
    }

    @Test
    fun changingContextWhileRunningCancelsRequest(): TestResult = runTest(dispatcher) {
        client.gate = CompletableDeferred()
        val viewModel = createViewModel()
        viewModel.onInputChange(ENGLISH)
        viewModel.submit()
        runCurrent()

        viewModel.onPolitenessSelect(Politeness.Formal)
        assertEquals(ResultState.Idle, viewModel.uiState.value.result)

        client.gate?.complete(Unit)
        advanceUntilIdle()
        assertEquals(ResultState.Idle, viewModel.uiState.value.result)
    }

    @Test
    fun editingAfterResultKeepsResultAndRerunReplacesIt(): TestResult = runTest(dispatcher) {
        val viewModel = createViewModel()
        viewModel.onInputChange(ENGLISH)
        viewModel.submit()
        advanceUntilIdle()
        val first = assertIs<ResultState.Proofread>(viewModel.uiState.value.result)

        val edited = "I look forward to seeing you."
        viewModel.onInputChange(edited)
        assertEquals(first, viewModel.uiState.value.result)

        viewModel.submit()
        assertEquals(ResultState.Loading(InputMode.Proofread), viewModel.uiState.value.result)
        advanceUntilIdle()

        val second = assertIs<ResultState.Proofread>(viewModel.uiState.value.result)
        assertEquals(edited, second.input)
        assertEquals(2, client.requests.size)
    }

    private companion object {
        const val JAPANESE = "来週の会議を金曜日に変更できますか？"
        const val ENGLISH = "I look forward to see you."
    }
}
