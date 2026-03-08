# PHASE 1: FOUNDATION

**Duration:** ~5-7 days  
**Effort:** 40-50 person-hours  
**Tasks:** 8 major deliverables (each includes tests)  
**Status:** Ready to execute

---

## Overview

Phase 1 establishes the project foundation: build configuration, project structure, core domain models, and essential infrastructure. This phase has no external dependencies and must complete before PHASE 2 and PHASE 3 can begin.

**Testing Strategy (PHASE 1):**
- Each task includes unit tests written alongside code
- Tests executed and fixed immediately as code is written
- Focus on module interfaces and basic contracts (not comprehensive coverage)
- By phase end: Foundation modules are tested and stable

**Critical Success Factor:** Complete this phase fully before moving to PHASE 2 or PHASE 3. A solid foundation prevents rework later.

---

## Task 1: Gradle Build Configuration

**Objective:** Set up Kotlin Multiplatform build system targeting macOS

**Deliverables:**
- `build.gradle.kts` configured for KMP
- macOS target (x86_64 and arm64 support)
- Dependencies: kotlinx-coroutines, kotlinx-serialization, Kotlin Koog
- Test framework: Kotlin Test + JUnit 5
- Linting: ktlint configured

**Success Criteria:**
- `./gradlew build` completes successfully
- `./gradlew test` runs without errors (even if no tests exist yet)
- `./gradlew ktlint` checks code style
- All dependencies resolve correctly

**Tests for This Task:**
- Create `src/commonTest/kotlin/com/wallpaperqualifier/BuildConfigTest.kt`
- Test: Gradle can build and run tests
- Test: Kotlin version is correct
- Test: Test framework can execute a simple test

**Implementation Notes:**
- Use Kotlin 1.9.x or later
- Gradle 8.0+ required
- macOS target: Use `macosX64` and `macosArm64` (or unified `macosNative`)
- Include dependency versions:
  - `org.jetbrains.kotlin:kotlin-multiplatform` (latest stable)
  - `org.jetbrains.kotlinx:kotlinx-coroutines-core` (latest stable)
  - `org.jetbrains.kotlinx:kotlinx-serialization-json` (latest stable)
  - `com.aallam.koog:koog-core` (check availability for KMP)
  - `junit:junit` + `org.junit.jupiter:junit-jupiter` for testing

**Risks:**
- Kotlin Koog may not have stable KMP support; prototype early
- macOS-specific dependencies might require native bindings

---

## Task 2: Project Directory Structure

**Objective:** Create organized module and package structure

**Deliverables:**
- Directory hierarchy reflecting modules
- `src/commonMain/kotlin/com/wallpaperqualifier/{module}` structure
- macOS-specific code in `src/macosMain/`
- Test structure mirroring source

**Expected Structure:**
```
wallpaper-qualifier/
├── src/
│   ├── commonMain/
│   │   ├── kotlin/
│   │   │   └── com/wallpaperqualifier/
│   │   │       ├── cli/              # CLI handling
│   │   │       ├── config/           # Configuration parsing
│   │   │       ├── domain/           # Domain models
│   │   │       ├── image/            # Image processing
│   │   │       ├── llm/              # LLM client
│   │   │       ├── profile/          # Profile generation
│   │   │       ├── workflow/         # Workflows
│   │   │       ├── utils/            # Shared utilities
│   │   │       └── App.kt            # Main entry point
│   │   └── resources/
│   ├── commonTest/
│   │   └── kotlin/com/wallpaperqualifier/{mirrors above}
│   └── macosMain/
│       └── kotlin/com/wallpaperqualifier/
│           └── platform/            # macOS-specific implementations
├── build.gradle.kts
├── settings.gradle.kts
├── docs/
│   ├── specs/
│   └── implementation-breakdown/
├── README.md
└── LICENSE
```

**Success Criteria:**
- All directories created
- Package structure follows Kotlin conventions
- IDE recognizes module structure correctly

---

## Task 3: Domain Models

**Objective:** Define core data structures

