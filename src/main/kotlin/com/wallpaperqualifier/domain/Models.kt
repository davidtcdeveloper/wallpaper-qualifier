package com.wallpaperqualifier.domain

import kotlinx.serialization.Serializable

/**
 * Represents the format of an image file.
 */
@Serializable
enum class ImageFormat {
    JPEG, PNG, HEIC, WEBP, TIFF, BMP, GIF, RAW, UNKNOWN
}

/**
 * Represents a loaded image with metadata.
 */
@Serializable
data class Image(
    val path: String,
    val format: ImageFormat,
    val width: Int,
    val height: Int,
    val fileSize: Long,
    val aspectRatio: Float,
    val hash: String? = null // For duplicate detection
) {
    companion object {
        fun create(
            path: String,
            format: ImageFormat,
            width: Int,
            height: Int,
            fileSize: Long
        ): Image {
            val aspectRatio = width.toFloat() / height.toFloat()
            return Image(
                path = path,
                format = format,
                width = width,
                height = height,
                fileSize = fileSize,
                aspectRatio = aspectRatio
            )
        }
    }
}

/**
 * Represents characteristics extracted from image analysis via LLM.
 */
@Serializable
data class ImageCharacteristics(
    val colorPalette: List<String>,
    val style: String,
    val mood: String,
    val composition: String,
    val subject: String,
    val technicalNotes: String,
    val quality: Float, // 0-1 confidence
    val extractedAt: Long = System.currentTimeMillis()
)

/**
 * Resolution range preferences for filtering images.
 */
@Serializable
data class ResolutionRange(
    val minWidth: Int = 1920,
    val minHeight: Int = 1080,
    val maxWidth: Int = 3840,
    val maxHeight: Int = 2160
)

/**
 * Represents aggregated user preferences from sample analysis.
 */
@Serializable
data class QualityProfile(
    val preferredColorPalettes: List<String>,
    val preferredStyles: List<String>,
    val preferredMoods: List<String>,
    val preferredCompositions: List<String>,
    val commonSubjects: List<String>,
    val resolutionPreferences: ResolutionRange,
    val averageQuality: Float,
    val generatedAt: Long = System.currentTimeMillis(),
    val sampleCount: Int
)

/**
 * Represents the evaluation result for a candidate image.
 */
@Serializable
data class EvaluationResult(
    val imagePath: String,
    val qualified: Boolean,
    val confidenceScore: Float, // 0-1
    val reasoning: String,
    val evaluatedAt: Long = System.currentTimeMillis()
)

/**
 * Result type for error handling - either Success or Failure.
 */
sealed class Result<T> {
    data class Success<T>(val value: T) : Result<T>()
    data class Failure<T>(val error: Throwable) : Result<T>()

    fun getOrNull(): T? = when (this) {
        is Success -> value
        is Failure -> null
    }

    fun getOrThrow(): T = when (this) {
        is Success -> value
        is Failure -> throw error
    }

    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Result.Success(transform(value))
        is Failure -> Result.Failure(error)
    }

    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(value)
        return this
    }

    inline fun onFailure(action: (Throwable) -> Unit): Result<T> {
        if (this is Failure) action(error)
        return this
    }
}

/**
 * Custom exceptions for domain-specific errors.
 */

class ConfigurationException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

class ImageProcessingException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

open class LLMException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

class FileIOException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
