# Dependency PR Merge Checklist

Use this checklist for pull requests that update dependencies (Dependabot or manual).

## 1. Scope and Risk

- [ ] PR batch is coherent (build tooling, UI/charting, AndroidX runtime, or test/utils).
- [ ] Risk label is present (`deps:risk-high`, `deps:risk-medium`, or `deps:risk-low`).
- [ ] High-risk updates are isolated from other update types.

## 2. Build and Test Gates

- [ ] `./gradlew assembleDebug && ./gradlew installDebug`
- [ ] `./gradlew test`
- [ ] `./gradlew spotlessCheck`

If any command fails:

- [ ] Failures were triaged as update-related vs pre-existing.
- [ ] Regression fix is included in the same PR, or the problematic dependency was excluded.

## 3. Behavioral Validation

- [ ] Core flows compile and run locally after update.
- [ ] Critical UI components affected by updated libraries were validated.
- [ ] For charting/Compose updates, key screens were smoke-tested manually.

## 4. Security and Compliance

- [ ] No new secrets or sensitive files introduced.
- [ ] Security advisories are reviewed when update is security-related.
- [ ] Licensing implications were checked for newly introduced libraries.

## 5. Merge Decision

- [ ] Ready to merge now.
- [ ] Needs split (remove one risky dependency from this batch).
- [ ] Needs follow-up fix PR before merge.
