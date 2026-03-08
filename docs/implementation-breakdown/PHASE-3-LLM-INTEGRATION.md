# PHASE 3: LLM INTEGRATION

**Duration:** ~6-8 days  
**Effort:** 45-60 person-hours  
**Tasks:** 7 major deliverables (each includes tests)  
**Status:** Ready to execute after PHASE 1

**Dependency:** PHASE 1 must be complete

---

## Overview

Phase 3 implements LLM integration leveraging **Kotlin Koog framework as the primary integration layer**, with minimal custom code required. The strategy prioritizes using Koog's built-in LLM client capabilities, extensible executor/transport pattern, agent framework, and multimodal support to handle LMStudio integration efficiently.

**Testing Strategy (PHASE 3):**
- Each task includes unit tests written alongside code
- Use FakeLLMClient (simulated LLM) and mock Koog executor for tests
- Tests executed and fixed immediately as code is written
- Focus on request queueing, sequential processing, multimodal support
- By phase end: LLM integration fully tested and stable

**Koog-First Philosophy:**
- Maximize use of Koog's features (agents, request management, error handling, streaming)
- Minimize custom code (implement only thin LMStudio adapter if needed)
- Leverage Koog's multimodal support for image analysis
- Use Koog's built-in orchestration for sequential request management

**Critical Success Factors:**

1. Leverage Koog's request management for sequential processing (no parallelism)
2. Minimize custom LMStudio transport code via Koog's extensibility points
3. Use Koog's multimodal agent capabilities for image handling
4. Robust error handling using Koog's built-in error mechanisms and retry strategies

**Parallel Execution:** PHASE 2 and PHASE 3 can start simultaneously once PHASE 1 is complete.

**High Risk:** Task 1 (Koog evaluation) is blocking; complete early. Results will determine implementation complexity for Tasks 2-7.

---

## Task 1: Koog Framework Exploration and LMStudio Adapter Design (CRITICAL)

**Objective:** Evaluate Koog's LLM integration capabilities and design minimal-code adapter for LMStudio

**Priority:** **HIGHEST** — Must complete before full implementation

**Deliverables:**

- Comprehensive Koog LLM documentation review (agent framework, executors, tools, multimodal)
- Feasibility assessment: Can Koog's extensible patterns support LMStudio?
- Design document: LMStudio adapter architecture (how to integrate with Koog)
- Proof-of-concept: Minimal integration connecting Koog to LMStudio
- Recommendation report: Implementation path and effort estimate

**Koog Integration Strategies to Evaluate:**

Koog provides multiple integration points. Evaluate each to find minimal-code approach:

### Strategy 1: Koog Built-in OpenAI Support with Custom Base URL
- **Concept:** LMStudio exposes OpenAI-compatible `/chat/completions` API
- **Approach:** Check if Koog's OpenAI client allows custom base URL override
- **Potential:** Direct integration with zero custom code
- **Effort:** 0.5-1 day if available
- **Koog Feature Used:** `AIAgent` with custom LLMModel provider config

**Pseudo-code:**
```kotlin
val model = LLMModel(
    provider = LLMProvider.Custom, // Or OpenAI with custom endpoint
    id = "llama2",
    baseUrl = "http://localhost:1234/api/v1",
    apiKey = System.getenv("LM_API_TOKEN")
)

val agent = AIAgent(
    llmModel = model,
    systemPrompt = "..."
)
```

### Strategy 2: Koog Custom Executor/Transport Layer
- **Concept:** Implement custom `LLMExecutor` or `HttpTransport` interface (if exposed by Koog)
- **Approach:** Provide LMStudio transport; Koog handles orchestration, request queuing, retry logic
- **Potential:** Koog manages agents/tools; custom code handles HTTP only
- **Effort:** 1-2 days (medium complexity)
- **Koog Features Used:** Agent framework, executor extensibility, retry mechanisms

**Pseudo-code:**
```kotlin
class LMStudioTransport : HttpTransport {
    override suspend fun execute(request: HttpRequest): HttpResponse {
        // Custom implementation for LMStudio
        return httpClient.post("http://localhost:1234/api/v1/chat/completions", ...)
    }
}

val agent = AIAgent(
    executor = simpleAIExecutor(transport = LMStudioTransport()),
    ...
)
```

