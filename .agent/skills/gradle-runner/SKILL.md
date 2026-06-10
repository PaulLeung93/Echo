---
name: gradle-runner
description: Runs arbitrary Gradle tasks. Use this when you need to run specific tasks like 'test', 'lint', 'compileDebugKotlin', or 'signingReport'.
---

# Gradle Runner Skill

## Goal
Execute a specific Gradle task provided by the user or a workflow.

## Instructions
1.  Identify the task name (e.g., `testDebugUnitTest`, `ktlintCheck`).
2.  Run the command:
    ```bash
    ./gradlew <task_name>
    ```
3.  Report the outcome (Success/Failure) and any relevant output.

## Examples
* **User:** "Run the unit tests."
    * **Action:** `./gradlew testDebugUnitTest`
* **User:** "Check for lint errors."
    * **Action:** `./gradlew lintDebug`
* **Workflow:** "Verify compilation."
    * **Action:** `./gradlew compileDebugKotlin`