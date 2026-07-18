# Runbook: Google Drive Google Cloud Console setup

## Document control

| Field | Value |
| --- | --- |
| Capability | User-owned Google Drive backup |
| Specs | 0204, 0301–0307 |
| Provider | Google Drive API v3 |
| Authorization | Google Identity Services `AuthorizationClient` |
| Scope | `https://www.googleapis.com/auth/drive.appdata` |
| Petit account required | No |
| Firebase required | No |

## Objective

Create the Google Cloud configuration required for Petit to request
`drive.appdata` authorization and store user-owned backup archives in the
Google Drive application data folder.

This runbook configures external infrastructure only. It does not implement the
Android integration and does not authorize implementation while the related
specs remain in `Draft`.

## Expected result

At completion:

- one Google Cloud project is owned by the Petit maintainers;
- Google Drive API is enabled;
- Google Auth Platform branding, audience, and data access are configured;
- the only Drive scope requested by the backup capability is `drive.appdata`;
- every installed Android signing identity has a matching OAuth client;
- test users can complete consent in testing mode;
- no client secret, service account, API key, Firebase project, or
  `google-services.json` is required for device-only Drive backup.

## Responsibilities

- **Cloud owner:** creates the project, configures consent, and controls access.
- **Release owner:** supplies the release and Google Play App Signing certificate fingerprints.
- **Developer:** supplies the debug application ID and certificate fingerprint.
- **Privacy owner:** supplies public support, homepage, and privacy-policy URLs before production publication.

One person may perform multiple roles, but do not grant broad project access
only to complete this runbook.

## Preconditions

