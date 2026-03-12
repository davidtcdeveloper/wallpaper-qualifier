package com.wallpaperqualifier.llm

import com.wallpaperqualifier.domain.EvaluationResult
import com.wallpaperqualifier.domain.Image
import com.wallpaperqualifier.domain.ImageCharacteristics
import com.wallpaperqualifier.domain.QualityProfile
import com.wallpaperqualifier.domain.Result
import java.io.File

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
        val canonicalPath = File(image.path).canonicalPath
        val response = analysisResponses[image.path] ?: analysisResponses[canonicalPath] ?: ImageCharacteristics(
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
        val canonicalPath = File(image.path).canonicalPath
        val filename = File(image.path).name
        
        // Match by full path, canonical path, or filename (for temp files)
        val response = evaluationResponses[image.path] 
            ?: evaluationResponses[canonicalPath]
            ?: evaluationResponses.entries.find { (key, _) -> 
                key == filename || filename.startsWith(File(key).nameWithoutExtension)
            }?.value
            ?: EvaluationResult(
                imagePath = image.path,
                qualified = true,
                confidenceScore = 0.8f,
                reasoning = "Auto-generated fake response"
            )
        return Result.Success(response)
    }
}
