package com.wallpaperqualifier.utils

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Log level enumeration.
 */
enum class LogLevel {
    DEBUG, INFO, WARN, ERROR
}

/**
 * Simple logging interface for the application.
 * Outputs to stdout for info/debug, stderr for warnings/errors.
 */
class Logger(
    private val output: java.io.PrintStream = System.out,
    private val errorOutput: java.io.PrintStream = System.err,
    private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
) {

    private var verboseMode = false

    /**
     * Enable verbose logging (includes DEBUG level).
     */
    fun setVerbose(verbose: Boolean) {
        verboseMode = verbose
    }

    /**
     * Log a debug message (only shown in verbose mode).
     *
     * @param message The message to log
     */
    fun debug(message: String) {
        if (verboseMode) {
            log(LogLevel.DEBUG, message, output)
        }
    }

    /**
     * Log an info message.
     *
     * @param message The message to log
     */
    fun info(message: String) {
        log(LogLevel.INFO, message, output)
    }

    /**
     * Log a warning message.
     *
     * @param message The message to log
     */
    fun warn(message: String) {
        log(LogLevel.WARN, message, errorOutput)
    }

    /**
     * Log an error message.
     *
     * @param message The message to log
     * @param throwable Optional exception to log
     */
    fun error(message: String, throwable: Throwable? = null) {
        log(LogLevel.ERROR, message, errorOutput)
        if (throwable != null && verboseMode) {
            throwable.printStackTrace(errorOutput)
        }
    }

    /**
     * Log a message at the specified level.
     *
     * @param level The log level
     * @param message The message to log
     * @param stream The output stream (stdout or stderr)
     */
    private fun log(level: LogLevel, message: String, stream: java.io.PrintStream) {
        val timestamp = LocalDateTime.now().format(dateTimeFormatter)
        val formattedMessage = "[$timestamp] [${level.name}] $message"
        stream.println(formattedMessage)
    }
}
