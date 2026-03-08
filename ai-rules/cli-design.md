<primary_directive>
You design CLI experiences that are CLEAR, HELPFUL, and PREDICTABLE. You provide PROGRESS feedback. You make ERROR messages ACTIONABLE. You respect the user's shell environment.
</primary_directive>

# CLI Design: User Interface & Experience

## Overview

Wallpaper Qualifier runs as a command-line tool. Users interact solely through CLI. This document covers argument handling, output formatting, and error reporting.

---

## Argument Handling

<rule_1 priority="HIGHEST">
**SIMPLE, DISCOVERABLE ARGUMENTS**: Users should understand what to pass without reading docs.
- Main argument: path to configuration file
- Optional flags: verbose output, help
- No complex subcommands (wallpaper-qualifier is single-purpose)
- Clear error if arguments missing or invalid

**MUST**:
- ✓ Accept config file as positional argument
- ✓ Support `--help` and `-h`
- ✓ Support `--verbose` and `-v` for debug output
- ✓ Support `--version` to show version
- ✓ Exit with code 0 on success, non-zero on error

**Example - Good**:
```kotlin
fun main(args: Array<String>) = runBlocking {
    val parser = ArgParser("wallpaper-qualifier")
    parser.apply {
        argument(ArgType.String, description = "Configuration file path")
        option(ArgType.Unit, shortName = "v", longName = "verbose", description = "Enable verbose output")
        option(ArgType.Unit, shortName = "h", longName = "help", description = "Show help message")
        option(ArgType.Unit, longName = "version", description = "Show version")
    }
    
    try {
        val parsed = parser.parse(args)
        
        if (parsed.flags["help"]) {
            println(parser.helpMessage())
            exitProcess(0)
        }
        
        if (parsed.flags["version"]) {
            println("Wallpaper Qualifier v1.0.0")
            exitProcess(0)
        }
        
        val configPath = parsed.arguments.firstOrNull()
            ?: throw Exception("Configuration file required. Usage: wallpaper-qualifier config.json")
        
        val verbose = parsed.flags.containsKey("verbose")
        runQualifier(configPath, verbose)
        exitProcess(0)
    } catch (e: Exception) {
        System.err.println("Error: ${e.message}")
        exitProcess(1)
    }
}
```

**Example - Avoid**:
```kotlin
// Anti-pattern: Complex subcommands nobody needs
fun main(args: Array<String>) {
    when (args.firstOrNull()) {
        "analyze" -> analyzeCommand(args.drop(1))
        "profile" -> profileCommand(args.drop(1))
        "evaluate" -> evaluateCommand(args.drop(1))
        // User has to guess which subcommand to use
    }
}
```
</rule_1>

---

<rule_2 priority="HIGHEST">
**PROGRESS REPORTING**: Long operations must show progress. Users need feedback.
- Show current phase (Analyzing, Generating Profile, Evaluating)
- Show progress percentage or item count (X of Y)
- Show estimated time remaining (optional)
- Update on single line (no spam)

**MUST**:
- ✓ Show phase name clearly
- ✓ Show progress: "Analyzing sample 1 of 50"
- ✓ Update in-place (use terminal control codes if possible)
- ✓ Final summary with results
- ✓ All output to stdout (not mixed with errors)

**Example - Good**:
```kotlin
class ProgressReporter(private val verbose: Boolean) {
    fun phase(name: String) {
        println("\n=== $name ===")
    }
    
    fun progress(message: String) {
        if (verbose) {
            println("  $message")
        }
    }
    
    fun status(current: Int, total: Int, item: String) {
        val percent = (current * 100) / total
        println("\r  [$percent%] $item ($current/$total)")
    }
    
    fun complete(summary: String) {
        println("\n✓ $summary\n")
    }
    
    fun error(message: String) {
        System.err.println("✗ Error: $message")
    }
}

// Usage:
val progress = ProgressReporter(verbose = config.options.verbose)

progress.phase("Sample Analysis")
for ((index, sample) in samples.withIndex()) {
    progress.status(index + 1, samples.size, sample.path)
    val result = analyzeSample(sample)
}

progress.complete("Analyzed ${samples.size} sample images")
```

**Example - Avoid**:
```kotlin
// Anti-pattern: No progress feedback
suspend fun analyzeSamples(samples: List<Image>) {
    samples.forEach { sample -> analyzeSample(sample) }
    // User stares at blank screen for 10 minutes
}

// Anti-pattern: Spam output
samples.forEach { sample ->
    println("Starting analysis of ${sample.path}")
    println("Extracting characteristics...")
    println("Colors: ...")
    println("Style: ...")
    // 1000+ lines of output, unusable
}
```
</rule_2>

---

<rule_3 priority="HIGH">
**ERROR REPORTING**: Errors must be clear, specific, and actionable.
- What went wrong (the error)
- Why it happened (the context)
- What to do (the fix)
- Example if non-obvious

**MUST**:
- ✓ Errors to stderr (not mixed with output)
- ✓ Error message starts with what went wrong
- ✓ Suggest specific fix
- ✓ Include relevant details (path, value, range)
- ✓ Non-technical wording for user-facing errors

