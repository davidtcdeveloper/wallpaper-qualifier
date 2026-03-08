<primary_directive>
You write tests that are THOROUGH and CLEAR. Your tests DOCUMENT behavior. You test HAPPY PATHS, ERROR CASES, and EDGE CASES. You use MOCKS appropriately.
</primary_directive>

# Testing: Strategies & Patterns

## Overview

The Wallpaper Qualifier must be testable and well-tested. This document covers testing strategies, mocking patterns, and concurrency-specific concerns.

---

## Testing Strategy

<rule_1 priority="HIGHEST">
**TEST PYRAMID**: Focus test effort where it matters most.
- **Unit Tests** (60%): Individual functions/classes in isolation
- **Integration Tests** (30%): Module interactions (config + workflow)
- **End-to-End Tests** (10%): Full workflow with mock LLM

This prevents test bloat while ensuring coverage of critical paths.

**MUST**:
- ✓ Unit tests: Fast (<50ms), many (100+)
- ✓ Integration tests: Medium speed (<500ms), moderate count (20+)
- ✓ E2E tests: Slower but comprehensive (5+)
- ✓ Mock external services (LLM)
- ✓ Use real file I/O in integration tests

**Example - Good**:
```kotlin
// Unit Test: Fast, focused
class ImageOptimizerTest {
    @Test
    fun shouldCompressJpegBelowTargetSize() {
        val optimizer = ImageOptimizer()
        val image = createTestImage(width = 4000, height = 3000)
        
        val result = optimizer.optimizeToJpeg(image, maxFileSize = 2_097_152)
        
        assertTrue(result.sizeBytes < 2_097_152)
        assertTrue(result.qualityScore > 0.85) // Not too aggressive
    }
    
    @Test
    fun shouldPreservePrimaryColorsAfterOptimization() {
        val optimizer = ImageOptimizer()
        val colors = listOf(0xFF0000, 0x00FF00, 0x0000FF) // R, G, B
        val image = createTestImage(dominantColors = colors)
        
        val result = optimizer.optimizeToJpeg(image, maxFileSize = 2_097_152)
        
        assertTrue(result.dominantColors.size >= 3)
        // Colors preserved
    }
}

// Integration Test: Tests config + processing interaction
class ConfiguredImageProcessingTest {
    @Test
    suspend fun shouldProcessBatchAccordingToConfig() {
        val config = Config(
            folders = FoldersConfig(
                samples = testSamplesDir,
                candidates = testCandidatesDir,
                output = testOutputDir
            ),
            processing = ProcessingConfig(
                maxParallelTasks = 2,
                outputFormat = ImageFormat.PNG
            )
        )
        
        val processor = ImageProcessor(config)
        val results = processor.processBatch(listOf(image1, image2))
        
        assertEquals(2, results.size)
        results.forEach { assertTrue(it.path.endsWith(".png")) }
    }
}

// E2E Test: Full workflow
class WallpaperQualifierE2ETest {
    @Test
    suspend fun shouldRunCompleteWorkflow() {
        val mockLLM = MockLLMClient()
        mockLLM.respondWith(testAnalyses)
        
        val qualifier = WallpaperQualifier(
            configLoader = DefaultConfigLoader(),
            llmClient = mockLLM,
            imageProcessor = DefaultImageProcessor()
        )
        
        val result = qualifier.run(testConfigPath)
        
        assertEquals(Result.Success, result)
        assertEquals(3, result.qualified)
    }
}
```

**Example - Avoid**:
```kotlin
// Anti-pattern: All integration tests (slow, brittle)
class WallpaperQualifierTest {
    @Test
    suspend fun testEverythingWithRealLLM() {
        // Calls actual LMStudio, takes 5 minutes
        // Flaky: network issues cause failure
        // Too broad: can't isolate the problem
    }
}
```
</rule_1>

---

<rule_2 priority="HIGHEST">
**HAPPY PATH + ERROR CASES**: Test success and failure paths equally.
- Happy path: normal operation, expected inputs
- Error cases: invalid input, missing files, LLM failure
- Edge cases: empty lists, zero values, boundary conditions

**MUST**:
- ✓ At least one happy path test per function
- ✓ At least one error case test per error type
- ✓ Document what error case each test covers
- ✓ Use descriptive test names (describe the scenario)

