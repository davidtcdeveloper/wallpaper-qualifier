package com.wallpaperqualifier.image

import com.wallpaperqualifier.domain.Image
import com.wallpaperqualifier.domain.ImageProcessingException
import com.wallpaperqualifier.domain.Result
import com.wallpaperqualifier.utils.Logger
import java.io.File

/**
 * Loads images from a directory recursively and returns Image objects with metadata.
 * Handles format filtering, error recovery, and progress reporting.
 */
class ImageLoader(
    private val logger: Logger,
    private val proto: ImageLoaderProto,
    private val formatDetector: FormatDetector = FormatDetector()
) {

    /**
     * Discover all images in a folder recursively.
     *
     * @param folderPath Path to folder containing images
     * @return Result<List<Image>> with all discovered images
     */
    fun discoverImages(folderPath: String): Result<List<Image>> {
        return try {
            val folder = File(folderPath)

            if (!folder.exists()) {
                return Result.Failure(
                    ImageProcessingException("Folder not found: $folderPath")
                )
            }

            if (!folder.isDirectory) {
                return Result.Failure(
                    ImageProcessingException("Path is not a directory: $folderPath")
                )
            }

            val images = mutableListOf<Image>()
            val errors = mutableListOf<String>()

            // Recursively find all images
            walkDirectory(folder) { file ->
                val imagePath = file.absolutePath
                when (val result = loadImageFile(imagePath)) {
                    is Result.Success -> images.add(result.value)
                    is Result.Failure -> {
                        val errorMsg = "${file.name}: ${result.error.message}"
                        errors.add(errorMsg)
                        logger.debug("Skipped image: $errorMsg")
                    }
                }
            }

            if (errors.isNotEmpty()) {
                logger.info("Discovered ${images.size} images (${errors.size} skipped)")
            }

            if (images.isEmpty()) {
                return Result.Failure(
                    ImageProcessingException(
                        "No valid images found in: $folderPath" +
                                if (errors.isNotEmpty()) " (${errors.size} files could not be loaded)" else ""
                    )
                )
            }

            Result.Success(images)
        } catch (e: Exception) {
            Result.Failure(
                ImageProcessingException("Failed to discover images in: $folderPath - ${e.message}", e)
            )
        }
    }

    /**
     * Load a single image file and return Image object.
     */
    private fun loadImageFile(imagePath: String): Result<Image> {
        // Validate file
        val validation = formatDetector.isValidImageFile(imagePath)
        if (validation is Result.Failure) {
            return Result.Failure(validation.error)
        }

        // Detect format
        val formatResult = formatDetector.detectFormat(imagePath)
        if (formatResult is Result.Failure) {
            return Result.Failure(formatResult.error)
        }

        val format = (formatResult as Result.Success).value

        // Load metadata using ImageLoaderProto
        val metadataResult = proto.loadImage(imagePath)
        if (metadataResult is Result.Failure) {
            return Result.Failure(metadataResult.error)
        }

        val metadata = (metadataResult as Result.Success).value
        val file = File(imagePath)

        // Create Image object
        val image = Image.create(
            path = imagePath,
            format = format,
            width = metadata.width,
            height = metadata.height,
            fileSize = file.length()
        )

            return Result.Success(image)
    }

    /**
     * Recursively walk directory and apply action to image files.
     */
    private fun walkDirectory(directory: File, action: (File) -> Unit) {
        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                // Recursive descent
                walkDirectory(file, action)
            } else if (isSupportedImageFile(file)) {
                action(file)
            }
        }
    }

    /**
     * Check if file has a supported image extension.
     */
    private fun isSupportedImageFile(file: File): Boolean {
        val supportedExtensions = formatDetector.getSupportedExtensions()
        return supportedExtensions.contains(file.extension.lowercase())
    }
}
