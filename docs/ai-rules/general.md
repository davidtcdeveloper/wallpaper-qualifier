<primary_directive>
You are an ELITE software engineer specializing in Kotlin and LLM integrations. Your code exhibits MASTERY through CLARITY and SIMPLICITY. ALWAYS clarify ambiguities BEFORE coding. NEVER assume requirements without verification.
</primary_directive>

<cognitive_anchors>
TRIGGERS: Wallpaper Qualifier, software architecture, code quality, Kotlin, KMP, production code, design patterns, SOLID principles, error handling
SIGNAL: When triggered → Apply ALL rules below systematically
</cognitive_anchors>

# General: Core Engineering Principles

## Overview

This document establishes the engineering standards for the Wallpaper Qualifier project. All code contributions must align with these principles.

---

## Architecture Principles

<rule_1 priority="HIGHEST">
**MODULAR DESIGN**: Each module has a single responsibility.
- Image processing → Image module
- LLM interaction → LLM module
- Configuration → Config module
- Workflow coordination → Workflow module
- User interface → CLI module

**MUST**:
- ✓ Avoid god objects or functions
- ✓ Define clear module boundaries
- ✓ Export minimal public APIs
- ✓ Use dependency injection for cross-module communication

**Example - Good**:
```kotlin
// Workflow orchestrates, doesn't implement everything
class WallpaperQualifier(
    private val configLoader: ConfigLoader,
    private val imageAnalyzer: ImageAnalyzer,
    private val profileGenerator: ProfileGenerator
) {
    suspend fun run(configPath: String) {
        val config = configLoader.load(configPath)
        val samples = imageAnalyzer.analyzeSamples(config)
        val profile = profileGenerator.generate(samples)
        // ...
    }
}
```

**Example - Avoid**:
```kotlin
// Anti-pattern: One class doing everything
class WallpaperQualifier {
    fun run(configPath: String) {
        // Parsing config, loading images, analyzing, generating profile...
        // 2000+ lines of mixed concerns
    }
}
```
</rule_1>

---

<rule_2 priority="HIGHEST">
**EXPLICIT ERROR HANDLING**: No silent failures. All error paths must be explicit and recoverable.
- Use `Result<T>` or sealed types for error cases
- NEVER silently catch and ignore exceptions
- Provide actionable error messages to users
- Log errors with sufficient context for debugging

**MUST**:
- ✓ Return `Result<T>` or `sealed class` for potentially failing operations
- ✓ Propagate errors up the call stack with context
- ✓ Show users what went wrong and how to fix it
- ✓ Log stack traces in debug builds only

**Example - Good**:
```kotlin
sealed class ImageLoadResult {
    data class Success(val image: Image) : ImageLoadResult()
    data class FailedCorrupted(val path: String) : ImageLoadResult()
    data class FailedUnsupported(val path: String, val format: String) : ImageLoadResult()
}

fun loadImage(path: String): ImageLoadResult = try {
    val image = ImageDecoder.decode(path)
    ImageLoadResult.Success(image)
} catch (e: CorruptedImageException) {
    logger.warn("Corrupted image: $path")
    ImageLoadResult.FailedCorrupted(path)
} catch (e: UnsupportedFormatException) {
    logger.warn("Unsupported format in $path: ${e.format}")
    ImageLoadResult.FailedUnsupported(path, e.format)
}
```

**Example - Avoid**:
```kotlin
// Anti-pattern: Silent failures
fun loadImage(path: String): Image? = try {
    ImageDecoder.decode(path)
} catch (e: Exception) {
    null // User doesn't know why image failed to load
}
```
</rule_2>

---

<rule_3 priority="HIGH">
**IMMUTABILITY BY DEFAULT**: Prefer `val` and immutable data structures. Use `var` and mutable types only when necessary and documented.
- Immutable data reduces bugs and enables concurrent access
- Use `data class` for value objects
- Use `copy()` for transformations
- Use `sealed class` for type hierarchies

**MUST**:
- ✓ Declare data as `val` unless mutation is required
- ✓ Use immutable collections (not `MutableList`, `MutableMap`)
- ✓ Document why mutation is necessary if you use `var`

**Example - Good**:
```kotlin
data class WallpaperProfile(
    val aesthetics: Aesthetics,
    val technicalRequirements: TechnicalRequirements,
    val createdAt: Instant
)

val profile = WallpaperProfile(...)
val updatedProfile = profile.copy(
    technicalRequirements = profile.technicalRequirements.copy(
        maxFileSize = 3_000_000
    )
)
```

**Example - Avoid**:
```kotlin
// Anti-pattern: Mutable state everywhere
class WallpaperProfile {
    var aesthetics: Aesthetics? = null
    var technicalRequirements: TechnicalRequirements? = null
    var createdAt: Long = 0L
}
```
</rule_3>

---

<rule_4 priority="HIGH">
**NAMING CLARITY**: Names reveal intent. A reader should understand code's purpose without comments.
- Functions describe what they do
- Variables describe what they hold
- Classes describe what they represent
- Avoid abbreviations except standard ones (e.g., `id`, `config`)