- A Google account or Google Workspace organization that will own Petit's cloud project.
- Access to [Google Cloud Console](https://console.cloud.google.com/).
- Final application IDs:
  - debug: `com.woliveiras.petit.debug`;
  - release: `com.woliveiras.petit`.
- Access to the local debug signing certificate.
- Access to the release/upload certificate used for directly installed builds.
- Access to Google Play Console if Play App Signing is or will be enabled.
- Public app homepage, privacy policy, and support contact before publishing OAuth externally.

## Security rules

- Never commit OAuth client secrets, access tokens, refresh tokens, keystores, or exported Cloud credentials.
- Do not create a service account for user-owned Drive backup.
- Do not create an API key for Drive authorization.
- Do not request `drive`, `drive.file`, or another broader Drive scope.
- Do not create a web OAuth client unless a separately approved backend needs offline access.
- Do not add Firebase merely to obtain Google Drive authorization.
- Grant the minimum Google Cloud IAM role required for each maintainer.

## Step 1: Collect Android signing identities

From the repository root, run:

```bash
./gradlew signingReport
```

Record the SHA-1 fingerprint for each installable identity. SHA-256 may also be
recorded for release management, but the Android OAuth client requires the
matching package name and SHA-1 certificate fingerprint.

Prepare this matrix before creating clients:

| Environment | Application ID | Certificate source | SHA-1 |
| --- | --- | --- | --- |
| Local debug | `com.woliveiras.petit.debug` | Local debug keystore | `<sha1>` |
| Direct release | `com.woliveiras.petit` | Release/upload signing certificate | `<sha1>` |
| Google Play release | `com.woliveiras.petit` | Play App Signing certificate | `<sha1>` |

If direct release and Play App Signing use different certificates, they need
separate Android OAuth clients even though the package name is the same.

Do not copy keystore passwords or private keys into the evidence for this runbook.

## Step 2: Create or select the Google Cloud project

1. Open [Google Cloud Console](https://console.cloud.google.com/).
2. Create a project dedicated to Petit, or select the approved existing project.
3. Use an organization and billing account only if required by the owner's governance policy.
4. Record the project name, project ID, project number, and owning organization.
5. Review IAM and remove temporary or unnecessary principals.

Recommended project evidence:

```text
Project name:
Project ID:
Project number:
Owning organization/account:
Cloud owners:
```

Project IDs are not secrets, but avoid publishing internal organization details unnecessarily.

## Step 3: Enable Google Drive API

1. Open **APIs & Services > Library**.
2. Search for **Google Drive API**.
3. Open the Google Drive API entry.
4. Select **Enable**.
5. Open **APIs & Services > Enabled APIs & services**.
6. Confirm that Google Drive API is enabled for the selected project.

Do not enable Firebase Storage, Firestore, or unrelated Google APIs for this capability.

Official reference: [Google Drive API overview](https://developers.google.com/workspace/drive/api/guides/about-sdk).

## Step 4: Configure Google Auth Platform branding

Open **Google Auth Platform > Branding** and configure:

- app name: `Petit`;
- user support email;
- app logo approved for production use;
- app homepage URL;
- privacy policy URL;
- terms of service URL, if available;
- authorized domains for every public URL;
- developer contact email addresses.

Verify that the consent copy describes access as Google Drive backup, not Petit
Cloud login, subscription, synchronization, or broad Drive access.

Do not claim that Petit can see other Drive files. The `appDataFolder` is hidden
and accessible only to the app that created its contents.

## Step 5: Configure the audience

Open **Google Auth Platform > Audience**.

For development:

1. Select the appropriate user type for the owning organization.
2. Keep the app in testing status.
3. Add every Google account that will execute provider integration tests as a test user.
4. Record who owns test-user maintenance.

For production:

1. Confirm that branding and public policy URLs are final.
2. Review Google API Services User Data Policy compliance.
3. Publish the app for the intended external or internal audience.
4. Complete any review requested by Google before release.

The `drive.appdata` scope is currently classified by Google as non-sensitive,
but the project must still present accurate branding and data-use information.

Official references:

- [Google authorization production readiness](https://developers.google.com/identity/protocols/oauth2/production-readiness/policy-compliance)
- [Google API Services User Data Policy](https://developers.google.com/terms/api-services-user-data-policy)

## Step 6: Configure data access

Open **Google Auth Platform > Data Access**.

1. Add this exact scope:

   ```text
   https://www.googleapis.com/auth/drive.appdata
   ```

2. Confirm that no broader Drive scope is configured for the backup capability.
3. Save the configuration.
4. Record the scope classification displayed by the console.

The Android app must request this scope only when the user chooses a Google
Drive capability. It must not request Drive access during first launch or local-only use.

Official reference: [Store application-specific data](https://developers.google.com/workspace/drive/api/guides/appdata).

## Step 7: Create Android OAuth clients

Open **Google Auth Platform > Clients** or **APIs & Services > Credentials**, depending on the current console navigation.

For each row in the signing matrix:

1. Select **Create client**.
2. Select application type **Android**.
3. Enter a descriptive name, such as:
   - `Petit Android debug`;
   - `Petit Android direct release`;
   - `Petit Android Play release`.
4. Enter the exact package name.
5. Enter the matching SHA-1 fingerprint.
6. Create the client.
7. Record the generated Android OAuth client ID.

Required minimum during local development:

```text
Petit Android debug
Package: com.woliveiras.petit.debug
SHA-1: <local debug SHA-1>
```

Required before production:

```text
Petit Android Play release
Package: com.woliveiras.petit
SHA-1: <Play App Signing SHA-1>
```

If testers install a release APK signed outside Google Play, also create the
direct-release client for that certificate.

Do not download or commit a client-secret JSON file. Android OAuth clients do
not require a client secret in the app.

## Step 8: Configure quota visibility and alerts

1. Open **APIs & Services > Google Drive API > Quotas & System Limits**.
2. Review per-project and per-user request quotas.
3. Configure monitoring or alerts appropriate for the expected beta audience.
4. Record the operational contact responsible for quota incidents.

Petit does not impose a backup-count or retention limit. Google Drive storage
and API quotas still apply and must be reported accurately to the user.

Official references:

- [Google Drive API usage limits](https://developers.google.com/workspace/drive/api/guides/limits)
- [Google Drive API error handling](https://developers.google.com/workspace/drive/api/guides/handle-errors)

## Step 9: Review the final project configuration

Confirm all of the following:

- [ ] The intended Google Cloud project is selected.
- [ ] Google Drive API is enabled.
- [ ] Branding identifies Petit accurately.
- [ ] Homepage and privacy-policy URLs are public and use authorized domains.
- [ ] The audience and test users are correct.
- [ ] `drive.appdata` is the only Drive scope used by backup.
- [ ] A debug Android OAuth client matches `com.woliveiras.petit.debug` and its SHA-1.
- [ ] Production Android OAuth clients match every distribution certificate.
- [ ] The Play client uses the Play App Signing certificate, not only the upload certificate.
- [ ] No API key, service account, web client secret, Firebase dependency, or `google-services.json` was added for Drive backup.
- [ ] No credential or private signing material was added to Git.

## Step 10: Hand off non-secret configuration

Record only the non-secret values needed by the implementation and release owners:

```text
Google Cloud project ID:
Google Cloud project number:
Drive API enabled: yes / no
OAuth publication status: testing / production
Configured scope: https://www.googleapis.com/auth/drive.appdata

Debug Android OAuth client ID:
Debug package / SHA-1:

Direct-release Android OAuth client ID, if applicable:
Direct-release package / SHA-1:

Play Android OAuth client ID:
Play package / App Signing SHA-1:

Test users:
Privacy policy URL:
Support email:
Quota/incident owner:
Configuration date:
Executor:
Reviewer:
```

Store operational evidence outside the repository if it contains account names,
organization details, or internal screenshots.

## Troubleshooting

### Authorization returns a developer configuration error

Check:

- the installed build's actual application ID;
- the certificate that signed the installed APK;
- the matching SHA-1 in the Android OAuth client;
- that the client belongs to the same project where Drive API is enabled;
- that the account is an allowed test user while the OAuth app is in testing.

Debug and release builds commonly fail differently because they use different
application IDs or signing certificates.

### Consent requests an unexpectedly broad Drive permission

Stop testing and inspect both Cloud Data Access and the Android authorization
request. The backup flow must request only `drive.appdata`.

### Authorization succeeds but appDataFolder calls fail

Check:

- Google Drive API is enabled in the correct project;
- the access token contains `drive.appdata`;
- requests use `spaces=appDataFolder` when listing;
- uploads set `appDataFolder` as the parent;
- the user's Drive storage quota is not exhausted.

### Automatic backup later requires interaction

This is not a Cloud Console error by itself. Google grants can be revoked or
require renewed consent. Background work must record `Authorization required`
and direct the user to a foreground reconnect action; it must not store a
long-lived refresh token on the device.

Official reference: [Authorize access to Google user data on Android](https://developer.android.com/identity/authorization).

## Completion record

```text
Date/time:
Executor:
Reviewer:
Project ID:
Drive API enabled: Pass / Fail
Branding complete: Pass / Fail / Blocked
Audience/test users configured: Pass / Fail / Blocked
drive.appdata scope configured: Pass / Fail
Debug Android client configured: Pass / Fail
Direct-release Android client configured: Pass / Not applicable / Fail
Play Android client configured: Pass / Not applicable / Fail
Quota owner recorded: Pass / Fail
No secrets committed: Pass / Fail
Overall result: Pass / Fail / Blocked
Evidence location:
Notes:
```
