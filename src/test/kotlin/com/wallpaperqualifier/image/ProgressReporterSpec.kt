package com.wallpaperqualifier.image

import com.wallpaperqualifier.utils.Logger
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.longs.shouldBeGreaterThanOrEqual
import io.kotest.matchers.longs.shouldBeLessThanOrEqual
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class ProgressReporterSpec : FunSpec({

    test("reports percentage and eta updates") {
        runBlocking {
            val reporter = ProgressReporter(Logger())
            reporter.startPhase("eta-test", totalItems = 5)
            delay(5)
            reporter.reportItem("item1")
            reporter.reportItem("item2")

            reporter.getPercentage().shouldBe(40)
            reporter.getEstimatedTimeRemaining().shouldBeGreaterThanOrEqual(0L)

            reporter.endPhase()
        }
    }

    test("reportBatch increments counts correctly") {
        runBlocking {
            val reporter = ProgressReporter(Logger())
            reporter.startPhase("batch-test", totalItems = 4)

            reporter.reportBatch(2)
            reporter.reportBatch(2)

            reporter.getPercentage().shouldBe(100)
            reporter.getEstimatedTimeRemaining().shouldBeLessThanOrEqual(0L)

            reporter.endPhase()
        }
    }
})
