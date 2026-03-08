# Agent Guide: Wallpaper Qualifier

## Purpose

Agents act as senior software engineers collaborating on the Wallpaper Qualifier project. Keep responses concise, clarify uncertainty before coding, and align suggestions with the rules linked below.

This document serves as the entry point for understanding the project, its architecture, and coding standards.

---

## Rule Index

**Always load this file first to understand which other files you need to load:**
- @docs/ai-rules/rule-loading.md

**Core project rules:**
- @docs/ai-rules/general.md — Engineering principles and project standards
- @docs/ai-rules/kotlin-multiplatform.md — Kotlin/KMP specific patterns and requirements

**Domain-specific rules (load as needed):**
- @docs/ai-rules/image-processing.md — Image handling and optimization patterns
- @docs/ai-rules/llm-integration.md — LLM API interaction and sequential request management
- @docs/ai-rules/cli-design.md — Command-line interface patterns and user experience
- @docs/ai-rules/configuration.md — JSON configuration schema and parsing
- @docs/ai-rules/testing.md — Testing strategies and patterns for this project

---

## Repository Overview

### Project Summary

**Wallpaper Qualifier** is a macOS command-line tool that learns from user-provided sample wallpapers and automatically evaluates candidate images, copying qualified wallpapers to an output folder.

**Core workflow:**
1. **Sample Analysis** — Analyze user-provided sample wallpapers via LLM to extract aesthetic and technical characteristics
2. **Profile Generation** — Compile characteristics into a quality profile representing user preferences
3. **Candidate Evaluation** — Evaluate candidate wallpapers against the profile
4. **Curation** — Optimize and copy qualified images to output folder

### Key Architecture Decisions

- **Language**: Kotlin Multiplatform (KMP) targeting macOS
- **UI**: Command-line interface only (no GUI)
- **LLM Integration**: External LLM service (User choice, any provider supported by Kotlin Koog; LMStudio as local default)
- **Image Processing**: Kotlin Koog framework
- **File Formats**: Support JPEG, PNG, HEIC, WebP, TIFF, BMP, GIF, and RAW formats
- **Concurrency**: Parallel file I/O and image processing; sequential LLM requests only

### Directory Structure

```
wallpaper-qualifier/
├── src/
│   ├── commonMain/
│   │   ├── kotlin/
│   │   │   └── com/wallpaperqualifier/
│   │   │       ├── cli/                 # CLI argument handling
│   │   │       ├── config/              # Configuration parsing
│   │   │       ├── image/               # Image processing pipeline
│   │   │       ├── llm/                 # LLM client and request queue
│   │   │       ├── profile/             # Profile generation logic
│   │   │       ├── workflow/            # Main workflow orchestration
│   │   │       └── utils/               # Shared utilities
│   └── macosMain/                       # macOS-specific implementations
├── docs/
│   └── specs/                           # Original specification documents
├── AGENTS.md                            # This file
├── build.gradle.kts                     # Build configuration
└── README.md                            # User-facing documentation
```

### Core Dependencies

**Build & Runtime:**
- Kotlin Multiplatform (latest stable)
- Kotlin Koog (image processing)
- kotlinx.serialization (JSON handling)
- kotlinx.coroutines (concurrency)

**Testing:**
- Kotest (framework & assertions)
- Fakes (preferred for internal interfaces)
- MockK (reserved for external/final dependencies)

**External Services:**
- LMStudio API (local LLM service)

---

## Commands

**Build & Compilation:**
```bash
./gradlew build                 # Build for all platforms
./gradlew buildDebug            # Build debug variant
./gradlew macosX64Binaries      # Build macOS binary
./gradlew assemble              # Assemble without tests
```

**Testing:**
```bash
./gradlew test                  # Run all tests
./gradlew test --info           # Run tests with verbose output
./gradlew testDebug             # Run tests on debug build
```

**Linting & Formatting:**
```bash
./gradlew ktlintFormat          # Format Kotlin code
./gradlew ktlint                # Check Kotlin code style
```

**Running the Application:**
```bash
java -jar build/libs/wallpaper-qualifier.jar config.json
./wallpaper-qualifier config.json  # After release build
```

**Development Workflow:**
```bash
./gradlew clean build           # Full clean build
./gradlew run --args="config.json"  # Run with arguments
./gradlew test --watch          # Watch mode for tests
```