### Strategy 3: Koog Tool/Agent Pattern with Wrapped LMStudio
- **Concept:** Wrap LMStudio calls as Koog tools; Koog agent orchestrates tool invocations
- **Approach:** Expose LMStudio as a tool that Koog agent can call
- **Potential:** Koog manages agent flow, tool orchestration, sequential execution
- **Effort:** 1-2 days (medium complexity)
- **Koog Features Used:** Tool system, agent orchestration, sequential tool calls

**Pseudo-code:**
```kotlin
@Tool
suspend fun analyzWithLMStudio(prompt: String, imageBase64: String?): String {
    // Call LMStudio API
    return lmStudioClient.chat(prompt, imageBase64)
}

val agent = AIAgent(tools = listOf(::analyzeWithLMStudio), ...)
```

### Strategy 4: Hybrid Approach (RECOMMENDED)
- **Concept:** Use Koog for agents + multimodal; custom minimal transport for LMStudio OpenAI API
- **Approach:** 
  - Use Koog's agent framework and multimodal support
  - Implement thin OpenAI-compatible HTTP adapter for LMStudio
  - Koog handles sequential request queuing, error handling, retry
- **Potential:** Best balance of Koog capabilities + minimal custom code
- **Effort:** 2-3 days total (1-2 day prototypes, choose best approach)
- **Koog Features Used:** Agents, multimodal, error handling, streaming support

**Why Hybrid is Recommended:**
```
Koog handles:
  - Agent orchestration (keeping logic clean)
  - Sequential request management (built-in, no custom queue needed)
  - Error handling and retries (robust)
  - Multimodal support (images in prompts)
  - Streaming responses (if needed)
  - Request/response logging (observability)

Custom handles:
  - OpenAI-compatible HTTP adapter (~100-200 LOC)
  - LMStudio-specific configuration
  - Bearer token handling
  - Custom error mapping
```

**Research Activities (All Required):**

- [ ] Review Koog documentation (agents, executors, tools, multimodal)
- [ ] Check Koog API: Custom executors, HTTP transport overrides, provider extensibility
- [ ] Investigate Koog's multimodal support (how to send images to LLM)
- [ ] Test Koog with any built-in provider (OpenAI, Anthropic) for baseline
- [ ] Check Koog community/GitHub for LMStudio examples or similar integrations
- [ ] Document Koog's extension points and plugin architecture
- [ ] Test OpenAI-compatible protocol compatibility with Koog's OpenAI client

**LMStudio Endpoint Details (Reference):**

- **Base URL:** `http://localhost:1234/api/v1` (OpenAI-compatible)
- **Endpoint:** `/chat/completions` (POST)
- **Protocol:** OpenAI Chat Completions API specification
- **Auth:** Optional bearer token via `Authorization: Bearer $LM_API_TOKEN`
- **Multimodal:** Supports image_url in message content (OpenAI format)

**OpenAI-Compatible Request Example:**
```json
{
  "model": "llama2",
  "messages": [
    {
      "role": "user",
      "content": [
        { "type": "text", "text": "Analyze this wallpaper..." },
        { "type": "image_url", "image_url": { "url": "data:image/jpeg;base64,..." } }
      ]
    }
  ],
  "max_tokens": 500,
  "temperature": 0.7
}
```

**Success Criteria:**

- [ ] Koog capabilities fully mapped (LLM clients, agents, executors, tools, multimodal)
- [ ] Extension points identified (where to hook custom code if needed)
- [ ] 3+ integration strategies evaluated with pros/cons
- [ ] Proof-of-concept implemented (whichever shows most promise)
- [ ] Custom code minimized (prefer Koog features over custom implementation)
- [ ] Clear recommendation for Tasks 2-7 implementation path
- [ ] Identified Koog limitations and mitigation strategies
- [ ] Effort estimate for Tasks 2-7 based on chosen approach

**Tests for This Task:**
- Create `src/commonTest/kotlin/com/wallpaperqualifier/llm/KoogIntegrationTest.kt`
- Test: Koog agent can be instantiated with LMStudio config
- Test: Mock LLM response can be configured
- Test: Sequential request processing verified
- Run: `./gradlew test` — all tests must pass

**Estimated Effort:** 2-3 days (research + design + multiple PoCs)

**Key Questions to Answer:**

1. Can Koog's LLM client support OpenAI-compatible custom endpoint natively?
2. Does Koog expose executor/transport layer for custom implementation?
3. How does Koog handle sequential request management? (needed for LMStudio)
4. What's Koog's multimodal support level? (images in prompts)
5. Are there existing Koog + OpenAI-compatible examples/plugins?

