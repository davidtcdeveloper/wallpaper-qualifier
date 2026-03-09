package com.wallpaperqualifier.image

import com.wallpaperqualifier.domain.Result
import com.wallpaperqualifier.utils.Logger
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import java.io.File

class TempFileManagerSpec : FunSpec({

    val manager = TempFileManager(Logger)

    test("creates temp file with original name and extension") {
        val result = manager.createTempFile("/path/to/original.jpg", "png")

        val path = result.shouldBeInstanceOf<Result.Success<String>>().value
        path.shouldContain("original")
        path.shouldContain("png")
    }

    test("registers temp file") {
        val samplePath = "/tmp/test-temp-file.png"
        manager.registerTempFile(samplePath)

        manager.getTempFiles().shouldNotBeEmpty()
    }

    test("cleans up a temporary file") {
        val tempFile = File.createTempFile("wallpaper-test", ".png")
        val result = manager.cleanupFile(tempFile.absolutePath)

        result.shouldBeInstanceOf<Result.Success<Unit>>()
        tempFile.exists().shouldBeFalse()
    }

    test("validates temp directory exists and is writable") {
        manager.validateTempDirectory()
            .shouldBeInstanceOf<Result.Success<Unit>>()
    }

    test("cleans up all registered temp files") {
        val file1 = File.createTempFile("wq-test1", ".png")
        val file2 = File.createTempFile("wq-test2", ".jpg")

        manager.registerTempFile(file1.absolutePath)
        manager.registerTempFile(file2.absolutePath)

        manager.cleanupAll().shouldBeInstanceOf<Result.Success<Unit>>()
        manager.getTempFiles().shouldBeEmpty()
    }
})
