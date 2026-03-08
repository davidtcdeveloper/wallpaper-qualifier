# DEPENDENCY MAP

**Visual guide to task dependencies and parallel execution opportunities**

---

## Task Dependency Graph

```
PHASE 1: Foundation
├── Task 1: Gradle Build Config
│   └─ Required by: All other tasks
├── Task 2: Directory Structure
│   └─ Required by: All code tasks
├── Task 3: Domain Models
│   └─ Required by: PHASE 2, 3, 4
├── Task 4: CLI Parser
│   └─ Required by: PHASE 4 (main orchestration)
├── Task 5: Config Parser
│   └─ Required by: PHASE 3 (LLM config), PHASE 4 (workflow config)
├── Task 6: Logging Framework
│   └─ Required by: All modules (cross-cutting)
├── Task 7: Error Handling
│   └─ Required by: All modules (cross-cutting)
└── Task 8: Test Framework Setup
    └─ Required by: PHASE 2, 3, 4, 5

         ↓ (All PHASE 1 must complete)
         ↓
    ┌────────────────────────────────────────────┐
    │  Parallel Execution: PHASE 2 + PHASE 3    │
    └────────────────────────────────────────────┘

PHASE 2: Image Processing        PHASE 3: LLM Integration (Koog-Centric)
├── Task 1: Koog Prototype ◄─────── NO DEPENDENCY
├── Task 2: Format Detection       ├── Task 1: Koog Evaluation ◄─ CRITICAL
├── Task 3: Image Loader           │   (Design LMStudio adapter for Koog)
├── Task 4: Format Converter       ├── Task 2: Koog Agent Config
├── Task 5: Parallel I/O           ├── Task 3: Multimodal Images (Koog)
├── Task 6: Duplicate Detection    ├── Task 4: Sequential Processing (Koog)
├── Task 7: Metadata Extraction    ├── Task 5: Prompt Templates
├── Task 8: Temp Manager           ├── Task 6: Response Parser
├── Task 9: Error Recovery         └── Task 7: Error Handling (Koog)
└── Task 10: Progress Reporting

    PHASE 2 & 3 must complete before PHASE 4
         ↓
         ↓
PHASE 4: Core Logic
├── Task 1: Sample Analysis Orchestration ◄─ Requires PHASE 2 + 3
├── Task 2: Characteristic Extraction ◄─ Requires PHASE 3
├── Task 3: Profile Generation ◄─ Requires Task 2
├── Task 4: Profile Serialization ◄─ Requires Task 3
├── Task 5: Candidate Evaluation Orchestration ◄─ Requires PHASE 2 + 3
├── Task 6: Scoring Logic ◄─ Requires PHASE 3
├── Task 7: Curation Workflow ◄─ Requires PHASE 2
├── Task 8: Duplicate Detection in Output ◄─ Requires PHASE 2, Task 7
├── Task 9: Atomic File Ops ◄─ Requires Task 7
├── Task 10: State Management ◄─ Optional, non-blocking
└── Task 11: Error Recovery ◄─ Requires Task 10

    PHASE 4 must complete before PHASE 5
         ↓
         ↓
PHASE 5: Polish & Testing
├── Task 1: Unit Tests ◄─ Requires all PHASE 1-4
├── Task 2: Integration Tests ◄─ Requires PHASE 4
├── Task 3: E2E Tests ◄─ Requires PHASE 4
├── Task 4: Performance Profiling ◄─ Requires PHASE 4
├── Task 5: User Documentation ◄─ Requires PHASE 4
└── Task 6: Release Package ◄─ Requires PHASE 5 Tasks 1-5
```

---

## Critical Path Analysis

**Longest dependency chain (slowest possible execution):**

```
PHASE 1 (7 days)
  → PHASE 2 Task 1: Koog Prototype (3 days) [blocks Tasks 2-10]
  → PHASE 2 Tasks 2-10 (5 days)
  → PHASE 3 Task 1: LMStudio Prototype (3 days) [blocks Tasks 2-7]
  → PHASE 3 Tasks 2-7 (5 days)
  → PHASE 4 (12 days)
  → PHASE 5 (10 days)

Total Sequential: ~45 days (without parallelization)
```

**Critical path with parallelization:**