**Output Documentation:**
- `KOOG_INTEGRATION_REPORT.md` — findings, recommendations, architecture
- `KOOG_PROOF_OF_CONCEPT/` — working PoC code (minimal example)

---

## Task 2: Koog Agent Setup and Configuration

**Objective:** Configure Koog agent framework for LLM analysis tasks

**Deliverables:**

- Koog agent initialization and configuration
- LMStudio provider setup (using result from Task 1)
- System prompt templates for analysis and evaluation
- Agent lifecycle management (startup, shutdown)
- Configuration integration with PHASE 1 Config module

**Koog Agent Components to Configure:**

```kotlin
// Based on Koog's structure (to be confirmed in Task 1)
val llmModel = LLMModel(
    provider = /* Determined by Task 1 */,
    id = "llama2",
    apiKey = config.llm.apiKey,
    baseUrl = config.llm.endpoint,
    maxTokens = 500,
    temperature = 0.7
)

val agent = AIAgent(
    llmModel = llmModel,
    systemPrompt = "You are an expert wallpaper analyst...",
    tools = listOf(/* Optional: custom tools */),
    // Sequential execution (Koog's default or configured)
)
```

**Success Criteria:**

- Agent initializes without errors
- Configuration loads from PHASE 1 Config module
- System prompts set correctly
- Agent ready for use in Tasks 3-5

**Tests for This Task:**
- Create `src/commonTest/kotlin/com/wallpaperqualifier/llm/KoogAgentSetupTest.kt`
- Test: Agent can be configured with LMStudio endpoint
- Test: System prompt can be set and retrieved
- Test: Agent initialization succeeds
- Run: `./gradlew test` — all tests must pass

---

## Task 3: Koog Multimodal Image Handling

**Objective:** Integrate Koog's multimodal capabilities for image analysis

**Deliverables:**

- Multimodal message construction (text + image)
- Image encoding for Koog (format: Base64, URL, file path)
- Integration with PHASE 2 image processing (use converted JPEG/PNG)
- Error handling for image encoding failures

**Implementation:**

Koog should handle multimodal natively. The task is to:
1. Encode images (Base64 from PHASE 2 converted files)
2. Construct multimodal prompts (text + image)
3. Send to Koog agent
4. Parse response

```kotlin
suspend fun analyzeImageWithKoog(
    imageBase64: String,
    prompt: String
): Result<AnalysisResponse> {
    val multimodalMessage = Message(
        role = "user",
        content = listOf(
            TextContent("$prompt"),
            ImageContent(base64 = imageBase64, format = "jpeg")
        )
    )
    
    return agent.execute(multimodalMessage)
}
```

**Success Criteria:**

- Koog sends images to LMStudio successfully
- Multimodal responses parsed correctly
- Image formats (JPEG, PNG) supported
- Performance acceptable

**Tests for This Task:**
- Create `src/commonTest/kotlin/com/wallpaperqualifier/llm/MultimodalHandlingTest.kt`
- Test: Multimodal message can be constructed (text + image)
- Test: Image encoding (Base64) works correctly
- Test: Mock multimodal response is parsed
- Run: `./gradlew test` — all tests must pass

---

## Task 4: Request Sequential Processing with Koog

**Objective:** Ensure Koog's request management enforces sequential processing

**Deliverables:**

- Verification that Koog agent sends requests one-at-a-time
- Configuration to disable parallelism (if option exists)
- Request queue testing (sequential ordering verified)
- Monitoring and logging of request lifecycle

**Critical Constraint Enforcement:**

Koog should handle this automatically if it uses sequential agent execution. Tasks include:
1. Verify Koog doesn't parallelize by default
2. Configure if needed (disable parallelism, set max concurrent to 1)
3. Test with multiple requests to confirm ordering
4. Add logging to verify sequential processing

```kotlin
// If Koog allows configuration
val agent = AIAgent(
    maxConcurrentRequests = 1, // Enforce sequential
    requestTimeout = 30_000,
    logRequests = true // Enable logging
)
```

**Success Criteria:**

- Only one LLM request in-flight at any time (verified by logging)
- Requests processed in order (FIFO)
- No race conditions or parallel sends
- Timeouts handled gracefully

**Tests for This Task:**
- Create `src/commonTest/kotlin/com/wallpaperqualifier/llm/SequentialProcessingTest.kt`
- Test: Multiple requests processed sequentially
- Test: Requests maintain order (FIFO)
- Test: No concurrent requests in flight
- Test: Timeout handling works correctly
- Run: `./gradlew test` — all tests must pass

