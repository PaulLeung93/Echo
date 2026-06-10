---
name: toml-manager
description: Safely edits the gradle/libs.versions.toml file. Use this when you need to add new dependencies or update versions in the version catalog.
---

# TOML Manager Skill

## Goal
Manage dependencies in the Android Version Catalog (`libs.versions.toml`) without breaking syntax.

## Instructions
1.  **Analyze the Request:** Determine if you need to add a Version, a Library, or both.
2.  **Order of Operations:**
    * First, add the version variable using `add-version`.
    * Second, add the library definition using `add-lib` referencing that version.
3.  **Execution:**
    Run the python script `catalog_manager.py` located in this directory.

## Examples

### Scenario: User wants to add Retrofit 2.9.0
**Step 1: Add Version**
```bash
python3 .agent/skills/toml-manager/catalog_manager.py add-version retrofit "2.9.0"