# Wallpaper Qualifier - Implementation Breakdown

**Version:** 1.0  
**Date:** 2026-03-08  
**Status:** Implementation Guide

---

## Overview

This document provides a detailed step-by-step breakdown for implementing the Wallpaper Qualifier application, a macOS CLI tool that learns from sample wallpapers to automatically evaluate and curate candidate images.

The breakdown organizes implementation into **5 major phases**, each containing specific milestones and deliverables. Each phase builds upon previous phases and respects critical constraints (sequential LLM requests, file isolation, format support).

**Total Implementation Steps:** 42 major tasks across 5 phases

---

## Quick Start: Execution Path

For a developer starting from scratch:

1. **Read this README** for overview and phase summaries
2. **Review PHASE-1-FOUNDATION.md** and execute in sequence
3. **Move to PHASE-2-IMAGE-PROCESSING.md** once foundation complete
4. Continue through PHASE-3, PHASE-4, PHASE-5
5. **Reference the DEPENDENCY-MAP.md** to understand task relationships
6. **Consult @docs/ai-rules/troubleshooting.md** if issues arise during development

---

## Project Context

| Aspect | Value |
|--------|-------|
| **Language** | Kotlin Multiplatform |
| **Target Platform** | macOS (native) |
| **Key Framework** | Kotlin Koog (AI/LLM integration) |
| **Image Processing** | Kotlin Koog + macOS native APIs |
| **LLM Service** | LMStudio API (localhost:1234) |
| **Configuration** | JSON-based |
| **UI** | CLI only |

### Critical Constraints

- **Sequential LLM Requests:** Only one LLM request at a time
- **File Isolation:** All intermediate files in designated temp folder only
- **Format Support:** Input (JPEG, PNG, HEIC, WebP, TIFF, BMP, GIF, RAW); Output (original format)
- **Parallelization:** Max 8 threads for file I/O, image processing; LLM single-threaded
- **Temp Cleanup:** Delete intermediate files immediately after use (not batch)

---

## Phase Summary

### PHASE 1: FOUNDATION (Steps 1-8)
**Focus:** Project structure, build configuration, core utilities

Deliverables:
- Gradle build configuration for Kotlin Multiplatform
- Project module structure
- Basic logging and error handling framework
- Type definitions for domain models
- CLI argument parser skeleton

**Duration:** ~5-7 days | **Effort:** 40-50 person-hours

### PHASE 2: IMAGE PROCESSING (Steps 9-18)
**Focus:** Image format handling, loading, temporary conversion

Deliverables:
- Image format detection and validation
- Loader for all supported formats (8+)
- Temporary JPEG/PNG conversion pipeline
- Parallel I/O operations
- Duplicate detection mechanism

**Duration:** ~8-10 days | **Effort:** 60-80 person-hours

**Blocker Note:** Requires successful Kotlin Koog integration prototype (PHASE-2 Step 1)

### PHASE 3: LLM INTEGRATION (Steps 19-25)
**Focus:** Koog-centric LLM integration with LMStudio (minimal custom code)

Deliverables:
- Evaluate Koog LLM capabilities and LMStudio adapter design
- Koog agent configuration for analysis tasks
- Multimodal image handling via Koog
- Sequential request processing verification
- Prompt template system
- Response parsing and validation
- Error handling leveraging Koog's mechanisms

**Duration:** ~6-8 days | **Effort:** 45-60 person-hours

**Key Principle:** Maximize Koog framework usage; minimize custom code

**Blocker Note:** Custom LMStudio integration is complex; prototype early (PHASE-3 Step 1)

### PHASE 4: CORE LOGIC (Steps 26-36)
**Focus:** Analysis, profile generation, evaluation, curation

Deliverables:
- Sample image analysis workflow
- Profile generation and aggregation
- Candidate evaluation against profile
- Curation and image copying logic
- Duplicate handling in output

**Duration:** ~10-12 days | **Effort:** 75-100 person-hours

**Depends On:** PHASE 1, 2, and 3 complete

### PHASE 5: POLISH & TESTING (Steps 37-42)
**Focus:** Testing, optimization, documentation, release

Deliverables:
- Comprehensive unit test suite
- Integration tests for workflows
- Performance optimization
- User documentation
- Example configurations
- Release package

**Duration:** ~8-10 days | **Effort:** 60-80 person-hours

**Depends On:** PHASE 1-4 complete

---

## Phase Details

### PHASE 1: FOUNDATION
**File:** [PHASE-1-FOUNDATION.md](./PHASE-1-FOUNDATION.md)

**Tasks:**
1. Set up Gradle build configuration (Kotlin Multiplatform, macOS target)
2. Create project directory structure
3. Define domain models (Image, ImageCharacteristics, QualityProfile, etc.)
4. Implement CLI argument parser and help system
5. Create configuration JSON parser with validation
6. Implement logging framework (stdout/stderr handling)
7. Create error handling system (sealed Result types)
8. Set up basic test framework

