# PHASE 4: CORE LOGIC

**Duration:** ~10-12 days  
**Effort:** 75-100 person-hours  
**Tasks:** 11 major deliverables (each includes tests)  
**Status:** Ready to execute after PHASE 2 & 3

**Dependency:** PHASE 1, PHASE 2, and PHASE 3 must be complete

---

## Overview

Phase 4 implements the core business logic: sample analysis workflow, profile generation, candidate evaluation, and curation. This phase orchestrates PHASE 2 (image processing) and PHASE 3 (LLM integration) into the complete application workflow.

**Testing Strategy (PHASE 4):**
- Each task includes unit and integration tests written alongside code
- Use FakeLLMClient and mock image loader for tests
- Tests executed and fixed immediately as code is written
- Focus on workflow orchestration, profile accuracy, and file safety
- By phase end: Complete workflows tested end-to-end before PHASE 5

**Critical Success Factors:**
1. Workflows correctly orchestrate image and LLM components
2. Profile generation accurately aggregates characteristics
3. Curation safely copies qualified images

---

## Task 1: Sample Analysis Workflow Orchestration

**Objective:** Implement workflow for analyzing sample images

**Deliverables:**
- Orchestrator that coordinates:
  1. Load sample images (PHASE 2)
  2. Convert to JPEG/PNG for LLM (PHASE 2)
  3. Send to LLM for analysis (PHASE 3)
  4. Extract characteristics
  5. Store results
- Progress tracking and error collection
- State checkpointing (resume on failure)

**Workflow Steps:**
```
1. Discover sample images in folder
2. For each image (batch of 100):
   a. Load image and extract metadata
   b. Convert to JPEG/PNG (temp)
   c. Send to LLM with analysis prompt
   d. Parse characteristics response
   e. Store characteristics
3. Return list of all characteristics
```

**Success Criteria:**
- All sample images analyzed
- Characteristics extracted for each
- Progress clearly reported
- Partial results preserved if interrupted
- State can be checkpointed and resumed

**Tests for This Task:**
- Create `src/commonTest/kotlin/com/wallpaperqualifier/workflow/SampleAnalysisWorkflowTest.kt`
- Test: Sample images are discovered and loaded
- Test: Each image is sent to LLM for analysis
- Test: Characteristics extracted from responses
- Test: Progress tracking works correctly
- Run: `./gradlew test` — all tests must pass

**Implementation Notes:**

## Task 2: Image Characteristic Extraction

**Objective:** Parse and validate LLM analysis responses

**Deliverables:**
- Extraction of `ImageCharacteristics` from LLM response
- Validation of required fields
- Type conversion (strings to enums where applicable)
- Storage format (JSON serializable)
- Metadata: timestamp, source image path, LLM model used

**Success Criteria:**
- Characteristics extracted reliably
- Validation catches malformed responses
- Format consistent across samples
- Searchable and aggregatable

**Tests for This Task:**
- Create `src/commonTest/kotlin/com/wallpaperqualifier/workflow/CharacteristicExtractionTest.kt`
- Test: Valid LLM response extracted correctly
- Test: Malformed response caught and error returned
- Test: Format consistent across different responses
- Run: `./gradlew test` — all tests must pass

---

## Task 3: Quality Profile Generation and Aggregation

**Objective:** Compile characteristics from all samples into profile

**Deliverables:**
- Aggregation algorithm:
  - Merge color palettes (most common colors, frequency)
  - Merge styles (consensus styles)
  - Merge moods (most frequent moods)
  - Average quality scores
- Profile includes statistics (mean, frequency, distribution)
- JSON serialization for storage and review
- Profile metadata (sample count, generated date, version)

**Aggregation Strategy:**
```
For Color Palettes:
  1. Collect all colors from all samples
  2. Group similar colors
  3. Sort by frequency
  4. Return top 10 colors with frequencies

For Styles:
  1. Collect all styles
  2. Count occurrences
  3. Return most common 5 styles

For Mood:
  1. Average mood scores
  2. Return weighted moods

For Quality:
  1. Calculate average quality score
  2. Store distribution (median, stdev)
```

