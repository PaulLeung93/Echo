---
name: dependency-search
description: Searches Maven Central for Android/Kotlin library versions. Use this when the user asks to add a library (like Retrofit, Hilt, Room) or when you need to verify the latest version of a dependency.
---

# Dependency Search Skill

## Goal
Find the correct Group ID, Artifact ID, and Latest Version for a requested library to prevent version hallucinations.

## Instructions
1.  Identify the library name from the user's request (e.g., "Gson", "Hilt", "Compose Navigation").
2.  Run the python script located in this directory:
    ```bash
    python3 .agent/skills/dependency-search/search_maven.py "query_string"
    ```
3.  Read the output.
4.  **Verification Step:**
    * If the user asked for a specific library (e.g., "Retrofit"), ensure the Group ID matches the standard provider (e.g., `com.squareup.retrofit2` is correct; `com.fake.retrofit` is not).
5.  Present the Gradle string or Version Catalog (TOML) entry to the user, or insert it into `libs.versions.toml` if instructed.

## Examples
* User: "Add Retrofit to the project."
    * Action: `python3 search_maven.py "retrofit"`
* User: "What is the latest version of Coroutines?"
    * Action: `python3 search_maven.py "kotlinx-coroutines-android"`