---

## Code Style

### Kotlin Conventions

- **Indentation**: 4 spaces (no tabs)
- **Line width**: Max 120 characters
- **Naming**: camelCase for functions/variables, PascalCase for classes
- **Visibility**: Explicit modifiers (private by default, public when needed)
- **Immutability**: Use `val` by default, `var` only when necessary

### Patterns & Idioms

- Prefer **composition** over inheritance
- Use **sealed classes** for type hierarchies
- Use **extension functions** for utility methods
- Prefer **data classes** for value objects
- Use **scope functions** (let, run, apply) appropriately
- Avoid **null references** — use `Result<T>` or sealed types for error handling

### File Organization

```kotlin
// 1. Package declaration
package com.wallpaperqualifier.image

// 2. Imports (organized)
import kotlinx.coroutines.*
import java.io.File
import kotlin.math.min

// 3. Type aliases (if any)
typealias ImagePath = String

// 4. Public API (classes, interfaces, objects)
class ImageProcessor(/* ... */) { /* ... */ }

// 5. Private implementation
private fun processPixels() { /* ... */ }
```

---

## Architecture & Patterns

### Core Modules

**CLI Module** (`cli/`)
- Handles command-line argument parsing
- Maps user input to internal operations
- Reports progress and results to stdout

**Configuration Module** (`config/`)
- Loads and validates JSON configuration
- Maps configuration to domain objects
- Provides sensible defaults

**Image Module** (`image/`)
- Format detection and conversion
- Optimization (compression, resizing)
- File I/O with parallel processing
- Uses Kotlin Koog for native image handling

**LLM Module** (`llm/`)
- HTTP client for LMStudio API
- Request queue ensuring sequential sending
- Response parsing and validation
- Error handling with retry logic (future version)

**Profile Module** (`profile/`)
- Aggregates characteristics from analysis
- Generates quality criteria
- Serialization/deserialization

**Workflow Module** (`workflow/`)
- Orchestrates phases: initialization → sample analysis → profile generation → evaluation → curation
- Coordinates between modules
- Manages concurrency (parallel tasks, sequential LLM)

### Design Patterns in Use

**1. Result Type Pattern**
```kotlin
sealed class Result<T> {
    data class Success<T>(val value: T) : Result<T>()
    data class Failure<T>(val error: Throwable) : Result<T>()
}
```

**2. Request Queue Pattern (LLM)**
```kotlin
class LLMRequestQueue {
    private val queue = Channel<Request>()
    suspend fun enqueue(request: Request): Response { /* ... */ }
}
```

**3. Configuration Validation**
- Parse → Validate → Load pattern
- Clear error messages for configuration issues

**4. Coroutine-based Concurrency**
- Sequential LLM: single coroutine processing queue
- Parallel image processing: multiple coroutines with controlled pool size

---

## Key Integration Points

### Configuration

- **Source**: JSON file provided by user at runtime
- **Schema**: See `@docs/ai-rules/configuration.md`
- **Validation**: All paths verified, LLM connection tested during initialization
- **Usage**: Injected into workflow orchestrator

### LLM Service

- **Endpoint**: Local LMStudio API (`http://localhost:1234/api/v1`)
- **Operations**: Chat completions with image attachments
- **Constraints**: Sequential requests only; no parallelism
- **Response Format**: JSON (parsed to domain objects)

### Image Processing

- **Input Formats**: JPEG, PNG, HEIC, WebP, TIFF, BMP, GIF, RAW
- **Processing**: Format detection → conversion → optimization
- **Output**: PNG or JPEG (configurable) under 2MB
- **Error Handling**: Graceful skipping of corrupted images

### File System

- **Sample Folder**: User-provided samples for learning
- **Candidate Folder**: User-provided wallpapers to evaluate
- **Output Folder**: Qualified wallpapers copied here
- **Permissions**: Read access required for samples/candidates; write access for output

---

## Workflow

### When Starting New Features or Making Architectural Changes

1. **Clarify requirements**: Ask for 2-3 options with trade-offs if uncertain
2. **Reference rules**: Link decisions to specific rule files
3. **Update relevant rules**: If introducing new patterns, document them
4. **Consider concurrency model**: Verify alignment with sequential LLM constraint
5. **Test incrementally**: Write tests alongside implementation

