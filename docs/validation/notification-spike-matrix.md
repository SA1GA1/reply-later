# Notification spike validation

Validated on 12 September 2026 using a physical realme RMX3938 running Android 15 over wireless debugging.

## Completed checks

| Check | Result | Evidence |
|---|---|---|
| Debug app installation and launch | PASS | `app.replylater.android` installed and opened on the physical device |
| Notification-listener access | PASS | Android secure settings list `ReplyLaterNotificationListener` as enabled |
| Canonical WhatsApp direct-message `MessagingStyle` | PASS | On-device test passed through Android notification serialization, `NotificationNormalizer`, and `SupportedNotificationParser` |
| Canonical WhatsApp group `MessagingStyle` | PASS | On-device test produced `GROUP_CHAT` rejection |
| Local Telegram and WhatsApp structural fixtures | PASS | Covered by the JVM parser test suite |

The on-device report recorded 2 tests, 0 failures, 0 errors, and 0 skipped tests. Fixtures use only `CONTACT_A`, `CONTACT_B`, `MESSAGE_1`, and `MESSAGE_2`.

## Deferred live-payload checks

No incoming WhatsApp or Telegram message was available during this session. Therefore exact extras emitted by the currently installed messenger versions were not observed, and no claim of version-specific compatibility is made.

Before a public release, passively validate ordinary direct messages, repeated messages, group notifications, bundled summaries, and hidden previews from each supported messenger. Also validate dismissal, process death, lock-screen delivery, and listener disable/enable behavior. Record only structural field presence and synthetic replacements—never real contact names or message text.
