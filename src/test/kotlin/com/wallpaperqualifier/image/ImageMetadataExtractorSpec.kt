package com.wallpaperqualifier.image

import com.wallpaperqualifier.domain.Result
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class ImageMetadataExtractorSpec : FunSpec({

    val testDir = File("src/test/resources/images").apply { mkdirs() }

    fun createTestImage(filename: String, width: Int = 1920, height: Int = 1080): File {
        val file = File(testDir, filename)
        val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        ImageIO.write(bufferedImage, "PNG", file)
        return file
    }

    test("extracts metadata for valid image") {
        val imageFile = createTestImage("metadata-test.png", 1024, 768)

        val metadata = ImageMetadataExtractor.extractMetadata(imageFile.absolutePath)
            .shouldBeInstanceOf<Result.Success<ImageMetadataExtractor.DetailedMetadata>>()
            .value

        metadata.width shouldBe 1024
        metadata.height shouldBe 768
        metadata.aspectRatio shouldBe 1024.0 / 768.0
    }

    test("fails metadata extraction for nonexistent file") {
        ImageMetadataExtractor.extractMetadata("/nonexistent/image.png")
            .shouldBeInstanceOf<Result.Failure<ImageMetadataExtractor.DetailedMetadata>>()
    }

    test("validates resolution requirement") {
        val imageFile = createTestImage("resolution-test.png", 1920, 1080)

        val metadata = ImageMetadataExtractor.extractMetadata(imageFile.absolutePath)
            .shouldBeInstanceOf<Result.Success<ImageMetadataExtractor.DetailedMetadata>>()
            .value

        ImageMetadataExtractor.meetsResolutionRequirement(metadata, 1920, 1080) shouldBe true
        ImageMetadataExtractor.meetsResolutionRequirement(metadata, 3840, 2160) shouldBe false
    }

    test("estimates quality score between 0 and 1") {
        val imageFile = createTestImage("quality-test.png", 1920, 1080)

        val metadata = ImageMetadataExtractor.extractMetadata(imageFile.absolutePath)
            .shouldBeInstanceOf<Result.Success<ImageMetadataExtractor.DetailedMetadata>>()
            .value

        val score = ImageMetadataExtractor.estimateQualityScore(metadata)
        score.shouldBeGreaterThan(0f)
        score.shouldBeLessThan(1f)
    }

    test("extracts batch metadata for multiple images") {
        val file1 = createTestImage("batch1.png", 800, 600)
        val file2 = createTestImage("batch2.png", 1024, 768)

        val metadata = ImageMetadataExtractor.extractMetadataBatch(listOf(file1.absolutePath, file2.absolutePath))
            .shouldBeInstanceOf<Result.Success<List<ImageMetadataExtractor.DetailedMetadata>>>()
            .value

        metadata.shouldHaveSize(2)
    }
})
