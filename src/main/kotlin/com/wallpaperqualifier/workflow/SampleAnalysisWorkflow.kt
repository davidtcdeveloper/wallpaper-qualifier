package com.wallpaperqualifier.workflow

import com.wallpaperqualifier.domain.ImageCharacteristics
import com.wallpaperqualifier.domain.QualityProfile
import com.wallpaperqualifier.domain.ResolutionRange
import com.wallpaperqualifier.domain.Result
import com.wallpaperqualifier.image.FileIOCoordinator
import com.wallpaperqualifier.image.ImageConverter
import com.wallpaperqualifier.image.ImageLoader
import com.wallpaperqualifier.image.TempFileManager
import com.wallpaperqualifier.llm.LLMService
import com.wallpaperqualifier.profile.ProfileGenerator
import com.wallpaperqualifier.utils.Logger

/**
 * Orchestrates the analysis of sample images to generate a quality profile.
 * Coordinates image loading, conversion, LLM analysis, and aggregation.
 */
class SampleAnalysisWorkflow(
    private val loader: ImageLoader,
    private val coordinator: FileIOCoordinator,
    private val converter: ImageConverter,
    private val tempFileManager: TempFileManager,
    private val llmService: LLMService,
    private val profileGenerator: ProfileGenerator,
    private val logger: Logger
) {

    /**
     * Analyze sample images and generate a quality profile.
     *
     * @param samplesPath Directory containing sample wallpapers
     * @param resolutionRange Desired resolution for the generated profile
     * @return Result<QualityProfile> with aggregated characteristics
     */
    suspend fun analyzeSamples(
        samplesPath: String,
        resolutionRange: ResolutionRange = ResolutionRange()
    ): Result<QualityProfile> {
        logger.info("Starting sample analysis in: $samplesPath")

        // 1. Discover sample images
        val discoveryResult = loader.discoverImages(samplesPath)
        if (discoveryResult is Result.Failure) {
            return Result.Failure(discoveryResult.error)
        }
        val images = (discoveryResult as Result.Success).value
        logger.info("Discovered ${images.size} sample images")

        // 2. Process each image in parallel
        val analysisResults = coordinator.processBatch(
            imagePaths = images.map { it.path },
            phase = "Analyzing Samples"
        ) { path ->
            val image = images.find { it.path == path }!!
            
            // a. Convert to LLM-ready format in temp
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

            // b. Analyze with LLM
            val llmImage = image.copy(path = tempPath)
            val analysisResult = llmService.analyzeSampleImage(llmImage)

            // c. Cleanup temp file
            tempFileManager.cleanupFile(tempPath)
            
            if (analysisResult is Result.Failure) {
                val error = (analysisResult as Result.Failure).error
                return@processBatch Result.Failure(Exception("LLM analysis failed: ${error.message}", error))
            }
            
            analysisResult
        }

        return when (analysisResults) {
            is Result.Success -> {
                val characteristics = analysisResults.value
                val profile = profileGenerator.aggregate(characteristics, resolutionRange)
                logger.info("Successfully generated profile from ${characteristics.size} samples")
                Result.Success(profile)
            }
            is Result.Failure -> Result.Failure((analysisResults as Result.Failure).error)
        }
    }
}
