<primary_directive>
You integrate with LLM services PRECISELY. You maintain STRICT sequential ordering of requests. You NEVER parallelize LLM calls. You ALWAYS parse responses carefully and validate structure.
</primary_directive>

# LLM Integration: API & Request Queue Management

## Overview

The Wallpaper Qualifier communicates with LLM services (OpenAI, Anthropic, Google, or LMStudio) via HTTP API using any provider supported by Kotlin Koog. All LLM requests MUST be sequential - never parallel. This document covers client implementation, prompt design, and response handling.

---

## LLM Request Queue Pattern

<rule_1 priority="HIGHEST">
**SEQUENTIAL REQUESTS ONLY**: LLM requests must be sent one at a time. No parallelism. EVER.
- This is a hard architectural constraint
- Sequential ordering ensures fair LLM resource usage
- Prevents API rate limit issues
- Makes progress reporting meaningful (request N of M)
- No retry strategy for failed requests in the first version (fail-fast behavior)

**MUST**:
- ✓ Use `Channel<Request>` to enforce sequentiality
- ✓ Single consumer coroutine processes requests
- ✓ Producers enqueue, consumer dequeues and sends
- ✓ Never use `launch { llmClient.send() }` directly
- ✓ Never use `runBlocking { }` for LLM calls

**Example - Good (Sequential)**:
```kotlin
class LLMRequestQueue(private val client: LLMClient) {
    private val requestChannel = Channel<QueuedRequest>(capacity = 100)
    private val responseMap = mutableMapOf<String, Response>()
    
    // Start this in background
    suspend fun startProcessing() {
        for (queuedRequest in requestChannel) {
            try {
                val response = client.send(queuedRequest.request) // Sequential by design
                responseMap[queuedRequest.id] = response
                queuedRequest.continuation.resume(response)
            } catch (e: Exception) {
                queuedRequest.continuation.resumeWithException(e)
            }
        }
    }
    
    suspend fun enqueue(request: Request): Response {
        val id = UUID.randomUUID().toString()
        return suspendCancellableCoroutine { continuation ->
            requestChannel.trySend(QueuedRequest(id, request, continuation))
        }
    }
}

// Usage:
val queue = LLMRequestQueue(llmClient)

// Start processor in background
launch {
    queue.startProcessing()
}

// Enqueue requests - they wait in queue
val sample1Analysis = queue.enqueue(AnalyzeImageRequest(sample1))
val sample2Analysis = queue.enqueue(AnalyzeImageRequest(sample2)) // Waits for sample1 to complete
```

**Example - AVOID (Parallel - WRONG)**:
```kotlin
// Anti-pattern: NEVER DO THIS
suspend fun analyzeSamplesInParallel(samples: List<Image>) {
    samples.map { sample ->
        async { llmClient.analyze(sample) } // WRONG! Parallelizes LLM requests
    }.awaitAll()
}

// Anti-pattern: Fire-and-forget (WRONG)
fun analyzeSamples(samples: List<Image>) {
    samples.forEach { sample ->
        launch { llmClient.analyze(sample) } // WRONG! No sequencing
    }
}
```
</rule_1>

---

<rule_2 priority="HIGHEST">
**RESPONSE PARSING & VALIDATION**: Always validate LLM responses before use. Structure is never guaranteed.
- LLM responses can be partially formed or contain hallucinations
- Always check required fields exist
- Always verify data types match expectations
- Always provide fallback for missing fields

**MUST**:
- ✓ Parse JSON strictly (not lenient)
- ✓ Check all required fields present
- ✓ Validate data types (string, number, array)
- ✓ Provide clear error on parse failure
- ✓ Log problematic responses for debugging

