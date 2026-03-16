package com.wallpaperqualifier.test

import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.imageio.ImageIO

object TestTempManager {

    val baseDir: File = Files.createTempDirectory("wq-tests-").toFile().apply { deleteOnExit() }

    private val registered = mutableListOf<File>()

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            try {
                cleanup()
            } catch (_: Throwable) {
                // best-effort
            }
        })
    }

    @Synchronized
    fun registerForCleanup(file: File) {
        if (!registered.contains(file)) registered.add(file)
    }

    fun createTestImage(filename: String, format: String = "PNG", width: Int = 128, height: Int = 128): File {
        val file = File(baseDir, filename)
        file.parentFile?.mkdirs()
        val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        ImageIO.write(bufferedImage, format, file)
        registerForCleanup(file)
        return file
    }

    fun copyResourceImage(resourceName: String, dest: File): File {
        val resourcePath = "/images/$resourceName"
        val resourceStream = TestTempManager::class.java.getResourceAsStream(resourcePath)
            ?: throw IllegalArgumentException("Resource not found: $resourcePath")
        dest.parentFile?.mkdirs()
        Files.copy(resourceStream, dest.toPath(), StandardCopyOption.REPLACE_EXISTING)
        registerForCleanup(dest)
        return dest
    }

    fun createCorruptedImage(filename: String): File {
        val file = File(baseDir, filename)
        file.parentFile?.mkdirs()
        file.writeBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()) + "This is not valid image data".toByteArray())
        registerForCleanup(file)
        return file
    }

    @Synchronized
    fun cleanup() {
        // Delete registered files and directories recursively
        for (f in registered) {
            try {
                deleteRecursively(f)
            } catch (_: Throwable) {
                // ignore
            }
        }
        registered.clear()

        // Also attempt to delete the baseDir if empty
        try {
            if (baseDir.exists()) baseDir.deleteRecursively()
        } catch (_: Throwable) {
        }
    }

    private fun deleteRecursively(file: File) {
        if (!file.exists()) return
        if (file.isDirectory) {
            file.listFiles()?.forEach { deleteRecursively(it) }
        }
        file.delete()
    }
}