**Success Criteria:**
- Profile accurately represents sample preferences
- Human-readable (not just numbers)
- Can be reviewed and edited manually
- Serializes to JSON correctly

**Tests for This Task:**
- Create `src/commonTest/kotlin/com/wallpaperqualifier/profile/ProfileGeneratorTest.kt`
- Test: Profile generated from sample characteristics
- Test: Color palette aggregation works correctly
- Test: Style aggregation accurate
- Test: Quality scores calculated correctly
- Run: `./gradlew test` — all tests must pass

**Implementation Notes:**

## Task 4: Quality Profile Serialization and Storage

**Objective:** Save profile to JSON file for review and reuse

**Deliverables:**
- JSON serialization of `QualityProfile`
- Pretty-printed output (human-readable)
- Optional: save to file configured in config
- Optional: allow loading existing profile (skip sample analysis)
- Versioning (schema version in profile)

**JSON Output Example:**
```json
{
  "version": "1.0",
  "generatedAt": "2026-03-08T19:15:00Z",
  "sampleCount": 12,
  "preferredColorPalettes": [
    { "color": "#2E5090", "frequency": 0.92 },
    { "color": "#A8D5BA", "frequency": 0.75 }
  ],
  "preferredStyles": [
    { "style": "minimalist", "frequency": 0.83 },
    { "style": "photography", "frequency": 0.67 }
  ],
  "preferredMoods": ["calming", "serene"],
  "averageQuality": 0.87
}
```

**Success Criteria:**
- Profile saved to file if configured
- JSON valid and parseable
- Profile can be reloaded and reused
- Version compatibility checking

**Tests for This Task:**
- Create `src/commonTest/kotlin/com/wallpaperqualifier/profile/ProfileSerializationTest.kt`
- Test: Profile serialized to JSON correctly
- Test: JSON can be parsed back to profile
- Test: Human-readable format verified
- Run: `./gradlew test` — all tests must pass

---

## Task 5: Candidate Evaluation Workflow Orchestration

**Objective:** Implement workflow for evaluating candidate images

**Deliverables:**
- Orchestrator that coordinates:
  1. Load candidate images (PHASE 2)
  2. Convert to JPEG/PNG for LLM (PHASE 2)
  3. Send to LLM for evaluation with profile context (PHASE 3)
  4. Parse qualification results
  5. Track qualified vs rejected
- Progress tracking
- Summary statistics (X qualified, Y rejected, Z errors)

**Workflow Steps:**
```
1. Load quality profile (generated or from file)
2. Discover candidate images in folder
3. For each image (batch of 100):
   a. Load image and extract metadata
   b. Convert to JPEG/PNG (temp)
   c. Send to LLM with evaluation prompt (profile context included)
   d. Parse evaluation result (qualified?, confidence, reasoning)
   e. Store result
4. Separate qualified and rejected images
5. Return results and summary
```

**Success Criteria:**
- All candidates evaluated
- Results include confidence scores
- Progress clearly reported
- Summary statistics calculated
- Partial results preserved if interrupted

**Tests for This Task:**
- Create `src/commonTest/kotlin/com/wallpaperqualifier/workflow/CandidateEvaluationWorkflowTest.kt`
- Test: Candidate images are discovered and loaded
- Test: Each image is evaluated with LLM
- Test: Results include confidence scores
- Test: Summary statistics calculated correctly
- Run: `./gradlew test` — all tests must pass

---

## Task 6: Evaluation Scoring and Qualification Logic

**Objective:** Determine if candidate image qualifies

**Deliverables:**
- Binary classification: GOOD (qualified) or POOR (rejected)
- Confidence score (0-100% or 0-1)
- Scoring based on LLM evaluation and profile match
- Optional: threshold configuration (minimum score to qualify)
- Reasoning captured for user review

