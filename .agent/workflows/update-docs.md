---
name: Update Documentation
trigger: "/docs"
description: Updates ARCHITECTURE.md (Current State) and appends to DEVLOG.md (History).
---

# Workflow: Update Documentation

## Step 1: Analyze Changes
1.  **Context Scan:** Review the recent conversation and git status to understand what was just built.
2.  **Structure Scan:** Run `tree -L 4 -I "build|.*"` to get the raw structure.
3.  **Navigation Scan:** Search for `NavHost` and `.navigate` calls in the codebase to understand the screen flow.

## Step 2: Update Architecture (Snapshot)
**Target:** `ARCHITECTURE.md`
**Action:** Overwrite the file to reflect the *current* reality.

### Section 1: Project Status
* List currently active features and their completion status.

### Section 2: Navigation Flow (New!)
* Create a text-based graph of the app's navigation.
* **Format:**
    ```mermaid
    graph TD
    LoginScreen -->|Success| HomeScreen
    LoginScreen -->|Forgot Password| ForgotPasswordScreen
    HomeScreen -->|Click Profile| ProfileScreen
    ```
* *Note: If a specific flow isn't clear, mark it as disconnected.*

### Section 3: Directory Map (Annotated)
* Generate the file tree.
* **CRITICAL:** Next to *every* file/folder, append a short comment (`# ...`) explaining its purpose.
    * *Example:* `LoginViewModel.kt # Manages auth state and API calls`

### Section 4: Key Decisions
* Record any new libraries or patterns added since the last update.

## Step 3: Update Dev Log (Journal)
**Target:** `DEVLOG.md`
**Action:** **APPEND** a new entry to the top of the file.
1.  **Header:** `### [Date] - [Feature/Bug Name]`
2.  **Changes:** Bullet points of what changed.
3.  **Issues & Solutions:** Note any build errors or "Gotchas" encountered and how they were fixed.

## Step 4: Final Verification
1.  Show the user the new Dev Log entry and ask: "Does this summary look correct?"