---

## Task 5: Prompt Template System with Koog

**Objective:** Create flexible, reusable prompts for image analysis and evaluation

**Deliverables:**

- System prompt templates (analysis, evaluation)
- Prompt variable substitution (profile context, image description)
- Template versioning for reproducibility
- Integration with Koog agent configuration

**Templates:**

**Sample Analysis Prompt:**
```
You are an expert wallpaper analyst. Analyze the provided wallpaper image 
and describe its key aesthetic qualities in structured JSON format.

Focus on:
1. Color palette (primary and accent colors with hex codes)
2. Visual style (minimalist, vibrant, abstract, photographic, etc.)
3. Mood/atmosphere (calming, energetic, dramatic, contemplative, etc.)
4. Composition (centered, rule of thirds, symmetrical, dynamic, etc.)
5. Subject matter (nature, urban, abstract, art, patterns, etc.)
6. Technical quality (resolution, focus, lighting, clarity 0-100)

Return JSON:
{
  "colorPalette": ["#color1", "#color2"],
  "style": "...",
  "mood": "...",
  "composition": "...",
  "subject": "...",
  "qualityScore": 85
}
```

**Candidate Evaluation Prompt:**
```
You are a wallpaper curation expert. Evaluate if this candidate wallpaper 
matches the user's preferences based on their quality profile.

User Profile Summary:
{{PROFILE_SUMMARY}}

Evaluate the candidate image and determine if it's a GOOD match or POOR match.

Return JSON:
{
  "qualified": true/false,
  "confidence": 0.85,
  "reasoning": "Matches color palette and mood..."
}
```

**Success Criteria:**

- Templates produce consistent responses
- Variables substitute correctly
- JSON responses parse cleanly
- Versioning allows reproducibility

**Tests for This Task:**
- Create `src/commonTest/kotlin/com/wallpaperqualifier/llm/PromptTemplateTest.kt`
- Test: Analysis prompt template renders correctly
- Test: Evaluation prompt template renders correctly
- Test: Variable substitution works
- Test: Template versioning system works
- Run: `./gradlew test` — all tests must pass

---

## Task 6: Response Parsing and Validation with Koog

**Objective:** Extract structured data from Koog agent responses

**Deliverables:**

- Parser for analysis responses → `ImageCharacteristics` domain object
- Parser for evaluation responses → `EvaluationResult` domain object
- Validation (required fields, score ranges, type safety)
- Error handling (unexpected format, missing fields)
- Fallback values for partial data

**Implementation:**

```kotlin
class KoogResponseParser {
    fun parseAnalysisResponse(agentResponse: String): Result<ImageCharacteristics> {
        return try {
            val json = JSON.parseToJsonElement(agentResponse).jsonObject
            Success(ImageCharacteristics(
                colorPalette = json["colorPalette"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
                style = json["style"]?.jsonPrimitive?.content ?: "unknown",
                mood = json["mood"]?.jsonPrimitive?.content ?: "neutral",
                // ... etc
            ))
        } catch (e: Exception) {
            Failure(LLMException("Failed to parse Koog response: ${e.message}"))
        }
    }
    
    fun parseEvaluationResponse(agentResponse: String): Result<EvaluationResult> {
        // Similar implementation for evaluation
    }
}
```

**Success Criteria:**

- Responses parsed reliably
- Validation catches malformed responses
- Missing fields handled gracefully
- Domain objects created correctly
- Type safety maintained

**Tests for This Task:**
- Create `src/commonTest/kotlin/com/wallpaperqualifier/llm/ResponseParsingTest.kt`
- Test: Analysis response parsed correctly
- Test: Evaluation response parsed correctly
- Test: Malformed response caught and error returned
- Test: Missing fields handled gracefully
- Run: `./gradlew test` — all tests must pass

---

## Task 7: Error Handling and Retry Logic with Koog

**Objective:** Robust error handling leveraging Koog's built-in mechanisms

**Deliverables:**

