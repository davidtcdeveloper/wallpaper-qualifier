package com.wallpaperqualifier.profile

import com.wallpaperqualifier.domain.QualityProfile
import com.wallpaperqualifier.domain.Result
import com.wallpaperqualifier.domain.FileIOException
import com.wallpaperqualifier.utils.Logger
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

/**
 * Persists and loads quality profiles as JSON files.
 * Provides consistent serialization for storage and sharing.
 */
class ProfileStorage(
    private val logger: Logger,
    private val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
) {

    /**
     * Save the given quality profile to a JSON file.
     *
     * @param profile Profile to save
     * @param targetFile File where to save the profile
     * @return Result<Unit> indicating success or failure
     */
    fun save(profile: QualityProfile, targetFile: File): Result<Unit> {
        return try {
            val jsonString = json.encodeToString(profile)
            targetFile.writeText(jsonString)
            logger.info("Saved quality profile to: ${targetFile.absolutePath}")
            Result.Success(Unit)
        } catch (e: Exception) {
            logger.error("Failed to save quality profile: ${targetFile.absolutePath}", e)
            Result.Failure(FileIOException("Failed to save profile: ${e.message}", e))
        }
    }

    /**
     * Load a quality profile from a JSON file.
     *
     * @param sourceFile File from which to load the profile
     * @return Result<QualityProfile> containing the profile or an error
     */
    fun load(sourceFile: File): Result<QualityProfile> {
        return try {
            if (!sourceFile.exists()) {
                return Result.Failure(FileIOException("Profile file not found: ${sourceFile.absolutePath}"))
            }

            val jsonString = sourceFile.readText()
            val profile = json.decodeFromString<QualityProfile>(jsonString)
            logger.info("Loaded quality profile from: ${sourceFile.absolutePath}")
            Result.Success(profile)
        } catch (e: Exception) {
            logger.error("Failed to load quality profile: ${sourceFile.absolutePath}", e)
            Result.Failure(FileIOException("Failed to load profile: ${e.message}", e))
        }
    }
}
