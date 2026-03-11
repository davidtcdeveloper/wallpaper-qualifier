package com.wallpaperqualifier.llm

import com.wallpaperqualifier.domain.Result
import com.wallpaperqualifier.utils.Logger
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Sequential request queue for all LLM operations.
 *
 * This enforces the architectural constraint that only one LLM request
 * may be in-flight at any given time.
 */
class LLMRequestQueue(
    private val client: LLMHttpClient,
    private val logger: Logger,
    capacity: Int = 100
) {

    private data class QueuedRequest(
        val request: LLMRequest,
        val onResult: (Result<String>) -> Unit,
        val onError: (Throwable) -> Unit
    )

    private val channel = Channel<QueuedRequest>(capacity = capacity)

    /**
     * Enqueue a request and suspend until the response is available.
     *
     * Callers MUST start [run] in a background coroutine before using this.
     */
    suspend fun enqueue(request: LLMRequest): Result<String> {
        return suspendCancellableCoroutine { continuation ->
            val queued = QueuedRequest(
                request = request,
                onResult = { result -> continuation.resume(result) },
                onError = { error -> continuation.resumeWithException(error) }
            )

            val sendResult = channel.trySend(queued)
            if (sendResult.isFailure) {
                continuation.resume(
                    Result.Failure(
                        LLMError.Network(
                            "LLM request queue is full or closed: ${sendResult.exceptionOrNull()?.message}"
                        )
                    )
                )
            }
        }
    }

    /**
     * Process the queue sequentially. This should be launched exactly once
     * in a long-lived coroutine scope.
     */
    suspend fun run() {
        for (queued in channel) {
            logger.debug("Processing LLM request from queue")
            val result = client.send(queued.request)
            queued.onResult(result)
        }
    }

    fun close() {
        channel.close()
    }
}

