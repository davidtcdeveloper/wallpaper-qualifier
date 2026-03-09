package com.wallpaperqualifier.image

import com.wallpaperqualifier.domain.ImageProcessingException
import com.wallpaperqualifier.domain.Result
import com.wallpaperqualifier.utils.Logger
import java.io.File
import java.util.*

/**
 * Manages creation and cleanup of temporary image files.
 * Ensures temp files are tracked and cleaned up properly.
 */
class TempFileManager(private val logger: Logger, private val baseTempDir: String = System.getProperty("java.io.tmpdir")) {

    private val tempFiles = mutableSetOf<String>()
    private val tempDir = File(baseTempDir, "wallpaper-qualifier-temp").apply {
        mkdirs()
    }

    init {
        logger.debug("Temp directory: ${tempDir.absolutePath}")
    }

    /**
     * Create a temporary file for an image.
     *
     * @param originalPath Original image file path
     * @param targetFormat Target file format (e.g., "png", "jpg")
     * @return Result<String> containing path to temp file or error
     */
    fun createTempFile(originalPath: String, targetFormat: String): Result<String> {
        return try {
            // Generate unique filename
            val originalName = File(originalPath).nameWithoutExtension
            val uuid = UUID.randomUUID().toString().take(8)
            val tempFilename = "$originalName-$uuid.$targetFormat"
            val tempFile = File(tempDir, tempFilename)

            // Ensure temp directory exists
            tempDir.mkdirs()

            logger.debug("Created temp file: ${tempFile.absolutePath}")
            tempFiles.add(tempFile.absolutePath)

            Result.Success(tempFile.absolutePath)
        } catch (e: Exception) {
            Result.Failure(
                ImageProcessingException("Failed to create temp file for: $originalPath - ${e.message}", e)
            )
        }
    }

    /**
     * Register an existing temp file for cleanup.
     */
    fun registerTempFile(path: String) {
        tempFiles.add(path)
        logger.debug("Registered temp file: $path")
    }

    /**
     * Get all tracked temp files.
     */
    fun getTempFiles(): List<String> {
        return tempFiles.toList()
    }

    /**
     * Clean up a specific temp file.
     *
     * @param path Path to temp file
     * @return Result<Unit> success or error
     */
    fun cleanupFile(path: String): Result<Unit> {
        return try {
            val file = File(path)
            if (file.exists()) {
                val deleted = file.delete()
                if (deleted) {
                    tempFiles.remove(path)
                    logger.debug("Deleted temp file: $path")
                    Result.Success(Unit)
                } else {
                    logger.warn("Failed to delete temp file: $path")
                    Result.Failure(
                        ImageProcessingException("Failed to delete temp file: $path")
                    )
                }
            } else {
                tempFiles.remove(path)
                Result.Success(Unit)
            }
        } catch (e: Exception) {
            logger.error("Error cleaning up temp file: $path", e)
            Result.Failure(
                ImageProcessingException("Failed to cleanup temp file: $path - ${e.message}", e)
            )
        }
    }

    /**
     * Clean up all tracked temp files.
     *
     * @return Result<Unit> with count of deleted files
     */
    fun cleanupAll(): Result<Unit> {
        return try {
            var deletedCount = 0
            val failedPaths = mutableListOf<String>()

            tempFiles.forEach { path ->
                try {
                    val file = File(path)
                    if (file.exists() && file.delete()) {
                        deletedCount++
                    } else if (file.exists()) {
                        failedPaths.add(path)
                    }
                } catch (e: Exception) {
                    failedPaths.add(path)
                }
            }

            tempFiles.clear()
            logger.info("Cleaned up $deletedCount temp files")

            if (failedPaths.isNotEmpty()) {
                logger.warn("Failed to delete ${failedPaths.size} temp files")
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(
                ImageProcessingException("Failed to cleanup temp files - ${e.message}", e)
            )
        }
    }

    /**
     * Get size of all temp files.
     */
    fun getTempFileSize(): Long {
        return tempFiles.sumOf { path ->
            val file = File(path)
            if (file.exists()) file.length() else 0L
        }
    }

    /**
     * Validate that temp directory is writable.
     */
    fun validateTempDirectory(): Result<Unit> {
        return try {
            if (!tempDir.exists()) {
                tempDir.mkdirs()
            }
            if (!tempDir.canWrite()) {
                return Result.Failure(
                    ImageProcessingException("Temp directory not writable: ${tempDir.absolutePath}")
                )
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(
                ImageProcessingException("Failed to validate temp directory - ${e.message}", e)
            )
        }
    }
}
