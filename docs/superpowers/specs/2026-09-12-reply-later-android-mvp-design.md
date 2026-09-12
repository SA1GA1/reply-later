# Reply Later — Android MVP Design

**Status:** Approved for MVP implementation
**Date:** 2026-09-12  
**Platform:** Android 8.0+ (API 26+)  
**Language/UI:** Kotlin, Jetpack Compose, Material 3

## 1. Product goal

Reply Later is a private, local-first inbox for messages that the user intentionally chooses to answer later.

Core flow:

1. A direct message arrives in Telegram or WhatsApp.
2. Reply Later reads the system notification and posts its own quiet companion notification.
3. The user explicitly taps **Reply later**.
4. The user selects **30 minutes**, **60 minutes**, or **More** to choose a date and time.
5. Reply Later saves the reminder locally and notifies the user at the chosen time.
6. The user opens the original conversation and later marks the item as answered.

The app never saves a message automatically. Group chats are excluded from the MVP.

## 2. MVP scope

### Included

- Telegram (`org.telegram.messenger`) direct-message notifications.
- WhatsApp (`com.whatsapp`) direct-message notifications.
- Explicit saving through a Reply Later companion notification.
- Reminder presets: 30 minutes, 60 minutes, and a custom date/time.
- Local inbox grouped chronologically into Overdue, Today, Tomorrow, and later dates.
- Sticky date-section headers while scrolling.
- Date picker defaulting to Today.
- Messenger filters: All, Telegram, WhatsApp.
- Small messenger badge on the contact avatar; no redundant messenger name in the row.
- Reminder detail screen with opening of the source conversation when Android and the messenger permit it.
- Manual **Mark as answered** action.
- Completed-items archive accessible from Settings.
- Light white/gray visual system with restrained red accents, translucency, layering, and soft shadows.
- Local storage only.
- Runtime permission and special-access onboarding with useful degraded modes after refusal.

### Excluded

- Group chats, channels, bots, broadcast lists, and community notifications.
- Telegram X, WhatsApp Business, and unofficial messenger clients.
- Automatic inference that the user has replied.
- Account creation, cloud synchronization, and multi-device support.
- AI classification or suggested answers.
- Importing notification history that appeared before notification access was granted.
- Recurring overdue reminders and the privacy mode that omits message text; both remain post-MVP features.

## 3. Product rules

### Explicit user action

Notification access allows Reply Later to observe supported notifications, but observation alone must not create an inbox item. A database record is created only after the user selects a reminder time in the companion notification or custom-time screen.

### Direct chats only

The notification parser accepts an event only when it can classify it as a direct human conversation with sufficient confidence. Known group/channel indicators cause the event to be ignored. Ambiguous notifications are ignored rather than incorrectly captured.

Because Telegram and WhatsApp do not expose a stable public cross-app schema for chat type, this classifier must be validated against real devices and current messenger versions during a technical spike. Parser decisions should be isolated behind a testable interface so messenger-specific rules can be updated without changing the rest of the application.

### Privacy

- No network permission is required by the MVP.
- Data remains on-device.
- Message text is encrypted at rest using AES-GCM; the encryption key is generated and retained in Android Keystore.
- Notification contents must not be written to logs or analytics.
- System backups for the encrypted database are disabled for the MVP to avoid restoring data without its device-bound key.
- Deleting an item removes its locally stored content.

The data model permits nullable message text now so the post-MVP “do not store text” mode does not require a migration of the core domain model.

## 4. Onboarding and permissions

Onboarding asks for required access early, in this order:

1. Product introduction and privacy explanation.
2. **Notification access**: open Android's notification-listener settings and verify access when the user returns.
3. **Post notifications** on Android 13+: request the runtime permission so Reply Later can display companion and reminder notifications.
4. **Exact alarms and reminders** where the OS requires special access: open the system access screen and re-check on return.

Refusal behavior:

- Without notification-listener access, the inbox remains available but capture is disabled; the app shows a persistent setup card with a system-settings shortcut.
- Without notification-posting permission, capture cannot provide the intended notification workflow; the app shows a setup card and allows retrying the request/settings flow.
- Without exact-alarm access, the app remains usable and schedules inexact reminders. The UI explains that delivery may be delayed by Android battery optimizations.
- The app must never loop permission dialogs or block access to Settings after refusal.