**Example - Good**:
```kotlin
@Serializable
data class AnalysisResponse(
    @SerialName("primary_colors")
    val primaryColors: List<String>,
    @SerialName("composition_style")
    val compositionStyle: String,
    @SerialName("visual_style")
    val visualStyle: String,
    @SerialName("quality_score")
    val qualityScore: Float,
    @SerialName("mood")
    val mood: String? = null // Optional
)

suspend fun parseAnalysisResponse(jsonString: String): Result<AnalysisResponse> = try {
    // Strict parsing - fails on unknown fields or missing required fields
    val response = Json {
        ignoreUnknownKeys = false
        isLenient = false
    }.decodeFromString<AnalysisResponse>(jsonString)
    
    // Validate ranges
    if (response.qualityScore !in 0f..1f) {
        return Result.failure(
            InvalidResponseException("Quality score outside valid range: ${response.qualityScore}")
        )
    }
    
    Result.success(response)
} catch (e: SerializationException) {
    logger.error("Failed to parse LLM response: ${e.message}")
    logger.debug("Response was: $jsonString")
    Result.failure(InvalidResponseException("Invalid response format: ${e.message}"))
}

// Usage:
when (val result = parseAnalysisResponse(llmResponseJson)) {
    is Result.Success -> analyzeCharacteristics(result.value)
    is Result.Failure -> handleParseError(result.error)
}
```

**Example - Avoid**:
```kotlin
// Anti-pattern: Lenient parsing with null coalescing
val response = Json {
    ignoreUnknownKeys = true
    isLenient = true // WRONG: Accepts malformed JSON
}.decodeFromString<AnalysisResponse>(jsonString)

val colors = response.primaryColors ?: emptyList() // Silent default if missing
```
</rule_2>

---

<rule_3 priority="HIGH">
**PROMPT ENGINEERING**: Design prompts for consistent, structured output.
- Be explicit about format (JSON, specific fields)
- Provide examples
- Constrain output scope
- Separate concerns (analysis vs. judgment)

**MUST**:
- ✓ Always request JSON output with specific schema
- ✓ Include example of desired output format
- ✓ Be explicit about required fields
- ✓ Separate image analysis from image judgment
- ✓ Use role-based prompts (e.g., "You are an image aesthetics expert")

**Example - Good**:
```kotlin
fun createSampleAnalysisPrompt(imagePath: String): String = """
You are an expert in image aesthetics and technical quality assessment.

Analyze this wallpaper image and extract characteristics in JSON format:

REQUIRED OUTPUT FORMAT:
{
  "primary_colors": ["color1", "color2", "color3"],
  "composition_style": "one of: minimalist, balanced, busy, centered, rule-of-thirds",
  "subject_matter": "one of: abstract, nature, architecture, geometric, portrait, landscape",
  "visual_style": "one of: photographic, illustrated, vector, painted, digital",
  "quality_score": 0.0 to 1.0,
  "mood": "calm, energetic, dark, bright, serene, vibrant",
  "technical_notes": "observations about resolution, clarity, or artifacts"
}

Be precise. Extract only observable characteristics, not opinions.
""".trimIndent()

fun createEvaluationPrompt(
    qualityProfile: WallpaperProfile,
    candidatePath: String
): String = """
You are an expert in evaluating wallpapers against aesthetic and technical criteria.

This is the quality profile derived from sample wallpapers the user likes:
${qualityProfile.toHumanReadableDescription()}

Now evaluate this candidate wallpaper against the profile.

REQUIRED OUTPUT FORMAT:
{
  "qualified": true or false,
  "confidence": 0.0 to 1.0,
  "reasoning": "explanation of decision",
  "matches": ["specific aspects that match"],
  "mismatches": ["specific aspects that differ"]
}

Be critical. If it doesn't clearly match, mark as unqualified.
""".trimIndent()
```

**Example - Avoid**:
```kotlin
// Anti-pattern: Vague prompt, no structure
fun createPrompt(imagePath: String): String = """
Analyze this wallpaper image.
Tell me what you think about it.
"""
// Response could be anything - paragraph, JSON, random format
```
</rule_3>

---

<rule_4 priority="HIGH">
**ERROR HANDLING & RECOVERY**: LLM services can fail. Handle gracefully with fail-fast strategies in the first version.
- Network errors should report and stop (no retries)
- API errors should fail fast
- Timeout should report clearly
- Invalid responses should not crash
- Log all failures for debugging

**MUST**:
- ✓ Distinguish network errors from API errors
- ✓ Log request/response for debugging
- ✓ Timeout after reasonable interval (30s for this project)
- ✓ Fail fast on all LLM errors in first version
- ✓ Propagate errors up with context

