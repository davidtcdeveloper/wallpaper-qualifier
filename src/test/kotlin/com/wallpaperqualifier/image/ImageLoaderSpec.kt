package com.wallpaperqualifier.image

import com.wallpaperqualifier.domain.Result
import com.wallpaperqualifier.utils.Logger
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.types.shouldBeInstanceOf
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class ImageLoaderSpec : FunSpec({

    val logger = Logger()
    val loader = ImageLoader(logger, ImageLoaderProto(logger))
    val testDir = File("src/test/resources/images").apply { mkdirs() }

    fun createTestImage(filename: String, width: Int = 100, height: Int = 100, directory: File = testDir): File {
        directory.mkdirs()
        val file = File(directory, filename)
        val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        ImageIO.write(bufferedImage, "PNG", file)
        return file
    }

    test("discovers images in existing folder") {
        val subDir = File(testDir, "discover-test-${System.nanoTime()}").apply { mkdirs() }
        createTestImage("test1.png", directory = subDir)
        createTestImage("test2.png", directory = subDir)
        createTestImage("test3.jpg", directory = subDir)

        val result = loader.discoverImages(subDir.absolutePath)

        val success = result.shouldBeInstanceOf<Result.Success<List<com.wallpaperqualifier.domain.Image>>>()
        success.value.shouldHaveSize(3)
    }

    test("fails when folder does not exist") {
        val result = loader.discoverImages("/nonexistent/path")

        result.shouldBeInstanceOf<Result.Failure>()
    }

    test("discovered image metadata includes dimensions and file size") {
        val imageFile = createTestImage("metadata-test.png", 800, 600)

        val result = loader.discoverImages(testDir.absolutePath)

        val success = result.shouldBeInstanceOf<Result.Success<List<com.wallpaperqualifier.domain.Image>>>()
        val image = success.value.first { it.path == imageFile.absolutePath }
        image.width shouldBe 800
        image.height shouldBe 600
        image.fileSize.shouldBeGreaterThan(0L)
    }
})
