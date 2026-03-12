package com.wallpaperqualifier.workflow

import com.wallpaperqualifier.config.AppConfig
import com.wallpaperqualifier.domain.QualityProfile
import com.wallpaperqualifier.domain.Result
import com.wallpaperqualifier.image.DuplicateDetector
import com.wallpaperqualifier.image.FileIOCoordinator
import com.wallpaperqualifier.image.FormatDetector
import com.wallpaperqualifier.image.ImageConverter
import com.wallpaperqualifier.image.ImageLoader
import com.wallpaperqualifier.image.ImageLoaderProto
import com.wallpaperqualifier.image.TempFileManager
import com.wallpaperqualifier.llm.LLMService
import com.wallpaperqualifier.llm.createDefaultLLMService
import com.wallpaperqualifier.profile.ProfileGenerator
import com.wallpaperqualifier.profile.ProfileStorage
import com.wallpaperqualifier.utils.Logger
import java.io.File

/**
 * High-level component coordinating the entire wallpaper qualification process.
 * Ties all phases together: discovery, analysis, evaluation, and curation.
 */
class WorkflowOrchestrator(
    private val config: AppConfig,
    private val logger: Logger,
    private val llmService: LLMService = createDefaultLLMService(config, logger)
) {

    private val tempFileManager = TempFileManager(logger, config.folders.temp)
    private val coordinator = FileIOCoordinator(logger, maxThreads = config.processing.maxParallelTasks)
    private val loader = ImageLoader(logger, ImageLoaderProto(logger))
    private val converter = ImageConverter()
    private val profileGenerator = ProfileGenerator()
    private val profileStorage = ProfileStorage(logger)
    private val duplicateDetector = DuplicateDetector()

    private val sampleAnalysis = SampleAnalysisWorkflow(
        loader, coordinator, converter, tempFileManager, llmService, profileGenerator, logger
    )
    
    private val candidateEvaluation = CandidateEvaluationWorkflow(
        loader, coordinator, converter, tempFileManager, llmService, logger
    )
    
    private val curation = CurationWorkflow(
        duplicateDetector, logger
    )

    /**
     * Run the full wallpaper qualification pipeline.
     */
    suspend fun runFullWorkflow(): Result<CurationWorkflow.CurationSummary> {
        logger.info("--- Starting Full Wallpaper Qualification Workflow ---")

        try {
            // 1. Initialization
            val initResult = initialize()
            if (initResult is Result.Failure) return Result.Failure(initResult.error)

            // 2. Sample Analysis & Profile Generation
            val profileFile = File(config.folders.output, "quality-profile.json")
            val profile = if (profileFile.exists()) {
                logger.info("Found existing quality profile, skipping sample analysis.")
                profileStorage.load(profileFile).getOrThrow()
            } else {
                val profileResult = sampleAnalysis.analyzeSamples(config.folders.samples)
                if (profileResult is Result.Failure) return Result.Failure(profileResult.error)
                val p = profileResult.value
                profileStorage.save(p, profileFile)
                p
            }

            // 3. Candidate Evaluation
            val evaluationResult = candidateEvaluation.evaluateCandidates(config.folders.candidates, profile)
            if (evaluationResult is Result.Failure) return Result.Failure(evaluationResult.error)
            val results = evaluationResult.value

            // 4. Curation
            val curationResult = curation.curate(results, config.folders.output, config.processing.confidenceThreshold)
            
            logger.info("--- Workflow Completed Successfully ---")
            return curationResult
        } finally {
            // Cleanup all temp files
            tempFileManager.cleanupAll()
            coordinator.shutdown()
        }
    }

    /**
     * Initialize folders and validate configuration.
     */
    private fun initialize(): Result<Unit> {
        val folders = listOf(
            config.folders.samples,
            config.folders.candidates,
            config.folders.output,
            config.folders.temp
        )

        for (path in folders) {
            val dir = File(path)
            if (!dir.exists() && !dir.mkdirs()) {
                return Result.Failure(Exception("Failed to ensure directory exists: $path"))
            }
        }

        return tempFileManager.validateTempDirectory()
    }
}
