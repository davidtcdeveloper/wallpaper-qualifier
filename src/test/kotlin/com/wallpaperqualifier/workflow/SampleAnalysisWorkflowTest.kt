package com.wallpaperqualifier.workflow

import com.wallpaperqualifier.domain.QualityProfile
import com.wallpaperqualifier.domain.Result
import com.wallpaperqualifier.image.*
import com.wallpaperqualifier.llm.FakeLLMService
import com.wallpaperqualifier.profile.ProfileGenerator
import com.wallpaperqualifier.test.TestTempManager
import com.wallpaperqualifier.utils.Logger
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.io.File

class SampleAnalysisWorkflowTest : FunSpec({

    val logger = Logger()
    val tempDir = tempdir()
    val samplesDir = File(tempDir, "samples").apply { mkdirs() }
    val tempManagerDir = File(tempDir, "temp").apply { mkdirs() }
    
    val loader = ImageLoader(logger, ImageLoaderProto(logger))
    val coordinator = FileIOCoordinator(logger, maxThreads = 2)
    val converter = ImageConverter()
    val tempFileManager = TempFileManager(logger, tempManagerDir.absolutePath)
    val llmService = FakeLLMService()
    val profileGenerator = ProfileGenerator()

    val workflow = SampleAnalysisWorkflow(
        loader, coordinator, converter, tempFileManager, llmService, profileGenerator, logger
    )

    fun copyResourceImage(name: String, target: File) {
        TestTempManager.copyResourceImage(name, target)
    }

    test("should analyze samples and generate profile") {
        // Use real image files from resources
        val sample1 = File(samplesDir, "sample1.png")
        val sample2 = File(samplesDir, "sample2.jpg")
        copyResourceImage("format-test.png", sample1)
        copyResourceImage("format-test.jpg", sample2)

        val result = workflow.analyzeSamples(samplesDir.absolutePath)

        result.shouldBeInstanceOf<Result.Success<QualityProfile>>()
        val profile = result.value
        profile.sampleCount shouldBe 2
        // FakeLLMService returns 0.5f by default
        profile.averageQuality shouldBe 0.5f
    }

    test("should return failure if no samples found") {
        val emptyDir = File(tempDir, "empty").apply { mkdirs() }
        val result = workflow.analyzeSamples(emptyDir.absolutePath)
        result.shouldBeInstanceOf<Result.Failure>()
    }


    afterProject {
        TestTempManager.cleanup()
    }
})
