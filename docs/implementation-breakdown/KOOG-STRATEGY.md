# Koog-Centric LLM Integration Strategy

**Date:** 2026-03-08  
**Decision:** Prioritize Kotlin Koog framework usage over custom HTTP integration

---

## Overview

The implementation strategy for PHASE 3: LLM Integration has been adjusted to **maximize Kotlin Koog framework usage** while **minimizing custom code**. This decision prioritizes framework features over building custom infrastructure.

---

## Key Principles

1. **Koog First:** Use Koog's built-in capabilities wherever possible
2. **Custom Only When Needed:** Implement custom code only for LMStudio-specific integration gaps
3. **Framework Features:** Leverage Koog's agents, request management, error handling, multimodal support
4. **Minimal Code:** Target ~300-500 LOC for custom code (vs. ~1000+ for full custom HTTP client)

---

## What Changed

### PHASE 3: LLM Integration

**Before (Custom HTTP Approach):**
- Task 1: Custom Koog wrapper prototype
- Task 2: HTTP client implementation
- Task 3: Sequential request queue (custom)
- Tasks 4-7: Supporting functionality

**After (Koog-Centric Approach):**
- Task 1: **Koog Framework Exploration** — evaluate Koog's LLM capabilities and design LMStudio adapter (THIS IS CRITICAL)
- Task 2: Koog agent configuration
- Task 3: Multimodal image handling (via Koog)
- Task 4: Sequential request processing (verify Koog handles this)
- Task 5: Prompt templates
- Task 6: Response parsing
- Task 7: Error handling (using Koog's mechanisms)

**Key Difference:** Framework takes ownership of orchestration; custom code handles only LMStudio-specific protocol details.

---

## Integration Strategies to Evaluate (Task 1)

### Strategy 1: Direct Koog OpenAI Support (Best Case)
```
LMStudio (OpenAI-compatible API)
  ↓
Koog's OpenAI client (if supports custom base URL)
  ↓
Result: No custom code needed; 0 days additional effort
```

### Strategy 2: Koog Custom Executor Pattern (Medium Case)
```
LMStudio (OpenAI-compatible API)
  ↓
Custom Executor implementing Koog's interface (~100-200 LOC)
  ↓
Koog's agent framework (handles orchestration, retry, sequential)
  ↓
Result: Minimal custom code; 1-2 days implementation
```

### Strategy 3: Koog Tool System (Medium-Complex Case)
```
LMStudio (OpenAI-compatible API)
  ↓
Custom LMStudio tool wrapper (~200-300 LOC)
  ↓
Koog's agent framework (calls tool sequentially, manages state)
  ↓
Result: Custom tool; Koog orchestrates; 1-2 days implementation
```

### Strategy 4: Hybrid (Fallback)
```
If Koog insufficient, implement thin OpenAI-compatible adapter (~300 LOC)
but still leverage Koog for agents and multimodal where possible
```

---

## Benefits of Koog-Centric Approach

| Benefit | Impact |
|---------|--------|
| **Built-in Request Management** | Koog handles sequential queuing; no custom queue needed |
| **Error Handling** | Koog's built-in timeouts, retries, logging (robust) |
| **Multimodal Support** | Koog handles image encoding/transmission natively |
| **Agent Framework** | Clean separation of concerns; orchestration is framework's job |
| **Observability** | Koog's built-in logging, monitoring, debugging |
| **Maintenance** | Less custom code = fewer bugs, easier updates |
| **Future Extensions** | Koog's plugin system enables future features (streaming, tools, etc.) |
| **Performance** | Koog's optimizations benefit application automatically |

---

## Implementation Guidance

### PHASE 3 Task 1: Critical Research (2-3 days)

**Must Answer:**
1. Does Koog support custom OpenAI-compatible endpoints natively?
2. What are Koog's extension points? (executors, transport layers, tools)
3. How does Koog handle sequential request management?
4. What's Koog's multimodal support level?
5. Are there existing Koog + OpenAI-compatible examples?

**Deliverables:**
- `KOOG_INTEGRATION_REPORT.md` — findings and recommendations
- Proof-of-concept code (whichever approach most promising)
- Effort estimates for Tasks 2-7 based on chosen strategy

**Output determines:** How much custom code Tasks 2-7 need to write

### PHASE 3 Tasks 2-7: Implementation (4-5 days)

Once Task 1 complete, implement following chosen strategy:

**All tasks use Koog's features:**
- Agent framework for orchestration
- Koog's multimodal message handling
- Built-in request queuing (sequential by design)
- Koog's error handling and retry mechanisms

**Custom code limited to:**
- LMStudio HTTP endpoint (if Koog doesn't support directly)
- Bearer token handling (if needed)
- Response mapping (domain objects)
- Prompt templates (domain-specific, not framework)

---

## Risk Mitigation

### What if Koog Doesn't Support Needed Capability?

**Risk:** Koog doesn't have extensibility for custom LLM provider

**Mitigation:** Fallback approach already documented in PHASE 3 Task 1
- Hybrid approach: Use Koog for agents + multimodal
- Implement thin custom HTTP client for LMStudio transport
- Still reduces custom code vs. full custom HTTP solution

### What if Koog Multimodal Insufficient?

**Risk:** Koog can't send images to LMStudio properly

**Mitigation:**
- PHASE 3 Task 3 includes multimodal testing
- Fallback: Encode images as Base64; send as text description + Base64 data
- Document limitation; plan for v1.1 enhancement

### What if Sequential Processing Difficult with Koog?

**Risk:** Koog naturally parallelizes requests

**Mitigation:**
- Koog agents typically process sequentially by default
- PHASE 3 Task 4 verifies this
- If needed: Configuration to disable parallelism, or custom wrapper

---

## Timeline Impact

**Optimistic Case (Koog has OpenAI support with custom endpoint):**
- PHASE 3: 6-7 days (vs. 8 days original)
- Overall: 37-38 days (vs. 39 days original)
- **Savings:** 1-2 days

**Expected Case (Koog custom executor pattern):**
- PHASE 3: 7-8 days
- Overall: 38-39 days (matches original)
- **Benefit:** Cleaner code, fewer bugs, easier maintenance

**Pessimistic Case (Koog insufficient, fallback needed):**
- PHASE 3: 8-10 days
- Overall: 39-41 days
- **Mitigation:** Task 1 identifies this early; plan accordingly

---

## Future Enhancements (v1.1+)

Once Koog integration mature, potential enhancements:

- **Streaming responses:** Use Koog's streaming support for real-time LLM updates
- **Custom tools:** Implement Koog tools for image analysis utilities
- **Agent graph:** Complex workflows using Koog's graph-based agent system
- **Multi-provider support:** Add support for other LLM providers (OpenAI, Claude, etc.) via Koog
- **Fine-tuning:** Use Koog's capability to fine-tune models on user data

---

## References

- [PHASE-3-LLM-INTEGRATION.md](./PHASE-3-LLM-INTEGRATION.md) — Detailed implementation tasks
- [DEPENDENCY-MAP.md](./DEPENDENCY-MAP.md) — Critical path and timeline impact
- [TROUBLESHOOTING.md](./TROUBLESHOOTING.md) — Koog integration issues and solutions

---

## Questions to Discuss

1. Should we start PHASE 3 Task 1 immediately after PHASE 1, or wait for PHASE 1 completion?
   - **Recommendation:** Start early (Day 5-6) as background task; PHASE 1 dev can research while others wrap up

2. What's Koog's maturity level? Any production usage?
   - **Action:** Verify Koog stability before committing to this strategy

3. Should we have a fallback if Koog evaluation is negative?
   - **Answer:** Yes. PHASE 3 Task 1 includes fallback approach already documented

4. Can we prototype Koog + LMStudio in parallel with PHASE 1?
   - **Answer:** Yes. Make it optional prep work to reduce critical path

---

**Decision Date:** 2026-03-08  
**Status:** Approved and integrated into implementation plan  
**Review Date:** After PHASE 3 Task 1 completion
