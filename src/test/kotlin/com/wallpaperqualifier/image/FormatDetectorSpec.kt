package com.wallpaperqualifier.image

import com.wallpaperqualifier.domain.ImageFormat
import com.wallpaperqualifier.domain.ImageProcessingException
import com.wallpaperqualifier.domain.Result
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.shouldBe
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class FormatDetectorSpec : FunSpec({

    val testDir = File("src/test/resources/images").apply { mkdirs() }
    val detector = FormatDetector()

    fun createTestImage(filename: String, format: String): File {
        val file = File(testDir, filename)
        val bufferedImage = BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB)
        ImageIO.write(bufferedImage, format, file)
        return file
    }

    test("detects JPEG format via magic bytes") {
        val imageFile = createTestImage("format-test.jpg", "JPEG")
        val result = detector.detectFormat(imageFile.absolutePath)

        val success = result.shouldBeInstanceOf<Result.Success<ImageFormat>>()
        success.value shouldBe ImageFormat.JPEG
    }

    test("detects PNG format via magic bytes") {
        val imageFile = createTestImage("format-test.png", "PNG")
        val result = detector.detectFormat(imageFile.absolutePath)

        val success = result.shouldBeInstanceOf<Result.Success<ImageFormat>>()
        success.value shouldBe ImageFormat.PNG
    }

    test("detects GIF format via magic bytes") {
        val imageFile = createTestImage("format-test.gif", "GIF")
        val result = detector.detectFormat(imageFile.absolutePath)

        val success = result.shouldBeInstanceOf<Result.Success<ImageFormat>>()
        success.value shouldBe ImageFormat.GIF
    }

    test("detects BMP format via magic bytes") {
        val imageFile = createTestImage("format-test.bmp", "BMP")
        val result = detector.detectFormat(imageFile.absolutePath)

        val success = result.shouldBeInstanceOf<Result.Success<ImageFormat>>()
        success.value shouldBe ImageFormat.BMP
    }

    test("detects TIFF format via magic bytes") {
        val imageFile = createTestImage("format-test.tiff", "TIFF")
        val result = detector.detectFormat(imageFile.absolutePath)

        val success = result.shouldBeInstanceOf<Result.Success<ImageFormat>>()
        success.value shouldBe ImageFormat.TIFF
    }

    test("fails on nonexistent file") {
        val result = detector.detectFormat("/nonexistent/path/image.jpg")

        val failure = result.shouldBeInstanceOf<Result.Failure>()
        failure.error.shouldBeInstanceOf<ImageProcessingException>()
        failure.error.message.shouldContain("not found")
    }

    test("fails on unsupported format") {
        val file = File(testDir, "unsupported.xyz")
        file.writeText("not an image")

        val result = detector.detectFormat(file.absolutePath)

        result.shouldBeInstanceOf<Result.Failure>() // ensures failure
    }

    test("validates existing image file") {
        val imageFile = createTestImage("validate-valid.png", "PNG")
        val result = detector.isValidImageFile(imageFile.absolutePath)

        result.shouldBeInstanceOf<Result.Success<Unit>>()
    }

    test("fails validation for nonexistent file") {
        val result = detector.isValidImageFile("/nonexistent/image.png")

        result.shouldBeInstanceOf<Result.Failure>()
    }

    test("fails validation for empty file") {
        val file = File(testDir, "empty-file.png")
        file.writeText("")

        val result = detector.isValidImageFile(file.absolutePath)

        result.shouldBeInstanceOf<Result.Failure>()
    }

    test("lists supported extensions") {
        val extensions = detector.getSupportedExtensions()

        extensions.shouldContainAll(listOf("jpg", "jpeg", "png", "gif", "bmp", "webp"))
    }
})