**Example - Good**:
```kotlin
sealed class LLMError {
    data class NetworkError(val message: String, val cause: Throwable) : LLMError()
    data class APIError(val code: Int, val message: String) : LLMError()
    data class TimeoutError(val seconds: Int) : LLMError()
    data class InvalidResponse(val reason: String) : LLMError()
}

suspend fun sendRequest(request: Request): Result<String> = try {
    // Set timeout
    withTimeoutOrNull(timeMillis = 30_000) {
        val response = httpClient.post(llmEndpoint) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        
        when (response.status) {
            HttpStatusCode.OK -> Result.success(response.bodyAsText())
            HttpStatusCode.Unauthorized -> Result.failure(
                LLMError.APIError(401, "Invalid API token")
            )
            HttpStatusCode.TooManyRequests -> Result.failure(
                LLMError.APIError(429, "Rate limit exceeded")
            )
            else -> Result.failure(
                LLMError.APIError(response.status.value, response.bodyAsText())
            )
        }
    } ?: Result.failure(LLMError.TimeoutError(seconds = 30))
} catch (e: IOException) {
    logger.error("Network error sending to LLM", e)
    Result.failure(LLMError.NetworkError("Network failure: ${e.message}", e))
} catch (e: Exception) {
    logger.error("Unexpected error calling LLM", e)
    Result.failure(LLMError.NetworkError("Unexpected error: ${e.message}", e))
}
```

**Example - Avoid**:
```kotlin
// Anti-pattern: Silent failures
suspend fun sendRequest(request: Request): String {
    return try {
        httpClient.post(llmEndpoint) { setBody(request) }.bodyAsText()
    } catch (e: Exception) {
        "" // Caller gets empty string, doesn't know what happened
    }
}
```
</rule_4>

---

<rule_5 priority="MEDIUM">
**IMAGE ENCODING FOR LLM**: Images sent to LLM should be optimized for transmission and processing.
- Use base64 encoding for direct embedding
- Use file upload for large images
- Resize to reasonable size (LMStudio default 2048px)
- Verify format compatibility

**MUST**:
- ✓ Encode images as base64 for JSON payload
- ✓ Resize images before encoding (max 2048px)
- ✓ Prefer JPEG for file size
- ✓ Handle encoding errors explicitly

**Example - Good**:
```kotlin
suspend fun encodeImageForLLM(
    imagePath: String,
    maxDimension: Int = 2048
): Result<String> {
    val image = ImageDecoder.decode(imagePath)
    
    // Resize if needed
    val resized = if (image.width > maxDimension || image.height > maxDimension) {
        image.resizeToFit(maxDimension)
    } else {
        image
    }
    
    // Encode to JPEG for smaller payload
    val jpegBytes = JPEGEncoder.encode(resized, quality = 85)
    val base64 = jpegBytes.encodeBase64()
    
    return Result.success("data:image/jpeg;base64,$base64")
}

fun createImageMessage(imagePath: String, prompt: String): Request = Request(
    messages = listOf(
        Message(
            role = "user",
            content = listOf(
                ContentPart.Text(prompt),
                ContentPart.Image(
                    imageUrl = ImageUrl(url = encodeImageForLLM(imagePath).getOrThrow())
                )
            )
        )
    )
)
```

**Example - Avoid**:
```kotlin
// Anti-pattern: Sending massive unoptimized images
val base64 = File(imagePath).readBytes().encodeBase64() // Huge payload
val message = Message(role = "user", content = base64)
```
</rule_5>

---

## Checklist

Before implementing LLM integration:

☐ **Sequential Queue**: Using `Channel` to enforce one-at-a-time
☐ **Response Validation**: Parsing strictly, checking all required fields
☐ **Prompt Structure**: Requesting JSON, providing examples
☐ **Error Types**: Distinguishing network, API, timeout, validation errors
☐ **Timeouts**: 30s timeout with explicit handling
☐ **Logging**: Request/response logged for debugging
☐ **Image Encoding**: Optimizing size before transmission
☐ **Documentation**: Prompts documented for transparency

---

**Last Updated**: 2026-03-08
