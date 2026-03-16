package com.wallpaperqualifier.image

import com.wallpaperqualifier.domain.Result
import com.wallpaperqualifier.test.TestTempManager
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.io.File

class ImageConverterSpec : FunSpec({

    val testDir = File("src/test/resources/images").apply { mkdirs() }
    val converter = ImageConverter()

    fun createTestImage(filename: String, format: String = "PNG", width: Int = 128, height: Int = 128): File {
        return TestTempManager.createTestImage(filename, format, width, height)
    }

    test("converts PNG to JPEG successfully") {
        val source = createTestImage("converter-source.png", "PNG")
        val target = File.createTempFile("converter-png-", ".jpg")

        try {
            val result = converter.convertImage(
                source.absolutePath,
                target.absolutePath,
                config = ImageConverter.ConversionConfig(targetFormat = ImageConverter.TargetFormat.JPEG)
            )

            val success = result.shouldBeInstanceOf<Result.Success<String>>()
            success.value shouldBe target.absolutePath
            target.exists() shouldBe true
            target.length().shouldBeGreaterThan(0L)
        } finally {
            target.delete()
        }
    }

    test("converts JPEG to PNG successfully") {
        val source = createTestImage("converter-source.jpg", "JPEG")
        val target = File.createTempFile("converter-jpeg-", ".png")

        try {
            val result = converter.convertImage(
                source.absolutePath,
                target.absolutePath,
                config = ImageConverter.ConversionConfig(targetFormat = ImageConverter.TargetFormat.PNG)
            )

            val success = result.shouldBeInstanceOf<Result.Success<String>>()
            success.value shouldBe target.absolutePath
            target.exists() shouldBe true
            target.length().shouldBeGreaterThan(0L)
        } finally {
            target.delete()
        }
    }

    test("fails when output exceeds max size") {
        val source = createTestImage("converter-large.png", "PNG", width = 2048, height = 2048)
        val target = File.createTempFile("converter-fail-", ".jpg")

        try {
            val result = converter.convertImage(
                source.absolutePath,
                target.absolutePath,
                config = ImageConverter.ConversionConfig(
                    targetFormat = ImageConverter.TargetFormat.JPEG,
                    maxFileSizeBytes = 1
                )
            )

            result.shouldBeInstanceOf<Result.Failure>()
            target.exists().shouldBe(false)
        } finally {
            target.delete()
        }
    }


    afterProject {
        TestTempManager.cleanup()
    }
})
