package com.wallpaperqualifier

import com.wallpaperqualifier.cli.ArgumentParser
import com.wallpaperqualifier.cli.ParsedArgs
import com.wallpaperqualifier.cli.USAGE
import com.wallpaperqualifier.cli.VERSION
import com.wallpaperqualifier.config.ConfigParser
import com.wallpaperqualifier.utils.Logger

/**
 * Main entry point for Wallpaper Qualifier application.
 */
fun main(args: Array<String>) {
    // Enable verbose mode for debugging (can be made configurable)
    val verbose = args.contains("--verbose") || args.contains("-v")
    if (verbose) {
        Logger.setVerbose(true)
        Logger.debug("Verbose mode enabled")
    }

    // Parse arguments
    Logger.debug("Parsing arguments: ${args.joinToString(", ")}")
    val parseResult = ArgumentParser.parse(args)

    when (parseResult) {
        is com.wallpaperqualifier.domain.Result.Success -> {
            when (val parsedArgs = parseResult.value) {
                is ParsedArgs.ShowHelp -> {
                    println(USAGE)
                }
                is ParsedArgs.ShowVersion -> {
                    println("Wallpaper Qualifier v$VERSION")
                }
                is ParsedArgs.RunWithConfig -> {
                    runWithConfig(parsedArgs.configPath)
                }
            }
        }
        is com.wallpaperqualifier.domain.Result.Failure -> {
            Logger.error("Failed to parse arguments: ${parseResult.error.message}")
            System.err.println("\nUse --help for usage information")
            System.exit(1)
        }
    }
}

/**
 * Run the application with a configuration file.
 *
 * @param configPath Path to the configuration file
 */
private fun runWithConfig(configPath: String) {
    Logger.info("Loading configuration from: $configPath")

    val configResult = ConfigParser.parseFile(configPath)

    when (configResult) {
        is com.wallpaperqualifier.domain.Result.Success -> {
            val config = configResult.value
            Logger.info("Configuration loaded successfully")
            Logger.debug("Samples folder: ${config.folders.samples}")
            Logger.debug("Candidates folder: ${config.folders.candidates}")
            Logger.debug("Output folder: ${config.folders.output}")
            Logger.debug("LLM endpoint: ${config.llm.endpoint}")
            Logger.debug("LLM model: ${config.llm.model}")
            Logger.debug("Max parallel tasks: ${config.processing.maxParallelTasks}")

            Logger.info("Wallpaper Qualifier initialized successfully")
            // TODO: Implement main workflow orchestration
        }
        is com.wallpaperqualifier.domain.Result.Failure -> {
            Logger.error("Failed to load configuration: ${configResult.error.message}")
            System.exit(1)
        }
    }
}
