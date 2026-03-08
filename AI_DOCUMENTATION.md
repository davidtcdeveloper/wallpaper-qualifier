# AI Documentation System for Wallpaper Qualifier

This directory contains AI-friendly documentation designed to guide language models (LLMs) and development agents on how to work with the Wallpaper Qualifier codebase.

## Overview

The AI documentation system follows the pattern described in [Stop Getting Average Code from Your LLM](https://merowing.info/posts/stop-getting-average-code-from-your-llm/). Instead of inline comments or vague READMEs, we provide:

1. **AGENTS.md** — Entry point for understanding the project
2. **ai-rules/** — Domain-specific coding standards
3. **rule-loading.md** — Guide to selecting which rules apply to your task

## Quick Start

### For AI Agents (Copilot, Claude, GPT, etc.)

1. **Start here**: Read `/AGENTS.md` for project overview
2. **Then read**: `ai-rules/rule-loading.md` to understand which rules apply to your task
3. **Load specific rules**: Based on what you're working on (e.g., `ai-rules/image-processing.md` for image tasks)

### For Humans

**Start with AGENTS.md** — it documents:
- Project purpose and goals
- Architecture overview
- Directory structure
- Build commands
- Key integration points
- Workflow guidelines

Then reference specific rule files as needed for detailed patterns.

## File Structure

```
AGENTS.md                           # Project overview & quick reference
ai-rules/
├── rule-loading.md                # Navigation guide (read first!)
├── general.md                      # Core engineering principles
├── kotlin-multiplatform.md         # Kotlin & KMP patterns
├── image-processing.md             # Image handling & optimization
├── llm-integration.md              # LLM API integration
├── configuration.md                # JSON configuration & validation
├── cli-design.md                   # CLI user experience
└── testing.md                      # Testing strategies
```

## Rule Files at a Glance

| Rule File | Lines | Focus |
|-----------|-------|-------|
| **general.md** | 332 | Code quality, architecture, naming |
| **kotlin-multiplatform.md** | 447 | Kotlin idioms, coroutines, KMP |
| **image-processing.md** | 321 | Format handling, optimization |
| **llm-integration.md** | 377 | LLM API, sequential requests, parsing |
| **configuration.md** | 313 | JSON schema, validation, defaults |
| **cli-design.md** | 380 | Arguments, progress, error reporting |
| **testing.md** | 480 | Unit/integration/E2E strategies |

**Total**: ~2,940 lines of focused, structured guidance

## How to Use This System

### When Starting a New Feature

```
1. Load: AGENTS.md (overview)
2. Load: ai-rules/rule-loading.md (which rules apply?)
3. Load: ai-rules/general.md (always) + domain-specific rule
4. Implement according to patterns
```

### When Fixing a Bug

```
1. Load: ai-rules/general.md (foundation)
2. Load: domain-specific rule for affected module
3. Load: ai-rules/testing.md (verify your fix)
```

### When Working with Configuration

```
1. Load: ai-rules/configuration.md (primary)
2. Load: ai-rules/cli-design.md (for user messages)
```

### When Implementing Image Processing

```
1. Load: ai-rules/image-processing.md (primary)
2. Load: ai-rules/general.md (code quality)
3. Load: ai-rules/testing.md (verify correctness)
```

### When Integrating with LLM

```
1. Load: ai-rules/llm-integration.md (primary)
2. Load: ai-rules/kotlin-multiplatform.md (async patterns)
3. Load: ai-rules/testing.md (mock LLM)
```

## Key Principles Encoded

### Architecture
- **Modular design** with clear separation of concerns
- **Dependency injection** for testability
- **Sequential LLM requests** (hard constraint)
- **Parallel image processing** (where possible)

### Code Quality
- **Immutability by default** (val before var)
- **Explicit error handling** (no silent failures)
- **Clear naming** (names reveal intent)
- **Single responsibility** (one thing per function)

### User Experience
- **Clear progress reporting** (not staring at blank screen)
- **Actionable error messages** (not cryptic stack traces)
- **Structured configuration** (JSON validation before runtime)
- **Standard exit codes** (Unix conventions)

### Testing
- **Test pyramid**: Unit (60%) → Integration (30%) → E2E (10%)
- **Happy paths + error cases** (both tested equally)
- **Mocked LLM** (no real HTTP calls in tests)
- **Concurrent code** (tested explicitly)

## Why This System?

LLMs trained on the average of the internet produce average code. This system teaches AI agents:

- **What good looks like** for this specific project
- **How we handle** async code, image processing, LLM integration
- **Why we constrain** parallelism for LLM requests
- **How we test** concurrent code, LLM interactions
- **Why we validate** configuration early

Result: **AI agents produce production-quality code** that matches your standards, not generic patterns.

## Progressive Disclosure

Instead of loading 3,000 lines of rules for every task, agents load only what's relevant:

- **New feature**: ~400 lines (general + domain-specific)
- **Bug fix**: ~300 lines (general + affected domain)
- **Config work**: ~200 lines (configuration + CLI)

Small, focused guidance → Better decisions → Less context pollution.

## Integration with Tools

### With GitHub Copilot (in Editor)

```kotlin
// When you ask Copilot to help with image processing:
// It automatically loads the relevant rules from ai-rules/
// Code suggestions align with project standards
```

### With Copilot CLI

```bash
copilot explain "how does image optimization work?"
copilot generate "add TIFF format support"
```

### With Other LLMs

Copy the relevant rule file into your prompt. AI models respect the structure and apply the patterns.

## Maintaining This System

When you discover patterns worth encoding:

1. **Identify the pattern** (e.g., "we always validate configuration early")
2. **Document it** in the appropriate rule file (or create a new one)
3. **Add examples** of good vs. avoid patterns
4. **Update rule-loading.md** if adding new rules
5. **Update AGENTS.md** quick reference

Example: If you create advanced retry logic, create `ai-rules/error-recovery.md` and document when to load it.

## References

- **Inspiration**: [Stop Getting Average Code from Your LLM](https://merowing.info/posts/stop-getting-average-code-from-your-llm/)
- **MCP Protocol**: [Model Context Protocol](https://modelcontextprotocol.io/)
- **Project**: Wallpaper Qualifier - macOS CLI tool for intelligent wallpaper curation

---

**Last Updated**: 2026-03-08
**Total Rules**: 7 files, ~2,940 lines
**System Goal**: Produce production-quality code from AI agents, not average code
