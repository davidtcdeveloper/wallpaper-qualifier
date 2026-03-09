package com.wallpaperqualifier.image

import com.wallpaperqualifier.utils.Logger

/**
 * Reports progress during image processing operations.
 * Provides feedback on current phase, completion percentage, and ETA.
 */
class ProgressReporter(private val logger: Logger) {

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
    @Synchronized
    fun startPhase(phase: String, totalItems: Int) {
        currentPhase = phase
        this.totalItems = totalItems
        processedItems = 0
        startTime = System.currentTimeMillis()
        logger.info("$phase: Starting (total: $totalItems items)")
    }

    /**
     * Report progress on current item.
     *
     * @param itemName Name or identifier of current item
     */
    @Synchronized
    fun reportItem(itemName: String = "") {
        processedItems++
        if (processedItems % 10 == 0 || processedItems == totalItems) {
            reportProgress(itemName)
        }
    }

    /**
     * Report progress on batch of items.
     *
     * @param count Number of items completed in batch
     */
    @Synchronized
    fun reportBatch(count: Int) {
        processedItems += count
        if (processedItems <= totalItems) {
            reportProgress()
        }
    }

    /**
     * Get current progress percentage.
     */
    @Synchronized
    fun getPercentage(): Int {
        if (totalItems == 0) return 0
        return (processedItems * 100) / totalItems
    }

    /**
     * Estimate time remaining.
     */
    @Synchronized
    fun getEstimatedTimeRemaining(): Long {
        if (processedItems == 0) return 0
        val elapsedMs = System.currentTimeMillis() - startTime
        val itemsPerMs = processedItems.toDouble() / elapsedMs
        val remainingItems = totalItems - processedItems
        return (remainingItems / itemsPerMs).toLong()
    }

    /**
     * End current phase and report summary.
     */
    @Synchronized
    fun endPhase() {
        val elapsedMs = System.currentTimeMillis() - startTime
        val elapsedSeconds = elapsedMs / 1000.0
        logger.info("$currentPhase: Completed $processedItems/$totalItems items in ${String.format("%.1f", elapsedSeconds)}s")
    }

    /**
     * Report progress to logger.
     */
    @Synchronized
    private fun reportProgress(itemName: String = "") {
        val percentage = getPercentage()
        val eta = getEstimatedTimeRemaining()
        val etaSeconds = eta / 1000

        val itemInfo = if (itemName.isNotEmpty()) " ($itemName)" else ""
        val etaInfo = if (eta > 0) ", ETA: ${etaSeconds}s" else ""

        val message = "$currentPhase: $processedItems/$totalItems ($percentage%)$etaInfo$itemInfo"
        logger.info(message)
    }
}
