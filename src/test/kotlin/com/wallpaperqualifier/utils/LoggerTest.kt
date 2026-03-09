package com.wallpaperqualifier.utils

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LoggerTest {

    @Test
    fun testLogInfo() {
        val outputCapture = ByteArrayOutputStream()
        val oldOut = System.out
        System.setOut(PrintStream(outputCapture))

        val logger = Logger()

        try {
            logger.info("Test info message")

            val output = outputCapture.toString()
            assertTrue(output.contains("Test info message"))
            assertTrue(output.contains("[INFO]"))
        } finally {
            System.setOut(oldOut)
        }
    }

    @Test
    fun testLogWarn() {
        val outputCapture = ByteArrayOutputStream()
        val oldErr = System.err
        System.setErr(PrintStream(outputCapture))

        val logger = Logger()

        try {
            logger.warn("Test warning message")

            val output = outputCapture.toString()
            assertTrue(output.contains("Test warning message"))
            assertTrue(output.contains("[WARN]"))
        } finally {
            System.setErr(oldErr)
        }
    }

    @Test
    fun testLogError() {
        val outputCapture = ByteArrayOutputStream()
        val oldErr = System.err
        System.setErr(PrintStream(outputCapture))

        val logger = Logger()

        try {
            logger.error("Test error message")

            val output = outputCapture.toString()
            assertTrue(output.contains("Test error message"))
            assertTrue(output.contains("[ERROR]"))
        } finally {
            System.setErr(oldErr)
        }
    }

    @Test
    fun testDebugNotLoggedNormally() {
        val outputCapture = ByteArrayOutputStream()
        val oldOut = System.out
        System.setOut(PrintStream(outputCapture))

        val logger = Logger()

        try {
            logger.setVerbose(false)
            logger.debug("Test debug message")

            val output = outputCapture.toString()
            assertFalse(output.contains("Test debug message"))
        } finally {
            System.setOut(oldOut)
        }
    }

    @Test
    fun testDebugLoggedInVerboseMode() {
        val outputCapture = ByteArrayOutputStream()
        val oldOut = System.out
        System.setOut(PrintStream(outputCapture))

        val logger = Logger()

        try {
            logger.setVerbose(true)
            logger.debug("Test debug message")

            val output = outputCapture.toString()
            assertTrue(output.contains("Test debug message"))
            assertTrue(output.contains("[DEBUG]"))
        } finally {
            System.setOut(oldOut)
        }
    }

    @Test
    fun testLogIncludesTimestamp() {
        val outputCapture = ByteArrayOutputStream()
        val oldOut = System.out
        System.setOut(PrintStream(outputCapture))

        val logger = Logger()

        try {
            logger.info("Timestamped message")

            val output = outputCapture.toString()
            assertTrue(output.contains("-") && output.contains(":"))
        } finally {
            System.setOut(oldOut)
        }
    }

    @Test
    fun testLogIncludesLevel() {
        val outputCapture = ByteArrayOutputStream()
        val oldOut = System.out
        System.setOut(PrintStream(outputCapture))

        val logger = Logger()

        try {
            logger.info("Leveled message")

            val output = outputCapture.toString()
            assertTrue(output.contains("[INFO]"))
        } finally {
            System.setOut(oldOut)
        }
    }

    @Test
    fun testErrorWithThrowable() {
        val outputCapture = ByteArrayOutputStream()
        val oldErr = System.err
        System.setErr(PrintStream(outputCapture))

        val logger = Logger()

        try {
            logger.setVerbose(true)
            val exception = Exception("Test exception")
            logger.error("Error with exception", exception)

            val output = outputCapture.toString()
            assertTrue(output.contains("Error with exception"))
            assertTrue(output.contains("Test exception"))
        } finally {
            System.setErr(oldErr)
        }
    }

    @Test
    fun testMultipleMessages() {
        val outputCapture = ByteArrayOutputStream()
        val oldOut = System.out
        System.setOut(PrintStream(outputCapture))

        val logger = Logger()

        try {
            logger.info("First message")
            logger.info("Second message")
            logger.info("Third message")

            val output = outputCapture.toString()
            assertTrue(output.contains("First message"))
            assertTrue(output.contains("Second message"))
            assertTrue(output.contains("Third message"))
        } finally {
            System.setOut(oldOut)
        }
    }
}
