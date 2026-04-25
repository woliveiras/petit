# Security Policy

## Supported Versions

Security fixes are provided for:

| Version               | Support |
| --------------------- | ------- |
| `main`                | Yes     |
| Latest stable release | Yes     |
| Older releases        | No      |

## Reporting a Vulnerability

Do not open public issues for vulnerabilities.

1. Open a private GitHub Security Advisory.
2. If that is not possible, use the maintainer contact listed in the repository.
3. Include at least:

- Problem description and impact.
- Reproduction steps.
- Affected version/commit.
- Evidence (logs, screenshot, minimum PoC).

## Response SLA

- Acknowledgement: up to 3 business days.
- Initial triage: up to 7 business days.
- Status update cadence: at least weekly.
- Initial fix for high/critical severity: target up to 30 days when feasible.

## Severity Matrix

Vulnerabilities are classified by impact and exploitability.
When relevant, CVSS v3.1 is used as a reference.

| Severity | CVSS (reference) | Example                                                             |
| -------- | ---------------- | ------------------------------------------------------------------- |
| Critical | 9.0 - 10.0       | Remote code execution, large-scale sensitive data leak, auth bypass |
| High     | 7.0 - 8.9        | Privilege escalation, relevant data exposure, high-impact flaw      |
| Medium   | 4.0 - 6.9        | Limited exposure, preconditioned abuse, moderate impact             |
| Low      | 0.1 - 3.9        | Low impact, difficult exploitation, no meaningful data risk         |

## Severity Handling Targets

The timelines below are operational targets and can vary by complexity.

| Severity | Acknowledgement       | Initial triage         | Mitigation/fix target |
| -------- | --------------------- | ---------------------- | --------------------- |
| Critical | up to 24h             | up to 72h              | up to 7 days          |
| High     | up to 3 business days | up to 7 business days  | up to 30 days         |
| Medium   | up to 5 business days | up to 10 business days | up to 60 days         |
| Low      | up to 7 business days | up to 15 business days | prioritized backlog   |

## Assessment Scope

In scope:

- Android app (Kotlin/Compose), local data flows, and app integrations.
- Local persistence (Room/DataStore), export/import, and family group sync (Nearby Connections).
- Build/runtime dependencies and release/compliance configuration.

Out of scope:

- Physical attacks on devices unrelated to the app itself.
- Social engineering, phishing, DDoS, spam, or third-party infrastructure abuse.
- Reports without minimum reproducibility or verifiable impact.

## Responsible Disclosure

- Keep reports private until a fix is published.
- After remediation, a security note may be published with impact and affected versions.
- Active exploitation, unauthorized access, social engineering, DDoS, or illegal activity are not accepted.

## Disclosure Process

1. Private report intake.
2. Technical validation and severity classification.
3. Mitigation/fix planning.
4. Advisory publication after fix availability (when applicable).
5. Researcher credit upon request.

## Security Requirements for Contributions

- Do not commit secrets (tokens, keys, credentials, keystores).
- Validate user input before persistence.
- Keep dependencies free from known vulnerabilities.
- Review security-sensitive changes in PRs.

## Safe Harbor

Researchers acting in good faith under this policy, without violating user privacy or degrading services, will not be targeted by the maintainer for responsible security testing.

## Recommended PR Checks

- `./gradlew assembleDebug && ./gradlew installDebug`
- `./gradlew test`
- `./gradlew spotlessCheck`