### When Working with Configuration

1. Load @docs/ai-rules/configuration.md
2. Follow JSON schema strictly
3. Validate early with clear error messages
4. Provide sensible defaults where appropriate

### When Implementing Image Processing

1. Load @docs/ai-rules/image-processing.md
2. Verify format support
3. Test optimization output quality
4. Handle errors gracefully (corrupted files)

### When Integrating with LLM

1. Load @docs/ai-rules/llm-integration.md
2. Remember: Sequential requests only
3. Queue-based request management
4. Parse responses to domain objects
5. Log interactions for debugging

### Before Committing

1. Run: `./gradlew ktlintFormat ktlint test`
2. Verify no test failures
3. Update documentation if introducing new patterns
4. Use commit format: `<type>(<scope>): <summary>`
   - Types: feat, fix, refactor, docs, test, chore
   - Scope: module or feature name
   - Example: `feat(image): add HEIC format support`

---

## Testing

### Testing Strategy

- **Unit Tests**: Individual function/class behavior
- **Integration Tests**: Module interactions (e.g., config → workflow)
- **End-to-End Tests**: Full workflow with mock LLM
- **Mock LLM**: Provides deterministic responses for testing

### Test File Organization

```kotlin
// Location: src/commonTest/kotlin/com/wallpaperqualifier/{module}/
class ImageProcessorTest {
    @Test
    fun shouldConvertJpegToPng() { /* ... */ }
    
    @Test
    fun shouldHandleCorruptedImages() { /* ... */ }
}
```

### Testing Concurrency

- Mock `LLMRequestQueue` to verify sequential ordering
- Test parallel image processing with controlled thread pool
- Verify no race conditions in profile generation

---

### Environment

### Development Requirements

- **macOS 26+** (development machine)
- **Kotlin 1.9+**
- **Gradle 8.0+**
- **Java 17+** (for Kotlin compilation)
- **LMStudio** (running locally on port 1234 during development)

### Build Environment

- **Target**: macOS 26+ binary
- **Architecture**: x86_64 (Intel) or arm64 (Apple Silicon)
- **Output**: Self-contained executable or JAR

### Runtime Requirements

- **macOS 26+**
- **LMStudio** service running on local network
- **Disk Space**: Depends on image batch sizes (recommend 10GB+ free)
- **Memory**: <2GB for processing batches of 1000 images

---

## Special Notes

- **Sequential LLM Requests**: This is a hard constraint. Do not parallelize LLM calls even if tempted for performance. The system architecture depends on this.
- **File Safety**: Original sample/candidate images are never modified. Only optimized copies go to output folder.
- **Error Messages**: Be explicit and actionable. Users are running this CLI tool and need clear guidance on what went wrong.
- **Progress Reporting**: Show clear feedback during long operations. Indicate phase and percentage completion.
- **Configuration as Contract**: JSON config is the user's contract with the system. Any changes require documentation updates.
- **Atomic Operations**: File writes must be atomic (no partial copies). Use temporary files and move atomically.
- **Memory Conscious**: Batch processing prevents memory issues with large image sets. Use streaming where possible.

---

## Quick Reference: Rule Loading by Task

### When starting a new feature:
Load: `general.md`, `kotlin-multiplatform.md`, and domain-specific rules (e.g., `image-processing.md`)

### When working with configuration:
Load: `configuration.md`, `general.md`

### When implementing image operations:
Load: `image-processing.md`, `general.md`, `kotlin-multiplatform.md`

### When integrating with LLM:
Load: `llm-integration.md`, `general.md`, `kotlin-multiplatform.md`

### When designing CLI:
Load: `cli-design.md`, `general.md`

### When writing tests:
Load: `testing.md`, `general.md`, `kotlin-multiplatform.md`

### Before committing:
Load: `general.md` (review code style and patterns)

---

## Getting Started

1. **Read this document** to understand project scope and architecture
2. **Load @docs/ai-rules/rule-loading.md** to understand which rules apply to your task
3. **Load domain-specific rules** based on the task at hand
4. **Reference AGENTS.md** when context-switching between tasks
5. **Update rules** if you discover patterns worth encoding for future work

---

**Last Updated**: 2026-03-08
**Project Status**: Active Development
