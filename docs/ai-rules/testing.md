<primary_directive>
You write tests that are THOROUGH and CLEAR. Your tests DOCUMENT behavior. You test HAPPY PATHS, ERROR CASES, and EDGE CASES. You use MOCKS appropriately.
</primary_directive>

# Testing: Strategies & Patterns

## Overview

The Wallpaper Qualifier prioritized **Integration Testing** over the traditional test pyramid. We focus on validating complete paths across multiple classes and ensuring that module interfaces meet their contracts. This allows internal implementation details to evolve without breaking the test suite.

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
// Module E2E Test: Tests the entire image pipeline path
class ImageModuleE2ETest {
    @Test
    suspend fun shouldProcessSampleBatchFromDisk() {
        val processor = ImageModule.create() // Real implementation
        val testFiles = createTestImageFiles("samples")
        
        val results = processor.processBatch(testFiles.map { it.path })
        
        // Validates the entire path: Detection -> Decoding -> Optimization -> Encoding
        assertEquals(testFiles.size, results.filterIsInstance<Success>().size)
        results.forEach { result ->
            assertTrue(File(result.outputPath).exists())
            assertTrue(ImageDecoder.verifyFormat(result.outputPath, ImageFormat.PNG))
        }
    }
}

// Integration Test: Cross-module workflow
class WorkflowIntegrationTest {
    @Test
    suspend fun shouldAnalyzeSamplesAndGenerateProfile() {
        val mockLLM = MockLLMClient()
        val workflow = WorkflowOrchestrator(
            imageModule = ImageModule.create(),
            llmModule = LLMModule.create(mockLLM),
            profileModule = ProfileModule.create()
        )
        
        val result = workflow.runSampleAnalysis(testConfig)
        
        // Validates that the modules work together across the full analysis path
        assertIs<ProfileResult.Success>(result)
        assertNotNull(result.profile.aesthetics)
    }
}
```

**Example - Avoid**:
```kotlin
// Anti-pattern: Over-mocking internal helpers
class ImageProcessorTest {
    @Test
    fun testInternalHelper() {
        val mockDecoder = mock<InternalDecoder>()
        val mockOptimizer = mock<InternalOptimizer>()
        val processor = ImageProcessor(mockDecoder, mockOptimizer)
        
        processor.doWork()
        
        verify(mockDecoder).call() // Brittle: breaks if internal structure changes
    }
}
```
</rule_1>

---

<rule_2 priority="HIGHEST">
**HAPPY PATH + ERROR CASES**: Test success and failure paths across the entire module.
- Happy path: Successful completion of a multi-step workflow.
- Error cases: How the module interface handles failures in internal steps (missing files, LLM timeouts).
- Edge cases: Empty inputs, massive batches, boundary value thresholds.

**MUST**:
- ✓ Ensure tests cover the "Surface Area" of the module.
- ✓ Verify that errors from deep within a path are correctly propagated to the module interface.
- ✓ Use descriptive names that reflect the business scenario.

**Example - Good**:
```kotlin
class ConfigModuleIntegrationTest {
    @Test
    fun shouldReturnAggregatedErrorsForInvalidJSON() {
        val invalidJson = "{ \"llm\": { \"timeout\": -1 } }"
        val result = ConfigLoader.loadContent(invalidJson)
        
        // Tests the path from raw string parsing through validation
        assertIs<ConfigResult.Failure>(result)
        assertTrue(result.errors.any { it.contains("timeout") })
    }
}
```
</rule_2>

---

<rule_3 priority="HIGH">
**MOCKING EXTERNAL SERVICES**: Mock only at the absolute system boundary (the LLM).
- The LLM Client is an external dependency and should be mocked to ensure determinism.
- Use mocks to simulate network failures, rate limits, and malformed LLM responses.

**MUST**:
- ✓ Inject the LLM client into the LLM Module.
- ✓ Mock success, timeout, and API error scenarios.
- ✓ Ensure the LLM Module interface handles these mocks correctly.

**Example - Good**:
```kotlin
@Test
suspend fun llmModuleShouldHandleTimeoutGracefully() {
    val mockApi = MockLLMApi()
    mockApi.delayResponse(5000) // Trigger timeout
    
    val module = LLMModule.create(mockApi, timeout = 1000)
    val result = module.analyze(testImage)
    
    assertIs<LLMResult.Error.Timeout>(result)
}
```
</rule_3>

---

<rule_4 priority="HIGH">
**CONCURRENCY & PATH VALIDATION**: Ensure parallel paths don't conflict.
- Test that parallel image processing paths don't have side effects on shared state.
- Test that the sequential LLM queue remains sequential even when bombarded with parallel requests.

**MUST**:
- ✓ Use `runTest` for coroutine lifecycle management.
- ✓ Validate the *order* of operations in the LLM queue.
- ✓ Validate the *throughput* of the Image module to confirm parallelism.

**Example - Good**:
```kotlin
@Test
fun llmQueueMustMaintainOrder() = runTest {
    val mockLlm = RecordingMockLLM()
    val queue = LLMRequestQueue(mockLlm)
    
    // Fire many parallel enqueues
    val jobs = (1..5).map { i ->
        launch { queue.enqueue(Request("Request $i")) }
    }
    jobs.joinAll()
    
    // Verify results processed sequentially and in received order
    assertEquals(listOf("Request 1", "Request 2", "Request 3", "Request 4", "Request 5"), mockLlm.received)
}
```
</rule_4>

---

<rule_5 priority="MEDIUM">
**TEST DATA & FIXTURES**: Use realistic test data to exercise real paths.
- Maintain a set of "Golden Master" images for testing.
- Use builders to create complex state without exposing internal fields.

**MUST**:
- ✓ Use `createTempDir()` for file-based tests.
- ✓ Provide clear cleanup logic.
- ✓ Keep test resources (images) small enough for quick execution but representative.
```
