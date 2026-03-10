package com.wallpaperqualifier.image

import com.wallpaperqualifier.domain.ImageProcessingException
import com.wallpaperqualifier.domain.Result
import java.io.File
import java.security.MessageDigest

/**
 * Detects duplicate or near-identical images using file hash.
 * Uses SHA-256 for exact duplicate detection (fast, efficient).
 */
class DuplicateDetector(
    private val digestFactory: () -> MessageDigest = { MessageDigest.getInstance("SHA-256") }
) {

    companion object {
        private const val BUFFER_SIZE = 8192
    }

    private fun newDigest(): MessageDigest = digestFactory()

    /**
     * Compute SHA-256 hash of a file.
     *
     * @param path Path to file
     * @return Result<String> containing hex hash or error
     */
    fun computeFileHash(path: String): Result<String> {
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

            val messageDigest = newDigest()
            val buffer = ByteArray(BUFFER_SIZE)

            file.inputStream().use { fis ->
                while (true) {
                    val bytesRead = fis.read(buffer)
                    if (bytesRead == -1) break
                    messageDigest.update(buffer, 0, bytesRead)
                }
            }

            val digest = messageDigest.digest()
            val hexHash = digest.joinToString("") { "%02x".format(it) }

            Result.Success(hexHash)
        } catch (e: Exception) {
            Result.Failure(
                ImageProcessingException("Failed to compute hash for: $path - ${e.message}", e)
            )
        }
    }

    /**
     * Find duplicate files from a list of paths.
     *
     * @param paths List of file paths
     * @return Result<Map<String, List<String>>> mapping hash to list of duplicate paths
     */
    fun findDuplicates(paths: List<String>): Result<Map<String, List<String>>> {
        return try {
            val hashToFiles = mutableMapOf<String, MutableList<String>>()

            for (path in paths) {
                when (val hashResult = computeFileHash(path)) {
                    is Result.Success -> {
                        val hash = hashResult.value
                        hashToFiles.getOrPut(hash) { mutableListOf() }.add(path)
                    }
                    is Result.Failure -> {
                        // Skip files that can't be hashed
                    }
                }
            }

            // Filter to only duplicates
            val duplicates = hashToFiles
                .filterValues { it.size > 1 }
                .toMap()

            Result.Success(duplicates)
        } catch (e: Exception) {
            Result.Failure(
                ImageProcessingException("Failed to find duplicates - ${e.message}", e)
            )
        }
    }

    /**
     * Check if two files are identical (by hash).
     */
    fun areIdentical(path1: String, path2: String): Result<Boolean> {
        return try {
            val hash1Result = computeFileHash(path1)
            val hash2Result = computeFileHash(path2)

            when {
                hash1Result is Result.Failure -> Result.Failure(hash1Result.error)
                hash2Result is Result.Failure -> Result.Failure(hash2Result.error)
                else -> {
                    val hash1 = (hash1Result as Result.Success).value
                    val hash2 = (hash2Result as Result.Success).value
                    Result.Success(hash1 == hash2)
                }
            }
        } catch (e: Exception) {
            Result.Failure(
                ImageProcessingException("Failed to compare files - ${e.message}", e)
            )
        }
    }
}
