# BestSMS Remaining Feature Plan

## Goal

Make BestSMS a dependable daily-driver SMS/MMS application while staying within documented Android APIs and avoiding proprietary RCS reverse engineering.

## Prioritized implementation roadmap

| Priority | Feature | Rationale | Planned status |
|---|---|---|---|
| 1 | Custom scheduled date and time | The current preset scheduler is useful but limited; users need a precise future date and time. | Implement in this batch |
| 2 | Scheduled-message management | Show scheduled messages and allow cancellation before delivery. | Implement in this batch if the current architecture permits |
| 3 | Draft indicators and per-conversation drafts | Users should see which conversations contain unsent text and recover the correct draft. | Next high-value batch |
| 4 | Rich notifications and inline reply | Modern notifications should support direct reply, mark-as-read, and conversation-aware actions. | Next high-value batch |
| 5 | MMS media gallery and caching | Provider URIs can become unavailable; cached media and a gallery improve reliability. | Next media batch |
| 6 | Spam workflow | Add a visible spam folder, report/block actions, and conservative local filtering. | Next safety batch |
| 7 | Backup and restore | Export currently provides a text backup; structured restore of messages, groups, preferences, and drafts is needed for migration. | Next migration batch |
| 8 | Conversation selection tools | Add multi-select delete/archive/share, contact details, and bulk actions. | Planned |
| 9 | Accessibility and privacy polish | Improve TalkBack labels, dynamic color, large text, biometric lock, and privacy controls. | Planned |
| 10 | RCS-compatible experience | Improve transport indicators and capability display using documented/device-exposed data; do not impersonate carrier RCS or private Google/Samsung providers. | Ongoing constraint |

## Scope for this batch

The implementation will add a custom date-and-time scheduler using Android’s date and time picker dialogs, validate that the selected time is in the future, and schedule delivery through the existing alarm receiver. It will also improve the scheduler’s user feedback and preserve drafts when scheduling is canceled or invalid.

The batch will include only additional improvements that can be implemented without changing the app’s SMS/MMS transport contract or introducing fragile proprietary dependencies. Every completed change will be compiled, checked, committed, and pushed to GitHub.
