package com.wallpaperqualifier.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class ModelsTest : FunSpec({

    test("image creation retains metadata and computes aspect ratio") {
        val image = Image.create(
            path = "/path/to/image.jpg",
            format = ImageFormat.JPEG,
            width = 1920,
            height = 1080,
            fileSize = 512000L
        )

        image.path shouldBe "/path/to/image.jpg"
        image.format shouldBe ImageFormat.JPEG
        image.width shouldBe 1920
        image.height shouldBe 1080
        image.fileSize shouldBe 512000L
        image.aspectRatio.shouldBeGreaterThan(1.77f)
        image.aspectRatio.shouldBeLessThan(1.78f)
    }

    test("image characteristics expose palette and quality") {
        val characteristics = ImageCharacteristics(
            colorPalette = listOf("blue", "green", "yellow"),
            style = "modern",
            mood = "calm",
            composition = "landscape",
            subject = "nature",
            technicalNotes = "High quality, well-lit",
            quality = 0.95f
        )

        characteristics.colorPalette.size shouldBe 3
        characteristics.style shouldBe "modern"
        characteristics.quality shouldBe 0.95f
    }

    test("quality profile aggregates preferences") {
        val profile = QualityProfile(
            preferredColorPalettes = listOf("blue", "green"),
            preferredStyles = listOf("modern", "minimalist"),
            preferredMoods = listOf("calm", "peaceful"),
            preferredCompositions = listOf("landscape"),
            commonSubjects = listOf("nature", "architecture"),
            resolutionPreferences = ResolutionRange(),
            averageQuality = 0.92f,
            sampleCount = 5
        )

        profile.sampleCount shouldBe 5
        profile.preferredColorPalettes.size shouldBe 2
        profile.averageQuality shouldBe 0.92f
    }

    test("evaluation result surfaces its confidence") {
        val result = EvaluationResult(
            imagePath = "/path/to/candidate.jpg",
            qualified = true,
            confidenceScore = 0.87f,
            reasoning = "Matches color palette and style preferences"
        )

        result.qualified.shouldBeTrue()
        result.confidenceScore shouldBe 0.87f
        result.imagePath shouldBe "/path/to/candidate.jpg"
        result.reasoning.shouldNotBeNull()
    }

    test("result success exposes the wrapped value") {
        val result: Result<String> = Result.Success("test value")

        result.getOrNull() shouldBe "test value"
        result.getOrThrow() shouldBe "test value"
    }

    test("result failure throws the wrapped exception") {
        val exception = Exception("Test error")
        val result: Result<String> = Result.Failure(exception)

        result.getOrNull().shouldBeNull()
        val thrown = shouldThrow<Exception> { result.getOrThrow() }
        thrown.message shouldBe "Test error"
    }

    test("mapping success result transforms the value") {
        val result: Result<Int> = Result.Success(5)
        val mapped = result.map { it * 2 }

        mapped.getOrNull() shouldBe 10
    }

    test("custom exceptions carry provided messages") {
        shouldThrow<ConfigurationException> { throw ConfigurationException("Invalid config") }
        shouldThrow<ImageProcessingException> { throw ImageProcessingException("Processing failed") }
        shouldThrow<LLMException> { throw LLMException("LLM error") }
        shouldThrow<FileIOException> { throw FileIOException("File not found") }
    }
})
