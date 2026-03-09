package com.wallpaperqualifier.image

import com.wallpaperqualifier.utils.Logger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.coroutines.newSingleThreadContext

@OptIn(ExperimentalCoroutinesApi::class)
/**
 * Reports progress during image processing operations.
 * Provides feedback on current phase, completion percentage, and ETA.
 * Runs entirely on a dedicated single-thread dispatcher instead of synchronized blocks.
 */
class ProgressReporter(private val logger: Logger) {

    private val dispatcher: ExecutorCoroutineDispatcher = newSingleThreadContext("progress-reporter")
    private var startTime: Long = 0
    private var totalItems: Int = 0
    private var processedItems: Int = 0
    private var currentPhase: String = ""

    /**
     * Start progress tracking for a new operation.
     *
     * @param phase Description of current phase
     * @param totalItems Total number of items to process
     */
    suspend fun startPhase(phase: String, totalItems: Int) = withContext(dispatcher) {
        currentPhase = phase
        this@ProgressReporter.totalItems = totalItems
        processedItems = 0
        startTime = System.currentTimeMillis()
        logger.info("$phase: Starting (total: $totalItems items)")
    }

    /**
     * Report progress on current item.
     *
     * @param itemName Name or identifier of current item
     */
    suspend fun reportItem(itemName: String = "") = withContext(dispatcher) {
        processedItems++
        if (processedItems % 10 == 0 || processedItems == totalItems) {
            reportProgressLocked(itemName)
        }
    }

    /**
     * Report progress on batch of items.
     *
     * @param count Number of items completed in batch
     */
    suspend fun reportBatch(count: Int) = withContext(dispatcher) {
        processedItems += count
        if (processedItems <= totalItems) {
            reportProgressLocked()
        }
    }

    /**
     * Get current progress percentage.
     */
    suspend fun getPercentage(): Int = withContext(dispatcher) {
        if (totalItems == 0) return@withContext 0
        (processedItems * 100) / totalItems
    }

    /**
     * Estimate time remaining.
     */
    suspend fun getEstimatedTimeRemaining(): Long = withContext(dispatcher) {
        if (processedItems == 0) return@withContext 0L
        val elapsedMs = System.currentTimeMillis() - startTime
        val itemsPerMs = processedItems.toDouble() / elapsedMs
        val remainingItems = totalItems - processedItems
        (remainingItems / itemsPerMs).toLong()
    }

    /**
     * End current phase and report summary.
     */
    suspend fun endPhase() = withContext(dispatcher) {
        val elapsedMs = System.currentTimeMillis() - startTime
        val elapsedSeconds = elapsedMs / 1000.0
        logger.info("$currentPhase: Completed $processedItems/$totalItems items in ${String.format("%.1f", elapsedSeconds)}s")
    }

    /**
     * Forcefully release dispatcher resources.
     */
    fun shutdown() {
        dispatcher.close()
    }

    /**
     * Report progress to logger.
     */
    private fun reportProgressLocked(itemName: String = "") {
        val percentage = if (totalItems == 0) 0 else (processedItems * 100) / totalItems
        val eta = if (processedItems == 0) 0L else {
            val elapsedMs = System.currentTimeMillis() - startTime
            val itemsPerMs = processedItems.toDouble() / elapsedMs
            val remainingItems = totalItems - processedItems
            (remainingItems / itemsPerMs).toLong()
        }
        val etaSeconds = eta / 1000

        val itemInfo = if (itemName.isNotEmpty()) " ($itemName)" else ""
        val etaInfo = if (eta > 0) ", ETA: ${etaSeconds}s" else ""

        val message = "$currentPhase: $processedItems/$totalItems ($percentage%)$etaInfo$itemInfo"
        logger.info(message)
    }
}