**Success Criteria:**
- Project builds without errors
- `wallpaper-qualifier --help` displays usage
- CLI parses valid config JSON
- Invalid configs produce clear error messages
- Logger outputs to correct streams

---

### PHASE 2: IMAGE PROCESSING
**File:** [PHASE-2-IMAGE-PROCESSING.md](./PHASE-2-IMAGE-PROCESSING.md)

**Tasks:**
1. Prototype Kotlin Koog image loading (all 8+ formats)
2. Implement image format detection (JPEG, PNG, HEIC, WebP, TIFF, BMP, GIF, RAW)
3. Create ImageLoader for discovering images in folders
4. Implement temporary JPEG/PNG conversion pipeline
5. Create parallel file I/O coordinator (8-thread max)
6. Implement duplicate detection (perceptual hash or checksum)
7. Add image metadata extraction (resolution, color depth, aspect ratio)
8. Create TempFileManager for cleanup
9. Implement error recovery for corrupted images
10. Add progress reporting for image processing

**Success Criteria:**
- All 8+ formats load successfully
- Temp conversion produces valid JPEG/PNG
- Parallel I/O respects 8-thread limit
- Duplicates correctly identified
- Corrupted images gracefully skipped with logging
- Temp files cleaned up immediately after use

---

### PHASE 3: LLM INTEGRATION
**File:** [PHASE-3-LLM-INTEGRATION.md](./PHASE-3-LLM-INTEGRATION.md)

**Strategy:** Maximize Kotlin Koog framework usage with minimal custom code

**Tasks:**
1. Evaluate Koog LLM capabilities and design LMStudio adapter (CRITICAL)
2. Configure Koog agent for LLM analysis tasks
3. Implement multimodal image handling via Koog
4. Ensure sequential request processing with Koog
5. Create prompt templates for analysis and evaluation
6. Implement response parsing and validation
7. Add error handling using Koog's mechanisms

**Success Criteria:**
- Koog integration evaluated and approach documented
- LMStudio works through Koog's agent framework
- Multimodal images processed correctly
- Sequential LLM requests verified
- Custom code minimized (prefer Koog features over custom implementation)
8. Add error handling (network errors, timeouts, invalid responses)
9. Create request/response logging for debugging
10. Implement configuration validation for LLM connection

**Success Criteria:**
- Successfully connects to LMStudio on localhost:1234
- Sends one request at a time (no parallel requests)
- Parses multimodal responses (text + image analysis)
- Handles connection failures gracefully
- Bearer token authentication optional but supported
- Request queue enforces sequential ordering

---

### PHASE 4: CORE LOGIC
**File:** [PHASE-4-CORE-LOGIC.md](./PHASE-4-CORE-LOGIC.md)

**Tasks:**
1. Create SampleAnalysisWorkflow orchestration
2. Implement image characteristic extraction via LLM
3. Build characteristic aggregation and profile generation
4. Create QualityProfile serialization (JSON output)
5. Create CandidateEvaluationWorkflow orchestration
6. Implement scoring/qualification logic against profile
7. Create CurationWorkflow for copying qualified images
8. Implement duplicate detection in output folder
9. Add atomic file operations for safety
10. Create workflow state management (checkpoints for resume)
11. Implement error recovery and partial result preservation

**Success Criteria:**
- Sample analysis produces valid QualityProfile
- Profile is human-readable and reviewable
- Candidates evaluated against profile
- Qualified images copied to output in original format
- No duplicate copies in output
- Partial results preserved if process interrupted

---

### PHASE 5: POLISH & TESTING
**File:** [PHASE-5-POLISH-TESTING.md](./PHASE-5-POLISH-TESTING.md)

**Tasks:**
1. Create unit test suite (60+ tests covering all modules)
2. Create integration tests for workflows
3. Create end-to-end tests with mock LLM
4. Implement performance profiling and optimization
5. Create user documentation (README, examples, troubleshooting)
6. Create configuration examples (minimal, full, advanced)
7. Perform security audit (no credentials in logs, safe temp handling)
8. Create release package (executable, config template)
9. Final testing on real macOS hardware
10. Create release notes and version tagging

**Success Criteria:**
- Test coverage >80% for critical paths
- All workflows pass E2E tests
- Performance baseline documented
- README clear for first-time users
- Example configs work without modification
- No sensitive data in logs or outputs
- Release package ready for distribution

---

## Dependencies Between Phases

```
PHASE 1 (Foundation)
    ↓
┌───────────────────────────────────────┐
│  PHASE 2 (Image Processing)           │
│  PHASE 3 (LLM Integration)            │
└───────────────────────────────────────┘
    ↓ (both must complete)
    ↓
PHASE 4 (Core Logic)
    ↓
PHASE 5 (Polish & Testing)
```

