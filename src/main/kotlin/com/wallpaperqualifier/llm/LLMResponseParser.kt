package com.wallpaperqualifier.llm

import com.wallpaperqualifier.domain.EvaluationResult
import com.wallpaperqualifier.domain.ImageCharacteristics
import com.wallpaperqualifier.domain.Result
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Strict JSON parsing for LLM responses.
 */
class LLMResponseParser(
    private val json: Json = Json {
        ignoreUnknownKeys = false
        isLenient = false
    }
) {

    /**
     * Minimal representation of an OpenAI/LMStudio chat completion response.
     * We only care about the first choice's first text content part.
     */
    @Serializable
    private data class ChatCompletionResponse(
        val choices: List<Choice>
    ) {
        @Serializable
        data class Choice(
            val message: Message
        )

        @Serializable
        data class Message(
            val content: List<ContentPart>
        )

        @Serializable
        data class ContentPart(
            val type: String,
            val text: String? = null
        )
    }

    @Serializable
    private data class AnalysisResponseDto(
        val colorPalette: List<String>,
        val style: String,
        val mood: String,
        val composition: String,
        val subject: String,
        val technicalNotes: String,
        val quality: Float
    )

    @Serializable
    private data class EvaluationResponseDto(
        val qualified: Boolean,
        @SerialName("confidenceScore")
        val confidenceScore: Float,
        val reasoning: String
    )

    fun parseAnalysisResponse(body: String): Result<ImageCharacteristics> {
        return try {
            val contentJson = extractContentJson(body).getOrThrow()
            val dto = json.decodeFromString<AnalysisResponseDto>(contentJson)

            if (dto.quality !in 0f..1f) {
                return Result.Failure(
                    LLMError.InvalidResponse("quality must be between 0.0 and 1.0, got ${dto.quality}")
                )
            }

            Result.Success(
                ImageCharacteristics(
                    colorPalette = dto.colorPalette,
                    style = dto.style,
                    mood = dto.mood,
                    composition = dto.composition,
                    subject = dto.subject,
                    technicalNotes = dto.technicalNotes,
                    quality = dto.quality
                )
            )
        } catch (e: SerializationException) {
            Result.Failure(
                LLMError.InvalidResponse("Failed to parse analysis response JSON: ${e.message}")
            )
        } catch (e: Exception) {
            Result.Failure(
                LLMError.InvalidResponse("Unexpected error parsing analysis response: ${e.message}")
            )
        }
    }

    fun parseEvaluationResponse(
        body: String,
        imagePath: String
    ): Result<EvaluationResult> {
        return try {
            val contentJson = extractContentJson(body).getOrThrow()
            val dto = json.decodeFromString<EvaluationResponseDto>(contentJson)

            if (dto.confidenceScore !in 0f..1f) {
                return Result.Failure(
                    LLMError.InvalidResponse(
                        "confidenceScore must be between 0.0 and 1.0, got ${dto.confidenceScore}"
                    )
                )
            }

            Result.Success(
                EvaluationResult(
                    imagePath = imagePath,
                    qualified = dto.qualified,
                    confidenceScore = dto.confidenceScore,
                    reasoning = dto.reasoning
                )
            )
        } catch (e: SerializationException) {
            Result.Failure(
                LLMError.InvalidResponse("Failed to parse evaluation response JSON: ${e.message}")
            )
        } catch (e: Exception) {
            Result.Failure(
                LLMError.InvalidResponse("Unexpected error parsing evaluation response: ${e.message}")
            )
        }
    }

    /**
     * Extract the inner JSON string from the first text content part of the
     * OpenAI/LMStudio chat completion response.
     */
    private fun extractContentJson(body: String): Result<String> {
        return try {
            val envelope = json.decodeFromString<ChatCompletionResponse>(body)
            val contentText = envelope.choices
                .firstOrNull()
                ?.message
                ?.content
                ?.firstOrNull { it.type == "text" && it.text != null }
                ?.text

            if (contentText.isNullOrBlank()) {
                Result.Failure(
                    LLMError.InvalidResponse("No text content found in LLM response.")
                )
            } else {
                Result.Success(contentText)
            }
        } catch (e: SerializationException) {
            Result.Failure(
                LLMError.InvalidResponse("Failed to parse LLM envelope JSON: ${e.message}")
            )
        }
    }
}