**Scoring Strategy:**
```
For each candidate:
  1. LLM evaluates against profile
  2. Returns: qualified (bool), confidence (0-100), reasoning (text)
  3. Optional: threshold check (confidence >= threshold to include)
  4. Store: image path, result, confidence, reasoning
```

**Success Criteria:**
- Qualification decisions accurate
- Confidence scores correlate with quality
- Threshold configurable
- Reasoning helps user understand decisions

**Tests for This Task:**
- Create `src/commonTest/kotlin/com/wallpaperqualifier/workflow/EvaluationScoringTest.kt`
- Test: High-confidence matches scored correctly
- Test: Low-confidence mismatches scored correctly
- Test: Threshold logic applied properly
- Test: Reasoning captured and retrievable
- Run: `./gradlew test` — all tests must pass

---

## Task 7: Curation and Image Copying Workflow

**Objective:** Copy qualified images to output folder

**Deliverables:**
- Safe, atomic copying of qualified images
- Preserve original format (no re-encoding)
- Preserve best quality
- Filename handling (original or with metadata)
- Progress reporting
- Error recovery (don't fail if one copy fails)

**Critical Constraints:**
- **Original files never modified**
- **Output folder is destination**
- **Atomic copy operations**

**Copy Strategy:**
```
For each qualified image:
  1. Generate unique output filename (avoid overwrite)
  2. Copy file to output folder (atomic: write to temp, then move)
  3. On success: record copied
  4. On failure: log error, continue with next
  5. Report summary: X copied, Y skipped/failed
```

**Success Criteria:**
- All qualified images copied to output
- Original format preserved
- No corrupted copies
- Atomic operations (no partial files)
- Summary report shows results

**Tests for This Task:**
- Create `src/commonTest/kotlin/com/wallpaperqualifier/workflow/CurationWorkflowTest.kt`
- Test: Qualified image copied to output folder
- Test: Original format preserved
- Test: Copy is atomic (no partial files)
- Test: Failures in one copy don't stop others
- Test: Summary report generated accurately
- Run: `./gradlew test` — all tests must pass

**Implementation Notes:**

## Task 8: Duplicate Detection in Output Folder

**Objective:** Prevent copying duplicate or redundant images to output

**Deliverables:**
- Check if image already exists in output folder
- Comparison: file hash or image hash
- Options: skip duplicate, replace, or append version number
- Configurable strategy (replace/skip/versioning)

**Duplicate Handling Strategies:**
1. **Skip:** Don't copy if image already in output (default)
2. **Replace:** Overwrite existing file with new version
3. **Version:** Save as `filename_v2.jpg`, `filename_v3.jpg`, etc.

**Success Criteria:**
- Duplicates detected reliably
- Duplicate handling strategy applies correctly
- No unintended overwrites
- User informed of duplicates skipped

**Tests for This Task:**
- Create `src/commonTest/kotlin/com/wallpaperqualifier/workflow/DuplicateDetectionTest.kt`
- Test: Identical files detected as duplicates
- Test: Duplicate skipped without overwrite
- Test: User is notified of skipped duplicates
- Run: `./gradlew test` — all tests must pass

---

## Task 9: Atomic File Operations for Safety

**Objective:** Ensure file operations never leave partial/corrupt files

**Deliverables:**
- Atomic copy: write to temp file, then move to destination
- Rollback on failure (delete temp if move fails)
- Atomic delete operations
- Error logging if operations fail

**Implementation Pattern:**
(Remove pseudo-code here)

**Tests for This Task:**
- Create `src/commonTest/kotlin/com/wallpaperqualifier/workflow/AtomicFileOpsTest.kt`
- Test: Atomic copy succeeds with file in destination
- Test: Temp file deleted if copy fails
- Test: No partial files left on failure
- Run: `./gradlew test` — all tests must pass

---

## Task 10: Workflow State Management and Checkpointing

**Objective:** Enable resuming interrupted workflows

**Deliverables:**
- Checkpoint system: save progress periodically
- State file format (JSON)
- Resume capability: load state, continue from checkpoint
- Optional feature (not required for v1, but infrastructure ready)

**Checkpoint Data:**
```json
{
  "workflow": "sample_analysis",
  "startTime": "2026-03-08T19:15:00Z",
  "phase": "analysis",
  "processedImages": 42,
  "totalImages": 100,
  "lastProcessedImage": "/path/to/image42.jpg",
  "characteristics": [/* ... */]
}
```

**Success Criteria:**
- Checkpoints saved after each batch
- State file valid JSON
- Resume from checkpoint works
- No duplicate processing if resumed

**Tests for This Task:**
- Create `src/commonTest/kotlin/com/wallpaperqualifier/workflow/CheckpointingTest.kt`
- Test: Checkpoint saved after batch
- Test: Checkpoint can be loaded
- Test: Workflow resumes from checkpoint correctly
- Test: No duplicate processing on resume
- Run: `./gradlew test` — all tests must pass

---

## Task 11: Error Recovery and Partial Result Preservation

**Objective:** Handle workflow failures gracefully

**Deliverables:**
- Collect errors without stopping workflow
- Preserve partial results (continue with next image)
- Summary report: X successful, Y errors, Z warnings
- Detailed error log (file, error type, message)
- Graceful shutdown on critical errors

**Error Collection:**
```
During workflow:
  - Per-image errors collected
  - Workflow continues with next image
  - Final summary: "Processed 95 images, 5 errors"
  - Error details logged to file
```

**Success Criteria:**
- Workflow doesn't crash on image-level errors
- Partial results usable
- Errors clearly reported to user
- Error log available for debugging

**Tests for This Task:**
- Create `src/commonTest/kotlin/com/wallpaperqualifier/workflow/ErrorRecoveryTest.kt`
- Test: Error in one image doesn't crash workflow
- Test: Partial results preserved on error
- Test: Error summary generated correctly
- Test: Error log written with details
- Run: `./gradlew test` — all tests must pass

---

## Integration with Previous Phases

- **PHASE 1:** Uses Logger, Result types, Config, CLI
- **PHASE 2:** Calls ImageLoader, ImageConverter, DuplicateDetector
- **PHASE 3:** Calls LLMRequestQueue, ResponseParser, LLMErrorHandler
- **PHASE 4:** Orchestrates all components into workflows

---

## Completion Checklist

Before moving to PHASE 5, verify:

- [ ] Sample analysis workflow complete
- [ ] Profile generation accurate
- [ ] Profile serializable to JSON
- [ ] Candidate evaluation workflow complete
- [ ] Qualification logic working
- [ ] Curation workflow copying images
- [ ] Duplicates detected and handled
- [ ] File operations atomic
- [ ] State checkpointing functional
- [ ] Partial results preserved on errors
- [ ] All workflows tested with real data

---

## Risk Mitigation

| Risk | Mitigation |
|------|-----------|
| Workflow coordination complexity | Start simple; add checkpointing in PHASE 5 |
| Profile generation inaccurate | Test with diverse sample sets; validate output |
| Image copying fails silently | Atomic operations + detailed error logging |
| Duplicate detection too strict | Test with real data; adjust thresholds |
| State checkpointing buggy | Test resume functionality thoroughly |

---

## Phase 4 Complete Indicators

✅ **Phase 4 is complete when:**
1. All 11 tasks delivered
2. Full workflow pipeline works end-to-end
3. Sample analysis → Profile generation works
4. Candidate evaluation → Curation works
5. Partial results preserved on failures
6. Ready for testing and optimization (PHASE 5)

---

**Next:** Proceed to [PHASE-5-POLISH-TESTING.md](./PHASE-5-POLISH-TESTING.md) once PHASE 4 complete.
