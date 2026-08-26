# BestSMS

BestSMS is an Android SMS/MMS application focused on reliable messaging, Samsung/Android Contacts group integration, privacy-aware media sharing, search, scheduling, and a clean contributor-friendly foundation.

> **Current transport boundary:** BestSMS uses documented Android SMS/MMS and Contacts Provider APIs. It does not impersonate or reverse-engineer private Google Messages or Samsung Messages RCS services.

## Features

| Area | Current capability |
|---|---|
| Messaging | Read and send SMS through the Android telephony provider and default-SMS-app role. |
| MMS | Send image/video/audio/document attachments, read standard MMS parts, render received image/video media, and report attachment failures. |
| Contact groups | Mirror Samsung/Android Contacts groups, merge duplicate group names, and filter conversations by group. |
| Search | Search message text across all conversations or within one conversation. |
| Inbox organization | Pin, mute, archive, block, show archived conversations, and display pinned/muted indicators. |
| Reliability | Draft recovery, failed-message retry, incoming-message refresh, notifications, and scheduled SMS. |
| Privacy | Ask before sending images/videos whether metadata should be stripped; rewrite images and supported video containers before sending. |
| Backup | Export a portable text backup through Android’s document picker. |
| Calling | Call the active conversation contact directly when permission is available, with a dialer fallback. |

## Project layout

```text
app/src/main/java/com/example/smsapp/
├── MainActivity.kt                 # Activity lifecycle, permissions, Compose entry point
├── model/                          # Immutable UI and domain models
├── receiver/                       # SMS, MMS, and scheduled-message broadcast receivers
├── ui/components/                  # Reusable Compose components
├── ui/screens/                     # Conversation list and conversation screens
├── ui/theme/                       # Material theme and typography
├── util/                           # Android provider, MMS, PDU, and media helpers
└── viewmodel/                      # State, filtering, persistence, and user actions
```

### Data flow

```text
Android SMS/MMS providers
        │
        ▼
SmsUtils / MediaSanitizer
        │
        ▼
SmsViewModel ───── SharedPreferences for lightweight app preferences and drafts
        │
        ▼
Compose screens and reusable components
```

`SmsUtils` is the boundary for Android telephony and Contacts Provider access. `SmsViewModel` owns screen state and user actions. Compose screens should call ViewModel methods rather than querying providers directly. New provider integrations should be isolated in `util/` and mapped into the existing models.

## Build requirements

Use Android Studio or a machine with Java 21, Android SDK Platform 35, and the Android Gradle Plugin version declared by the project. From the repository root:

```bash
./gradlew assembleDebug
```

On Windows:

```bat
gradlew.bat assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Running and testing

BestSMS is designed to operate as the device’s default SMS application. On a test device, install the debug APK, select BestSMS as the default SMS app when prompted, grant SMS and Contacts permissions, and grant notification and call permissions when requested. Test SMS and MMS behavior on a real carrier-connected device because emulator and carrier configurations do not reproduce every MMS condition.

Important manual tests include receiving and sending SMS, opening a conversation to mark messages read, selecting an image or video and choosing both metadata options, scheduling a message for a future date and time, retrying a failed send, exporting a backup, pinning and muting a conversation, and using the call button in an active conversation.

## Contributing

Keep provider access, transport code, state management, and UI responsibilities separated. Add new behavior through a small ViewModel method and a focused utility or receiver rather than embedding provider queries in a Composable. Prefer immutable data classes, explicit failure handling, and documented Android API limitations. Run `git diff --check` and `./gradlew assembleDebug` before opening a pull request.

For a detailed guide, see [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) and [`docs/CONTRIBUTING.md`](docs/CONTRIBUTING.md).

## Roadmap: full RCS in the future

The long-term goal is to provide a modern Google-Messages-like experience with richer delivery states, typing indicators, reactions, group chat, media transfer, and end-to-end encryption where the transport supports it. A true universal RCS implementation requires carrier provisioning, IMS registration, capability discovery, chat sessions, file transfer, encryption, delivery/read receipts, and interoperability with the carrier’s RCS network.

BestSMS will pursue this in stages:

1. Improve device-aware capability and transport indicators without falsely labeling SMS or MMS as RCS.
2. Use documented Android and carrier APIs where they become available.
3. Add a transport abstraction so SMS, MMS, and a future legitimate RCS provider share the same conversation UI.
4. Add conformance tests for message states, media, group chat, and fallback from RCS to SMS/MMS.
5. Only enable native RCS sending when a documented, authorized, and interoperable API is available for the target device/carrier.

Private provider reverse engineering, credential bypass, or impersonation of Google/Samsung RCS services is intentionally out of scope because it would be fragile, unsafe, and unlikely to interoperate reliably.

## License and project status

This repository is an actively developed personal/open-source project. Contributors should discuss major transport, privacy, and storage changes before implementation. Avoid committing signing keys, `local.properties`, API tokens, carrier credentials, or private test data.
