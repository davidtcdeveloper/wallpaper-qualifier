package com.wallpaperqualifier.image

import com.wallpaperqualifier.domain.ImageFormat
import com.wallpaperqualifier.domain.ImageProcessingException
import com.wallpaperqualifier.domain.Result
import java.io.File

/**
 * Detects image format from file content (magic bytes) and metadata.
 * Prioritizes magic byte detection over file extension for accuracy.
 */
object FormatDetector {
    private const val MAGIC_BYTES_TO_READ = 32

    /**
     * Detect image format by examining file magic bytes.
     * Falls back to extension detection if magic bytes don't match.
     *
     * @param path Path to image file
     * @return Result<ImageFormat> or error if detection fails
     */
    fun detectFormat(path: String): Result<ImageFormat> {
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

            val magicBytes = file.readBytes().take(MAGIC_BYTES_TO_READ).toByteArray()

            // Try magic byte detection first
            val formatFromMagic = detectByMagicBytes(magicBytes)
            if (formatFromMagic != ImageFormat.UNKNOWN) {
                return Result.Success(formatFromMagic)
            }

            // Fall back to extension detection
            val formatFromExtension = detectByExtension(file.extension)
            return if (formatFromExtension != ImageFormat.UNKNOWN) {
                Result.Success(formatFromExtension)
            } else {
                Result.Failure(
                    ImageProcessingException("Unsupported image format: $path")
                )
            }
        } catch (e: Exception) {
            Result.Failure(
                ImageProcessingException("Failed to detect format for: $path - ${e.message}", e)
            )
        }
    }

    /**
     * Detect format by examining file magic bytes (magic numbers).
     */
    private fun detectByMagicBytes(bytes: ByteArray): ImageFormat {
        if (bytes.isEmpty()) return ImageFormat.UNKNOWN

        return when {
            // JPEG: 0xFF 0xD8 0xFF
            bytes.size >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte() ->
                ImageFormat.JPEG

            // PNG: 0x89 0x50 0x4E 0x47
            bytes.size >= 4 && bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
                    bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte() ->
                ImageFormat.PNG

            // GIF: 0x47 0x49 0x46 ("GIF")
            bytes.size >= 3 && bytes[0] == 0x47.toByte() && bytes[1] == 0x49.toByte() && bytes[2] == 0x46.toByte() ->
                ImageFormat.GIF

            // BMP: 0x42 0x4D ("BM")
            bytes.size >= 2 && bytes[0] == 0x42.toByte() && bytes[1] == 0x4D.toByte() ->
                ImageFormat.BMP

            // TIFF (little-endian): 0x49 0x49 0x2A 0x00
            bytes.size >= 4 && bytes[0] == 0x49.toByte() && bytes[1] == 0x49.toByte() &&
                    bytes[2] == 0x2A.toByte() && bytes[3] == 0x00.toByte() ->
                ImageFormat.TIFF

            // TIFF (big-endian): 0x4D 0x4D 0x00 0x2A
            bytes.size >= 4 && bytes[0] == 0x4D.toByte() && bytes[1] == 0x4D.toByte() &&
                    bytes[2] == 0x00.toByte() && bytes[3] == 0x2A.toByte() ->
                ImageFormat.TIFF

            // WebP: "RIFF" followed by "WEBP"
            bytes.size >= 12 && bytes[0] == 0x52.toByte() && bytes[1] == 0x49.toByte() &&
                    bytes[2] == 0x46.toByte() && bytes[3] == 0x46.toByte() &&
                    bytes[8] == 0x57.toByte() && bytes[9] == 0x45.toByte() &&
                    bytes[10] == 0x42.toByte() && bytes[11] == 0x50.toByte() ->
                ImageFormat.WEBP

            // HEIC: Look for "ftyp" at offset 4 and "heic"
            bytes.size >= 12 && bytes[4] == 0x66.toByte() && bytes[5] == 0x74.toByte() &&
                    bytes[6] == 0x79.toByte() && bytes[7] == 0x70.toByte() &&
                    (containsSequence(bytes, "heic".toByteArray()) ||
                            containsSequence(bytes, "heif".toByteArray())) ->
                ImageFormat.HEIC

            // RAW formats - CR2 (Canon): 0x49 0x49 0x2A 0x00 + "CR" at specific offset
            bytes.size >= 10 && bytes[0] == 0x49.toByte() && bytes[1] == 0x49.toByte() &&
                    bytes[2] == 0x2A.toByte() && bytes[3] == 0x00.toByte() &&
                    bytes[8] == 0x43.toByte() && bytes[9] == 0x52.toByte() ->
                ImageFormat.RAW

            else -> ImageFormat.UNKNOWN
        }
    }

    /**
     * Helper: Check if bytes contain a sequence
     */
    private fun containsSequence(bytes: ByteArray, sequence: ByteArray): Boolean {
        if (sequence.isEmpty() || bytes.size < sequence.size) return false
        return (0..(bytes.size - sequence.size))
            .any { i -> (sequence.indices).all { j -> bytes[i + j] == sequence[j] } }
    }

    /**
     * Detect format by file extension (fallback).
     */
    private fun detectByExtension(extension: String): ImageFormat {
        return when (extension.lowercase()) {
            "jpg", "jpeg" -> ImageFormat.JPEG
            "png" -> ImageFormat.PNG
            "gif" -> ImageFormat.GIF
            "bmp" -> ImageFormat.BMP
            "tif", "tiff" -> ImageFormat.TIFF
            "webp" -> ImageFormat.WEBP
            "heic", "heif" -> ImageFormat.HEIC
            "cr2", "nef", "arw", "dng" -> ImageFormat.RAW
            else -> ImageFormat.UNKNOWN
        }
    }

    /**
     * Check if file is readable and appears to be a valid image.
     */
    fun isValidImageFile(path: String): Result<Unit> {
        return try {
            val file = File(path)

            when {
                !file.exists() -> Result.Failure(
                    ImageProcessingException("File not found: $path")
                )
                !file.canRead() -> Result.Failure(
                    ImageProcessingException("File not readable: $path")
                )
                file.length() == 0L -> Result.Failure(
                    ImageProcessingException("File is empty: $path")
                )
                else -> Result.Success(Unit)
            }
        } catch (e: Exception) {
            Result.Failure(
                ImageProcessingException("Failed to validate file: $path - ${e.message}", e)
            )
        }
    }

    /**
     * Get supported file extensions.
     */
    fun getSupportedExtensions(): List<String> {
        return listOf("jpg", "jpeg", "png", "gif", "bmp", "tif", "tiff", "webp", "heic", "heif", "cr2", "nef", "arw", "dng")
    }
}
