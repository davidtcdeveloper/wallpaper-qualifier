package com.wallpaperqualifier.image

import com.wallpaperqualifier.domain.Result
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.io.File

class DuplicateDetectorSpec : FunSpec({

    val testDir = File("src/test/resources/images").apply { mkdirs() }

    fun createTestFile(filename: String, content: String = "test content"): File {
        val file = File(testDir, filename)
        file.writeText(content)
        return file
    }

    test("computes SHA-256 hash for a file") {
        val file = createTestFile("hash-test.txt", "test content")

        val hash = DuplicateDetector.computeFileHash(file.absolutePath)
            .shouldBeInstanceOf<Result.Success<String>>()
            .value

        hash.length shouldBe 64
    }

    test("identical files produce identical hashes") {
        val file1 = createTestFile("file1.txt", "same content")
        val file2 = createTestFile("file2.txt", "same content")

        val hash1 = DuplicateDetector.computeFileHash(file1.absolutePath)
            .shouldBeInstanceOf<Result.Success<String>>()
            .value

        val hash2 = DuplicateDetector.computeFileHash(file2.absolutePath)
            .shouldBeInstanceOf<Result.Success<String>>()
            .value

        hash1 shouldBe hash2
    }

    test("different files produce different hashes") {
        val file1 = createTestFile("diff1.txt", "content 1")
        val file2 = createTestFile("diff2.txt", "content 2")

        val hash1 = DuplicateDetector.computeFileHash(file1.absolutePath)
            .shouldBeInstanceOf<Result.Success<String>>()
            .value

        val hash2 = DuplicateDetector.computeFileHash(file2.absolutePath)
            .shouldBeInstanceOf<Result.Success<String>>()
            .value

        hash1 shouldNotBe hash2
    }

    test("finds duplicate groups") {
        val file1 = createTestFile("dup1.txt", "duplicate content")
        val file2 = createTestFile("dup2.txt", "duplicate content")
        val file3 = createTestFile("unique.txt", "unique content")

        val duplicates = DuplicateDetector.findDuplicates(listOf(file1.absolutePath, file2.absolutePath, file3.absolutePath))
            .shouldBeInstanceOf<Result.Success<Map<String, List<String>>>>()
            .value

        duplicates.size shouldBe 1
        duplicates.values.forEach { it.size shouldBe 2 }
    }

    test("recognizes identical files via areIdentical") {
        val file1 = createTestFile("identical1.txt", "same")
        val file2 = createTestFile("identical2.txt", "same")

        val result = DuplicateDetector.areIdentical(file1.absolutePath, file2.absolutePath)
            .shouldBeInstanceOf<Result.Success<Boolean>>()
            .value

        result shouldBe true
    }
})
