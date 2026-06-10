---
name: logcat-fetcher
description: Retrieves logs from the connected Android device or emulator. Use this when the user reports a crash, a bug, or asks "What went wrong?".
---

# Logcat Fetcher Skill

## Goal
Retrieve stack traces and log messages to diagnose runtime issues.

## Instructions
1.  **Determine the need:**
    * If the user says "It crashed", look for Errors (`-level E`) or the keyword "FATAL".
    * If the user says "The button doesn't work", look for Debug/Info logs (`-level D`) or a specific tag related to that feature.
2.  **Execution:**
    Run the python script `fetch_logs.py` located in this directory.
3.  **Analysis:**
    * Read the output.
    * Identify the file name and line number in the stack trace (e.g., `MainActivity.kt:45`).
    * Cross-reference this with the code to propose a fix.

## Examples

### Scenario: App Crashing
User: "The app crashes when I open the profile."
Action:
```bash
python3 .agent/skills/logcat-fetcher/fetch_logs.py --level E --lines 100