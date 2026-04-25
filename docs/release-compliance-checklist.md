# Release and Compliance Checklist (Public Repository)

This document defines the minimum steps to publish updates safely in a public repository while protecting security, brand assets, and licensing constraints.

## 1. Scope of publication

Before every release, confirm that this repository contains only public technical content.

- Keep only technical docs in `docs/`.
- Keep product strategy, roadmap, and monetization docs in the separate specs repository.
- Do not publish business planning documents in this repository.

## 2. Sensitive files and secrets audit

Run before creating a release tag:

```bash
git ls-files | rg 'miw-release-key\.jks|keystore\.properties|local\.properties|google-services\.json' || true
```

Expected result:

- No sensitive files tracked.
- Only template files may appear (for example, `keystore.properties.template`).

Also check for accidental secrets in text files:

```bash
rg -n --hidden --glob '!build/**' --glob '!.git/**' \
  'api[_-]?key|secret|token|client_secret|storePassword|keyPassword'
```

If any real secret is found:

1. Remove it from source immediately.
2. Rotate compromised credentials.
3. Rewrite history only if needed and coordinated.

## 3. Legal and policy compliance gate

Confirm these files exist and are up to date:

- `LICENSE` (GNU AGPL-3.0)
- `NOTICE`
- Public privacy policy URL (`https://woliveiras.github.io/petit/privacy-policy/`)
- `TRADEMARK_POLICY.md`
- `SECURITY.md`
- `CONTRIBUTING.md`

Confirm public messaging consistency:

- Open-source use, modification, and self-hosting are allowed under AGPL terms.
- Brand assets (name/logo/identity) are restricted by trademark policy.

## 4. Build and quality gates

Run locally before release:

```bash
./gradlew assembleDebug && ./gradlew installDebug
./gradlew test
./gradlew spotlessCheck
```

Optional cleanup when style fails:

```bash
./gradlew spotlessApply
```

## 5. Dependency and security hygiene

Before tagging:

- Review Dependabot or dependency alerts.
- Check for critical/high CVEs in runtime and build dependencies.
- Prefer patch/minor upgrades for security updates.
- Confirm no newly introduced risky permissions/unsafe APIs.
- Keep `.github/dependabot.yml` active with:
  - grouped security updates,
  - reduced PR noise for minor/patch,
  - semver-major updates reviewed manually.

## 6. Release metadata checklist

- Version metadata updated where applicable.
- Release notes include:
  - What changed.
  - Any migration notes.
  - Security-relevant fixes.
- Public docs links still valid.

## 7. GitHub recommended settings

Apply once per repository (and review quarterly).

### Branch protection (main)

- Require pull request before merge.
- Require at least 1 approving review.
- Dismiss stale approvals on new commits.
- Require status checks to pass.
- Block force push and deletion on protected branch.

### Security settings

- Enable Dependabot alerts.
- Enable Dependabot security updates.
- Enable secret scanning and push protection (if available).
- Keep private vulnerability reporting enabled (Security Advisories).

### Collaboration governance

- Add CODEOWNERS for critical areas:
  - `app/src/main/java/com/miw/app/data/**`
  - `app/src/main/java/com/miw/app/worker/**`
  - `LICENSE`, `NOTICE`, `TRADEMARK_POLICY.md`, `SECURITY.md`
- Keep issue and PR templates active.

## 8. Pre-public-release final check (quick run)

```bash
# 1) Sensitive tracked files
git ls-files | rg 'miw-release-key\.jks|keystore\.properties|local\.properties|google-services\.json' || true

# 2) Secret patterns in repo
rg -n --hidden --glob '!build/**' --glob '!.git/**' \
  'api[_-]?key|secret|token|client_secret|storePassword|keyPassword'

# 3) Build + tests + style
./gradlew assembleDebug && ./gradlew installDebug
./gradlew test
./gradlew spotlessCheck
```

If all checks pass, release can be published.

## 9. Incident fallback (if something leaks)

- Revoke and rotate exposed credentials immediately.
- Remove sensitive artifact from latest commit.
- Evaluate history rewrite and communication impact.
- Publish security advisory if users are affected.
- Add a regression checklist item to prevent recurrence.
