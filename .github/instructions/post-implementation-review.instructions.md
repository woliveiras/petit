---
applyTo: "**/*.kt"
description: "Post-implementation code review workflow. Instructs agents to invoke the Code Review subagent after completing implementation tasks."
---

## Post-Implementation Code Review

After completing any implementation task that modifies or creates Kotlin files (features, bug fixes, refactors), you MUST invoke the `Code Review` agent as a subagent to review the changes before reporting completion to the user.

### When to Trigger

Invoke code review after:

- Creating new files (ViewModels, Screens, Repositories, Entities, DAOs, Workers)
- Modifying existing business logic, UI components, or architecture
- Refactoring code across multiple files

### When to Skip

Do NOT invoke code review for:

- Read-only operations (searching, exploring, explaining code)
- Documentation-only changes (markdown files, comments)
- Configuration-only changes (gradle, properties, yaml)
- Single-line trivial fixes (typos, import cleanup)

### How to Invoke

After your implementation is complete, delegate to the Code Review agent to review ALL files you changed. Include the list of changed files in your delegation.
