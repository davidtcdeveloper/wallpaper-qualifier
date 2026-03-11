## Koog Integration Report (Phase 3 Task 1)

**Date:** 2026-03-11  
**Context:** PHASE 3 – LLM Integration (Wallpaper Qualifier)  

---

### 1. Objective

Evaluate Kotlin Koog as the primary LLM integration layer for LMStudio and decide whether to:

- Use Koog directly, or
- Implement a custom OpenAI-compatible HTTP client with a request queue,

while keeping the public `llm` service interface stable for PHASE 4.

---

### 2. Findings About Koog

- Koog provides an `OpenAILLMClient` that talks to OpenAI-style chat/embeddings APIs and accepts a configurable base URL and API key.
- Koog is designed to integrate tightly with Ktor, Spring Boot, and its own agent framework, with a relatively rich surface (models, tools, streaming, moderation, etc.).
- For this project’s current setup:
  - The Gradle build is a simple JVM CLI (no Ktor, no Spring, no Koog dependency yet).
  - Introducing Koog would add a new dependency family and require wiring its abstractions into a small, single-purpose CLI.
  - The PHASE 3 requirements (OpenAI-compatible `/chat/completions`, strict sequential queue, JSON parsing, basic error handling) are all achievable with a lightweight HTTP client (e.g., Ktor client) and a hand-rolled request queue.

**Conclusion:** Koog is a good fit for larger applications or frameworks but is **not strictly necessary** to meet the current MVP requirements for Wallpaper Qualifier.

---

### 3. Decision for v0.1

For the initial implementation of PHASE 3:

- **Primary implementation:**  
  - Implement a **custom OpenAI-compatible HTTP client** that talks directly to LMStudio’s `/chat/completions` endpoint.
  - Implement an explicit **`LLMRequestQueue`** (channel-based) to guarantee strict sequential processing, following `docs/ai-rules/llm-integration.md`.
  - Keep the integration surface small and focused: only what PHASE 4 needs (sample analysis and candidate evaluation).

- **Koog status:**  
  - **Not introduced** as a runtime dependency in v0.1.
  - The `llm` module will be designed so that Koog (or another framework) can be slotted in later behind the same public interface if needed.

This choice:

- Aligns with the existing `llm-integration.md` rules (explicit HTTP client + channel-based queue).
- Avoids adding a framework that is not yet used elsewhere in the project.
- Keeps the implementation transparent and testable with simple fakes.

---

### 4. Public `llm` Service Interface (Stable Contract)

PHASE 4 will talk to the LLM through a small, framework-agnostic interface in `com.wallpaperqualifier.llm`:

- `LLMService`
  - `suspend fun analyzeSample(image: com.wallpaperqualifier.domain.Image): com.wallpaperqualifier.domain.Result<com.wallpaperqualifier.domain.ImageCharacteristics>`
  - `suspend fun evaluateCandidate(image: com.wallpaperqualifier.domain.Image, profile: com.wallpaperqualifier.domain.QualityProfile): com.wallpaperqualifier.domain.Result<com.wallpaperqualifier.domain.EvaluationResult>`

This interface:

- Hides HTTP and queue details from PHASE 4.
- Does not expose any Koog-specific types.
- Allows a future Koog-based implementation to be swapped in with minimal changes.

---

### 5. Chosen Implementation Strategy

For PHASE 3 implementation tasks:

1. **HTTP Client:**
   - Use Ktor `HttpClient` to call LMStudio’s OpenAI-compatible `/chat/completions` endpoint with:
     - `model` from `AppConfig.llm.model`
     - `endpoint` from `AppConfig.llm.endpoint`
     - Optional `Authorization: Bearer <apiKey>` if `AppConfig.llm.apiKey` is set.

2. **Sequential Request Queue:**
   - Implement an `LLMRequestQueue` using `Channel` and a single consumer coroutine, as described in `docs/ai-rules/llm-integration.md` and `docs/ai-rules/kotlin-multiplatform.md`.
   - All LLM calls from PHASE 4 go through this queue; no direct parallel calls are allowed.

3. **Prompt Templates & Parsing:**
   - Implement prompt templates and JSON response parsing according to `PHASE-3-LLM-INTEGRATION.md` and `docs/ai-rules/llm-integration.md`.

4. **Error Handling:**
   - Implement `LLMError` and map network / HTTP / parsing failures into `Result.Failure(LLMException(...))` or more specific domain errors.
   - Fail fast on LLM errors in v0.1 (no automatic retries), per `llm-integration.md`.

---

### 6. Future Koog Integration Path (v1.1+)

If we decide to adopt Koog later:

- Implement a new `KoogBasedLLMService` that also implements `LLMService`.
- Internally create an `OpenAILLMClient` pointing to LMStudio’s base URL.
- Reuse the same prompt templates and JSON parsing logic where possible.
- Optionally replace the explicit `LLMRequestQueue` with Koog’s own orchestration if it can be configured to be strictly sequential.

Because the current design keeps the `llm` boundary narrow and domain-centric, this migration can be done without touching PHASE 4 workflows.

---

### 7. Impact on Phase 3 Tasks

- **Task 1 (Koog evaluation):** Completed by this report; Koog is evaluated and explicitly deferred for v0.1.
- **Tasks 2–7:** Will proceed with the custom HTTP + `LLMRequestQueue` approach, while still following all constraints and patterns from `docs/ai-rules/llm-integration.md`.

