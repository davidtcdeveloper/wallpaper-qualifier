package com.wallpaperqualifier.llm

import com.wallpaperqualifier.domain.EvaluationResult
import com.wallpaperqualifier.domain.Image
import com.wallpaperqualifier.domain.ImageCharacteristics
import com.wallpaperqualifier.domain.QualityProfile
import com.wallpaperqualifier.domain.Result

/**
 * A fake implementation of LLMService for integration testing.
 * Provides deterministic responses without making network calls.
 */
class FakeLLMService : LLMService {

    private val analysisResponses = mutableMapOf<String, ImageCharacteristics>()
    private val evaluationResponses = mutableMapOf<String, EvaluationResult>()

    fun setAnalysisResponse(path: String, characteristics: ImageCharacteristics) {
        analysisResponses[path] = characteristics
    }

    fun setEvaluationResponse(path: String, result: EvaluationResult) {
        evaluationResponses[path] = result
    }

    override suspend fun analyzeSampleImage(image: Image): Result<ImageCharacteristics> {
        val response = analysisResponses[image.path] ?: ImageCharacteristics(
            colorPalette = listOf("#000000"),
            style = "Default",
            mood = "Neutral",
            composition = "Centered",
            subject = "Unknown",
            technicalNotes = "Auto-generated fake response",
            quality = 0.5f
        )
        return Result.Success(response)
    }

    override suspend fun evaluateCandidateImage(
        image: Image,
        profile: QualityProfile
    ): Result<EvaluationResult> {
        val response = evaluationResponses[image.path] ?: EvaluationResult(
            imagePath = image.path,
            qualified = true,
            confidenceScore = 0.8f,
            reasoning = "Auto-generated fake response"
        )
        return Result.Success(response)
    }
}
