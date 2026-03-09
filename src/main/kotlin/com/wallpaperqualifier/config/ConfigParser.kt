package com.wallpaperqualifier.config

import com.wallpaperqualifier.domain.ConfigurationException
import com.wallpaperqualifier.domain.Result
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Loads, validates, and parses JSON configuration files.
 */
class ConfigParser(private val json: Json = Json { ignoreUnknownKeys = false }) {

    /**
     * Parses a configuration file and returns the parsed AppConfig.
     *
     * @param configPath Path to the JSON configuration file
     * @return Result containing AppConfig on success, or exception on failure
     */
    fun parseFile(configPath: String): Result<AppConfig> {
        return try {
            val file = File(configPath)
            
            if (!file.exists()) {
                return Result.Failure(
                    ConfigurationException("Configuration file not found: $configPath")
                )
            }
            
            if (!file.isFile) {
                return Result.Failure(
                    ConfigurationException("Configuration path is not a file: $configPath")
                )
            }
            
            if (!file.canRead()) {
                return Result.Failure(
                    ConfigurationException("Configuration file is not readable: $configPath")
                )
            }

            val content = file.readText()
            parseJson(content)
        } catch (e: ConfigurationException) {
            Result.Failure(e)
        } catch (e: Exception) {
            Result.Failure(
                ConfigurationException("Failed to parse configuration: ${e.message}", e)
            )
        }
    }

    /**
     * Parses a JSON string into AppConfig.
     *
     * @param json JSON string
     * @return Result containing AppConfig on success, or exception on failure
     */
    fun parseJson(json: String): Result<AppConfig> {
        return try {
            val config = this.json.decodeFromString(AppConfig.serializer(), json)

            val validation = validateConfig(config)
            if (validation is Result.Failure) {
                return Result.Failure(validation.error)
            }

            Result.Success(config)
        } catch (e: SerializationException) {
            Result.Failure(
                ConfigurationException("Invalid JSON format: ${e.message}", e)
            )
        } catch (e: IllegalArgumentException) {
            Result.Failure(
                ConfigurationException("Invalid configuration value: ${e.message}", e)
            )
        } catch (e: Exception) {
            Result.Failure(
                ConfigurationException("Failed to parse JSON configuration: ${e.message}", e)
            )
        }
    }

    private fun validateConfig(config: AppConfig): Result<Unit> {
        val errors = mutableListOf<String>()

        if (config.folders.samples.isBlank()) {
            errors.add("folders.samples path cannot be empty")
        } else {
            errors += folderValidationErrors(config.folders.samples, "folders.samples", requireWritable = false)
        }

        if (config.folders.candidates.isBlank()) {
            errors.add("folders.candidates path cannot be empty")
        } else {
            errors += folderValidationErrors(config.folders.candidates, "folders.candidates", requireWritable = false)
        }

        if (config.folders.output.isBlank()) {
            errors.add("folders.output path cannot be empty")
        } else {
            errors += folderValidationErrors(config.folders.output, "folders.output", requireWritable = true)
        }

        if (config.folders.temp.isBlank()) {
            errors.add("folders.temp path cannot be empty")
        } else {
            errors += folderValidationErrors(config.folders.temp, "folders.temp", requireWritable = true)
        }

        if (config.llm.endpoint.isBlank()) {
            errors.add("llm.endpoint cannot be empty")
        }
        if (config.llm.model.isBlank()) {
            errors.add("llm.model cannot be empty")
        }

        if (config.processing.maxParallelTasks < 1 || config.processing.maxParallelTasks > 128) {
            errors.add("processing.maxParallelTasks must be between 1 and 128, got ${config.processing.maxParallelTasks}")
        }

        if (config.processing.jpegQuality < 1 || config.processing.jpegQuality > 100) {
            errors.add("processing.jpegQuality must be between 1 and 100, got ${config.processing.jpegQuality}")
        }

        if (config.processing.outputFormat !in listOf("original", "jpeg", "png")) {
            errors.add("processing.outputFormat must be 'original', 'jpeg', or 'png', got '${config.processing.outputFormat}'")
        }

        return if (errors.isEmpty()) {
            Result.Success(Unit)
        } else {
            Result.Failure(
                ConfigurationException("Configuration validation failed:\n${errors.joinToString("\n")}")
            )
        }
    }

    private fun folderValidationErrors(path: String, fieldName: String, requireWritable: Boolean): List<String> {
        val folder = File(path)
        val errors = mutableListOf<String>()

        if (!folder.exists()) {
            errors.add("$fieldName does not exist: $path")
            return errors
        }

        if (!folder.isDirectory) {
            errors.add("$fieldName is not a directory: $path")
            return errors
        }

        if (!folder.canRead()) {
            errors.add("$fieldName is not readable: $path")
        }

        if (requireWritable && !folder.canWrite()) {
            errors.add("$fieldName is not writable: $path")
        }

        return errors
    }
}
