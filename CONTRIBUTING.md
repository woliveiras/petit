# Contributing Guide

Thanks for contributing to Petit.

## Language Policy

English is the default language for:

- Issues
- Pull requests
- Commit messages
- Documentation updates

If needed, Portuguese notes are welcome as supplementary context, but include an English summary.

## Before You Start

1. Check existing issues/discussions.
2. For larger changes, post a short implementation plan in the issue.
3. DCO sign-off is required for all commits.

## Issue Triage

- Bug: include current behavior, expected behavior, and reproduction steps.
- Feature: describe the problem, proposed solution, and user impact.
- Security: follow [SECURITY.md](SECURITY.md); do not create a public issue.

## Branch Naming

- `feat/<short-description>`
- `fix/<short-description>`
- `docs/<short-description>`
- `refactor/<short-description>`
- `chore/<short-description>`

## Commits (Conventional Commits)

Format:

`type(scope): description`

Examples:

- `feat(pets): add pet type selection`
- `fix(weight): correct grams validation`
- `docs(security): update policy`

## Developer Certificate of Origin (DCO)

All commits must include a Signed-off-by trailer confirming authorship rights.

Use:

- `git commit -s`

Or configure Git once:

- `git config --global format.signoff true`

## Project Technical Standards

- Language: Kotlin.
- UI: Jetpack Compose.
- DI: Hilt.
- Persistence: Room.
- Architecture: MVVM + Repository.
- Avoid injecting DAOs directly into ViewModels; use repositories.

## Local Checklist Before Opening a PR

Run:

- `./gradlew assembleDebug && ./gradlew installDebug`
- `./gradlew test`
- `./gradlew spotlessCheck`

If needed:

- `./gradlew spotlessApply`

## Pull Request Checklist

- [ ] Change is focused on one clear problem.
- [ ] Build, tests, and formatting checks pass locally.
- [ ] No secrets or sensitive data included.
- [ ] PR description clearly explains what changed and how to validate.
- [ ] Screenshots/video added for visual changes.
- [ ] Tests added/updated when applicable.

## Review and Merge

- Small, focused PRs are prioritized.
- Architecture changes must justify trade-offs.
- The maintainer may request updates before merge.
