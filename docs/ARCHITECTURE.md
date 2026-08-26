# BestSMS Architecture

## Guiding principles

BestSMS separates platform integration from presentation. Android provider queries, SMS/MMS transport, media preparation, and contact lookups belong in utilities or receivers. The ViewModel converts those results into observable state. Compose screens render state and send user intent back to the ViewModel.

## Components

### `MainActivity`

`MainActivity` owns the application entry point, default-SMS role flow, runtime permission flow, lifecycle refresh, and the top-level screen switch. It should not contain provider queries or message business logic.

### Models

The `model` package contains data classes used across layers. `SmsMessage` represents an SMS, MMS, or device-exposed RCS-like message. `Conversation` represents an inbox row. Models should remain immutable and should use explicit enums for states rather than magic strings.

### `SmsUtils`

`SmsUtils` is the Android telephony and Contacts Provider boundary. It handles message retrieval, contact groups, phone matching, SMS/MMS transport, notification helpers, and read-state updates. When adding provider-specific behavior, isolate it behind a function with a standard-provider fallback.

### `MediaSanitizer` and `PduUtils`

`MediaSanitizer` prepares user-selected media for privacy-sensitive sending. `PduUtils` constructs the current MMS payload. Future work should replace ad-hoc PDU assembly with a tested transport abstraction when carrier-specific MMS behavior requires it.

### Receivers

Receivers are small event adapters. `SmsReceiver` handles incoming SMS broadcasts, `MmsReceiver` handles WAP push refresh events, and `ScheduledSmsReceiver` delivers scheduled SMS. Receivers should validate extras, delegate work, and return quickly.

### `SmsViewModel`

The ViewModel owns screen state, filtering, drafts, conversation preferences, scheduled-message actions, and calls into utilities. Long-running provider or media work should run on `Dispatchers.IO`. Public methods should describe user intent, such as `scheduleMessageAt`, `retryMessage`, `toggleMuted`, or `stripAttachmentMetadata`.

### Compose UI

`ui/screens` contains route-level screens. `ui/components` contains reusable rows, message cards, group tabs, and dialogs. Composables should avoid direct provider access and should expose callbacks for user actions. Keep accessibility content descriptions on icon-only controls.

## Adding a feature

1. Define the user-visible state and update the relevant immutable model if necessary.
2. Add platform/provider code to `util/` or a dedicated receiver/service.
3. Add a small ViewModel method that owns validation, threading, and state transitions.
4. Render the state in a screen or reusable component.
5. Add manual-test steps and document any Android/carrier limitation.
6. Run `git diff --check` and `./gradlew assembleDebug`.

## Transport and status model

SMS exposes local provider state and optional send/delivery callbacks. MMS exposes provider parts and carrier-dependent send results. Remote read receipts and typing indicators are not universal SMS features. A future RCS transport should implement a transport interface rather than adding more provider-specific checks throughout the UI.

A future interface could look like:

```kotlin
interface MessagingTransport {
    fun capabilities(address: String): TransportCapabilities
    suspend fun send(message: OutgoingMessage): SendResult
    fun observeStatus(messageId: String): Flow<RemoteMessageStatus>
}
```

The existing SMS/MMS path should remain the fallback transport when RCS is unavailable.