**Example - Good**:
```kotlin
class ImageDecoderTest {
    // Happy paths
    @Test
    suspend fun shouldDecodeValidJpegImage() { /* ... */ }
    
    @Test
    suspend fun shouldDetectMultipleImageFormats() { /* ... */ }
    
    // Error cases
    @Test
    suspend fun shouldFailGracefullyOnCorruptedImage() {
        val result = decoder.decode(corruptedImagePath)
        assertIs<ImageDecodeResult.Failed>(result)
        assertTrue(result.message.contains("corrupted"))
    }
    
    @Test
    suspend fun shouldFailOnUnsupportedFormat() {
        val result = decoder.decode(bitmapImagePath)
        assertIs<ImageDecodeResult.Failed>(result)
        assertTrue(result.message.contains("unsupported"))
    }
    
    @Test
    suspend fun shouldFailOnMissingFile() {
        val result = decoder.decode(nonexistentPath)
        assertIs<ImageDecodeResult.Failed>(result)
        assertTrue(result.message.contains("not found"))
    }
    
    // Edge cases
    @Test
    suspend fun shouldHandleEmptyFile() {
        val emptyFile = File.createTempFile()
        emptyFile.writeText("")
        
        val result = decoder.decode(emptyFile.path)
        assertIs<ImageDecodeResult.Failed>(result)
    }
    
    @Test
    suspend fun shouldHandleVeryLargeImage() {
        val largeImage = createTestImage(width = 10000, height = 8000)
        
        val result = decoder.decode(largeImage)
        assertIs<ImageDecodeResult.Success>(result)
    }
}
```

**Example - Avoid**:
```kotlin
// Anti-pattern: Only test happy path
class ImageDecoderTest {
    @Test
    suspend fun shouldDecodeImage() {
        val result = decoder.decode(validImagePath)
        assertTrue(result is ImageDecodeResult.Success)
    }
    // What about errors? Never tested, breaks in production!
}
```
</rule_2>

---

<rule_3 priority="HIGH">
**MOCKING LLM CLIENT**: LLM is external service. Mock it in tests.
- Deterministic responses (same input → same output)
- No network calls during tests
- Test error scenarios (timeout, rate limit)
- Track request history for verification

**MUST**:
- ✓ Inject LLM client as dependency
- ✓ Create mock that returns predictable responses
- ✓ Mock both success and failure scenarios
- ✓ Verify requests sent (spy pattern)
- ✓ Never make real HTTP calls in tests

**Example - Good**:
```kotlin
class MockLLMClient : LLMClient {
    private val requestHistory = mutableListOf<Request>()
    private var responseQueue = LinkedList<Response>()
    
    fun respondWith(response: Response) {
        responseQueue.add(response)
    }
    
    fun respondWithError(error: Exception) {
        responseQueue.add(ErrorResponse(error))
    }
    
    override suspend fun analyze(request: Request): Response {
        requestHistory.add(request)
        return responseQueue.poll() ?: throw Exception("No more responses mocked")
    }
    
    fun verifyRequestsSent(expectedCount: Int) {
        assertEquals(expectedCount, requestHistory.size)
    }
    
    fun getRequestAt(index: Int): Request = requestHistory[index]
}

// Usage in tests
@Test
suspend fun shouldSendAnalysisRequestForEachSample() {
    val mockLLM = MockLLMClient()
    mockLLM.respondWith(analysisResponse1)
    mockLLM.respondWith(analysisResponse2)
    
    val analyzer = SampleAnalyzer(mockLLM)
    analyzer.analyze(listOf(sample1, sample2))
    
    mockLLM.verifyRequestsSent(2)
    assertTrue(mockLLM.getRequestAt(0).prompt.contains("color"))
}

@Test
suspend fun shouldHandleLLMTimeoutGracefully() {
    val mockLLM = MockLLMClient()
    mockLLM.respondWithError(TimeoutException("LLM took too long"))
    
    val analyzer = SampleAnalyzer(mockLLM)
    val result = analyzer.analyze(listOf(sample1))
    
    assertIs<Result.Failure>(result)
    assertTrue(result.message.contains("timeout"))
}
```

**Example - Avoid**:
```kotlin
// Anti-pattern: Real HTTP calls in tests
@Test
suspend fun shouldAnalyzeSample() {
    val analyzer = SampleAnalyzer(RealLLMClient())
    val result = analyzer.analyze(sample1)
    // Test takes 30 seconds, flaky, depends on network
}
```
</rule_3>

---

<rule_4 priority="HIGH">
**TESTING CONCURRENT CODE**: Coroutines add complexity. Test explicitly.
- Sequential vs. parallel behavior
- Cancellation handling
- Timeout behavior
- Race conditions (use deterministic mocks)

**MUST**:
- ✓ Use `runTest { }` for coroutine tests (scoped)
- ✓ Test sequential LLM queue ordering
- ✓ Test cancellation doesn't leak resources
- ✓ Test timeout behavior
- ✓ Use deterministic mocks (no random delays)