**Deliverables:**
- `Image` — represents a loaded image with metadata
- `ImageCharacteristics` — extracted properties from LLM analysis
- `QualityProfile` — aggregated user preferences
- `EvaluationResult` — qualification result for a candidate
- `Config` — application configuration
- Error and Result types

**Key Models:**

```kotlin
// Domain models to implement
data class Image(
    val path: String,
    val format: ImageFormat,  // JPEG, PNG, HEIC, etc.
    val width: Int,
    val height: Int,
    val fileSize: Long,
    val aspectRatio: Float
)

data class ImageCharacteristics(
    val colorPalette: List<String>,
    val style: String,
    val mood: String,
    val composition: String,
    val subject: String,
    val technicalNotes: String,
    val quality: Float,  // 0-1 confidence
    val extractedAt: Long
)

data class QualityProfile(
    val preferredColorPalettes: List<String>,
    val preferredStyles: List<String>,
    val preferredMoods: List<String>,
    val preferredCompositions: List<String>,
    val commonSubjects: List<String>,
    val resolutionPreferences: ResolutionRange,
    val averageQuality: Float,
    val generatedAt: Long,
    val sampleCount: Int
)

data class EvaluationResult(
    val imagePath: String,
    val qualified: Boolean,
    val confidenceScore: Float,
    val reasoning: String
)

sealed class Result<T> {
    data class Success<T>(val value: T) : Result<T>()
    data class Failure<T>(val error: Throwable) : Result<T>()
}
```

**Success Criteria:**
- All models defined and compile
- Data classes support serialization
- Result type handles both success and failure

**Implementation Notes:**
- Use `@Serializable` annotations for JSON support
- Models immutable (data classes with `val`)
- Include factory functions for creation

---

## Task 4: CLI Argument Parser

**Objective:** Parse command-line arguments and display help

**Deliverables:**
- Argument parser for `config.json` file path
- `--help` flag displaying usage
- `--version` flag showing version
- Validation that config file exists
- Error messages for missing or invalid arguments

**Expected Usage:**
```bash
wallpaper-qualifier config.json
wallpaper-qualifier --help
wallpaper-qualifier --version
```

**Success Criteria:**
- Help text displays with `--help`
- Config file path required and validated
- Version flag shows current version
- Clear error messages for invalid arguments

**Implementation Notes:**
- Keep simple (no heavy argument library unless necessary)
- Parse directly in `main()` or small utility function
- Validate config file exists; don't parse yet (defer to Config task)

---

## Task 5: Configuration JSON Parser

**Objective:** Load, parse, and validate configuration file

**Deliverables:**
- JSON configuration parser
- Validation of required fields
- Sensible defaults for optional fields
- Clear error messages for invalid config
- Config data class with all parameters

**Required Config Fields:**
```json
{
  "folders": {
    "samples": "/path/to/samples",
    "candidates": "/path/to/candidates",
    "output": "/path/to/output",
    "temp": "/path/to/temp"
  },
  "llm": {
    "endpoint": "http://localhost:1234/api/v1",
    "model": "llama2",
    "apiKey": "optional-token"
  },
  "processing": {
    "maxParallelTasks": 8,
    "outputFormat": "original",
    "jpegQuality": 90
  }
}
```

**Success Criteria:**
- Valid config loads without errors
- Invalid config produces clear error message
- All required fields validated
- Defaults applied for optional fields
- Paths verified (folders exist or can be created)

**Tests for This Task:**
- Create `src/commonTest/kotlin/com/wallpaperqualifier/config/ConfigParserTest.kt` (Kotest FunSpec)
- Test: Valid config parses correctly
- Test: Invalid JSON rejected with clear error
- Test: Missing required fields detected
- Test: Optional fields use defaults
- Run: `./gradlew test` — all tests must pass

**Implementation Notes:**
- Use `kotlinx-serialization-json`
- Implement custom deserialization for validation
- Create config file example for reference

---

## Task 6: Logging Framework

**Objective:** Implement structured logging system

