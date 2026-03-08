# Rule Loading Guide: Wallpaper Qualifier

## Overview

This file tells you which rules to load based on your task. Instead of loading all rules at once (which pollutes context), use this guide to load only what's relevant to your current work.

**Progressive disclosure means you load rules on demand, not everything upfront.**

---

## Rule Loading Triggers

### 📋 general.md - Core Engineering Principles
**Load when:**
- Always (foundation for all other rules)
- Starting any new feature or module
- Making architectural decisions
- Reviewing code before committing

**Keywords:** architecture, design, patterns, quality, standards, conventions

**Size**: ~300 lines | **Purpose**: Sets tone and non-negotiables for code quality

---

### 🔷 kotlin-multiplatform.md - Kotlin & KMP Specific Patterns
**Load when:**
- Writing Kotlin code for this project
- Working with coroutines or concurrency
- Setting up platform-specific code
- Optimizing performance

**Keywords:** Kotlin, KMP, multiplatform, coroutines, performance, native, concurrency

**Size**: ~250 lines | **Purpose**: Kotlin idioms and KMP best practices for macOS

---

### 🖼️ image-processing.md - Image Handling & Optimization
**Load when:**
- Adding image format support
- Optimizing image compression or conversion
- Handling image decoding/encoding
- Working with Kotlin Koog library
- Fixing image-related bugs

**Keywords:** image, format, JPEG, PNG, HEIC, WebP, optimization, Koog, compression, quality

**Size**: ~280 lines | **Purpose**: Image processing patterns and format handling

---

### 🤖 llm-integration.md - LLM API Integration
**Load when:**
- Implementing LLM client code
- Adding prompt templates
- Modifying request/response handling
- Debugging LLM interactions
- Implementing retry logic (future)

**Keywords:** LLM, API, LMStudio, sequential, queue, request, response, prompt, model

**Size**: ~260 lines | **Purpose**: LLM integration patterns and request queue management

---

### ⚙️ configuration.md - JSON Configuration & Parsing
**Load when:**
- Designing or modifying configuration schema
- Parsing or validating configuration files
- Adding new configuration options
- Improving error messages for config issues

**Keywords:** configuration, JSON, config, parsing, schema, validation, settings

**Size**: ~200 lines | **Purpose**: Configuration design and validation patterns

---

### 💻 cli-design.md - Command-Line Interface Patterns
**Load when:**
- Designing CLI commands or arguments
- Adding progress reporting
- Improving user feedback
- Handling CLI input/output
- Error reporting to users

**Keywords:** CLI, command-line, arguments, output, progress, feedback, user experience

**Size**: ~200 lines | **Purpose**: CLI design and user interaction patterns

---

### 🧪 testing.md - Testing Strategies & Patterns
**Load when:**
- Writing unit tests
- Designing integration tests
- Setting up test fixtures or mocks
- Testing concurrent code
- Debugging test failures

**Keywords:** test, testing, unit, integration, mock, fixture, assert, concurrency

**Size**: ~250 lines | **Purpose**: Testing strategies specific to this project

---

## Quick Reference: Common Workflows

### When starting a new CLI command or feature:
```
1. Load: docs/ai-rules/general.md (foundation)
2. Load: docs/ai-rules/kotlin-multiplatform.md (coding patterns)
3. Load: docs/ai-rules/cli-design.md (if touching CLI)
4. Load: domain-specific rule (e.g., docs/ai-rules/image-processing.md if image-related)
```

### When implementing image processing:
```
1. Load: docs/ai-rules/general.md
2. Load: docs/ai-rules/kotlin-multiplatform.md
3. Load: docs/ai-rules/image-processing.md (primary focus)
4. Load: docs/ai-rules/testing.md (test your implementation)
```

### When integrating with LLM:
```
1. Load: docs/ai-rules/general.md
2. Load: docs/ai-rules/kotlin-multiplatform.md
3. Load: docs/ai-rules/llm-integration.md (primary focus)
4. Load: docs/ai-rules/testing.md (important for mocking LLM)
```

### When working on configuration:
```
1. Load: docs/ai-rules/general.md
2. Load: docs/ai-rules/configuration.md (primary focus)
3. Load: docs/ai-rules/cli-design.md (for error messages and user guidance)
```

### When fixing a bug:
```
1. Load: docs/ai-rules/general.md
2. Load: docs/ai-rules/kotlin-multiplatform.md
3. Load: domain-specific rule for the affected module
4. Load: docs/ai-rules/testing.md (to verify your fix)
```

### When writing tests:
```
1. Load: docs/ai-rules/general.md
2. Load: docs/ai-rules/kotlin-multiplatform.md (coroutines, async patterns)
3. Load: docs/ai-rules/testing.md (primary focus)
4. Load: domain-specific rule(s) being tested
```

