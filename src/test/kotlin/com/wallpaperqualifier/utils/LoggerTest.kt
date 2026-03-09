package com.wallpaperqualifier.utils

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class LoggerTest : FunSpec({

    test("info messages include level and text") {
        val output = captureStdOut {
            val logger = Logger()
            logger.info("Test info message")
        }

        output.shouldContain("Test info message")
        output.shouldContain("[INFO]")
    }

    test("warn messages include level and text on stderr") {
        val output = captureStdErr {
            val logger = Logger()
            logger.warn("Test warning message")
        }

        output.shouldContain("Test warning message")
        output.shouldContain("[WARN]")
    }

    test("error messages include level and text on stderr") {
        val output = captureStdErr {
            val logger = Logger()
            logger.error("Test error message")
        }

        output.shouldContain("Test error message")
        output.shouldContain("[ERROR]")
    }

    test("debug messages are suppressed when verbose is false") {
        val output = captureStdOut {
            val logger = Logger()
            logger.setVerbose(false)
            logger.debug("Test debug message")
        }

        output.shouldNotContain("Test debug message")
    }

    test("debug messages appear when verbose is true") {
        val output = captureStdOut {
            val logger = Logger()
            logger.setVerbose(true)
            logger.debug("Test debug message")
        }

        output.shouldContain("Test debug message")
        output.shouldContain("[DEBUG]")
    }

    test("info messages include timestamp punctuation") {
        val output = captureStdOut {
            val logger = Logger()
            logger.info("Timestamped message")
        }

        output.shouldContain("-")
        output.shouldContain(":")
    }

    test("info messages include level text") {
        val output = captureStdOut {
            val logger = Logger()
            logger.info("Leveled message")
        }

        output.shouldContain("[INFO]")
    }

    test("error logs with throwable include both message and exception") {
        val output = captureStdErr {
            val logger = Logger()
            logger.setVerbose(true)
            val exception = Exception("Test exception")
            logger.error("Error with exception", exception)
        }

        output.shouldContain("Error with exception")
        output.shouldContain("Test exception")
    }

    test("multiple info messages are emitted sequentially") {
        val output = captureStdOut {
            val logger = Logger()
            logger.info("First message")
            logger.info("Second message")
            logger.info("Third message")
        }

        output.shouldContain("First message")
        output.shouldContain("Second message")
        output.shouldContain("Third message")
    }
})

private fun captureStdOut(block: () -> Unit): String {
    return captureStream(System.out, System::setOut, block)
}

private fun captureStdErr(block: () -> Unit): String {
    return captureStream(System.err, System::setErr, block)
}

private fun captureStream(original: PrintStream, setter: (PrintStream) -> Unit, block: () -> Unit): String {
    val outputCapture = ByteArrayOutputStream()
    val printStream = PrintStream(outputCapture)
    return try {
        setter(printStream)
        block()
        outputCapture.toString()
    } finally {
        setter(original)
    }
}
