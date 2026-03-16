package com.wallpaperqualifier.image

import com.wallpaperqualifier.domain.Result
import com.wallpaperqualifier.test.TestTempManager
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.io.File

class ImageMetadataExtractorSpec : FunSpec({

    val testDir = TestTempManager.baseDir
    val extractor = ImageMetadataExtractor()

    fun createTestImage(filename: String, width: Int = 1920, height: Int = 1080): File {
        return TestTempManager.createTestImage(filename, "PNG", width, height)
    }

    test("extracts metadata for valid image") {
        val imageFile = createTestImage("metadata-test.png", 1024, 768)

        val metadata = extractor.extractMetadata(imageFile.absolutePath)
            .shouldBeInstanceOf<Result.Success<ImageMetadataExtractor.DetailedMetadata>>()
            .value

        metadata.width shouldBe 1024
        metadata.height shouldBe 768
        metadata.aspectRatio shouldBe 1024.0 / 768.0
    }

    test("fails metadata extraction for nonexistent file") {
        extractor.extractMetadata("/nonexistent/image.png")
            .shouldBeInstanceOf<Result.Failure>()
    }

    test("validates resolution requirement") {
        val imageFile = createTestImage("resolution-test.png", 1920, 1080)

        val metadata = extractor.extractMetadata(imageFile.absolutePath)
            .shouldBeInstanceOf<Result.Success<ImageMetadataExtractor.DetailedMetadata>>()
            .value

        extractor.meetsResolutionRequirement(metadata, 1920, 1080) shouldBe true
        extractor.meetsResolutionRequirement(metadata, 3840, 2160) shouldBe false
    }

    test("estimates quality score between 0 and 1") {
        val imageFile = createTestImage("quality-test.png", 1920, 1080)

        val metadata = extractor.extractMetadata(imageFile.absolutePath)
            .shouldBeInstanceOf<Result.Success<ImageMetadataExtractor.DetailedMetadata>>()
            .value

        val score = extractor.estimateQualityScore(metadata)
        score.shouldBeGreaterThan(0f)
        score.shouldBeLessThan(1f)
    }

    test("extracts batch metadata for multiple images") {
        val file1 = createTestImage("batch1.png", 800, 600)
        val file2 = createTestImage("batch2.png", 1024, 768)

        val metadata = extractor.extractMetadataBatch(listOf(file1.absolutePath, file2.absolutePath))
            .shouldBeInstanceOf<Result.Success<List<ImageMetadataExtractor.DetailedMetadata>>>()
            .value

        metadata.shouldHaveSize(2)
    }


    afterProject {
        TestTempManager.cleanup()
    }
})