### Before committing:
```
1. Load: docs/ai-rules/general.md (review code style and patterns)
2. Run: ./gradlew ktlintFormat ktlint test
3. Verify all checks pass
```

---

## Loading Strategy

### Best Practices

1. **Always load general.md first** — it's the foundation
2. **Load kotlin-multiplatform.md second** — foundational coding patterns
3. **Load domain-specific rules** based on what you're building
4. **Load testing.md when writing tests** — ensures quality
5. **Keep loaded rules minimal** — only what's directly relevant
6. **Refresh rules when context-switching** — load new rules, drop old ones

### Avoiding Context Pollution

The goal is to provide relevant guidance WITHOUT overwhelming context. Here's how:

- **Small focused files** (~200-300 lines each) mean quick reading
- **Clear section headers** let you skip irrelevant parts
- **Code examples** are concrete and specific to this project
- **Priority markers** highlight what matters most

### Progressive Disclosure in Action

Example: You're adding HEIC format support.

1. Load `docs/ai-rules/image-processing.md` to understand existing patterns
2. Find the "Format Support" section
3. Review examples for similar formats (e.g., WebP)
4. Check the checklist for what you need to verify
5. You have exactly what you need without loading unrelated rules

---

## Combining Multiple Rules

When working on a complex task, you may need multiple rule files:

### Example: Implementing new image analysis workflow
**Task**: Add new analysis step to profile generation

**Rules to load**:
1. `docs/ai-rules/general.md` — architectural decision making
2. `docs/ai-rules/kotlin-multiplatform.md` — implementation patterns
3. `docs/ai-rules/image-processing.md` — image-specific details
4. `docs/ai-rules/llm-integration.md` — how to call LLM for analysis
5. `docs/ai-rules/testing.md` — verify implementation with tests

**How to use them together**:
- Use `docs/ai-rules/general.md` to design the feature (separation of concerns, naming)
- Use `docs/ai-rules/kotlin-multiplatform.md` for async/await patterns
- Use `docs/ai-rules/image-processing.md` for image input handling
- Use `docs/ai-rules/llm-integration.md` for prompt construction and response parsing
- Use `docs/ai-rules/testing.md` to ensure correctness

---

## When to Create New Rules

Add a new rule file when:
- You find yourself repeating the same pattern guidance
- A module becomes complex enough to warrant its own standards
- Team members ask "how do we usually handle X?"
- You make the same mistake twice

Examples of future candidates:
- `caching-strategy.md` (if adding profile caching)
- `performance-optimization.md` (if hitting performance targets)
- `error-recovery.md` (if implementing advanced retry logic)

---

## Glossary of Rules

| Rule File | Scope | Focus |
|-----------|-------|-------|
| `docs/ai-rules/general.md` | Entire project | Code quality, architecture, naming |
| `docs/ai-rules/kotlin-multiplatform.md` | Language & runtime | Kotlin idioms, KMP specifics, async |
| `docs/ai-rules/image-processing.md` | Image module | Format handling, optimization, quality |
| `docs/ai-rules/llm-integration.md` | LLM module | API integration, queuing, serialization |
| `docs/ai-rules/configuration.md` | Config module | Schema design, validation, defaults |
| `docs/ai-rules/cli-design.md` | CLI module | Argument handling, feedback, errors |
| `docs/ai-rules/testing.md` | Test suite | Unit, integration, mocking, concurrency |

---

## How LLMs Use This Guide

When you ask an LLM agent to work on this project:

1. Agent reads **this file** (rule-loading.md)
2. Agent identifies task context
3. Agent loads **only relevant rules** from the list above
4. Agent has focused guidance specific to the task
5. Agent doesn't waste tokens on unrelated patterns

Example agent behavior:
```
User: "Add TIFF image format support"
Agent thinks:
  - This affects image processing
  - Load: docs/ai-rules/general.md, docs/ai-rules/kotlin-multiplatform.md, docs/ai-rules/image-processing.md
  - Not needed: docs/ai-rules/llm-integration.md, docs/ai-rules/testing.md (yet)
  - Reads docs/ai-rules/image-processing.md section on "Format Support"
  - Implements based on HEIC/WebP examples
  - When done, loads docs/ai-rules/testing.md to verify
```

---

## Integration with AGENTS.md

This file (`docs/ai-rules/rule-loading.md`) is referenced in **AGENTS.md** as:

> Always load this file to understand which other files you need to load

Think of it as:
- **AGENTS.md** = Project overview + quick reference
- **rule-loading.md** = Navigation map to specific rules
- **domain rules** = Focused guidance for implementation

---

**Last Updated**: 2026-03-08
**Total Rules**: 7 files
**Average Rule Size**: 230 lines each
**Total Guidance**: ~1,600 lines focused, contextual patterns
