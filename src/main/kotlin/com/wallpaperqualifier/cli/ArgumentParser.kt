package com.wallpaperqualifier.cli

import com.wallpaperqualifier.domain.Result
import java.io.File

const val VERSION = "0.1.0"

val USAGE = """
    Wallpaper Qualifier v$VERSION - Learn from samples, qualify wallpapers
    
    Usage:
        wallpaper-qualifier <config.json>     Run with configuration file
        wallpaper-qualifier --help            Display this help message
        wallpaper-qualifier --version         Show version information
    
    Arguments:
        <config.json>                         Path to JSON configuration file (required)
    
    Flags:
        --help                                Display help and exit
        --version                             Display version and exit
    
    Configuration:
        See docs/README.md for example configuration files.
    
    Examples:
        wallpaper-qualifier config.json
        wallpaper-qualifier /path/to/config.json
""".trimIndent()

/**
 * CLI argument parser for Wallpaper Qualifier.
 * Handles --help, --version, and config file path argument.
 */
object ArgumentParser {

    /**
     * Parses command-line arguments and returns parsed result.
     *
     * @param args Command-line arguments
     * @return Result containing ParsedArgs on success, or exception on failure
     */
    fun parse(args: Array<String>): Result<ParsedArgs> {
        return try {
            when {
                args.isEmpty() -> {
                    Result.Failure(IllegalArgumentException("No arguments provided. Use --help for usage information."))
                }
                args[0] == "--help" || args[0] == "-h" -> {
                    Result.Success(ParsedArgs.ShowHelp)
                }
                args[0] == "--version" || args[0] == "-v" -> {
                    Result.Success(ParsedArgs.ShowVersion)
                }
                args[0].endsWith(".json") -> {
                    val configPath = args[0]
                    val configFile = File(configPath)
                    
                    if (!configFile.exists()) {
                        Result.Failure(IllegalArgumentException("Configuration file not found: $configPath"))
                    } else if (!configFile.isFile) {
                        Result.Failure(IllegalArgumentException("Configuration path is not a file: $configPath"))
                    } else if (!configFile.canRead()) {
                        Result.Failure(IllegalArgumentException("Configuration file is not readable: $configPath"))
                    } else {
                        Result.Success(ParsedArgs.RunWithConfig(configFile.absolutePath))
                    }
                }
                else -> {
                    Result.Failure(IllegalArgumentException("Unknown argument: ${args[0]}. Use --help for usage information."))
                }
            }
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }
}

/**
 * Parsed command-line arguments.
 */
sealed class ParsedArgs {
    object ShowHelp : ParsedArgs()
    object ShowVersion : ParsedArgs()
    data class RunWithConfig(val configPath: String) : ParsedArgs()
}