**Deliverables:**
- Logger interface or simple logging utility
- Output to stdout (info, progress) and stderr (warnings, errors)
- Log levels: DEBUG, INFO, WARN, ERROR
- Timestamp and level prefixes
- Optional verbose mode from CLI

**Success Criteria:**
- Log messages formatted consistently
- Info logs go to stdout
- Error logs go to stderr
- Logger easily injectable into components
- Verbose mode increases detail level

**Implementation Notes:**
- Keep simple (don't import heavy logging library initially)
- Consider: Simple `Logger` object with `fun info(msg: String)`
- Future: Can switch to SLF4J/Logback if needed

---

## Task 7: Error Handling System

**Objective:** Create consistent error handling patterns

**Deliverables:**
- `Result<T>` sealed class (Success/Failure)
- Custom exception types:
  - `ConfigurationException`
  - `ImageProcessingException`
  - `LLMException`
  - `FileIOException`
- Extension functions for ergonomic error handling
- Error messages suitable for CLI users

**Success Criteria:**
- All errors use Result type or custom exceptions
- Error messages are actionable (not generic)
- Stack traces available in DEBUG mode
- CLI displays user-friendly error messages

**Tests for This Task:**
- Create `src/commonTest/kotlin/com/wallpaperqualifier/utils/ResultTest.kt`
- Test: Success case works correctly
- Test: Failure case works correctly
- Test: Extension functions (getOrNull, getOrThrow) work
- Test: Error messages are clear and actionable
- Run: `./gradlew test` — all tests must pass

**Implementation Notes:**
- Use sealed classes for type-safe error handling
- Provide `.getOrNull()` and `.getOrThrow()` extensions
- Include error codes for scripting/automation

---

## Task 8: Basic Test Framework Setup

**Objective:** Verify testing infrastructure is ready

**Deliverables:**
- Kotest and JUnit 5 properly configured
- Sample test file demonstrating setup
- Test runner working with parallel execution
- Test discovery automatic

**Success Criteria:**
- `./gradlew test` runs tests successfully
- All tests from earlier tasks pass
- Test discovery works automatically
- Tests can be run individually
- Test output is clear and actionable

**Tests for This Task:**
- Tests created in Tasks 1, 5, and 7 should all pass
- Create `src/commonTest/kotlin/com/wallpaperqualifier/SanityTest.kt` as smoke test
- Test: Basic test framework functions correctly
- Run: `./gradlew test` — all tests (including prior tasks) must pass

**Implementation Notes:**
- Use Kotlin Test framework
- JUnit 5 for assertions
- Create `src/commonTest/kotlin/` structure
- Add example test for Logger or simple utility

---

## Completion Checklist

Before moving to PHASE 2/3, verify:

- [ ] `./gradlew build` succeeds
- [ ] `./gradlew test` runs without errors
- [ ] `./gradlew ktlint` passes code style
- [ ] CLI help displays with `--help`
- [ ] Config parser loads valid JSON
- [ ] Config parser rejects invalid JSON with clear error
- [ ] Logger outputs to stdout/stderr correctly
- [ ] Result type used consistently
- [ ] All domain models defined
- [ ] Example config file provided in docs

---

## Risk Mitigation

| Risk | Mitigation |
|------|-----------|
| Kotlin Koog not KMP-compatible | Prototype in Task 1; consider Kotlin Coroutines as alternative |
| macOS-specific dependencies | Identify early; use platform-specific code where needed |
| Build configuration complexity | Start simple; add complexity incrementally |
| Configuration validation too strict | Provide detailed error messages; allow sensible defaults |

---

## Phase 1 Complete Indicators

✅ **Phase 1 is complete when:**
1. All 8 tasks delivered
2. Project builds successfully
3. Basic CLI works (help, version, config parsing)
4. Test framework functional
5. Team can start PHASE 2 and PHASE 3 independently

---

**Next:** Proceed to [PHASE-2-IMAGE-PROCESSING.md](./PHASE-2-IMAGE-PROCESSING.md) or [PHASE-3-LLM-INTEGRATION.md](./PHASE-3-LLM-INTEGRATION.md) once PHASE 1 complete.
