<primary_directive>
You write production-grade Kotlin code for multiplatform targets. Your code leverages Kotlin's idioms and coroutine patterns. ALWAYS prefer coroutines over callbacks. ALWAYS make thread-safe concurrent patterns explicit.
</primary_directive>

# Kotlin Multiplatform: KMP-Specific Patterns

## Overview

Kotlin Multiplatform (KMP) lets us write business logic once and compile for macOS native code. This document covers idioms and patterns specific to our KMP + Kotlin/Native setup.

---

## Coroutine Best Practices

<rule_1 priority="HIGHEST">
**USE SUSPENDING FUNCTIONS FOR ASYNC WORK**: Never use callbacks or `Future`/`Promise` patterns. Always use `suspend fun`.
- Coroutines integrate seamlessly with Kotlin
- Structured concurrency prevents resource leaks
- Stack traces remain readable

**MUST**:
- ✓ Mark async operations as `suspend fun`
- ✓ Use `launch` for fire-and-forget (rare in this project)
- ✓ Use `async` for parallel work that needs results
- ✓ Always use a scope: `runBlocking` (CLI), `viewModelScope` (future UI)

**Example - Good**:
```kotlin
// Suspending function - clear contract
suspend fun analyzeSampleImage(path: String): SampleAnalysis {
    val image = imageDecoder.decode(path) // Suspends if I/O
    val analysis = llmClient.analyze(image) // Suspends on LLM request
    return analysis
}

// Using suspending function
suspend fun analyzeSamples(paths: List<String>): List<SampleAnalysis> {
    return paths.map { path -> analyzeSampleImage(path) }
}
```

**Example - Avoid**:
```kotlin
// Anti-pattern: Callback hell
fun analyzeSampleImage(path: String, callback: (SampleAnalysis) -> Unit) {
    imageDecoder.decode(path) { image ->
        llmClient.analyze(image) { analysis ->
            callback(analysis) // Pyramid of doom
        }
    }
}
```
</rule_1>

---

<rule_2 priority="HIGHEST">
**STRUCTURED CONCURRENCY**: Use coroutine scopes properly to avoid leaks and ensure cancellation.
- CLI app uses `runBlocking` at entry point
- Use `coroutineScope { }` for parallel tasks that must complete
- Each scope creates a job that tracks children
- Cancellation cascades through scope hierarchy

**MUST**:
- ✓ Use `runBlocking` only at application entry point
- ✓ Use `coroutineScope { }` for grouping parallel operations
- ✓ Never create `GlobalScope` coroutines
- ✓ Propagate `CancellationException` up the stack

**Example - Good**:
```kotlin
fun main(args: Array<String>) = runBlocking {
    // Top-level scope for CLI app
    try {
        val result = runWallpaperQualifier(configPath)
        println("Success: $result")
    } catch (e: CancellationException) {
        println("Operation cancelled by user")
    } catch (e: Exception) {
        println("Error: ${e.message}")
    }
}

suspend fun runWallpaperQualifier(configPath: String) = coroutineScope {
    // Parallel analysis of samples
    val samples = samplePaths.map { path ->
        async { analyzeSampleImage(path) }
    }
    val results = samples.awaitAll()
    return@coroutineScope results
}
```

**Example - Avoid**:
```kotlin
// Anti-pattern: Fire-and-forget, no tracking
GlobalScope.launch {
    analyzeSamples() // Leaks, can't cancel
}

// Anti-pattern: Direct thread creation
Thread {
    // Hard to cancel, no cancellation support
    analyzeSamples()
}.start()
```
</rule_2>

---

<rule_3 priority="HIGH">
**CHANNELS FOR SEQUENTIAL PROCESSING**: Use `Channel` for queue-based patterns (especially LLM requests).
- Ensures sequential processing by design
- Suspends producer when buffer full
- Suspends consumer when empty
- Type-safe message passing

**MUST**:
- ✓ Use `Channel<Request>` for request queue
- ✓ `send()` in producer coroutine, `receive()` in consumer
- ✓ Close channel when no more items: `channel.close()`
- ✓ Handle `ClosedReceiveChannelException` gracefully

**Example - Good**:
```kotlin
class LLMRequestQueue {
    private val requestChannel = Channel<Request>(capacity = 10)
    
    suspend fun enqueue(request: Request): Response {
        requestChannel.send(request)
        // Suspends if channel is full, providing backpressure
    }
    
    suspend fun processQueue() {
        for (request in requestChannel) {
            val response = llmClient.process(request)
            // Sequential by design - only one request at a time
        }
    }
}

// Usage: Start processor in background, enqueue requests
suspend fun run() = coroutineScope {
    val queue = LLMRequestQueue()
    
    launch {
        queue.processQueue() // Consumer
    }
    
    // Producer
    samplePaths.forEach { path ->
        queue.enqueue(AnalysisRequest(path))
    }
}
```
</rule_3>

