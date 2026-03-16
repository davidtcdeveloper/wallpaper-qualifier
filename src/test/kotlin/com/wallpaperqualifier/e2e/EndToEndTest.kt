package com.wallpaperqualifier.e2e

import com.wallpaperqualifier.config.AppConfig
import com.wallpaperqualifier.config.FoldersConfig
import com.wallpaperqualifier.config.LLMConfig
import com.wallpaperqualifier.config.ProcessingConfig
import com.wallpaperqualifier.domain.EvaluationResult
import com.wallpaperqualifier.domain.Result
import com.wallpaperqualifier.llm.FakeLLMService
import com.wallpaperqualifier.test.TestTempManager
import com.wallpaperqualifier.utils.Logger
import com.wallpaperqualifier.workflow.WorkflowOrchestrator
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.io.File
import java.nio.file.Files

class EndToEndTest : FunSpec({
    val logger = Logger()
    val testBaseDir = Files.createTempDirectory("wallpaper-qualifier-e2e").toFile()
    
    fun createScenarioFolders(name: String): FoldersConfig {
        val scenarioDir = File(testBaseDir, name).apply { mkdirs() }
        val samplesDir = File(scenarioDir, "samples").apply { mkdirs() }
        val candidatesDir = File(scenarioDir, "candidates").apply { mkdirs() }
        val outputDir = File(scenarioDir, "output").apply { mkdirs() }
        val tempDir = File(scenarioDir, "temp").apply { mkdirs() }
        
        return FoldersConfig(
            samples = samplesDir.absolutePath,
            candidates = candidatesDir.absolutePath,
            output = outputDir.absolutePath,
            temp = tempDir.absolutePath
        )
    }

    fun copyResourceImage(resourcePath: String, destFile: File) {
        TestTempManager.copyResourceImage(resourcePath, destFile)
    }

    test("Scenario 1: Full Happy Path - analyze samples, generate profile, evaluate candidates, curate") {
        val folders = createScenarioFolders("scenario1")
        val config = AppConfig(
            folders = folders,
            llm = LLMConfig(endpoint = "http://localhost:1234/api/v1", model = "test-model"),
            processing = ProcessingConfig(maxParallelTasks = 2, confidenceThreshold = 0.5f)
        )

        // 1. Prepare data
        copyResourceImage("format-test.jpg", File(folders.samples, "sample1.jpg"))
        copyResourceImage("format-test.png", File(folders.samples, "sample2.png"))
        
        copyResourceImage("resolution-test.png", File(folders.candidates, "candidate-good.png"))
        copyResourceImage("corrupted.jpg", File(folders.candidates, "candidate-bad.jpg"))
        copyResourceImage("format-test.bmp", File(folders.candidates, "candidate-other.bmp"))

        // 2. Setup Fake LLM
        val fakeLLM = FakeLLMService()
        fakeLLM.setEvaluationResponse(
            File(folders.candidates, "candidate-good.png").canonicalPath,
            EvaluationResult(
                imagePath = File(folders.candidates, "candidate-good.png").canonicalPath,
                qualified = true,
                confidenceScore = 0.9f,
                reasoning = "Matches perfectly"
            )
        )
        fakeLLM.setEvaluationResponse(
            File(folders.candidates, "candidate-other.bmp").canonicalPath,
            EvaluationResult(
                imagePath = File(folders.candidates, "candidate-other.bmp").canonicalPath,
                qualified = false,
                confidenceScore = 0.2f,
                reasoning = "Poor quality"
            )
        )

        val orchestrator = WorkflowOrchestrator(config, logger, fakeLLM)
        val result = orchestrator.runFullWorkflow()
        
        result.shouldBeInstanceOf<Result.Success<*>>()
        val summary = result.getOrThrow()
        summary.copied shouldBe 1
        
        File(folders.output, "candidate-good.png").exists() shouldBe true
        File(folders.output, "quality-profile.json").exists() shouldBe true
    }

    test("Scenario 2: Partial Failures - corrupted files should be skipped but workflow continues") {
        val folders = createScenarioFolders("scenario2")
        val config = AppConfig(
            folders = folders,
            llm = LLMConfig(endpoint = "http://localhost:1234/api/v1", model = "test-model"),
            processing = ProcessingConfig(maxParallelTasks = 2, confidenceThreshold = 0.5f)
        )
        
        // 1. Prepare data - one good, one corrupted
        copyResourceImage("format-test.jpg", File(folders.samples, "sample-ok.jpg"))
        copyResourceImage("corrupted.jpg", File(folders.samples, "sample-bad.jpg"))
        
        copyResourceImage("resolution-test.png", File(folders.candidates, "cand-ok.png"))
        copyResourceImage("corrupted.jpg", File(folders.candidates, "cand-bad.jpg"))

        val fakeLLM = FakeLLMService()
        val orchestrator = WorkflowOrchestrator(config, logger, fakeLLM)
        
        val result = orchestrator.runFullWorkflow()
        
        result.shouldBeInstanceOf<Result.Success<*>>()
        val summary = result.getOrThrow()
        summary.qualified shouldBe 1
        summary.copied shouldBe 1
        
        File(folders.output, "cand-ok.png").exists() shouldBe true
        File(folders.output, "quality-profile.json").exists() shouldBe true
    }

    test("Scenario 3: Edge Cases - empty folders") {
        val folders = createScenarioFolders("scenario3")
        val config = AppConfig(
            folders = folders,
            llm = LLMConfig(endpoint = "http://localhost:1234/api/v1", model = "test-model"),
            processing = ProcessingConfig(maxParallelTasks = 2, confidenceThreshold = 0.5f)
        )

        val fakeLLM = FakeLLMService()
        val orchestrator = WorkflowOrchestrator(config, logger, fakeLLM)

        val result = orchestrator.runFullWorkflow()
        val failure = result.shouldBeInstanceOf<Result.Failure>()
        failure.error.message shouldBe "No valid images found in: ${folders.samples}"
    }

    afterProject {
        testBaseDir.deleteRecursively()
        TestTempManager.cleanup()
    }
})