Onboarding is considered complete after every access screen has been presented once, regardless of acceptance. Current access status remains visible in Settings.

## 5. Notification behavior

### Companion notification

For a supported direct-message notification, Reply Later posts a quiet companion notification in a dedicated low-importance channel. It should appear in the notification shade without producing a second sound or vibration.

Collapsed actions:

- **Reply later**

After selecting Reply later, the notification exposes:

- **30 min**
- **60 min**
- **More**

`More` opens a compact custom date/time flow in the app. Android notification layouts and action visibility vary by device, so the implementation uses standard notification actions rather than a custom remote view.

Companion notifications are keyed by messenger plus conversation identity. A newer message in the same conversation updates the existing companion instead of creating an unlimited stack. It is automatically removed when the source notification disappears, unless the user has already explicitly saved it.

### Scheduled reminder

At the chosen time, Reply Later posts a high-importance reminder notification containing contact, message preview when available, and actions:

- **Open chat**
- **Mark answered**
- **Later**

`Later` exposes the same 30/60/More choices and updates the existing item rather than creating a duplicate. In the MVP, an overdue item does not repeatedly notify by itself.

### Opening the chat

When observing the original notification, the app keeps only the information needed to invoke its content `PendingIntent` while it remains valid. On **Open chat**, it attempts that intent first because it offers the best chance of landing in the exact conversation.

If the source notification or its intent is no longer available, the app falls back to launching the messenger. The UI must say **Open Telegram/WhatsApp** rather than promise exact deep-link behavior when it cannot be guaranteed.

## 6. Screens and interaction

### First launch

- Original understated translucent red app mark.
- Short value proposition.
- Privacy statement: messages are processed and stored locally.
- Primary action to begin access setup.

### Notification access

- Neutral messenger marks without decorative red tiles.
- Plain-language explanation of what is read and when content is saved.
- Access-status rows for notification access, app notifications, and exact reminders.
- Primary action opens the relevant Android settings screen.
- The flow continues after either acceptance or refusal.

### Inbox

- Top date control defaults to Today and can select another date.
- Messenger filter chips: All, Telegram, WhatsApp.
- Overdue items are visually consistent with all other white cards; only the “Overdue” label and small status indicator use red.
- Sections appear in chronological order: Overdue, Today, Tomorrow, then explicit calendar dates.
- The active section header is sticky and is pushed away by the next section header.
- Selecting a future date scrolls/filters to that date. Overdue content is hidden, while a compact red overdue-count chip remains available to return to it.
- Each card shows contact avatar, small overlaid messenger badge, contact name, message preview, reminder time, and state.
- Bottom navigation contains only Inbox and Settings.

### Reminder detail

- Contact, messenger badge, message text if available, received time, and reminder time.
- Primary action: Open chat.
- Secondary actions: Change reminder, Mark answered, Delete.
- No automatic completion after opening the messenger.

### Settings

- Access status and shortcuts to system settings.
- Default custom reminder preferences, including the meaning of “evening” if later added.
- Completed items entry with count and archive list.
- Privacy/data section with delete-all-data action and confirmation.
- About/version information.

## 7. Architecture

Use a single Android application module initially, organized by feature and clear dependency boundaries:

```text
UI (Compose)
  -> ViewModels / use cases
    -> repositories
      -> Room database
      -> notification capture and parser adapters
      -> alarm scheduler
      -> Android settings/permission gateways
```

Core Android components:

- `NotificationListenerService` for observing active Telegram and WhatsApp notifications.
- `Room` for reminders and minimal source metadata.
- `AlarmManager` for delivery; exact when authorized, inexact otherwise.
- `BroadcastReceiver` for alarm delivery and notification actions.
- `BOOT_COMPLETED` receiver to restore future alarms after reboot.
- `WorkManager` only for non-time-critical maintenance if later required; it is not the primary reminder scheduler.
- Android Keystore for the encryption key.

Dependency injection can use Hilt. Coroutines and Flow provide asynchronous state and reactive inbox updates.

## 8. Domain model

Primary reminder fields:

```text
id: UUID
sourcePackage: String
conversationKey: String (one-way hashed where possible)
contactDisplayName: String
messageTextEncrypted: ByteArray?
receivedAt: Instant
remindAt: Instant
status: PENDING | OVERDUE | ANSWERED
answeredAt: Instant?
createdAt: Instant
updatedAt: Instant
sourceNotificationKey: String?
```

Transient `PendingIntent` references are not persisted in Room and may become invalid at any time. The app therefore treats exact-chat opening as a best-effort capability.

Time is stored as UTC instants and rendered in the device's current time zone. After a time-zone or clock change, pending alarms are recalculated from the stored target instants.

## 9. State and scheduling

- `PENDING`: reminder time is in the future.
- `OVERDUE`: reminder time has passed and the item is not answered.
- `ANSWERED`: explicitly completed by the user.

Overdue can be derived from `remindAt < now` plus pending status rather than requiring a fragile background database update at the exact boundary. Scheduling operations are idempotent and keyed by reminder ID.

On app start, reboot, package replacement, exact-alarm access change, or time-zone change, the scheduler reconciles all unfinished future reminders. If exact access is revoked, future exact alarms are replaced with inexact equivalents where Android allows.

## 10. Failure and edge cases

- Hidden lock-screen content: show only the metadata Android exposes; never fabricate a contact or message.
- Bundled messenger notifications: parse individual messaging-style entries where available; otherwise ignore ambiguous groupings.
- Duplicate updates: deduplicate by source package, conversation identity, message timestamp, and normalized content fingerprint.
- Notification listener disconnected: expose the state in Settings and reconnect through documented Android lifecycle behavior.
- Reminder permission revoked: retain saved items and visibly report that notifications are disabled.
- Exact alarm refused or revoked: automatically use inexact scheduling.
- Source app uninstalled: keep the saved item but disable Open chat and explain why.
- App process death/reboot: restore all persistable reminder state and alarms.

## 11. Validation strategy

Before completing the full UI, run a focused device spike against current Telegram and WhatsApp versions:

- Direct chat with visible preview.
- Preview hidden by messenger or lock-screen settings.
- Several messages from one person.
- Simultaneous messages from different people.
- Group/channel messages to confirm they are rejected.
- Source notification dismissed before and after saving.
- Device locked, app process killed, and device rebooted.
- Exact-alarm access accepted, refused, and later revoked.
- Open-chat behavior while the source `PendingIntent` is valid and after it expires.

Automated coverage:

- Unit tests for Telegram/WhatsApp parsing fixtures and group rejection.
- Unit tests for deduplication, scheduling choice, date sections, and status derivation.
- Room migration and repository tests.
- Compose tests for permission states, filters, sticky headers, future-date selection, and completion.
- Instrumented tests for receivers and notification actions where the Android framework permits reliable assertions.

## 12. Acceptance criteria

The MVP is ready when:

1. A supported direct Telegram or WhatsApp notification results in one quiet Reply Later companion notification.
2. No reminder is saved without an explicit user time selection.
3. Group/channel notifications are not presented for capture in validated fixtures and device scenarios.
4. 30-minute, 60-minute, and custom reminders survive process death and reboot.
5. Refusing exact-alarm access leaves the app functional with an explained inexact fallback.
6. Inbox filtering, chronological sections, sticky headers, and date navigation behave as specified.
7. Opening the source chat is attempted when possible and falls back honestly when not.
8. Only an explicit user action marks an item answered.
9. Stored message text is encrypted and notification content does not appear in logs.
10. The approved light/translucent visual direction is consistently represented across onboarding, inbox, detail, settings, and notifications.

## 13. Delivery sequence

1. Project foundation and design tokens.
2. Notification parsing spike on physical devices.
3. Encrypted persistence and reminder domain logic.
4. Permission onboarding and degraded states.
5. Companion notification actions and scheduling.
6. Inbox, date navigation, filters, and sticky headers.
7. Detail, open-chat fallback, completion, and Settings archive.
8. End-to-end device validation and release hardening.

## References

- [Notification listener service](https://developer.android.com/reference/android/service/notification/NotificationListenerService)
- [Notification runtime permission](https://developer.android.com/develop/ui/views/notifications/notification-permission)
- [Schedule alarms](https://developer.android.com/develop/background-work/services/alarms)