---

<rule_4 priority="HIGH">
**EXCEPTION HANDLING IN COROUTINES**: Coroutine exceptions are NOT automatically caught. Use proper exception handling.
- `try/catch` around suspending functions
- `CoroutineExceptionHandler` for unhandled exceptions
- `CancellationException` is NOT an error (normal cancellation)
- Never suppress `CancellationException`

**MUST**:
- ✓ Wrap suspending calls in `try/catch`
- ✓ Log exceptions with context
- ✓ Transform exceptions to domain types (not raw exceptions)
- ✓ Re-throw `CancellationException`

**Example - Good**:
```kotlin
suspend fun analyzeSamples(paths: List<String>): List<SampleAnalysis> = try {
    paths.map { path -> analyzeSampleImage(path) }
} catch (e: CancellationException) {
    throw e // Always re-throw cancellation
} catch (e: IOException) {
    throw AnalysisException("Failed to read images", e)
} catch (e: LLMException) {
    throw AnalysisException("LLM request failed", e)
}
```

**Example - Avoid**:
```kotlin
// Anti-pattern: Catching and suppressing exceptions
suspend fun analyzeSamples(paths: List<String>): List<SampleAnalysis> {
    return try {
        paths.map { path -> analyzeSampleImage(path) }
    } catch (e: Exception) {
        emptyList() // Silent failure, caller doesn't know what happened
    }
}
```
</rule_4>

---

## Type Safety & Null Handling

<rule_5 priority="HIGH">
**NO NULLABILITY**: Avoid `null` unless truly optional. Use sealed types or `Optional` equivalents instead.
- Kotlin's type system prevents null dereferences at compile time
- `null` is a code smell for missing error handling
- Use sealed classes for "nothing" cases

**MUST**:
- ✓ Mark all properties non-nullable by default
- ✓ Use `?` only for truly optional values
- ✓ Use sealed types: `sealed class Option<T> { data class Some<T>(val value: T); object None<T>}`
- ✓ Use smart casts to eliminate null checks

**Example - Good**:
```kotlin
data class CandidateImage(
    val path: String,
    val metadata: ImageMetadata,
    val qualificationScore: Float? = null // Optional score until evaluated
)

sealed class ImageLoadResult {
    data class Success(val image: Image) : ImageLoadResult()
    data class Failed(val reason: String) : ImageLoadResult()
}

val result: ImageLoadResult = loadImage(path)
when (result) {
    is ImageLoadResult.Success -> analyzeImage(result.image)
    is ImageLoadResult.Failed -> handleError(result.reason)
}
```

**Example - Avoid**:
```kotlin
// Anti-pattern: Nullable proliferation
data class CandidateImage(
    val path: String?,
    val metadata: ImageMetadata?,
    val qualificationScore: Float?
)

val image: Image? = loadImage(path)
if (image != null) {
    if (image.metadata != null) {
        // Pyramid of null checks
    }
}
```
</rule_5>

---

## Collections & Sequences

<rule_6 priority="MEDIUM">
**USE IMMUTABLE COLLECTIONS**: `List`, `Map`, `Set` are immutable by default. Prefer over `MutableList`, `MutableMap`.
- Enables safe concurrent access
- Reduces bugs from unexpected mutations
- Composition friendly

**MUST**:
- ✓ Return `List<T>` not `MutableList<T>` from functions
- ✓ Use `.toMutableList()` only when mutation is needed and local
- ✓ Use `.toList()` to create immutable copy
- ✓ Document if a function needs mutable collection

**Example - Good**:
```kotlin
fun filterQualifiedImages(
    candidates: List<CandidateImage>,
    threshold: Float
): List<CandidateImage> {
    return candidates.filter { it.qualificationScore >= threshold }
}

fun analyzeBatch(images: List<CandidateImage>) {
    val sorted = images.sortedByDescending { it.qualificationScore }
    // sorted is immutable, can't accidentally modify input
}
```

**Example - Avoid**:
```kotlin
fun filterQualifiedImages(
    candidates: MutableList<CandidateImage>,
    threshold: Float
): MutableList<CandidateImage> {
    candidates.removeIf { it.qualificationScore < threshold }
    return candidates // Mutated input, caller surprised
}
```
</rule_6>

---

## Extension Functions & Properties

