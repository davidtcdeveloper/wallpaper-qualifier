package com.wallpaperqualifier.cli

import com.wallpaperqualifier.domain.Result
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.io.File
import java.nio.file.Files

private val parser = ArgumentParser()

class ArgumentParserTest : FunSpec({

    test("help flag returns ShowHelp") {
        val result = parser.parse(arrayOf("--help"))

        val success = result.shouldBeInstanceOf<Result.Success<ParsedArgs>>()
        success.value shouldBe ParsedArgs.ShowHelp
    }

    test("short help flag returns ShowHelp") {
        val result = parser.parse(arrayOf("-h"))

        val success = result.shouldBeInstanceOf<Result.Success<ParsedArgs>>()
        success.value shouldBe ParsedArgs.ShowHelp
    }

    test("version flag returns ShowVersion") {
        val result = parser.parse(arrayOf("--version"))

        val success = result.shouldBeInstanceOf<Result.Success<ParsedArgs>>()
        success.value shouldBe ParsedArgs.ShowVersion
    }

    test("short version flag returns ShowVersion") {
        val result = parser.parse(arrayOf("-V"))

        val success = result.shouldBeInstanceOf<Result.Success<ParsedArgs>>()
        success.value shouldBe ParsedArgs.ShowVersion
    }

    test("valid config path returns RunWithConfig") {
        withTempConfigFile("""{"folders":{"samples":"/tmp","candidates":"/tmp","output":"/tmp","temp":"/tmp"}}""") { tempFile ->
            val result = parser.parse(arrayOf(tempFile.absolutePath))

            val success = result.shouldBeInstanceOf<Result.Success<ParsedArgs>>()
            val parsedArgs = success.value.shouldBeInstanceOf<ParsedArgs.RunWithConfig>()
            parsedArgs.configPath shouldBe tempFile.absolutePath
        }
    }

    test("config file not found returns failure") {
        val result = parser.parse(arrayOf("/nonexistent/path/config.json"))
        result.failureMessageShouldContain("not found")
    }

    test("config path with wrong format returns failure") {
        val result = parser.parse(arrayOf("/path/to/config.txt"))
        result.failureMessageShouldContain("not found")
    }

    test("config file not readable returns failure when permission change succeeds") {
        withTempConfigFile("{}") { tempFile ->
            if (!tempFile.setReadable(false)) return@withTempConfigFile

            val result = parser.parse(arrayOf(tempFile.absolutePath))
            result.shouldBeInstanceOf<Result.Failure>()
        }
    }

    test("no arguments returns failure") {
        val result = parser.parse(arrayOf())
        result.failureMessageShouldContain("No arguments provided")
    }

    test("unknown argument returns failure") {
        val result = parser.parse(arrayOf("--unknown"))
        result.failureMessageShouldContain("Unknown argument")
    }

    test("version constant maintained") {
        VERSION shouldBe "0.1.0"
    }

    test("usage text describes flags") {
        USAGE.shouldContain("Wallpaper Qualifier")
        USAGE.shouldContain("--help")
        USAGE.shouldContain("--version")
    }
})

private fun Result<ParsedArgs>.failureMessageShouldContain(expected: String) {
    val failure = this.shouldBeInstanceOf<Result.Failure>()
    failure.error.message.shouldNotBeNull().shouldContain(expected)
}

private fun withTempConfigFile(content: String, block: (File) -> Unit) {
    val tempFile = Files.createTempFile("test-config", ".json").toFile()
    try {
        tempFile.writeText(content)
        block(tempFile)
    } finally {
        if (tempFile.exists()) {
            tempFile.delete()
        }
    }
}
