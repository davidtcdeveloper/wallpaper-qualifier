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
object Logger {
    
    private var verboseMode = false
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

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
            log(LogLevel.DEBUG, message, System.out)
        }
    }

    /**
     * Log an info message.
     *
     * @param message The message to log
     */
    fun info(message: String) {
        log(LogLevel.INFO, message, System.out)
    }

    /**
     * Log a warning message.
     *
     * @param message The message to log
     */
    fun warn(message: String) {
        log(LogLevel.WARN, message, System.err)
    }

    /**
     * Log an error message.
     *
     * @param message The message to log
     * @param throwable Optional exception to log
     */
    fun error(message: String, throwable: Throwable? = null) {
        log(LogLevel.ERROR, message, System.err)
        if (throwable != null && verboseMode) {
            throwable.printStackTrace(System.err)
        }
    }

    /**
     * Log a message at the specified level.
     *
     * @param level The log level
     * @param message The message to log
     * @param output The output stream (System.out or System.err)
     */
    private fun log(level: LogLevel, message: String, output: java.io.PrintStream) {
        val timestamp = LocalDateTime.now().format(dateTimeFormatter)
        val formattedMessage = "[$timestamp] [${level.name}] $message"
        output.println(formattedMessage)
    }
}