```
PHASE 1 (7 days) [BLOCKER]
  ↓
PHASE 2 + PHASE 3 in parallel (10 days max: max(PHASE 2, PHASE 3))
  - PHASE 2: 10 days (Task 1: 3 days, Tasks 2-10: 7 days parallel)
  - PHASE 3: 7-8 days (Task 1 Koog evaluation: 2-3 days, Tasks 2-7: 4-5 days)
    [REDUCED from 8 days due to Koog-first strategy: less custom code]
  - Parallel execution: 10 days (both can start after PHASE 1)
  ↓
PHASE 4 (12 days)
  ↓
PHASE 5 (10 days)

Total with Parallelization: ~39 days (7+10+12+10)
Potential with Koog simplification: ~37-38 days (if PHASE 3 completes in 7 days)
```

---

## Koog-Centric Strategy Impact

**PHASE 3 LLM Integration (Koog-Focused):**

- **Minimize Custom Code:** Leverage Koog's built-in capabilities for request management, error handling, multimodal support
- **Task 1 (Critical):** Evaluate Koog integration options (2-3 days). This determines implementation approach for Tasks 2-7
- **Tasks 2-7:** Follow Koog patterns; much less custom code than traditional HTTP client approach
- **Result:** Reduced custom code from ~1000 LOC (custom HTTP + queue) to ~300-500 LOC (Koog adapter only)
- **Timeline Benefit:** Faster development, fewer bugs, easier maintenance

**Expected Impact:**
- Task 1 (Koog evaluation): 2-3 days → discover best Koog integration path
- Tasks 2-7: Simplified by using Koog features → potentially 4-5 days vs. 5-6 days
- **Potential PHASE 3 completion:** 7 days (vs. original 8 days estimate)
- **Overall timeline savings:** 1-2 days possible

---

## Parallel Execution Opportunities

### PHASE 1 → No Parallelization
All tasks must complete sequentially (they are all blockers for downstream work).

**Rationale:** Project structure and core utilities are prerequisites.

### PHASE 2 ↔ PHASE 3 → **MAXIMUM PARALLELIZATION**
Both phases can execute simultaneously after PHASE 1.

**Why:** 
- PHASE 2 (image processing) independent of PHASE 3 (LLM integration)
- No data dependencies between them
- Both feed into PHASE 4 (core logic)

**Execution Strategy:**
```
Day 1-7: PHASE 1 (sequential)
Day 8-17: PHASE 2 + PHASE 3 in parallel
  - Developer A: PHASE 2 (image processing)
  - Developer B: PHASE 3 (LLM integration)
  - Both complete by Day 17
Day 18-29: PHASE 4 (core logic)
  - Integrate results from PHASE 2 + 3
Day 30-39: PHASE 5 (testing, polish)
```

**Resource Allocation:**
- **PHASE 1:** 1-2 developers (2 weeks)
- **PHASE 2:** 1 developer (2 weeks)
- **PHASE 3:** 1 developer (1.5-2 weeks with Koog-centric approach) [can overlap PHASE 2]
- **PHASE 4:** 2 developers (3 weeks)
- **PHASE 5:** 2 developers (2-3 weeks)

### PHASE 4 → Limited Parallelization
Tasks 1-5 can start early (framework setup), but most depend on PHASE 2+3.

