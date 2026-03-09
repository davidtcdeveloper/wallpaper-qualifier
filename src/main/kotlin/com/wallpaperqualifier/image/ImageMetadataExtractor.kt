package com.wallpaperqualifier.image

import com.wallpaperqualifier.domain.ImageProcessingException
import com.wallpaperqualifier.domain.Result
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Extracts detailed metadata from image files.
 * Includes resolution, aspect ratio, color depth, bit depth, file size, etc.
 */
object ImageMetadataExtractor {

    data class DetailedMetadata(
        val path: String,
        val width: Int,
        val height: Int,
        val aspectRatio: Double,
        val colorDepth: Int,
        val bitDepth: Int,
        val dpi: Int = 72,
        val fileSize: Long,
        val modificationDate: Long,
        val orientation: Int = 1 // Default: normal orientation
    )

    /**
     * Extract detailed metadata from an image file.
     *
     * @param path Path to image file
     * @return Result<DetailedMetadata> with all available properties
     */
    fun extractMetadata(path: String): Result<DetailedMetadata> {
        return try {
            val file = File(path)

            if (!file.exists()) {
                return Result.Failure(
                    ImageProcessingException("File not found: $path")
                )
            }

            if (!file.canRead()) {
                return Result.Failure(
                    ImageProcessingException("File not readable: $path")
                )
            }

            val bufferedImage = ImageIO.read(file)
                ?: return Result.Failure(
                    ImageProcessingException("Failed to load image: $path")
                )

            val width = bufferedImage.width
            val height = bufferedImage.height
            val aspectRatio = width.toDouble() / height.toDouble()
            val colorModel = bufferedImage.colorModel
            val colorDepth = colorModel.pixelSize
            val bitDepth = colorModel.componentSize.maxOrNull() ?: 8

            val metadata = DetailedMetadata(
                path = path,
                width = width,
                height = height,
                aspectRatio = aspectRatio,
                colorDepth = colorDepth,
                bitDepth = bitDepth,
                fileSize = file.length(),
                modificationDate = file.lastModified()
            )

            Result.Success(metadata)
        } catch (e: Exception) {
            Result.Failure(
                ImageProcessingException("Failed to extract metadata from: $path - ${e.message}", e)
            )
        }
    }

    /**
     * Extract metadata for multiple images.
     *
     * @param paths List of file paths
     * @return Result<List<DetailedMetadata>> with metadata for all successfully loaded images
     */
    fun extractMetadataBatch(paths: List<String>): Result<List<DetailedMetadata>> {
        val results = mutableListOf<DetailedMetadata>()
        val errors = mutableListOf<String>()

        for (path in paths) {
            when (val result = extractMetadata(path)) {
                is Result.Success -> results.add(result.value)
                is Result.Failure -> errors.add(path)
            }
        }

        return if (results.isEmpty()) {
            Result.Failure(
                ImageProcessingException(
                    "Failed to extract metadata from any files (${errors.size} errors)"
                )
            )
        } else {
            Result.Success(results)
        }
    }

    /**
     * Check if image meets minimum resolution requirements.
     */
    fun meetsResolutionRequirement(metadata: DetailedMetadata, minWidth: Int, minHeight: Int): Boolean {
        return metadata.width >= minWidth && metadata.height >= minHeight
    }

    /**
     * Calculate image quality score based on resolution and file size.
     * Higher score = potentially better quality.
     */
    fun estimateQualityScore(metadata: DetailedMetadata): Float {
        val megapixels = (metadata.width * metadata.height) / 1_000_000.0
        val megabytes = metadata.fileSize / (1024.0 * 1024.0)

        // Score based on megapixels and file size efficiency
        val mpScore = minOf(megapixels / 20.0, 1.0) // 20MP = max score
        val fileSizeScore = minOf(megabytes / 2.0, 1.0) // 2MB = max score
        val depthScore = metadata.colorDepth / 32.0 // Deeper color = better

        return (mpScore * 0.5 + fileSizeScore * 0.3 + depthScore * 0.2).toFloat()
    }
}
