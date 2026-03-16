package com.wallpaperqualifier.image

import com.wallpaperqualifier.domain.ImageProcessingException
import com.wallpaperqualifier.domain.Result
import com.wallpaperqualifier.test.TestTempManager
import com.wallpaperqualifier.utils.Logger
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import java.io.File

class ImageLoaderProtoSpec : FunSpec({

    val proto = ImageLoaderProto(Logger())

    fun createTestImage(filename: String, width: Int, height: Int, format: String): File {
        val file = TestTempManager.createTestImage(filename, format, width, height)
        return file
    }

    fun createCorruptedImage(filename: String): File {
        return TestTempManager.createCorruptedImage(filename)
    }

    test("loads JPEG image metadata") {
        val imageFile = createTestImage("test-jpeg.jpg", 800, 600, "JPEG")

        val metadata = proto.loadImage(imageFile.absolutePath)
            .shouldBeInstanceOf<Result.Success<ImageMetadata>>()
            .value

        metadata.width shouldBe 800
        metadata.height shouldBe 600
        metadata.format shouldBe "JPG"
        metadata.aspectRatio shouldBe 800.0 / 600.0
    }

    test("loads PNG image metadata") {
        val imageFile = createTestImage("test-png.png", 1024, 768, "PNG")

        val metadata = proto.loadImage(imageFile.absolutePath)
            .shouldBeInstanceOf<Result.Success<ImageMetadata>>()
            .value

        metadata.width shouldBe 1024
        metadata.height shouldBe 768
        metadata.format shouldBe "PNG"
    }

    test("loads GIF image metadata") {
        val imageFile = createTestImage("test-gif.gif", 640, 480, "GIF")

        val metadata = proto.loadImage(imageFile.absolutePath)
            .shouldBeInstanceOf<Result.Success<ImageMetadata>>()
            .value

        metadata.width shouldBe 640
        metadata.height shouldBe 480
        metadata.format shouldBe "GIF"
    }

    test("loads BMP image metadata") {
        val imageFile = createTestImage("test-bmp.bmp", 512, 512, "BMP")

        val metadata = proto.loadImage(imageFile.absolutePath)
            .shouldBeInstanceOf<Result.Success<ImageMetadata>>()
            .value

        metadata.width shouldBe 512
        metadata.height shouldBe 512
        metadata.format shouldBe "BMP"
    }

    test("extracts complete metadata") {
        val imageFile = createTestImage("test-metadata.png", 1920, 1080, "PNG")

        val metadata = proto.loadImage(imageFile.absolutePath)
            .shouldBeInstanceOf<Result.Success<ImageMetadata>>()
            .value

        metadata.width shouldBe 1920
        metadata.height shouldBe 1080
        metadata.aspectRatio shouldBe 1920.0 / 1080.0
        metadata.fileSize.shouldNotBeNull().shouldBeGreaterThan(0L)
        metadata.modificationDate.shouldNotBeNull().shouldBeGreaterThan(0L)
    }

    test("detects color depth") {
        val imageFile = createTestImage("test-colordepth.png", 256, 256, "PNG")

        val metadata = proto.loadImage(imageFile.absolutePath)
            .shouldBeInstanceOf<Result.Success<ImageMetadata>>()
            .value

        metadata.colorDepth.shouldNotBeNull().shouldBeGreaterThan(0)
    }

    test("fails on corrupted image") {
        val corruptedFile = createCorruptedImage("corrupted.jpg")

        val failure = proto.loadImage(corruptedFile.absolutePath)
            .shouldBeInstanceOf<Result.Failure>()

        failure.error.shouldBeInstanceOf<ImageProcessingException>()
    }

    test("fails on nonexistent image") {
        val failure = proto.loadImage("/nonexistent/path/to/image.jpg")
            .shouldBeInstanceOf<Result.Failure>()

        failure.error.message.shouldContain("not found")
    }

    test("fails on unreadable image") {
        val imageFile = createTestImage("test-unreadable.png", 100, 100, "PNG")
        imageFile.setReadable(false)

        proto.loadImage(imageFile.absolutePath)
            .shouldBeInstanceOf<Result.Failure>()

        imageFile.setReadable(true)
    }

    test("loads multiple images with success") {
        val image1 = createTestImage("multi-1.jpg", 800, 600, "JPEG")
        val image2 = createTestImage("multi-2.png", 1024, 768, "PNG")
        val image3 = createTestImage("multi-3.gif", 640, 480, "GIF")

        val metadata = proto.loadImages(listOf(image1.absolutePath, image2.absolutePath, image3.absolutePath))
            .shouldBeInstanceOf<Result.Success<List<ImageMetadata>>>()
            .value

        metadata.shouldHaveSize(3)
    }

    test("loads valid images even when some paths fail") {
        val image1 = createTestImage("multi-valid-1.jpg", 800, 600, "JPEG")
        val invalidPath = "/nonexistent/invalid.jpg"
        val image2 = createTestImage("multi-valid-2.png", 1024, 768, "PNG")

        val metadata = proto.loadImages(listOf(image1.absolutePath, invalidPath, image2.absolutePath))
            .shouldBeInstanceOf<Result.Success<List<ImageMetadata>>>()
            .value

        metadata.shouldHaveSize(2)
    }

    test("calculates aspect ratio") {
        val imageFile = createTestImage("test-aspect.png", 1280, 720, "PNG")

        val metadata = proto.loadImage(imageFile.absolutePath)
            .shouldBeInstanceOf<Result.Success<ImageMetadata>>()
            .value

        metadata.aspectRatio shouldBe 1280.0 / 720.0
    }

    test("extracts filename") {
        val imageFile = createTestImage("test-filename.jpg", 100, 100, "JPEG")

        val metadata = proto.loadImage(imageFile.absolutePath)
            .shouldBeInstanceOf<Result.Success<ImageMetadata>>()
            .value

        metadata.filename shouldBe "test-filename.jpg"
    }


    afterProject {
        TestTempManager.cleanup()
    }
})
