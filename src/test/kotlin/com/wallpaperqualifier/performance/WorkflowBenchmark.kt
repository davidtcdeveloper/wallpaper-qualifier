package com.wallpaperqualifier.performance

import com.wallpaperqualifier.config.AppConfig
import com.wallpaperqualifier.config.FoldersConfig
import com.wallpaperqualifier.config.LLMConfig
import com.wallpaperqualifier.config.ProcessingConfig
import com.wallpaperqualifier.llm.FakeLLMService
import com.wallpaperqualifier.utils.Logger
import com.wallpaperqualifier.workflow.WorkflowOrchestrator
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import java.io.File
import java.nio.file.Files
import kotlin.system.measureTimeMillis

class WorkflowBenchmark : FunSpec({
    val logger = Logger()
    val testBaseDir = Files.createTempDirectory("wallpaper-qualifier-bench").toFile()
    
    val folders = FoldersConfig(
        samples = File(testBaseDir, "samples").apply { mkdirs() }.absolutePath,
        candidates = File(testBaseDir, "candidates").apply { mkdirs() }.absolutePath,
        output = File(testBaseDir, "output").apply { mkdirs() }.absolutePath,
        temp = File(testBaseDir, "temp").apply { mkdirs() }.absolutePath
    )
    
    val config = AppConfig(
        folders = folders,
        llm = LLMConfig(endpoint = "http://localhost:1234/api/v1", model = "bench-model"),
        processing = ProcessingConfig(maxParallelTasks = 8, confidenceThreshold = 0.5f)
    )

    fun copyResourceImage(resourcePath: String, destFile: File) {
        val resourceStream = File("src/test/resources/images/$resourcePath").inputStream()
        destFile.outputStream().use { resourceStream.copyTo(it) }
    }

    test("Benchmark full workflow with 10 samples and 20 candidates") {
        val sampleCount = 10
        val candidateCount = 20
        
        // 1. Prepare data
        for (i in 1..sampleCount) {
            copyResourceImage("format-test.jpg", File(folders.samples, "sample$i.jpg"))
        }
        for (i in 1..candidateCount) {
            copyResourceImage("resolution-test.png", File(folders.candidates, "candidate$i.png"))
        }

        val fakeLLM = FakeLLMService()
        val orchestrator = WorkflowOrchestrator(config, logger, fakeLLM)
        
        val start = System.currentTimeMillis()
        orchestrator.runFullWorkflow()
        val duration = System.currentTimeMillis() - start
        
        println("BENCHMARK: Full workflow took $duration ms for $sampleCount samples and $candidateCount candidates")
        
        // Performance targets:
        // Image loading/processing/copying should be fast even with many images.
        // For 30 images total, it should definitely take less than 10 seconds (most of it is file I/O).
        // Since we use FakeLLM, the bottleneck is purely disk I/O and format conversion.
        val maxAllowedTime = 10000L // 10 seconds
        duration shouldBeLessThan maxAllowedTime
    }

    afterProject {
        testBaseDir.deleteRecursively()
    }
})
