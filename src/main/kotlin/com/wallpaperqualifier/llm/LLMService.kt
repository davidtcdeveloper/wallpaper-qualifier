package com.wallpaperqualifier.llm

import com.wallpaperqualifier.config.AppConfig
import com.wallpaperqualifier.domain.EvaluationResult
import com.wallpaperqualifier.domain.Image
import com.wallpaperqualifier.domain.ImageCharacteristics
import com.wallpaperqualifier.domain.LLMException
import com.wallpaperqualifier.domain.QualityProfile
import com.wallpaperqualifier.domain.Result
import com.wallpaperqualifier.utils.Logger

/**
 * Public LLM service interface consumed by Phase 4 workflows.
 *
 * This interface is intentionally framework-agnostic: it does not expose HTTP,
 * Koog, or LMStudio details. Implementations are responsible for ensuring
 * sequential request processing and correct error handling.
 */
interface LLMService {

    /**
     * Analyze a sample wallpaper image and extract characteristics.
     */
    suspend fun analyzeSampleImage(image: Image): Result<ImageCharacteristics>

    /**
     * Evaluate a candidate wallpaper against the given quality profile.
     */
    suspend fun evaluateCandidateImage(
        image: Image,
        profile: QualityProfile
    ): Result<EvaluationResult>
}

/**
 * Lightweight factory for creating the default LLM service implementation
 * backed by the LMStudio OpenAI-compatible HTTP API and an internal
 * sequential request queue.
 */
fun createDefaultLLMService(
    config: AppConfig,
    logger: Logger
): LLMService {
    val httpClient = LMStudioHttpClient(
        endpoint = config.llm.endpoint,
        model = config.llm.model,
        apiKey = config.llm.apiKey,
        logger = logger
    )

    val requestQueue = LLMRequestQueue(httpClient, logger)

    return DefaultLLMService(
        requestQueue = requestQueue,
        prompts = PromptTemplates,
        parser = LLMResponseParser(),
        logger = logger
    )
}

/**
 * Domain-level error type for LLM operations that can be used in tests and
 * higher-level workflows when inspecting failures.
 */
sealed class LLMError(message: String, cause: Throwable? = null) : LLMException(message, cause) {
    class Network(message: String, cause: Throwable? = null) : LLMError(message, cause)
    class Api(message: String) : LLMError(message)
    class Timeout(message: String) : LLMError(message)
    class InvalidResponse(message: String) : LLMError(message)
}