**Early Starters:**
- Task 10: State Management (doesn't depend on anything, can start after PHASE 1)
- Task 11: Error Recovery (can start after Task 10)

### PHASE 5 → No Parallelization
Must wait for PHASE 4 complete, but testing tasks can run in parallel.

**Parallelization:**
- Unit tests and integration tests can run simultaneously
- Performance profiling can run while E2E tests run
- Documentation can be written in parallel with testing

---

## Blocking Dependencies (Do Not Parallelize)

### CRITICAL BLOCKERS

1. **PHASE 1 → Everything**
   - Must complete fully before PHASE 2/3 start
   - Cannot parallelize any PHASE 1 tasks

2. **PHASE 2 & 3 → PHASE 4**
   - PHASE 4 requires results from both PHASE 2 and PHASE 3
   - Cannot start PHASE 4 until both PHASE 2 and 3 complete

3. **PHASE 4 → PHASE 5**
   - PHASE 5 testing depends on completed PHASE 4 code
   - Cannot parallelize PHASE 4 and PHASE 5

### Task-Level Blockers

**PHASE 2:**
- Task 1 (Koog prototype) blocks Tasks 2-10 (blocks 70% of phase)
- Task 3 (Image loader) blocks Tasks 6-10
- Task 5 (Parallel I/O) blocks Task 8 (Temp manager)

**PHASE 3:**
- Task 1 (LMStudio prototype) blocks Tasks 2-7 (blocks 85% of phase)
- Task 2 (HTTP client) blocks Tasks 3, 4, 6, 7
- Task 3 (Request queue) blocks Task 6 (response parser depends on queue)

**PHASE 4:**
- Task 1 (Sample analysis) blocks Task 2
- Task 2 blocks Task 3
- Task 3 blocks Task 4
- Task 5 (Candidate evaluation) blocks Task 6
- Task 6 blocks Task 8
- Task 7 blocks Tasks 8, 9

---

## Optimization Strategies

### Reduce PHASE 2 Critical Path
- **Task 1 (Koog prototype):** Can be started in PHASE 1 as research task
- **Parallel Tasks 2-10:** After Task 1, don't block on each other
  - Task 2 (format detection) doesn't need Task 3 (image loader)
  - Task 7 (metadata) doesn't need Task 5 (parallel I/O)

**Estimated Reduction:** 2-3 days if research starts early

### Reduce PHASE 3 Critical Path
- **Task 1 (LMStudio prototype):** Can be started in PHASE 1 as research task
- **Parallel Tasks 2-7:** After Task 1, most don't block on each other
  - Task 5 (prompt templates) independent of Task 2-4
  - Task 4 (image encoding) independent of Task 3 (request queue)

**Estimated Reduction:** 2-3 days if research starts early

### Reduce PHASE 4 Critical Path
- **Implement Tasks 10-11 early:** No dependencies, can start after PHASE 1
- **Parallelize Task 1 + 5:** Sample and candidate workflows similar, can code in parallel

**Estimated Reduction:** 1-2 days

---

## Recommended Team Structure

### Option 1: Serial Development (1 developer)
- **Timeline:** ~39-45 days
- **Cost:** Low
- **Risk:** Low (one person knows entire system)

**Execution:**
```
Days 1-7:   PHASE 1
Days 8-17:  PHASE 2
Days 18-25: PHASE 3
Days 26-37: PHASE 4
Days 38-47: PHASE 5
```

### Option 2: Parallel Development (3 developers)
- **Timeline:** ~25-30 days
- **Cost:** Medium
- **Risk:** Medium (coordination overhead)

**Execution:**
```
Days 1-7:   PHASE 1 (Dev A leads)
Days 8-17:  PHASE 2 (Dev B) + PHASE 3 (Dev C)
Days 18-29: PHASE 4 (Dev A + B)
Days 30-39: PHASE 5 (Dev A + B + C)
```

### Option 3: Aggressive Parallel (4+ developers)
- **Timeline:** ~20-25 days
- **Cost:** High
- **Risk:** High (complex coordination)

**Execution:**
```
Days 1-7:   PHASE 1 (Dev A)
Days 1-7:   PHASE 1 research: Koog prototype (Dev B), LMStudio prototype (Dev C)
Days 8-17:  PHASE 2 (Dev B) + PHASE 3 (Dev C)
Days 18-29: PHASE 4 (Dev A + Dev D)
Days 30-39: PHASE 5 (All devs)
```

---

## Risk Mitigation Timing

| Risk | Identified | Mitigated | Critical |
|------|------------|-----------|----------|
| Koog insufficient | PHASE 1 | PHASE 2 Task 1 | YES |
| LMStudio integration | PHASE 1 | PHASE 3 Task 1 | YES |
| Image format support | PHASE 2 Task 1 | PHASE 2 | YES |
| Performance issues | PHASE 4 | PHASE 5 Task 4 | NO |
| Documentation gaps | PHASE 5 Task 5 | PHASE 5 | NO |

**Recommendation:** Start Koog and LMStudio prototypes in PHASE 1 or as prep work to reduce risk.

---

## Next Steps

1. **Review this dependency map** with team
2. **Choose team structure** (serial, parallel, or aggressive)
3. **Assign critical path owners** (Koog prototype, LMStudio prototype)
4. **Create project timeline** based on chosen structure
5. **Monitor critical path** throughout execution
