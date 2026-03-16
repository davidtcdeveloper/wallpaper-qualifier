package com.wallpaperqualifier.workflow

import com.wallpaperqualifier.domain.EvaluationResult
import com.wallpaperqualifier.domain.FileIOException
import com.wallpaperqualifier.domain.Result
import com.wallpaperqualifier.image.DuplicateDetector
import com.wallpaperqualifier.utils.Logger
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Orchestrates the curation process: copying qualified images to the output folder.
 * Ensures atomic file operations and prevents duplicate copying.
 */
class CurationWorkflow(
    private val duplicateDetector: DuplicateDetector,
    private val logger: Logger
) {

    data class CurationSummary(
        val totalEvaluated: Int,
        val qualified: Int,
        val rejected: Int,
        val copied: Int,
        val duplicates: Int,
        val errors: Int
    )

    /**
     * Curate the evaluated candidates: copy qualified images to the output directory.
     *
     * @param results List of evaluation results
     * @param outputPath Destination directory for qualified wallpapers
     * @param confidenceThreshold Minimum confidence score to qualify (0.0 to 1.0)
     * @return Result<CurationSummary> summarizing the curation outcomes
     */
    fun curate(
        results: List<EvaluationResult>,
        outputPath: String,
        confidenceThreshold: Float = 0.5f
    ): Result<CurationSummary> {
        logger.info("Starting curation to: $outputPath")

        val outputDir = File(outputPath)
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            return Result.Failure(FileIOException("Failed to create output directory: $outputPath"))
        }

        var copied = 0
        var duplicates = 0
        var errors = 0

        val qualifiedResults = results.filter { it.qualified && it.confidenceScore >= confidenceThreshold }
        val rejectedCount = results.size - qualifiedResults.size

        logger.info("Found ${qualifiedResults.size} qualified candidates (threshold: $confidenceThreshold)")

        qualifiedResults.forEach { result ->
            val sourceFile = File(result.imagePath)
            if (!sourceFile.exists()) {
                logger.error("Source file for curation not found: ${result.imagePath}")
                errors++
                return@forEach
            }

            val targetFile = File(outputDir, sourceFile.name)

            // 1. Check for duplicates in output folder
            if (targetFile.exists()) {
                val isDuplicate = duplicateDetector.areIdentical(sourceFile.absolutePath, targetFile.absolutePath)
                if (isDuplicate is Result.Success && isDuplicate.value) {
                    logger.info("Skipping duplicate: ${sourceFile.name}")
                    duplicates++
                    return@forEach
                }

                // If not identical but filename exists, generate unique name
                // For MVP, we'll just skip or overwrite if name conflicts but content is different
                // Following Task 8: skip duplicate (default)
                logger.warn("Filename conflict but not identical: ${sourceFile.name}. Skipping to be safe.")
                duplicates++
                return@forEach
            }

            // 2. Atomic Copy (Task 9)
            val outcome = atomicCopy(sourceFile, targetFile)
            if (outcome is Result.Success) {
                copied++
                logger.info("✓ Copied: ${sourceFile.name}")
            } else {
                errors++
                val error = (outcome as Result.Failure).error
                logger.error("✗ Failed to copy ${sourceFile.name}: ${error.message}")
            }
        }

        val summary = CurationSummary(
            totalEvaluated = results.size,
            qualified = qualifiedResults.size,
            rejected = rejectedCount,
            copied = copied,
            duplicates = duplicates,
            errors = errors
        )

        logger.info("Curation complete: ${summary.copied} copied, ${summary.duplicates} duplicates, ${summary.errors} errors.")
        return Result.Success(summary)
    }
    /**
     * Perform an atomic copy: write to a temporary file in the same filesystem, then move.
     */
    private fun atomicCopy(source: File, destination: File): Result<Unit> {
        return try {
            val tempFile = File(destination.parentFile, ".tmp-${destination.name}")
            
            // 1. Copy to temp
            Files.copy(source.toPath(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            
            // 2. Atomic move
            Files.move(tempFile.toPath(), destination.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(FileIOException("Atomic copy failed: ${e.message}", e))
        }
    }
}
