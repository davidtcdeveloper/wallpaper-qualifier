<primary_directive>
You write tests that are THOROUGH and CLEAR. Your tests DOCUMENT behavior. You test HAPPY PATHS, ERROR CASES, and EDGE CASES. You use MOCKS appropriately.
</primary_directive>

# Testing: Strategies & Patterns

## Overview

The Wallpaper Qualifier prioritizes **Integration Testing** using the **Kotest** framework. We focus on validating complete paths across multiple classes and ensuring that module interfaces meet their contracts. This allows internal implementation details to evolve without breaking the test suite.

---

## Testing Framework & Style

<rule_0 priority="HIGHEST">
**KOTEST & BEHAVIOR SPECS**: Use Kotest for expressive, readable tests.
- Prefer **FunSpec** or **StringSpec** for clear test descriptions.
- Use Kotest assertions (`shouldBe`, `shouldNotBe`, `shouldThrow`) for readable checks.
- Avoid JUnit annotations (`@Test`) or assertions (`assertEquals`).

**MUST**:
- ✓ Write tests using Kotest DSL (`test("should do something") { ... }`).
- ✓ Use `io.kotest.matchers` for assertions.
- ✓ Use `io.kotest.assertions.throwables` for exception testing.

**Example - Good**:
```kotlin
class ImageModuleE2ETest : FunSpec({
    test("should process a batch of sample images from disk") {
        val processor = ImageModule.create()
        val testFiles = createTestImageFiles("samples")
        
        val results = processor.processBatch(testFiles.map { it.path })
        
        // Fluent assertions
        results.filterIsInstance<Success>().size shouldBe testFiles.size
        
        results.forEach { result ->
            File(result.outputPath).exists() shouldBe true
            ImageDecoder.verifyFormat(result.outputPath, ImageFormat.PNG) shouldBe true
        }
    }
})
```

**Example - Avoid**:
```kotlin
// Anti-pattern: JUnit style
class ImageModuleTest {
    @Test
    fun testProcessing() {
        assertEquals(5, result.size) // Less readable
    }
}
```
</rule_0>

---

## Testing Strategy

<rule_1 priority="HIGHEST">
**INTEGRATION-FIRST APPROACH**: Focus on the interaction between components.
- **Module E2E Tests** (70%): Validate the entire package/module interface (e.g., full Image Module pipeline).
- **System Integration** (20%): Cross-module workflows (e.g., Config loading → Image processing).
- **Critical Unit Tests** (10%): Reserved only for complex pure logic (e.g., math-heavy score calculations).

**MUST**:
- ✓ Test paths across multiple classes to ensure structural integrity.
- ✓ Validate module boundaries: if you call `ImageModule.process()`, it should work regardless of internal helper classes.
- ✓ Use real file I/O for integration tests where practical.
- ✓ Mock only external boundary services (like the LLM API).
- ✓ Focus on "What" the module does, not "How" it does it.

**Example - Good**:
```kotlin
class WorkflowIntegrationTest : StringSpec({
    "should analyze samples and generate a profile" {
        val mockLLM = MockLLMClient()
        val workflow = WorkflowOrchestrator(
            imageModule = ImageModule.create(),
            llmModule = LLMModule.create(mockLLM),
            profileModule = ProfileModule.create()
        )
        
        val result = workflow.runSampleAnalysis(testConfig)
        
        result.shouldBeInstanceOf<ProfileResult.Success>()
        result.profile.aesthetics.shouldNotBeNull()
    }
})
```
</rule_1>

---

<rule_2 priority="HIGHEST">
**HAPPY PATH + ERROR CASES**: Test success and failure paths across the entire module.
- Happy path: Successful completion of a multi-step workflow.
- Error cases: How the module interface handles failures in internal steps (missing files, LLM timeouts).

**MUST**:
- ✓ Ensure tests cover the "Surface Area" of the module.
- ✓ Verify that errors from deep within a path are correctly propagated to the module interface.