**Example - Good**:
```kotlin
class LLMQueueTest {
    @Test
    fun shouldProcessRequestsSequentially() = runTest {
        val mockLLM = RecordingMockLLM()
        val queue = LLMRequestQueue(mockLLM)
        
        // Start queue processor
        launch {
            queue.process()
        }
        
        // Enqueue multiple requests
        queue.enqueue(Request("image1"))
        queue.enqueue(Request("image2"))
        queue.enqueue(Request("image3"))
        
        // Verify they're processed in order (sequential, not parallel)
        val order = mockLLM.getProcessingOrder()
        assertEquals(listOf("image1", "image2", "image3"), order)
    }
    
    @Test
    fun shouldHandleCancellation() = runTest {
        val mockLLM = SlowMockLLM() // 1 second per request
        val queue = LLMRequestQueue(mockLLM)
        
        val job = launch {
            queue.process()
        }
        
        queue.enqueue(Request("image1"))
        queue.enqueue(Request("image2"))
        
        delay(500) // Cancel mid-way
        job.cancel()
        
        assertTrue(queue.isIdleAfterCancellation())
    }
}

class ImageProcessorTest {
    @Test
    suspend fun shouldProcessImagesInParallel() {
        val processor = ImageProcessor(maxParallel = 4)
        val images = (1..10).map { createTestImage(id = it) }
        
        val startTime = System.currentTimeMillis()
        processor.process(images)
        val duration = System.currentTimeMillis() - startTime
        
        // 10 images with 4 parallel = 3 batches
        // If sequential: ~3 seconds, if parallel: ~1 second
        assertTrue(duration < 1500) // Parallel processing is fast
    }
}
```

**Example - Avoid**:
```kotlin
// Anti-pattern: Not testing concurrency at all
@Test
fun shouldProcessImages() {
    val results = processor.process(images)
    assertEquals(images.size, results.size)
    // Doesn't verify parallel behavior
}

// Anti-pattern: Non-deterministic mocks
class NonDeterministicMockLLM : LLMClient {
    override suspend fun analyze(request: Request): Response {
        delay(Random.nextLong(1000)) // Random delay!
        return response // Can't predict ordering
    }
}
```
</rule_4>

---

<rule_5 priority="MEDIUM">
**FIXTURES & BUILDERS**: Reduce test setup boilerplate.
- Create builder functions for common objects
- Reusable fixtures for files, directories
- Minimize duplication in test data

**MUST**:
- ✓ Factory functions for test objects
- ✓ Builders for complex objects
- ✓ Temporary directories/files cleaned up
- ✓ Sensible defaults for test data

**Example - Good**:
```kotlin
// Test fixtures
fun createTestImage(
    width: Int = 1024,
    height: Int = 768,
    format: ImageFormat = ImageFormat.PNG,
    dominantColors: List<Int> = listOf(0x333333)
): Image = Image(
    width = width,
    height = height,
    format = format,
    data = ByteArray(width * height * 3),
    metadata = ImageMetadata(dominantColors = dominantColors)
)

fun createTestConfig(
    samplesDir: String = "/tmp/samples",
    candidatesDir: String = "/tmp/candidates",
    outputDir: String = "/tmp/output"
): Config = Config(
    folders = FoldersConfig(
        samples = samplesDir,
        candidates = candidatesDir,
        output = outputDir
    ),
    llm = LLMConfig(model = "test-model"),
    processing = ProcessingConfig()
)

fun createTemporaryTestDirs(): TestDirs {
    val temp = createTempDir()
    return TestDirs(
        samples = File(temp, "samples").apply { mkdirs() },
        candidates = File(temp, "candidates").apply { mkdirs() },
        output = File(temp, "output").apply { mkdirs() },
        cleanup = { temp.deleteRecursively() }
    )
}

// Usage in tests
@Test
suspend fun shouldProcessImages() {
    val dirs = createTemporaryTestDirs()
    try {
        val config = createTestConfig(
            samplesDir = dirs.samples.path,
            candidatesDir = dirs.candidates.path,
            outputDir = dirs.output.path
        )
        
        // Test code...
    } finally {
        dirs.cleanup()
    }
}
```

**Example - Avoid**:
```kotlin
// Anti-pattern: Duplicated setup in every test
@Test
suspend fun test1() {
    val config = Config(
        folders = FoldersConfig(
            samples = "/tmp/samples",
            candidates = "/tmp/candidates",
            output = "/tmp/output"
        ),
        // ... 10 more lines
    )
    // Duplicated in every test!
}
```
</rule_5>

---

## Checklist

Before committing code:

☐ **Coverage**: Happy path + error cases tested
☐ **Mock LLM**: No real HTTP calls in tests
☐ **Concurrency**: Sequential queue behavior verified
☐ **Fixtures**: Reusable test data builders
☐ **Cleanup**: Temporary files/directories deleted
☐ **Naming**: Test names describe the scenario
☐ **Speed**: Unit tests < 50ms, integration < 500ms
☐ **All Tests Pass**: `./gradlew test` runs green

---

**Last Updated**: 2026-03-08
