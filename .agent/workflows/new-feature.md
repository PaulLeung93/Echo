---
name: New Feature Scaffold
trigger: "/new-feature"
description: Standard procedure for creating a new Android feature (Screen + VM + Repo).
---

# Workflow: Create New Feature

## Step 1: Context & Planning
**Goal:** Define the scope and integrate with the CURRENT project state.
1.  **Inquiry:** Ask the user: "What is the name of the feature and what should it do?"
2.  **Context Download:**
    * **Read `ARCHITECTURE.md`:** Identify the correct parent package, existing modules, and where this feature fits in the Navigation Graph.
    * **Read `DEVLOG.md`:** Scan the last 3 entries for recent architectural changes, "gotchas," or specific bugs to avoid.
    * **Read `standards.md`:** Ensure adherence to Clean Architecture.
3.  **Proposal:** Propose a file structure plan to the user (e.g., `feature/profile/presentation/...`) and confirm where it hooks into the `NavHost`.

## Step 2: Dependency Check
**Goal:** Ensure we have the necessary libraries before coding.
1.  Analyze the feature requirements (e.g., Does it need Camera? Maps? Biometrics?).
2.  **Action:** Use the `dependency-search` skill to find any missing libraries.
3.  **Action:** Use `toml-manager` to add them to `libs.versions.toml` if needed (checking `standards.md` for BOM rules).

## Step 3: Scaffold Code
**Goal:** Create the files.
1.  **Data Layer:** Create the Repository Interface and DTOs.
2.  **Domain Layer:** Create the UseCase (only if logic is complex; otherwise skip per Standards).
3.  **Presentation Layer:**
    * Create the `ViewModel` (using Hilt `@HiltViewModel`).
    * Create the `UiState` (Immutable Data Class).
    * Create the `Screen` (Composable).

## Step 4: Verification Loop
**Goal:** Ensure it compiles.
1.  **Action:** Run `gradle-runner` with `./gradlew compileDebugKotlin`.
2.  If it fails, **read the error log**, fix the errors, and re-run (Self-Correction).
3.  **Action:** If successful, ask the user: "Would you like me to run this on the emulator?"

## Step 5: Integration & Documentation
1.  **Navigation:** Add the new screen to the Navigation Graph (e.g., `NavHost`).
2.  **Documentation:** Ask the user: "Feature complete. Should I run `/docs` to update the Architecture and Dev Log?"