**Parallel Execution:** PHASE 2 and PHASE 3 can be developed in parallel once PHASE 1 is complete (they don't depend on each other).

**Critical Path:**
1. PHASE 1 (blocks everything)
2. PHASE 2 + PHASE 3 (parallel, ~8-10 days each)
3. PHASE 4 (must wait for 2+3, ~10-12 days)
4. PHASE 5 (final polish, ~8-10 days)

**Total Critical Path:** ~30-40 days with adequate parallelization

---

## Detailed Task List

See the following files for complete task breakdowns:

- **[PHASE-1-FOUNDATION.md](./PHASE-1-FOUNDATION.md)** — 8 detailed tasks
- **[PHASE-2-IMAGE-PROCESSING.md](./PHASE-2-IMAGE-PROCESSING.md)** — 10 detailed tasks
- **[PHASE-3-LLM-INTEGRATION.md](./PHASE-3-LLM-INTEGRATION.md)** — 7 detailed tasks
- **[PHASE-4-CORE-LOGIC.md](./PHASE-4-CORE-LOGIC.md)** — 11 detailed tasks
- **[PHASE-5-POLISH-TESTING.md](./PHASE-5-POLISH-TESTING.md)** — 6 detailed tasks

**Supporting Documentation:**
- **[DEPENDENCY-MAP.md](./DEPENDENCY-MAP.md)** — Task dependencies and parallel execution strategy
- **[@docs/ai-rules/troubleshooting.md](../ai-rules/troubleshooting.md)** — Common issues and decision-making guidance
- **[KOOG-STRATEGY.md](./KOOG-STRATEGY.md)** — Strategic decision for Koog-centric LLM integration

---

## Constraint Verification Checklist

Before each phase, verify these critical constraints:

### Sequential LLM Requests
- [ ] Request queue implementation allows only one in-flight request
- [ ] Tests verify sequential ordering (no parallel sends)
- [ ] Configuration validates LLM endpoint accessibility

### File Isolation
- [ ] Temp folder path required in configuration
- [ ] No file operations outside temp and output folders
- [ ] All temp files cleaned up immediately after use
- [ ] Temp folder initialized and validated at startup

### Format Support
- [ ] All 8+ formats (JPEG, PNG, HEIC, WebP, TIFF, BMP, GIF, RAW) load successfully
- [ ] Temporary conversion produces valid JPEG/PNG
- [ ] Output copies preserve original format
- [ ] Unsupported formats handled gracefully

### Parallelization
- [ ] File I/O operations use thread pool with 8-thread max
- [ ] LLM requests use single thread (queue-based)
- [ ] Image processing (decode, convert) parallelized
- [ ] Batch size limited to 100 images for memory management

### Configuration-Driven
- [ ] All parameters from JSON config (no hardcoding)
- [ ] Configuration schema documented
- [ ] Invalid configs produce clear error messages
- [ ] Sensible defaults provided where applicable

---

## File Organization

```
docs/implementation-breakdown/
├── README.md (this file)
├── PHASE-1-FOUNDATION.md
├── PHASE-2-IMAGE-PROCESSING.md
├── PHASE-3-LLM-INTEGRATION.md
├── PHASE-4-CORE-LOGIC.md
├── PHASE-5-POLISH-TESTING.md
├── DEPENDENCY-MAP.md
├── TROUBLESHOOTING.md
├── ARCHITECTURE-DETAILS.md
├── CONFIG-SCHEMA.md
└── TESTING-STRATEGY.md
```

---

## Success Metrics

### MVP Completion
- ✅ Application builds and runs on macOS
- ✅ Accepts valid config JSON
- ✅ Analyzes sample images via LLM
- ✅ Generates quality profile
- ✅ Evaluates candidates against profile
- ✅ Copies qualified images to output folder

### Quality Metrics
- ✅ Test coverage >80% for critical paths
- ✅ No crashes on invalid input (graceful errors)
- ✅ Performance: Process 100 images in <10 minutes (LLM-dependent)
- ✅ Memory usage <2 GB for 1000-image batch
- ✅ All temp files cleaned up after completion

### Documentation Metrics
- ✅ User-facing README clear for first-time users
- ✅ Example configurations provided (minimal, full, advanced)
- ✅ API documentation for internal modules
- ✅ Troubleshooting guide covers common issues

---

## Next Steps

1. **Start with PHASE 1:** Review [PHASE-1-FOUNDATION.md](./PHASE-1-FOUNDATION.md)
2. **Set up development environment:** Java 17+, Kotlin 1.9+, Gradle 8.0+
3. **Prototype LMStudio integration:** Complete PHASE 3 Step 1 early (critical blocker)
4. **Establish Git workflow:** Commit strategy aligned with phases
5. **Plan parallel execution:** PHASE 2 and 3 can start simultaneously after PHASE 1

---

## Contact & Escalation

If you encounter:

- **Kotlin Koog integration issues:** See @docs/ai-rules/troubleshooting.md → LMStudio Integration
- **Image format problems:** See @docs/ai-rules/troubleshooting.md → Image Processing
- **Architecture clarifications:** See ARCHITECTURE-DETAILS.md
- **Configuration questions:** See CONFIG-SCHEMA.md

---

**Document Version:** 1.0  
**Last Updated:** 2026-03-08  
**Status:** Ready for Implementation
