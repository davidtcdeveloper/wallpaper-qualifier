package com.wallpaperqualifier.cli

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ArgumentParserTest {

    @Test
    fun testHelpFlag() {
        val result = ArgumentParser.parse(arrayOf("--help"))
        
        assertTrue(result is com.wallpaperqualifier.domain.Result.Success)
        assertEquals(ParsedArgs.ShowHelp, (result as com.wallpaperqualifier.domain.Result.Success).value)
    }

    @Test
    fun testHelpFlagShortForm() {
        val result = ArgumentParser.parse(arrayOf("-h"))
        
        assertTrue(result is com.wallpaperqualifier.domain.Result.Success)
        assertEquals(ParsedArgs.ShowHelp, (result as com.wallpaperqualifier.domain.Result.Success).value)
    }

    @Test
    fun testVersionFlag() {
        val result = ArgumentParser.parse(arrayOf("--version"))
        
        assertTrue(result is com.wallpaperqualifier.domain.Result.Success)
        assertEquals(ParsedArgs.ShowVersion, (result as com.wallpaperqualifier.domain.Result.Success).value)
    }

    @Test
    fun testVersionFlagShortForm() {
        val result = ArgumentParser.parse(arrayOf("-v"))
        
        assertTrue(result is com.wallpaperqualifier.domain.Result.Success)
        assertEquals(ParsedArgs.ShowVersion, (result as com.wallpaperqualifier.domain.Result.Success).value)
    }

    @Test
    fun testValidConfigFile() {
        // Create a temporary config file
        val tempFile = Files.createTempFile("test-config", ".json").toFile()
        tempFile.writeText("""{"folders":{"samples":"/tmp","candidates":"/tmp","output":"/tmp","temp":"/tmp"}}""")
        
        try {
            val result = ArgumentParser.parse(arrayOf(tempFile.absolutePath))
            
            assertTrue(result is com.wallpaperqualifier.domain.Result.Success)
            val parsedArgs = (result as com.wallpaperqualifier.domain.Result.Success).value
            assertTrue(parsedArgs is ParsedArgs.RunWithConfig)
            assertEquals(tempFile.absolutePath, (parsedArgs as ParsedArgs.RunWithConfig).configPath)
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun testConfigFileNotFound() {
        val result = ArgumentParser.parse(arrayOf("/nonexistent/path/config.json"))
        
        assertTrue(result is com.wallpaperqualifier.domain.Result.Failure)
        val error = (result as com.wallpaperqualifier.domain.Result.Failure).error
        assertTrue(error.message?.contains("not found") == true)
    }

    @Test
    fun testConfigPathNotJsonFormat() {
        val result = ArgumentParser.parse(arrayOf("/path/to/config.txt"))
        
        assertTrue(result is com.wallpaperqualifier.domain.Result.Failure)
        val error = (result as com.wallpaperqualifier.domain.Result.Failure).error
        assertTrue(error.message?.contains("Unknown argument") == true)
    }

    @Test
    fun testConfigFileNotReadable() {
        // Create a temporary config file
        val tempFile = Files.createTempFile("test-config", ".json").toFile()
        tempFile.writeText("{}")
        
        try {
            // Make it unreadable (this may not work on all systems)
            val readable = tempFile.setReadable(false)
            if (readable) {
                val result = ArgumentParser.parse(arrayOf(tempFile.absolutePath))
                assertTrue(result is com.wallpaperqualifier.domain.Result.Failure)
            }
        } finally {
            tempFile.setReadable(true)
            tempFile.delete()
        }
    }

    @Test
    fun testNoArguments() {
        val result = ArgumentParser.parse(arrayOf())
        
        assertTrue(result is com.wallpaperqualifier.domain.Result.Failure)
        val error = (result as com.wallpaperqualifier.domain.Result.Failure).error
        assertTrue(error.message?.contains("No arguments provided") == true)
    }

    @Test
    fun testUnknownArgument() {
        val result = ArgumentParser.parse(arrayOf("--unknown"))
        
        assertTrue(result is com.wallpaperqualifier.domain.Result.Failure)
        val error = (result as com.wallpaperqualifier.domain.Result.Failure).error
        assertTrue(error.message?.contains("Unknown argument") == true)
    }

    @Test
    fun testVersionConstant() {
        assertEquals("0.1.0", VERSION)
    }

    @Test
    fun testUsageText() {
        assertTrue(USAGE.contains("Wallpaper Qualifier"))
        assertTrue(USAGE.contains("--help"))
        assertTrue(USAGE.contains("--version"))
    }
}