- Error detection (network, timeout, malformed response, model errors)
- User-friendly error messages
- Request/response logging (DEBUG mode)
- Retry strategy (leverage Koog's built-in or implement custom)
- Graceful degradation when LMStudio unavailable

**Error Handling Strategy:**

Delegate to Koog where possible:
1. Koog's built-in error handling (connection, timeout)
2. Koog's retry mechanisms (if available)
3. Custom error mapping for LMStudio-specific errors

```kotlin
// Error types to handle
sealed class LLMError {
    data class ConnectionFailed(val message: String) : LLMError()
    data class TimeoutError(val durationMs: Long) : LLMError()
    data class AuthenticationFailed(val message: String) : LLMError()
    data class RateLimitExceeded(val retryAfterMs: Long) : LLMError()
    data class InvalidResponse(val message: String) : LLMError()
    data class ModelError(val message: String) : LLMError()
}

// Wrap Koog calls with error mapping
suspend fun callKoogAgent(message: Message): Result<String> {
    return try {
        val response = agent.execute(message)
        Success(response)
    } catch (e: TimeoutException) {
        Failure(LLMError.TimeoutError(e.message?.toLongOrNull() ?: 0))
    } catch (e: ConnectException) {
        Failure(LLMError.ConnectionFailed("LMStudio unavailable: ${e.message}"))
    } catch (e: Exception) {
        Failure(LLMError.ModelError("LMStudio error: ${e.message}"))
    }
}
```

**Retry Strategy:**

- **Option 1:** Use Koog's built-in retry (preferred if available)
- **Option 2:** Implement custom retry with exponential backoff (1s, 2s, 4s)
- **Only retry on:** Connection errors, timeouts, 5xx server errors
- **Don't retry on:** Auth errors, 4xx client errors (malformed request)

**Success Criteria:**

- All error types detected and logged
- User sees actionable error messages
- Retries happen transparently
- Timeouts handled without hanging
- Failed requests don't cascade

**Tests for This Task:**
- Create `src/commonTest/kotlin/com/wallpaperqualifier/llm/ErrorHandlingTest.kt`
- Test: Network error detected and handled gracefully
- Test: Timeout detected and appropriate message shown
- Test: Invalid response detected and logged
- Test: Retry logic works (if implemented)
- Run: `./gradlew test` — all tests must pass

---

## Integration with Previous Phases

- **PHASE 1:** Uses Logger, Result types, Config, CLI
- **PHASE 2:** Receives converted JPEG/PNG images for analysis
- **PHASE 3:** Orchestrates all LLM interactions through Koog
- **PHASE 4:** Calls PHASE 3 tasks for image analysis and evaluation

---

## Koog-Centric Design Benefits

By prioritizing Koog integration:

| Benefit | Impact |
|---------|--------|
| **Request Management** | Koog handles sequential queuing (no custom code) |
| **Error Handling** | Koog's built-in mechanisms (timeouts, retries, logging) |
| **Multimodal Support** | Koog handles image encoding/transmission |
| **Agent Framework** | Clean orchestration of analysis tasks |
| **Observability** | Koog's built-in logging and monitoring |
| **Maintenance** | Less custom code to maintain long-term |

---

## Completion Checklist

Before moving to PHASE 4, verify:

- [ ] Task 1 complete: Koog evaluation and recommendation documented
- [ ] LMStudio connection working through Koog (PoC functional)
- [ ] Agent configured and initialized
- [ ] Multimodal image handling working
- [ ] Sequential processing verified
- [ ] Prompt templates producing consistent responses
- [ ] Response parsing robust
- [ ] Error handling comprehensive
- [ ] Bearer token authentication optional but supported
- [ ] Logging captures request/response data (DEBUG mode)
- [ ] Performance acceptable (requests <5s typical, LMStudio-dependent)

---

## Risk Mitigation

| Risk | Mitigation |
|------|-----------|
| Koog doesn't support needed extensibility | Use fallback: custom HTTP client (minimal, ~300 LOC) |
| LMStudio integration more complex than expected | Task 1 proof-of-concept mitigates; escalate early |
| Multimodal support insufficient | Test in Task 1; fallback to text descriptions |
| Sequential processing difficult to enforce | Koog's agent framework should handle; verify in Task 4 |
| Response parsing brittle | Validate strictly; log unexpected responses for debugging |

---

## Phase 3 Complete Indicators

✅ **Phase 3 is complete when:**
1. All 7 tasks delivered
2. LMStudio integration working reliably through Koog
3. Sequential request processing tested and verified
4. Multimodal image analysis functional
5. Response parsing robust
6. Error handling comprehensive
7. Ready for integration with PHASE 4 (Core Logic)

---

**Next:** Proceed to [PHASE-4-CORE-LOGIC.md](./PHASE-4-CORE-LOGIC.md) once both PHASE 2 and PHASE 3 complete.
