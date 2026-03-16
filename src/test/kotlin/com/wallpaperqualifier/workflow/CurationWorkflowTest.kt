package com.wallpaperqualifier.workflow

import com.wallpaperqualifier.domain.EvaluationResult
import com.wallpaperqualifier.domain.Result
import com.wallpaperqualifier.image.DuplicateDetector
import com.wallpaperqualifier.utils.Logger
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.io.File

class CurationWorkflowTest : FunSpec({

    val logger = Logger()
    val tempDir = tempdir()
    val outputDir = File(tempDir, "output").apply { mkdirs() }
    val candidatesDir = File(tempDir, "candidates").apply { mkdirs() }
    
    val duplicateDetector = DuplicateDetector()
    val workflow = CurationWorkflow(duplicateDetector, logger)

    test("should curate qualified candidates") {
        val image1 = File(candidatesDir, "image1.jpg").apply { writeText("image1 data") }
        val image2 = File(candidatesDir, "image2.jpg").apply { writeText("image2 data") }
        
        val results = listOf(
            EvaluationResult(image1.absolutePath, true, 0.9f, "Good match"),
            EvaluationResult(image2.absolutePath, false, 0.3f, "Poor match")
        )

        val result = workflow.curate(results, outputDir.absolutePath)

        result.shouldBeInstanceOf<Result.Success<CurationWorkflow.CurationSummary>>()
        val summary = result.value
        summary.copied shouldBe 1
        summary.qualified shouldBe 1
        
        File(outputDir, "image1.jpg").exists() shouldBe true
        File(outputDir, "image2.jpg").exists() shouldBe false
    }

    test("should skip duplicates in output folder") {
        val image1 = File(candidatesDir, "image1.jpg").apply { writeText("identical data") }
        val existing = File(outputDir, "image1.jpg").apply { writeText("identical data") }

        val results = listOf(
            EvaluationResult(image1.absolutePath, true, 0.9f, "Good match")
        )

        val result = workflow.curate(results, outputDir.absolutePath)

        result.shouldBeInstanceOf<Result.Success<CurationWorkflow.CurationSummary>>()
        val summary = result.value
        summary.copied shouldBe 0
        summary.duplicates shouldBe 1
    }
})
