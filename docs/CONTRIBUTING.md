# Contributing to BestSMS

## Before coding

Read the root README and `docs/ARCHITECTURE.md`. Confirm that the change belongs in the Android app and identify the smallest layer that should own it. Do not add secrets or real phone numbers to the repository.

## Coding conventions

Use Kotlin and Compose conventions already present in the project. Prefer descriptive method names, immutable data classes, explicit state enums, and early validation. Keep provider access out of Composables. Use `viewModelScope` for asynchronous work initiated by the UI and `Dispatchers.IO` for provider, file, or media operations.

Every permission-sensitive or carrier-sensitive behavior must have a graceful failure path. For example, a call action should fall back to the dialer when direct-call permission is unavailable, and an unsupported RCS capability should not be presented as a confirmed RCS session.

## Testing checklist

Before submitting a change, run:

```bash
./gradlew assembleDebug
git diff --check
```

On a test phone, verify the changed flow with SMS permissions, Contacts permissions, and default-SMS role configured. For media changes, test image and video attachments with both metadata choices. For provider changes, test empty-provider, denied-permission, and carrier-failure cases.

## Commit and pull requests

Use focused commits with imperative messages, such as `Add direct call action to conversation toolbar`. Explain the user-visible behavior, the files or layers changed, and any platform limitation. Include manual test steps and a screenshot or APK when appropriate.

## Privacy and security

Never commit API keys, carrier credentials, signing keys, personal backups, raw MMS files, or logs containing phone numbers. Treat external provider data as untrusted input. Validate content URIs and use scoped storage/FileProvider patterns for attachments.

## RCS contributions

RCS-related contributions must identify the documented API, authorization model, carrier/device scope, and fallback behavior. Do not copy private provider schemas, bypass authentication, or claim that SMS/MMS is RCS. The target is a transport abstraction that can accept a legitimate RCS implementation in the future without destabilizing SMS/MMS.
