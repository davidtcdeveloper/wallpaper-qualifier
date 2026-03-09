package com.wallpaperqualifier.image

import com.wallpaperqualifier.domain.Result
import com.wallpaperqualifier.utils.Logger
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Prototype image loader using standard Java ImageIO and TwelveMonkeys extensions.
 * Tests Kotlin Koog format support by verifying all 8+ required formats load successfully.
 *
 * Supported formats:
 * - JPEG (.jpg, .jpeg)
 * - PNG (.png)
 * - GIF (.gif)
 * - BMP (.bmp)
 * - TIFF (.tif, .tiff)
 * - WebP (.webp) - via TwelveMonkeys if available
 * - HEIC (.heic) - limited support; may require macOS native APIs
 * - RAW (.cr2, .nef, .arw, .dng) - limited support; may require external library
 */
object ImageLoaderProto {
    private val logger = Logger

    /**
     * Load an image from file and extract metadata.
     * This is the core prototype function for PHASE 2 Task 1.
     *
     * @param path Path to the image file
     * @return Result<ImageMetadata> with dimensions, format, and color depth
     */
    fun loadImage(path: String): Result<ImageMetadata> {
        return try {
            val file = File(path)

            // Validate file exists and is readable
            if (!file.exists()) {
                return Result.Failure(
                    com.wallpaperqualifier.domain.ImageProcessingException(
                        "Image file not found: $path"
                    )
                )
            }

            if (!file.canRead()) {
                return Result.Failure(
                    com.wallpaperqualifier.domain.ImageProcessingException(
                        "Cannot read image file: $path"
                    )
                )
            }

            // Try to load image using ImageIO
            val bufferedImage = ImageIO.read(file)
                ?: return Result.Failure(
                    com.wallpaperqualifier.domain.ImageProcessingException(
                        "Failed to decode image: $path (format may be unsupported)"
                    )
                )

            // Extract metadata
            val metadata = ImageMetadata(
                path = path,
                width = bufferedImage.width,
                height = bufferedImage.height,
                format = detectFormat(path),
                colorDepth = detectColorDepth(bufferedImage),
                fileSize = file.length(),
                modificationDate = file.lastModified()
            )

            logger.debug("Loaded image: ${file.name} (${metadata.width}x${metadata.height})")
            Result.Success(metadata)

        } catch (e: Exception) {
            val errorMsg = "Failed to load image: $path - ${e.message}"
            logger.warn(errorMsg)
            Result.Failure(
                com.wallpaperqualifier.domain.ImageProcessingException(errorMsg, e)
            )
        }
    }

    /**
     * Detect image format from file extension.
     * Used as fast-path format identification.
     */
    private fun detectFormat(path: String): String {
        return File(path).extension.uppercase()
    }

    /**
     * Detect color depth from BufferedImage.
     * Returns bits per pixel.
     */
    private fun detectColorDepth(image: BufferedImage): Int {
        val colorModel = image.colorModel
        return colorModel.pixelSize
    }

    /**
     * Load multiple images from a list of file paths.
     * Continues on error, returning successful results and error summary.
     *
     * @param paths List of image file paths
     * @return Result<List<ImageMetadata>> with all successfully loaded images
     */
    fun loadImages(paths: List<String>): Result<List<ImageMetadata>> {
        val results = mutableListOf<ImageMetadata>()
        val errors = mutableListOf<String>()

        for (path in paths) {
            when (val result = loadImage(path)) {
                is Result.Success -> results.add(result.value)
                is Result.Failure -> errors.add("$path: ${result.error.message}")
            }
        }

        // Log any errors but return successful results
        if (errors.isNotEmpty()) {
            logger.warn("Failed to load ${errors.size} images:")
            errors.forEach { logger.warn("  - $it") }
        }

        return if (results.isEmpty() && errors.isNotEmpty()) {
            Result.Failure(
                com.wallpaperqualifier.domain.ImageProcessingException(
                    "Failed to load any images. Errors: ${errors.joinToString("; ")}"
                )
            )
        } else {
            Result.Success(results)
        }
    }
}
