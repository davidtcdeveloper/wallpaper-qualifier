package com.wallpaperqualifier

import com.wallpaperqualifier.cli.ArgumentParser
import com.wallpaperqualifier.cli.ParsedArgs
import com.wallpaperqualifier.cli.USAGE
import com.wallpaperqualifier.cli.VERSION
import com.wallpaperqualifier.config.ConfigParser
import com.wallpaperqualifier.utils.Logger
import com.wallpaperqualifier.workflow.WorkflowOrchestrator
import kotlinx.coroutines.runBlocking

/**
 * Main entry point for Wallpaper Qualifier application.
 */
fun main(args: Array<String>) {
    val logger = Logger()
    val argumentParser = ArgumentParser()
    val configParser = ConfigParser()

    val verboseRequested = argumentParser.hasVerboseFlag(args)
    if (verboseRequested) {
        logger.setVerbose(true)
        logger.debug("Verbose mode enabled")
    }

    logger.debug("Parsing arguments: ${args.joinToString(", ")}")
    val parseResult = argumentParser.parse(args)

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
                    logger.setVerbose(parsedArgs.verbose)
                    runWithConfig(parsedArgs.configPath, configParser, logger)
                }
            }
        }
        is com.wallpaperqualifier.domain.Result.Failure -> {
            logger.error("Failed to parse arguments: ${parseResult.error.message}")
            System.err.println("\nUse --help for usage information")
            System.exit(1)
        }
    }
}

/**
 * Run the application with a configuration file.
 *
 * @param configPath Path to the configuration file
 * @param configParser Parser instance used to load the config
 * @param logger Logger instance for reporting status
 */
private fun runWithConfig(configPath: String, configParser: ConfigParser, logger: Logger) {
    logger.info("Loading configuration from: $configPath")

    val configResult = configParser.parseFile(configPath)

    when (configResult) {
        is com.wallpaperqualifier.domain.Result.Success -> {
            val config = configResult.value
            logger.info("Configuration loaded successfully")
            logger.debug("Samples folder: ${config.folders.samples}")
            logger.debug("Candidates folder: ${config.folders.candidates}")
            logger.debug("Output folder: ${config.folders.output}")
            logger.debug("LLM endpoint: ${config.llm.endpoint}")
            logger.debug("LLM model: ${config.llm.model}")
            logger.debug("Max parallel tasks: ${config.processing.maxParallelTasks}")

            logger.info("Wallpaper Qualifier initialized successfully")
            
            runBlocking {
                val orchestrator = WorkflowOrchestrator(config, logger)
                val result = orchestrator.runFullWorkflow()
                
                when (result) {
                    is com.wallpaperqualifier.domain.Result.Success -> {
                        val summary = result.value
                        logger.info("Curation Summary:")
                        logger.info("  Processed: ${summary.totalEvaluated}")
                        logger.info("  Qualified: ${summary.qualified}")
                        logger.info("  Rejected:  ${summary.rejected}")
                        logger.info("  Copied:    ${summary.copied}")
                        logger.info("  Duplicates:${summary.duplicates}")
                        logger.info("  Errors:    ${summary.errors}")
                        logger.info("\nDone! Qualified wallpapers are in: ${config.folders.output}")
                    }
                    is com.wallpaperqualifier.domain.Result.Failure -> {
                        logger.error("Workflow failed: ${result.error.message}")
                        System.exit(1)
                    }
                }
            }
        }
        is com.wallpaperqualifier.domain.Result.Failure -> {
            logger.error("Failed to load configuration: ${configResult.error.message}")
            System.exit(1)
        }
    }
}
