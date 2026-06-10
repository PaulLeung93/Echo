---
name: android-build
description: Builds the Android debug APK. Use this to verify code compilation or when the user asks to "build the app".
---

# Android Build Skill

## Goal
Execute the standard Gradle build command to assemble the debug APK and report the outcome.

## Instructions
1.  Navigate to the project root.
2.  Run the following command in the terminal:
    ```bash
    ./gradlew assembleDebug
    ```
3.  **Analyze Output:**
    * **Success:** Locate the APK at `app/build/outputs/apk/debug/app-debug.apk` and report success.
    * **Failure:** Capture the lines starting with `e:` (error) and summarize the compilation error.

## Constraints
* Do not run `clean` unless explicitly asked (it slows down the build).
* Use the Gradle wrapper (`./gradlew`) to ensure the correct Gradle version.