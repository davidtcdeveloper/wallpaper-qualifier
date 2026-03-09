package com.wallpaperqualifier.cli

import com.wallpaperqualifier.domain.Result
import java.io.File

const val VERSION = "0.1.0"

val USAGE = """
    Wallpaper Qualifier v$VERSION - Learn from samples, qualify wallpapers
    
    Usage:
        wallpaper-qualifier <config.json> [--verbose]
        wallpaper-qualifier --help
        wallpaper-qualifier --version
    
    Arguments:
        <config.json>                         Path to JSON configuration file (required)
    
    Flags:
        --help, -h                            Display help and exit
        --version, -V                         Display version and exit
        --verbose, -v                         Enable verbose logging
    
    Examples:
        wallpaper-qualifier config.json --verbose
        wallpaper-qualifier --version
""".trimIndent()

/**
 * CLI argument parser for Wallpaper Qualifier.
 */
class ArgumentParser(
    private val helpFlags: Set<String> = setOf("--help", "-h"),
    private val versionFlags: Set<String> = setOf("--version", "-V"),
    private val verboseFlags: Set<String> = setOf("--verbose", "-v")
) {

    private val recognizedFlags = helpFlags + versionFlags + verboseFlags

    /**
     * Returns `true` if any verbose flag was provided.
     */
    fun hasVerboseFlag(args: Array<String>): Boolean =
        args.any { it in verboseFlags }

    /**
     * Parses command-line arguments and returns the parsed result.
     *
     * @param args Command-line arguments
     * @return Result containing ParsedArgs on success, or exception on failure
     */
    fun parse(args: Array<String>): Result<ParsedArgs> {
        if (args.isEmpty()) {
            return Result.Failure(IllegalArgumentException("No arguments provided. Use --help for usage information."))
        }

        val trimmedArgs = args.map { it.trim() }

        if (trimmedArgs.any { it in helpFlags }) {
            return Result.Success(ParsedArgs.ShowHelp)
        }

        if (trimmedArgs.any { it in versionFlags }) {
            return Result.Success(ParsedArgs.ShowVersion)
        }

        val unknownFlags = trimmedArgs.filter { it.startsWith("-") && it !in recognizedFlags }
        if (unknownFlags.isNotEmpty()) {
            return Result.Failure(
                IllegalArgumentException("Unknown argument: ${unknownFlags.first()}. Use --help for usage information.")
            )
        }

        val configArgument = trimmedArgs.firstOrNull { !it.startsWith("-") }
            ?: return Result.Failure(IllegalArgumentException("No configuration file provided. Use --help for usage information."))

        val configFile = File(configArgument)

        return try {
            when {
                !configFile.exists() ->
                    Result.Failure(IllegalArgumentException("Configuration file not found: ${configFile.path}"))
                !configFile.isFile ->
                    Result.Failure(IllegalArgumentException("Configuration path is not a file: ${configFile.path}"))
                !configFile.canRead() ->
                    Result.Failure(IllegalArgumentException("Configuration file is not readable: ${configFile.path}"))
                else ->
                    Result.Success(ParsedArgs.RunWithConfig(configFile.absolutePath, hasVerboseFlag(args)))
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
    data class RunWithConfig(val configPath: String, val verbose: Boolean) : ParsedArgs()
}