**MUST**:
- ✓ Use full names: `wallpaperQualityScore` not `wqs` or `score`
- ✓ Use consistent naming across the codebase
- ✓ Prefix predicates with `is`, `has`, `should`: `isValid`, `hasMetadata`
- ✓ Use verb phrases for functions: `calculateScore`, `optimizeImage`

**Example - Good**:
```kotlin
fun isImageQualified(profile: WallpaperProfile, candidate: CandidateImage): Boolean
fun calculateAestheticSimilarity(sample: Aesthetics, candidate: Aesthetics): Float
suspend fun optimizeImageForOutput(image: Image): OptimizedImage
```

**Example - Avoid**:
```kotlin
fun check(p: Profile, c: Image): Boolean
fun calc(a: Aes, c: Aes): Float
fun opt(img: Image): Image
```
</rule_4>

---

<rule_5 priority="HIGH">
**NO MAGIC NUMBERS**: All constants should be named and have clear purpose.
- Define constants at module level or as named parameters
- Group related constants in objects
- Document the origin/rationale for constants

**MUST**:
- ✓ Extract magic numbers to named `const val`
- ✓ Group image format constants together
- ✓ Group LLM settings together
- ✓ Comment constants with non-obvious meanings

**Example - Good**:
```kotlin
object ImageOptimization {
    const val DEFAULT_JPEG_QUALITY = 85
    const val MAX_FILE_SIZE_BYTES = 2_097_152 // 2MB
    const val SUPPORTED_FORMATS = listOf("jpeg", "png", "heic", "webp")
}

// Later in code:
val optimized = optimizeImage(candidate, quality = ImageOptimization.DEFAULT_JPEG_QUALITY)
```

**Example - Avoid**:
```kotlin
val optimized = optimizeImage(candidate, quality = 85) // What is 85?
if (image.sizeBytes > 2097152) { } // What is this magic number?
```
</rule_5>

---

<rule_6 priority="MEDIUM">
**FUNCTION CLARITY**: Functions should do one thing well and be understandable at a glance.
- Keep functions under 30 lines when possible
- Extract complex logic into helper functions
- Use type-level documentation for non-obvious functions
- One level of abstraction per function

**MUST**:
- ✓ Single responsibility per function
- ✓ Clear input/output (types tell the story)
- ✓ Extract complex boolean logic to named functions
- ✓ Document preconditions if non-obvious

**Example - Good**:
```kotlin
suspend fun analyzeSampleImage(imagePath: String): AnalysisResult {
    val image = loadImageOrThrow(imagePath)
    val characteristics = extractCharacteristics(image)
    return AnalysisResult(imagePath, characteristics)
}

private suspend fun extractCharacteristics(image: Image): Characteristics {
    // Complex logic extracted to focused function
    // ...
}
```

**Example - Avoid**:
```kotlin
suspend fun process(path: String) {
    // Loading, validation, analysis, serialization all mixed
    // 50+ lines of intertwined logic
}
```
</rule_6>

---

<rule_7 priority="MEDIUM">
**DEPENDENCY INJECTION**: Classes should receive dependencies, not create them.
- Enables testing (inject mocks)
- Enables flexibility (swap implementations)
- Makes dependencies explicit
- Use constructor injection or factory functions

**MUST**:
- ✓ Pass dependencies via constructor
- ✓ Define interfaces for services where needed
- ✓ Avoid `ServiceLocator` or global singletons
- ✓ Make dependencies `private` and immutable

**Example - Good**:
```kotlin
class ImageAnalyzer(
    private val imageDecoder: ImageDecoder,
    private val characteristicExtractor: CharacteristicExtractor
) {
    suspend fun analyze(path: String) = // ...
}

// In tests:
val mockDecoder = MockImageDecoder()
val analyzer = ImageAnalyzer(mockDecoder, mockExtractor)
```

**Example - Avoid**:
```kotlin
// Anti-pattern: Creating dependencies internally
class ImageAnalyzer {
    private val decoder = RealImageDecoder() // Can't test, tightly coupled
    
    fun analyze(path: String) = // ...
}
```
</rule_7>

---

## Code Quality Checklist

Before committing, verify:

☐ **No Silent Failures**: All exceptions are caught with explicit error handling
☐ **Immutability**: `val` by default, `var` only when documented
☐ **Clear Naming**: Anyone can read the code and understand purpose
☐ **Single Responsibility**: Each class/function does one thing
☐ **Dependency Injection**: Dependencies are injected, not created
☐ **No Magic Numbers**: All constants are named and documented
☐ **Error Messages**: Users know what went wrong and how to fix it
☐ **Tests Exist**: New logic has corresponding tests
☐ **No TODO/FIXME**: Unless with clear next steps and owner

---

## Commit Message Format

```
<type>(<scope>): <summary>

<body - optional>

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
```

**Types**: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`

**Examples**:
```
feat(image): add TIFF format support
fix(llm): handle sequential queue timeout gracefully
refactor(config): simplify validation logic
docs(cli): update usage examples
test(profile): add edge case tests for empty samples
```

---

**Last Updated**: 2026-03-08
