# Petit Android App

Android app built with Kotlin, Jetpack Compose, Room, Hilt, and WorkManager.

Petit is a multi-pet health tracker — track weight, vaccinations, deworming, and reminders for cats, dogs, rabbits, birds, fish, and more.

## Technical Docs

- [Data modeling](docs/data-modeling.md)
- [Testing guide](docs/running-tests.md)
- [Accessibility testing guide (TalkBack)](docs/talkback-testing-guide.md)
- [Design system](docs/design-system.md)
- [Public release/compliance checklist](docs/release-compliance-checklist.md)

## Development

- `./gradlew assembleDebug && ./gradlew installDebug`
- `./gradlew test`
- `./gradlew spotlessCheck`

## Policies

- [License](LICENSE)
- [NOTICE](NOTICE)
- [Code of Conduct](CODE_OF_CONDUCT.md)
- [Contributing](CONTRIBUTING.md)
- [Security](SECURITY.md)
- [Trademark policy](TRADEMARK_POLICY.md)
- [Code owners](.github/CODEOWNERS)
- [Issue templates](.github/ISSUE_TEMPLATE)
- [Pull request template](.github/pull_request_template.md)
- [Dependabot config](.github/dependabot.yml)

## Licensing Model

This repository is 100% open source:

- Code license: GNU AGPL-3.0.
- You can use, modify, and self-host the project under AGPL terms.

Commercial distribution and use of original brand assets are not allowed without explicit authorization.

## Source Code Availability

For any distributed binary build (APK/AAB), users must have clear access to the corresponding source code.

- Official source repository: this repository
- Preferred reference for releases: corresponding Git tag/commit hash
- If a network-deployed version is modified, make the corresponding source code publicly accessible under AGPL terms
