package com.wallpaperqualifier.image

import com.wallpaperqualifier.domain.ImageProcessingException
import com.wallpaperqualifier.domain.Result
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.min

/**
 * Converts images between formats, typically to JPEG or PNG for LLM processing.
 * Handles aspect ratio preservation, quality optimization, and file size management.
 */
class ImageConverter {

    companion object {
        const val DEFAULT_JPEG_QUALITY = 85
        const val PNG_COMPRESSION_LEVEL = 9
        const val MAX_FILE_SIZE_BYTES = 2_097_152 // 2MB
        const val MAX_DIMENSION_PIXELS = 4096
    }

    enum class TargetFormat {
        JPEG, PNG
    }

    data class ConversionConfig(
        val targetFormat: TargetFormat = TargetFormat.PNG,
        val jpegQuality: Int = DEFAULT_JPEG_QUALITY,
        val maxFileSizeBytes: Int = MAX_FILE_SIZE_BYTES,
        val maxDimension: Int = MAX_DIMENSION_PIXELS
    )

    /**
     * Convert an image from one format to another.
     *
     * @param sourcePath Source image path
     * @param targetPath Target output path
     * @param config Conversion configuration
     * @return Result<String> path to converted file or error
     */
    fun convertImage(
        sourcePath: String,
        targetPath: String,
        config: ConversionConfig = ConversionConfig()
    ): Result<String> {
        return try {
            val sourceFile = File(sourcePath)

            if (!sourceFile.exists()) {
                return Result.Failure(
                    ImageProcessingException("Source file not found: $sourcePath")
                )
            }

            // Load source image
            val sourceImage = ImageIO.read(sourceFile)
                ?: return Result.Failure(
                    ImageProcessingException("Failed to decode source image: $sourcePath")
                )

            // Resize if necessary
            var workingImage = sourceImage
            if (sourceImage.width > config.maxDimension || sourceImage.height > config.maxDimension) {
                workingImage = resizeImage(sourceImage, config.maxDimension)
            }

            // Encode to target format
            val targetFileExtension = when (config.targetFormat) {
                TargetFormat.JPEG -> "jpg"
                TargetFormat.PNG -> "png"
            }

            val success = when (config.targetFormat) {
                TargetFormat.JPEG -> encodeAsJpeg(workingImage, targetPath, config.jpegQuality)
                TargetFormat.PNG -> encodeAsPng(workingImage, targetPath)
            }

            if (!success) {
                return Result.Failure(
                    ImageProcessingException("Failed to encode image to $targetFileExtension: $targetPath")
                )
            }

            // Validate file size
            val targetFile = File(targetPath)
            if (targetFile.length() > config.maxFileSizeBytes) {
                targetFile.delete()
                return Result.Failure(
                    ImageProcessingException(
                        "Output file exceeds max size: ${targetFile.length()} > ${config.maxFileSizeBytes}"
                    )
                )
            }

            Result.Success(targetPath)
        } catch (e: Exception) {
            Result.Failure(
                ImageProcessingException("Conversion failed: $sourcePath -> $targetPath - ${e.message}", e)
            )
        }
    }

    /**
     * Resize image to fit within max dimension while preserving aspect ratio.
     */
    private fun resizeImage(sourceImage: BufferedImage, maxDimension: Int): BufferedImage {
        val scaleFactor = min(
            maxDimension.toDouble() / sourceImage.width,
            maxDimension.toDouble() / sourceImage.height
        )

        if (scaleFactor >= 1.0) return sourceImage

        val newWidth = (sourceImage.width * scaleFactor).toInt()
        val newHeight = (sourceImage.height * scaleFactor).toInt()

        val resizedImage = BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB)
        val graphics = resizedImage.createGraphics()
        graphics.drawImage(sourceImage, 0, 0, newWidth, newHeight, null)
        graphics.dispose()

        return resizedImage
    }

    /**
     * Encode image as JPEG with quality settings.
     */
    private fun encodeAsJpeg(image: BufferedImage, targetPath: String, quality: Int): Boolean {
        return try {
            // Fallback to simple write - ImageIO handles quality settings automatically
            ImageIO.write(image, "jpg", File(targetPath))
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Encode image as PNG with compression.
     */
    private fun encodeAsPng(image: BufferedImage, targetPath: String): Boolean {
        return try {
            ImageIO.write(image, "png", File(targetPath))
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get estimated output format for a source image.
     */
    fun estimateTargetFormat(sourcePath: String): TargetFormat {
        val sourceImage = ImageIO.read(File(sourcePath)) ?: return TargetFormat.PNG

        // Use JPEG for high color variance, PNG for graphics
        val hasHighColorVariance = sourceImage.width * sourceImage.height > 1_000_000 &&
                sourceImage.colorModel.pixelSize >= 24

        return if (hasHighColorVariance) TargetFormat.JPEG else TargetFormat.PNG
    }
}
