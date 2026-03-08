<primary_directive>
You design configuration systems that are PREDICTABLE and VALIDATING. You provide CLEAR error messages. You make DEFAULTS sensible but EXPLICIT about what they are.
</primary_directive>

# Configuration: JSON Schema & Validation

## Overview

The Wallpaper Qualifier is configured via JSON file. Users must not edit code to customize behavior. This document defines the configuration schema, validation rules, and error handling.

---

## Configuration Schema

<rule_1 priority="HIGHEST">
**SCHEMA STRUCTURE**: Configuration must be complete and valid before application starts.
- All paths must exist and be accessible
- All LLM parameters must be provided or default sensibly
- Invalid configuration produces clear error message
- Configuration is immutable once loaded

**MUST**:
- ✓ Define JSON schema as `config.schema.json` for documentation
- ✓ Validate all paths on load
- ✓ Validate all enum values (e.g., image format)
- ✓ Provide sensible defaults for optional fields
- ✓ Reject unknown fields (strict mode)

**Example - Good**:
```json
{
  "folders": {
    "samples": "/Users/user/Wallpapers/Samples",
    "candidates": "/Users/user/Wallpapers/Candidates",
    "output": "/Users/user/Wallpapers/Qualified"
  },
  "llm": {
    "provider": "openai|anthropic|google|lmstudio",
    "apiKey": "your-api-key",
    "endpoint": "http://localhost:1234/api/v1",
    "model": "mistral-7b",
    "timeout": 30,
    "retryAttempts": 3
  },
  "processing": {
    "maxParallelTasks": 8,
    "outputFormat": "png",
    "jpegQuality": 85,
    "maxFileSize": 2097152,
    "qualityThreshold": 0.7
  },
  "options": {
    "saveProfile": true,
    "profilePath": "/Users/user/.wallpaper-qualifier/profile.json",
    "verbose": true,
    "skipDuplicates": true
  }
}
```

**Example Schema (for documentation)**:
```kotlin
@Serializable
data class Config(
    val folders: FoldersConfig,
    val llm: LLMConfig,
    val processing: ProcessingConfig,
    val options: OptionsConfig
)

@Serializable
data class FoldersConfig(
    val samples: String,      // Required: path to samples folder
    val candidates: String,   // Required: path to candidates folder
    val output: String        // Required: path to output folder
)

@Serializable
data class LLMConfig(
    val provider: String,     // Required: openai, anthropic, google, or lmstudio
    val apiKey: String? = null,
    val endpoint: String = "http://localhost:1234/api/v1",
    val model: String,        // Required: model name/ID
    val timeout: Int = 30,
    val retryAttempts: Int = 0 // Not implemented in first version
)

@Serializable
data class ProcessingConfig(
    val maxParallelTasks: Int = 8,
    val outputFormat: ImageFormat = ImageFormat.PNG,
    val jpegQuality: Int = 85,
    val maxFileSize: Int = 2097152, // 2MB
    val qualityThreshold: Float = 0.7
)

@Serializable
data class OptionsConfig(
    val saveProfile: Boolean = true,
    val profilePath: String? = null,
    val verbose: Boolean = false,
    val skipDuplicates: Boolean = true
)

enum class ImageFormat {
    PNG, JPEG
}
```
</rule_1>

---

<rule_2 priority="HIGHEST">
**VALIDATION & ERROR MESSAGES**: Configuration errors must be caught early with actionable feedback.
- Check all paths exist and are readable/writable
- Check parameter ranges (e.g., quality 0-100)
- Check enum values
- Provide specific suggestion for each error

**MUST**:
- ✓ Validate before returning config
- ✓ Collect all errors, report together
- ✓ Suggest fixes (e.g., "create folder at /path")
- ✓ Distinguish required vs. optional errors