**Example - Good**:
```kotlin
class ConfigModuleIntegrationTest : FunSpec({
    test("should return aggregated errors for invalid JSON") {
        val invalidJson = "{ \"llm\": { \"timeout\": -1 } }"
        val result = ConfigLoader.loadContent(invalidJson)
        
        result.shouldBeInstanceOf<ConfigResult.Failure>()
        result.errors.forAny { it shouldContain "timeout" }
    }
})
```
</rule_2>

---

## Mocking vs. Faking

<rule_3 priority="HIGHEST">
**FAKES OVER MOCKS**: Prefer hand-written Fakes for internal interfaces.
- Use **Fakes** (real, simplified implementations) for internal module boundaries.
- Use **MockK** *only* for external libraries, final classes, or objects that cannot be interfaced/faked.
- Fakes ensure that tests exercise realistic state transitions and are less brittle than behavior-based mocks.

**MUST**:
- ✓ Define an interface for internal services and provide a `FakeService` for tests.
- ✓ Use `MockK` only for system/library boundaries (e.g., Ktor HttpClient, macOS system APIs).
- ✓ Ensure Fakes maintain internal state consistency (e.g., a `FakeLLMQueue` should actually queue items).

**Example - Good (Fake)**:
```kotlin
// Interface in commonMain
interface LLMClient {
    suspend fun analyze(request: Request): Response
}

// Fake in commonTest
class FakeLLMClient : LLMClient {
    private val responses = mutableListOf<Response>()
    val recordedRequests = mutableListOf<Request>()

    fun addResponse(response: Response) { responses.add(response) }

    override suspend fun analyze(request: Request): Response {
        recordedRequests.add(request)
        return responses.removeFirstOrNull() ?: throw IllegalStateException("No responses left")
    }
}

// Usage in Test
test("should analyze images through the module path") {
    val fakeLlm = FakeLLMClient()
    fakeLlm.addResponse(AnalysisResponse(score = 0.9f))
    
    val module = LLMModule.create(fakeLlm)
    module.analyze(testImage)
    
    fakeLlm.recordedRequests.size shouldBe 1
}
```

**Example - Avoid (Over-mocking)**:
```kotlin
// Anti-pattern: Mocking internal logic with MockK
val mockProcessor = mockk<InternalProcessor>()
coEvery { mockProcessor.process(any()) } returns Success() // Brittle behavior mocking
```
</rule_3>

---

<rule_4 priority="HIGH">
**CONCURRENCY & PATH VALIDATION**: Ensure parallel paths don't conflict.
- Test that parallel image processing paths don't have side effects on shared state.
- Test that the sequential LLM queue remains sequential even when bombarded with parallel requests.

**MUST**:
- ✓ Use Kotest's coroutine support (enabled by default in recent versions).
- ✓ Validate the *order* of operations in the LLM queue.

**Example - Good**:
```kotlin
test("LLM queue must maintain order under load") {
    val mockLlm = RecordingMockLLM()
    val queue = LLMRequestQueue(mockLlm)
    
    // Fire many parallel enqueues
    val jobs = (1..5).map { i ->
        launch { queue.enqueue(Request("Request $i")) }
    }
    jobs.joinAll()
    
    // Verify results processed sequentially and in received order
    mockLlm.received shouldBe listOf("Request 1", "Request 2", "Request 3", "Request 4", "Request 5")
}
```
</rule_4>

---

<rule_5 priority="MEDIUM">
**TEST DATA & FIXTURES**: Use realistic test data to exercise real paths.
- Maintain a set of "Golden Master" images for testing.
- Use Kotest's `tempdir()` or `tempfile()` extensions.

**MUST**:
- ✓ Use Kotest's lifecycle listeners (beforeTest, afterTest) if manual cleanup is needed.
- ✓ Prefer `tempdir()` fixture which auto-cleans.
```
