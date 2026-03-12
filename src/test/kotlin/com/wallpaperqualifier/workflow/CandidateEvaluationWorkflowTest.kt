package com.wallpaperqualifier.workflow

import com.wallpaperqualifier.domain.EvaluationResult
import com.wallpaperqualifier.domain.ImageFormat
import com.wallpaperqualifier.domain.QualityProfile
import com.wallpaperqualifier.domain.ResolutionRange
import com.wallpaperqualifier.domain.Result
import com.wallpaperqualifier.image.FileIOCoordinator
import com.wallpaperqualifier.image.ImageConverter
import com.wallpaperqualifier.image.ImageLoader
import com.wallpaperqualifier.image.ImageLoaderProto
import com.wallpaperqualifier.image.TempFileManager
import com.wallpaperqualifier.llm.FakeLLMService
import com.wallpaperqualifier.utils.Logger
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.io.File
import java.nio.file.Files

class CandidateEvaluationWorkflowTest : FunSpec({

    val logger = Logger()
    val tempDir = tempdir()
    val candidatesDir = File(tempDir, "candidates").apply { mkdirs() }
    val tempManagerDir = File(tempDir, "temp").apply { mkdirs() }
    
    val loader = ImageLoader(logger, ImageLoaderProto(logger))
    val coordinator = FileIOCoordinator(logger, maxThreads = 2)
    val converter = ImageConverter()
    val tempFileManager = TempFileManager(logger, tempManagerDir.absolutePath)
    val llmService = FakeLLMService()

    val workflow = CandidateEvaluationWorkflow(
        loader, coordinator, converter, tempFileManager, llmService, logger
    )

    val profile = QualityProfile(
        preferredColorPalettes = listOf("#000000"),
        preferredStyles = listOf("Minimalist"),
        preferredMoods = listOf("Calm"),
        preferredCompositions = listOf("Centered"),
        commonSubjects = listOf("Nature"),
        resolutionPreferences = ResolutionRange(),
        averageQuality = 0.8f,
        sampleCount = 10
    )

    fun copyResourceImage(name: String, target: File) {
        val resource = File("src/test/resources/images/$name")
        Files.copy(resource.toPath(), target.toPath())
    }

    test("should evaluate candidates against profile") {
        val candidate1 = File(candidatesDir, "candidate1.png")
        copyResourceImage("format-test.png", candidate1)

        llmService.setEvaluationResponse(
            candidate1.absolutePath,
            EvaluationResult(candidate1.absolutePath, true, 0.9f, "Good match")
        )

        val result = workflow.evaluateCandidates(candidatesDir.absolutePath, profile)

        result.shouldBeInstanceOf<Result.Success<List<EvaluationResult>>>()
        val evaluations = result.value
        evaluations shouldHaveSize 1
        evaluations.first().qualified shouldBe true
        evaluations.first().confidenceScore shouldBe 0.9f // Now matches correctly in FakeLLMService
    }
})