<rule_7 priority="MEDIUM">
**USE EXTENSION FUNCTIONS FOR UTILITIES**: Extend existing types with utility functions rather than creating utility classes.
- Keeps code close to usage
- Reads more naturally
- Reduces class proliferation

**MUST**:
- ✓ Define extensions in focused files or companion objects
- ✓ Document non-obvious extensions
- ✓ Don't overuse - keep to genuine utilities
- ✓ Prefer extension functions over static methods

**Example - Good**:
```kotlin
fun Image.toOptimized(
    targetFormat: ImageFormat,
    quality: Int = ImageOptimization.DEFAULT_JPEG_QUALITY
): OptimizedImage {
    return convertAndCompress(this, targetFormat, quality)
}

fun List<SampleAnalysis>.aggregateProfile(): WallpaperProfile {
    return WallpaperProfile(
        aesthetics = this.map { it.aesthetics }.average(),
        technicalRequirements = this.determineTechnicalRequirements()
    )
}

// Usage reads naturally:
val optimized = image.toOptimized(ImageFormat.JPEG)
val profile = samples.aggregateProfile()
```

**Example - Avoid**:
```kotlin
// Anti-pattern: Utility class with static methods
object ImageUtils {
    fun optimize(image: Image, format: ImageFormat): OptimizedImage { }
}

// Usage less natural:
val optimized = ImageUtils.optimize(image, ImageFormat.JPEG)
```
</rule_7>

---

## Platform-Specific Code

<rule_8 priority="MEDIUM">
**ISOLATE PLATFORM CODE**: Keep macOS-specific code in `macosMain`, use interfaces in `commonMain`.
- Changes to macOS implementation don't affect common code
- Makes multiplatform targets easier to add later
- Clear separation of concerns

**MUST**:
- ✓ Define interfaces in `commonMain`
- ✓ Implement in `macosMain` using native APIs
- ✓ Inject implementations into common code
- ✓ Use `expect/actual` pattern for small functions only

**Example - Good**:
```kotlin
// commonMain/ImageDecoder.kt
interface ImageDecoder {
    suspend fun decode(path: String): Image
}

// macosMain/MacOSImageDecoder.kt
class MacOSImageDecoder : ImageDecoder {
    override suspend fun decode(path: String): Image {
        // macOS-specific implementation using Kotlin Koog
    }
}

// Usage in common code:
class ImageAnalyzer(private val decoder: ImageDecoder) {
    suspend fun analyze(path: String) = decoder.decode(path)
}
```

**Example - Avoid**:
```kotlin
// Anti-pattern: Platform code scattered in common code
fun decodeImage(path: String): Image {
    #if target(macOS)
        // macOS-specific code mixed with common code
    #endif
}
```
</rule_8>

---

## Performance Considerations

<rule_9 priority="MEDIUM">
**SEQUENCE FOR LAZY EVALUATION**: Use `Sequence<T>` for chained operations on large collections.
- Evaluates lazily (one pass)
- Reduces intermediate allocations
- Good for processing large image batches

**MUST**:
- ✓ Use `.asSequence()` before chaining operations
- ✓ Call `.toList()` or `.toSet()` to materialize
- ✓ Document performance-critical operations

**Example - Good**:
```kotlin
fun processLargeBatch(images: List<Image>): List<OptimizedImage> {
    return images
        .asSequence()
        .filter { it.isValid() }
        .map { it.toOptimized() }
        .filter { it.sizeBytes < MAX_FILE_SIZE }
        .toList()
}
```

**Example - Avoid**:
```kotlin
// Anti-pattern: Multiple intermediate lists
fun processLargeBatch(images: List<Image>): List<OptimizedImage> {
    val valid = images.filter { it.isValid() }
    val optimized = valid.map { it.toOptimized() }
    val sized = optimized.filter { it.sizeBytes < MAX_FILE_SIZE }
    return sized // 3 passes, 3 intermediate lists
}
```
</rule_9>

---

## Checklist

Before committing Kotlin code:

☐ **Suspending Functions**: Async work uses `suspend fun`, not callbacks
☐ **Structured Concurrency**: Coroutines use proper scopes, no `GlobalScope`
☐ **Exception Handling**: `CancellationException` never suppressed
☐ **Type Safety**: Minimal nullability, sealed types for alternatives
☐ **Immutable Collections**: Functions return `List`, not `MutableList`
☐ **No Platform Leaks**: macOS code isolated in `macosMain`
☐ **Performance**: Large collections use `Sequence` for lazy evaluation

---

**Last Updated**: 2026-03-08
