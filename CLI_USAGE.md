# CLI Usage

Wallpaper Qualifier is a command-line interface tool.

## Basic Usage

```bash
wallpaper-qualifier [config-file]
```

-   **`config-file`**: Path to your `config.json` file. If not provided, it defaults to `config.json` in the current directory.

## Flags

-   **`--help`**, **`-h`**: Displays the help message and usage information.
-   **`--version`**, **`-v`**: Displays the current version of the tool.
-   **`--verbose`**: (Future) Enables detailed debug logging.

## Examples

**Run with default config.json:**
```bash
./wallpaper-qualifier
```

**Run with a specific config file:**
```bash
./wallpaper-qualifier my-settings.json
```

**Show help:**
```bash
./wallpaper-qualifier --help
```

## Exit Codes

-   **`0`**: Success.
-   **`1`**: Configuration error.
-   **`2`**: Image processing error.
-   **`3`**: LLM service error.
-   **`127`**: Unknown error.
