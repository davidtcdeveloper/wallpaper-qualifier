package com.wallpaperqualifier.workflow

import com.wallpaperqualifier.domain.EvaluationResult
import com.wallpaperqualifier.domain.QualityProfile
import com.wallpaperqualifier.domain.Result
import com.wallpaperqualifier.image.FileIOCoordinator
import com.wallpaperqualifier.image.ImageConverter
import com.wallpaperqualifier.image.ImageLoader
import com.wallpaperqualifier.image.TempFileManager
import com.wallpaperqualifier.llm.LLMService
import com.wallpaperqualifier.utils.Logger

/**
 * Orchestrates the evaluation of candidate images against a quality profile.
 * Coordinates image loading, conversion, and LLM evaluation.
 */
class CandidateEvaluationWorkflow(
    private val loader: ImageLoader,
    private val coordinator: FileIOCoordinator,
    private val converter: ImageConverter,
    private val tempFileManager: TempFileManager,
    private val llmService: LLMService,
    private val logger: Logger
) {

    /**
     * Evaluate candidate images against the given quality profile.
     *
     * @param candidatesPath Directory containing candidate wallpapers
     * @param profile Quality profile to evaluate against
     * @return Result<List<EvaluationResult>> containing the evaluation results
     */
    suspend fun evaluateCandidates(
        candidatesPath: String,
        profile: QualityProfile
    ): Result<List<EvaluationResult>> {
        logger.info("Starting candidate evaluation in: $candidatesPath")

        // 1. Discover candidate images
        val discoveryResult = loader.discoverImages(candidatesPath)
        if (discoveryResult is Result.Failure) {
            return Result.Failure(discoveryResult.error)
        }
        val images = (discoveryResult as Result.Success).value
        logger.info("Discovered ${images.size} candidate images")

        // 2. Process each image in parallel
        val evaluationResults = coordinator.processBatch(
            imagePaths = images.map { it.path },
            phase = "Evaluating Candidates"
        ) { path ->
            val image = images.find { it.path == path }!!
            
            // a. Convert to LLM-ready format
            val targetFormat = if (converter.estimateTargetFormat(path) == ImageConverter.TargetFormat.JPEG) "jpg" else "png"
            val tempFileResult = tempFileManager.createTempFile(path, targetFormat)
            if (tempFileResult is Result.Failure) return@processBatch Result.Failure(tempFileResult.error)
            val tempPath = (tempFileResult as Result.Success).value

            val conversionResult = converter.convertImage(path, tempPath)
            if (conversionResult is Result.Failure) {
                tempFileManager.cleanupFile(tempPath)
                val error = (conversionResult as Result.Failure).error
                return@processBatch Result.Failure(Exception("Image conversion failed: ${error.message}", error))
            }

            // b. Evaluate with LLM
            val llmImage = image.copy(path = tempPath)
            val evaluationResult = llmService.evaluateCandidateImage(llmImage, profile)

            // c. Cleanup temp file
            tempFileManager.cleanupFile(tempPath)
            
            if (evaluationResult is Result.Failure) {
                val error = (evaluationResult as Result.Failure).error
                return@processBatch Result.Failure(Exception("LLM evaluation failed: ${error.message}", error))
            }
            
            // Re-map evaluation result path to original path for curation
            (evaluationResult as Result.Success).value.copy(imagePath = path).let { Result.Success(it) }
        }

        return when (evaluationResults) {
            is Result.Success -> {
                logger.info("Successfully evaluated ${evaluationResults.value.size} candidates")
                Result.Success(evaluationResults.value)
            }
            is Result.Failure -> Result.Failure((evaluationResults as Result.Failure).error)
        }
    }
}
