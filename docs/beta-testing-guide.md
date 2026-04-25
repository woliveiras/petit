# Practical Google Play Beta Testing Guide

This guide assumes you already have a Google Play developer account.

## Prerequisites

- Active Google Play Console account
- Signed release AAB build
- Public privacy policy URL
- App icon (512x512 PNG)
- At least two screenshots
- Tester email list

## 1. Create the App in Play Console

1. Open https://play.google.com/console.
2. Select Create app.
3. Fill basic fields:

- App name
- Default language (English recommended)
- App type: App
- Distribution: Free or paid

4. Confirm required declarations and create.

## 2. Complete Mandatory App Content

In Play Console, complete these sections before publishing:

- Privacy policy
- App access
- Ads declaration
- Content rating questionnaire
- Target audience
- Data safety form

## 3. Configure Store Listing

Recommended fields:

- App name
- Short description (<= 80 chars)
- Full description (<= 4000 chars)
- Phone screenshots
- App icon
- Optional feature graphic

Keep listing text aligned with implemented app behavior.

## 4. Generate Release AAB

Before building, update app version in:

`app/build.gradle.kts`

Example:

```kotlin
defaultConfig {
	versionCode = 4
	versionName = "2.0.1"
}
```

Build commands:

```bash
./gradlew clean
./gradlew bundleRelease
```

Generated file:

`app/build/outputs/bundle/release/app-release.aab`

Quick validation:

```bash
ls -lh app/build/outputs/bundle/release/app-release.aab
```

Recommended pre-upload checks:

```bash
./gradlew test
./gradlew spotlessCheck
```

## 5. Internal Testing Track

1. Go to Testing -> Internal testing.
2. Create release.
3. Upload AAB.
4. Add release notes.
5. Start rollout to internal testing.

## 6. Add Testers

1. Open Testers tab.
2. Create email list.
3. Add tester emails.
4. Save and assign list to the track.
5. Share invite link with testers.

## 7. Tester Instructions

Share this checklist with testers:

1. Accept invite link.
2. Install app from Play Store.
3. Use core flows.
4. Report bugs with:

- Steps to reproduce
- Expected vs actual result
- Screenshot/video when possible

## 8. Collect Feedback

Use one or more channels:

- Play Console private reviews
- Google Forms
- Team chat channel

## 9. Release Updates

For each new beta iteration:

1. Increase `versionCode`.
2. Update `versionName`.
3. Build new AAB (`# from repo root: ./gradlew clean bundleRelease`).
4. Upload new internal release.
5. Publish rollout.

Notes:

- `versionCode` must be greater than the last uploaded build on Play Console.
- Keep `versionName` aligned with release notes to simplify tester communication.

## 10. Promote to Production

When beta quality is acceptable:

- No critical crashes
- Core flows validated
- Policy sections complete
- Support and rollback plan ready

Then promote the tested release to production rollout.

## Common Issues

### Data safety form incomplete

Complete all fields in Policy -> App content.

### targetSdk rejected

Ensure project `targetSdk` satisfies current Play policy.

### Signing issues

Use Google Play App Signing and verify upload key configuration.