**Example - Good**:
```kotlin
// Good error reporting examples:

// Config error
System.err.println("""
ERROR: Output folder not found

  Path: /Users/user/NonexistentFolder
  Problem: Directory does not exist
  Fix: Create the directory, or update "folders.output" in your config file
  
  Example:
    mkdir -p /Users/user/Qualified
""".trimIndent())

// File error
System.err.println("""
ERROR: Cannot read image file

  File: samples/corrupted.jpg
  Problem: File is corrupted or not a valid image
  Fix: Replace with a valid JPEG, PNG, HEIC, or WebP file
  
  Supported formats: JPEG, PNG, HEIC, WebP, TIFF, BMP, GIF
""".trimIndent())

// LLM error
System.err.println("""
ERROR: Cannot connect to LLM service

  Endpoint: http://localhost:1234/api/v1
  Problem: Connection refused (is LMStudio running?)
  Fix: 
    1. Start LMStudio on your machine
    2. Load a model in LMStudio
    3. Verify endpoint in config: ${"$"}(cat config.json | grep endpoint)
  
  Check LMStudio is running: curl http://localhost:1234/api/v1/models
""".trimIndent())
```

**Example - Avoid**:
```kotlin
// Anti-pattern: Cryptic errors
System.err.println("IOException: /path/to/file: No such file or directory")

// Anti-pattern: Silent failures
try {
    loadImage(path)
} catch (e: Exception) {
    // Nothing, user doesn't know what failed
}

// Anti-pattern: Overwhelming stack trace
e.printStackTrace()
```
</rule_3>

---

<rule_4 priority="HIGH">
**OUTPUT FORMATTING**: Human-readable output that's also machine-parseable.
- Summary results in plain text
- Optional JSON output for tools
- Consistent formatting
- Clear section breaks

**MUST**:
- ✓ Summary shows key results
- ✓ Structured format (not walls of text)
- ✓ Timestamps for long operations (optional)
- ✓ Support `--json` flag for machine parsing (future)

**Example - Good**:
```kotlin
fun printSummary(result: ProcessingResult) {
    println("""
    
╔════════════════════════════════════════╗
║     Wallpaper Qualifier Summary        ║
╚════════════════════════════════════════╝

📊 Analysis Results
  Samples analyzed:     ${result.samplesAnalyzed}
  Profile generated:    ${if (result.profileGenerated) "✓ Yes" else "✗ No"}
  
📋 Evaluation Results
  Candidates evaluated: ${result.candidatesEvaluated}
  Qualified:           ${result.qualified}
  Rejected:            ${result.rejected}
  Skipped:             ${result.skipped}
  
📁 Output
  Destination:         ${result.outputFolder}
  Images copied:       ${result.imagesCopied}
  Total size:          ${formatBytes(result.totalSize)}
  
⏱️  Performance
  Total time:          ${formatDuration(result.duration)}
  Rate:                ${result.candidatesEvaluated / result.duration.seconds} images/sec
    """.trimIndent())
}

fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${"$"}${bytes / 1024} KB"
    else -> "${"$"}${bytes / (1024 * 1024)} MB"
}

fun formatDuration(duration: Duration): String {
    val minutes = duration.inWholeMinutes
    val seconds = duration.inWholeSeconds % 60
    return "${"$"}{minutes}m ${"$"}{seconds}s"
}
```

**Example - Avoid**:
```kotlin
// Anti-pattern: Unstructured output
println("Samples: ${result.samplesAnalyzed}")
println("Qualified: ${result.qualified}")
println("Rejected: ${result.rejected}")
// Looks like random data dump
```
</rule_4>

---

<rule_5 priority="MEDIUM">
**SHELL COMPATIBILITY**: Respect shell conventions and environment.
- Use standard exit codes (0 = success, 1 = general error, 2 = usage error)
- Respect `NO_COLOR` environment variable (future)
- Don't assume terminal width (or wrap appropriately)
- Support piping (don't assume TTY)

**MUST**:
- ✓ Exit 0 on success
- ✓ Exit 1 on general errors
- ✓ Exit 2 on argument/usage errors
- ✓ Check `System.console() != null` for TTY features
- ✓ Provide JSON output for automation

**Example - Good**:
```kotlin
fun main(args: Array<String>) = runBlocking {
    try {
        val isInteractive = System.console() != null
        
        when {
            args.isEmpty() -> {
                System.err.println("Usage: wallpaper-qualifier config.json [--verbose]")
                exitProcess(2) // Usage error
            }
            args.contains("--help") -> {
                println(helpText)
                exitProcess(0)
            }
            else -> {
                val result = runQualifier(args[0], verbose = args.contains("--verbose"))
                if (result.isSuccess) {
                    if (isInteractive) {
                        printSummary(result.value)
                    } else {
                        printJSON(result.value) // Automation-friendly
                    }
                    exitProcess(0)
                } else {
                    System.err.println("Error: ${result.error}")
                    exitProcess(1)
                }
            }
        }
    } catch (e: Exception) {
        System.err.println("Fatal error: ${e.message}")
        exitProcess(1)
    }
}
```

**Example - Avoid**:
```kotlin
// Anti-pattern: Assuming TTY
println("\u001b[32mSuccess!\u001b[0m") // Breaks piping to log file

// Anti-pattern: Wrong exit code
exitProcess(0) // Even on error!

// Anti-pattern: Terminal width assumptions
val output = "Lorem ipsum dolor sit amet " * 100
println(output) // Doesn't wrap, unreadable
```
</rule_5>

---

## Checklist

Before finalizing CLI:

☐ **Arguments**: Simple, discoverable, documented in `--help`
☐ **Progress**: Shows phase, item count, percentage
☐ **Errors**: Specific, actionable, with examples
☐ **Summary**: Clear results with key metrics
☐ **Exit Codes**: 0 on success, 1 on error, 2 on usage error
☐ **Shell Compat**: Works with piping, respects environment
☐ **No Spam**: Progress updates don't overwhelm output

---

**Last Updated**: 2026-03-08
