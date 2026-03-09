package com.wallpaperqualifier.image

import com.wallpaperqualifier.domain.Result
import com.wallpaperqualifier.utils.Logger
import kotlinx.coroutines.*
import java.util.Collections
import java.util.concurrent.Semaphore
import kotlin.math.min

/**
 * Coordinates parallel image processing with configurable thread pool.
 * Manages batching, concurrency, progress tracking, and error recovery.
 */
class FileIOCoordinator(
    private val logger: Logger,
    private val maxThreads: Int = 8,
    private val batchSize: Int = 100
) {

    private val semaphore = Semaphore(maxThreads)
    private val progressReporter = ProgressReporter(logger)

    private data class ProcessingOutcome<T>(
        val path: String,
        val result: Result<T>
    )

    /**
     * Process a batch of images in parallel.
     *
     * @param imagePaths List of image file paths
     * @param processor Function to process each image
     * @return Result<List<T>> with processed results
     */
    suspend fun <T> processBatch(
        imagePaths: List<String>,
        phase: String = "Processing",
        processor: suspend (String) -> Result<T>
    ): Result<List<T>> {
        return try {
            val results = Collections.synchronizedList(mutableListOf<T>())
            val errors = Collections.synchronizedList(mutableListOf<String>())

            progressReporter.startPhase(phase, imagePaths.size)

            // Process in batches to manage memory
            imagePaths.chunked(batchSize).forEachIndexed { batchIndex, batch ->
                val batchNumber = batchIndex + 1
                val totalBatches = (imagePaths.size + batchSize - 1) / batchSize

                logger.debug("Processing batch $batchNumber/$totalBatches (${batch.size} items)")

                coroutineScope {
                    val deferredList = batch.map { path ->
                        async(Dispatchers.IO) {
                            semaphore.acquire()
                            try {
                                val result = processor(path)
                                progressReporter.reportItem(path)
                                ProcessingOutcome(path, result)
                            } finally {
                                semaphore.release()
                            }
                        }
                    }

                    // Wait for batch to complete
                    val batchResults = deferredList.awaitAll()
                    batchResults.forEach { outcome ->
                        when (val result = outcome.result) {
                            is Result.Success -> {
                                logger.debug("✓ Processed: ${outcome.path}")
                                results.add(result.value)
                            }
                            is Result.Failure -> {
                                val failureMessage = "${outcome.path}: ${result.error.message}"
                                errors.add(failureMessage)
                                logger.debug("✗ Failed: $failureMessage")
                            }
                        }
                    }
                }
            }

            progressReporter.endPhase()

            if (errors.isNotEmpty()) {
                logger.warn("Failed to process ${errors.size} items:")
                errors.take(5).forEach { logger.warn("  - $it") }
                if (errors.size > 5) {
                    logger.warn("  ... and ${errors.size - 5} more")
                }
            }

            if (results.isEmpty()) {
                Result.Failure(
                    Exception("No items processed successfully (${errors.size} errors)")
                )
            } else {
                Result.Success(results)
            }
        } catch (e: Exception) {
            Result.Failure(Exception("Batch processing failed: ${e.message}", e))
        }
    }

    /**
     * Process images sequentially (for operations that must be sequential).
     */
    suspend fun <T> processSequentially(
        imagePaths: List<String>,
        phase: String = "Processing",
        processor: suspend (String) -> Result<T>
    ): Result<List<T>> {
        return try {
            val results = mutableListOf<T>()
            val errors = mutableListOf<String>()

            progressReporter.startPhase(phase, imagePaths.size)

            for (path in imagePaths) {
                when (val result = processor(path)) {
                    is Result.Success -> {
                        results.add(result.value)
                        progressReporter.reportItem()
                    }
                    is Result.Failure -> {
                        errors.add("$path: ${result.error.message}")
                        progressReporter.reportItem()
                    }
                }
            }

            progressReporter.endPhase()

            if (results.isEmpty()) {
                Result.Failure(
                    Exception("No items processed successfully (${errors.size} errors)")
                )
            } else {
                Result.Success(results)
            }
        } catch (e: Exception) {
            Result.Failure(Exception("Sequential processing failed: ${e.message}", e))
        }
    }

    /**
     * Get thread pool info.
     */
    fun getPoolInfo(): String {
        return "ThreadPool(max=$maxThreads, batchSize=$batchSize)"
    }
}
