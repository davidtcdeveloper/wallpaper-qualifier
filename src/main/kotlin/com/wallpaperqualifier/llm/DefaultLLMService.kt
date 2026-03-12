package com.wallpaperqualifier.llm

import com.wallpaperqualifier.domain.EvaluationResult
import com.wallpaperqualifier.domain.Image
import com.wallpaperqualifier.domain.ImageCharacteristics
import com.wallpaperqualifier.domain.ImageFormat
import com.wallpaperqualifier.domain.Result
import com.wallpaperqualifier.domain.QualityProfile
import com.wallpaperqualifier.image.ImageConverter
import com.wallpaperqualifier.image.TempFileManager
import com.wallpaperqualifier.utils.Logger
import java.io.File
import java.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Default implementation of [LLMService] backed by an HTTP client and a
 * sequential [LLMRequestQueue].
 */
class DefaultLLMService(
    private val requestQueue: LLMRequestQueue,
    private val prompts: PromptTemplates,
    private val parser: LLMResponseParser,
    private val logger: Logger
) : LLMService {

    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        // Start processing the queue in background
        scope.launch {
            requestQueue.run()
        }
    }

    override suspend fun analyzeSampleImage(image: Image): Result<ImageCharacteristics> {
        val promptText = prompts.sampleAnalysisPrompt()
        val imageDataUrlResult = encodeImageForLLM(image)
        if (imageDataUrlResult is Result.Failure) {
            return Result.Failure(imageDataUrlResult.error)
        }
        val imageDataUrl = imageDataUrlResult.value

        val request = LLMRequest(
            messages = listOf(
                ChatMessage(
                    role = "user",
                    contentParts = listOf(
                        ChatContentPart.Text(promptText),
                        ChatContentPart.ImageDataUrl(imageDataUrl)
                    )
                )
            )
        )

        val rawResponse = requestQueue.enqueue(request)
        return when (rawResponse) {
            is Result.Success -> parser.parseAnalysisResponse(rawResponse.value)
            is Result.Failure -> Result.Failure(rawResponse.error)
        }
    }

    override suspend fun evaluateCandidateImage(
        image: Image,
        profile: QualityProfile
    ): Result<EvaluationResult> {
        val promptText = prompts.candidateEvaluationPrompt(profile)
        val imageDataUrlResult = encodeImageForLLM(image)
        if (imageDataUrlResult is Result.Failure) {
            return Result.Failure(imageDataUrlResult.error)
        }
        val imageDataUrl = imageDataUrlResult.value

        val request = LLMRequest(
            messages = listOf(
                ChatMessage(
                    role = "user",
                    contentParts = listOf(
                        ChatContentPart.Text(promptText),
                        ChatContentPart.ImageDataUrl(imageDataUrl)
                    )
                )
            )
        )

        val rawResponse = requestQueue.enqueue(request)
        return when (rawResponse) {
            is Result.Success -> parser.parseEvaluationResponse(rawResponse.value, image.path)
            is Result.Failure -> Result.Failure(rawResponse.error)
        }
    }

    private companion object {
        // Upper bound to protect against accidentally huge images being sent to the LLM.
        private const val MAX_LLM_IMAGE_BYTES: Long = 10L * 1024L * 1024L // 10 MB
    }

    /**
     * Encode the given image path as a base64 data URL suitable for LMStudio.
     *
     * This does not resize or re-encode; Phase 2 is responsible for ensuring
     * the image at [image.path] is already a reasonable JPEG/PNG for LLM use.
     */
    private fun encodeImageForLLM(image: Image): Result<String> {
        return try {
            val file = File(image.path)
            if (!file.exists() || !file.isFile) {
                return Result.Failure(
                    LLMError.InvalidResponse("Image file for LLM does not exist: ${image.path}")
                )
            }

            if (file.length() > MAX_LLM_IMAGE_BYTES) {
                return Result.Failure(
                    LLMError.InvalidResponse(
                        "Image file for LLM is too large (${file.length()} bytes, max $MAX_LLM_IMAGE_BYTES)"
                    )
                )
            }

            val bytes = file.readBytes()
            val base64 = Base64.getEncoder().encodeToString(bytes)

            val mime = when (image.format) {
                ImageFormat.PNG -> "image/png"
                ImageFormat.JPEG,
                ImageFormat.HEIC,
                ImageFormat.WEBP,
                ImageFormat.TIFF,
                ImageFormat.BMP,
                ImageFormat.GIF,
                ImageFormat.RAW,
                ImageFormat.UNKNOWN -> "image/jpeg"
            }

            Result.Success("data:$mime;base64,$base64")
        } catch (e: Exception) {
            logger.error("Failed to encode image for LLM: ${image.path}", e)
            Result.Failure(
                LLMError.Network("Failed to encode image for LLM: ${e.message}", e)
            )
        }
    }
}

