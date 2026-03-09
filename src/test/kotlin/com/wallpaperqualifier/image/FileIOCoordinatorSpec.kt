package com.wallpaperqualifier.image

import com.wallpaperqualifier.domain.Result
import com.wallpaperqualifier.utils.Logger
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class FileIOCoordinatorSpec : FunSpec({

    test("processes batch in parallel and returns results") {
        val logger = Logger
        val coordinator = FileIOCoordinator(logger, maxThreads = 2, batchSize = 2)
        val paths = listOf("alpha", "beta", "gamma")

        val result = runBlocking {
            coordinator.processBatch<Int>(paths, phase = "parallel-test") { path ->
                delay(5)
                Result.Success(path.length)
            }
        }

        val success = result.shouldBeInstanceOf<Result.Success<List<Int>>>()
        success.value.shouldContainExactlyInAnyOrder(paths.map { it.length })
    }

    test("returns success when some items fail") {
        val logger = Logger
        val coordinator = FileIOCoordinator(logger, maxThreads = 2, batchSize = 2)
        val paths = listOf("keep", "drop")

        val result = runBlocking {
            coordinator.processBatch<String>(paths, phase = "partial-failure") { path ->
                if (path == "drop") {
                    Result.Failure(Exception("simulated error"))
                } else {
                    Result.Success(path)
                }
            }
        }

        val success = result.shouldBeInstanceOf<Result.Success<List<String>>>()
        success.value shouldBe listOf("keep")
    }

    test("returns failure when all items fail") {
        val logger = Logger
        val coordinator = FileIOCoordinator(logger, maxThreads = 2, batchSize = 2)
        val paths = listOf("one", "two")

        val result = runBlocking {
            coordinator.processBatch<String>(paths, phase = "all-fail") { _ ->
                Result.Failure(Exception("boom"))
            }
        }

        result.shouldBeInstanceOf<Result.Failure<List<String>>>()
    }
})