**Example - Good**:
```kotlin
sealed class ConfigError {
    data class PathNotFound(val path: String, val role: String) : ConfigError()
    data class NotReadable(val path: String, val reason: String) : ConfigError()
    data class NotWritable(val path: String, val reason: String) : ConfigError()
    data class InvalidValue(val field: String, val value: String, val reason: String) : ConfigError()
    data class ParseError(val message: String, val line: Int? = null) : ConfigError()
}

fun validateConfig(config: Config): List<ConfigError> {
    val errors = mutableListOf<ConfigError>()
    
    // Validate folders
    validatePath(config.folders.samples, "samples") { errors.add(it) }
    validatePath(config.folders.candidates, "candidates") { errors.add(it) }
    validatePath(config.folders.output, "output", mustBeWritable = true) { errors.add(it) }
    
    // Validate processing
    if (config.processing.jpegQuality !in 1..100) {
        errors.add(
            ConfigError.InvalidValue(
                field = "processing.jpegQuality",
                value = config.processing.jpegQuality.toString(),
                reason = "Must be between 1 and 100"
            )
        )
    }
    
    if (config.processing.qualityThreshold !in 0f..1f) {
        errors.add(
            ConfigError.InvalidValue(
                field = "processing.qualityThreshold",
                value = config.processing.qualityThreshold.toString(),
                reason = "Must be between 0.0 and 1.0"
            )
        )
    }
    
    return errors
}

fun loadConfig(path: String): Result<Config> {
    return try {
        val jsonString = File(path).readText()
        val config = Json { ignoreUnknownKeys = false }.decodeFromString<Config>(jsonString)
        
        val errors = validateConfig(config)
        if (errors.isNotEmpty()) {
            val errorMessages = errors.map { error ->
                when (error) {
                    is ConfigError.PathNotFound -> 
                        "ERROR: ${error.role} folder not found: ${error.path}"
                    is ConfigError.NotReadable -> 
                        "ERROR: Cannot read ${error.path}: ${error.reason}"
                    is ConfigError.NotWritable -> 
                        "ERROR: Cannot write to ${error.path}: ${error.reason}\nTIP: Create directory and ensure write permissions"
                    is ConfigError.InvalidValue -> 
                        "ERROR: ${error.field} = ${error.value} (${error.reason})"
                    is ConfigError.ParseError -> 
                        "ERROR: JSON parse error: ${error.message}"
                }
            }
            return Result.failure(Exception(errorMessages.joinToString("\n")))
        }
        
        Result.success(config)
    } catch (e: FileNotFoundException) {
        Result.failure(Exception("Configuration file not found: $path"))
    } catch (e: SerializationException) {
        Result.failure(Exception("Invalid JSON in configuration: ${e.message}"))
    } catch (e: Exception) {
        Result.failure(Exception("Failed to load configuration: ${e.message}"))
    }
}

private fun validatePath(path: String, role: String, mustBeWritable: Boolean = false, onError: (ConfigError) -> Unit) {
    val file = File(path)
    
    if (!file.exists()) {
        onError(ConfigError.PathNotFound(path, role))
        return
    }
    
    if (!file.canRead()) {
        onError(ConfigError.NotReadable(path, "permission denied"))
        return
    }
    
    if (mustBeWritable && !file.canWrite()) {
        onError(ConfigError.NotWritable(path, "permission denied"))
    }
}
```

**Example - Avoid**:
```kotlin
// Anti-pattern: Silent defaults or unclear errors
fun loadConfig(path: String): Config {
    val jsonString = File(path).readText()
    return Json.decodeFromString<Config>(jsonString) // Throws cryptic parse error
}

// Anti-pattern: Generic error messages
if (!file.exists()) {
    throw Exception("Path error") // User has no idea what to fix
}
```
</rule_2>

---

<rule_3 priority="HIGH">
**DEFAULTS & SENSIBLE BEHAVIOR**: Optional fields should have clear defaults that reflect best practices.
- Default parallel tasks to CPU count (capped at 8)
- Default output format to PNG (lossless)
- Default quality threshold to 0.7 (70% confidence)
- Document all defaults clearly

**MUST**:
- ✓ Provide defaults for optional fields
- ✓ Defaults should be safe (not aggressive)
- ✓ Document why each default is chosen
- ✓ Allow override for all defaults

**Example - Good**:
```kotlin
@Serializable
data class ProcessingConfig(
    @SerialName("max_parallel_tasks")
    val maxParallelTasks: Int = Runtime.getRuntime().availableProcessors().coerceAtMost(8),
    
    @SerialName("output_format")
    val outputFormat: ImageFormat = ImageFormat.PNG, // Lossless by default
    
    @SerialName("jpeg_quality")
    val jpegQuality: Int = 85, // Balances quality and file size
    
    @SerialName("max_file_size_mb")
    val maxFileSizeMb: Int = 2, // Conservative, covers most use cases
    
    @SerialName("quality_threshold")
    val qualityThreshold: Float = 0.7 // 70% confidence = good selection
)

// Documented defaults
val CONFIG_DEFAULTS = """
Processing Defaults:
- max_parallel_tasks: CPU count (max 8)
- output_format: PNG (lossless quality)
- jpeg_quality: 85 (visual quality preserved)
- max_file_size_mb: 2 (quick downloads, broad compatibility)
- quality_threshold: 0.7 (conservative selection)
""".trimIndent()
```

**Example - Avoid**:
```kotlin
// Anti-pattern: Aggressive or surprising defaults
@Serializable
data class ProcessingConfig(
    val maxParallelTasks: Int = 64, // Way too many!
    val outputFormat: ImageFormat = ImageFormat.JPEG, // Lost compression!
    val jpegQuality: Int = 30, // Visible artifacts!
)
```
</rule_3>

---

## Checklist

Before implementing configuration loading:

☐ **Schema Defined**: Clear JSON schema documented
☐ **Validation**: All paths and values validated before use
☐ **Error Messages**: Clear, actionable guidance for each error type
☐ **Defaults**: Sensible, documented, safe
☐ **Types**: Using enums for fixed values (image format, etc.)
☐ **Immutability**: Config loaded once, never modified
☐ **Documentation**: Example config file provided

---

**Last Updated**: 2026-03-08
