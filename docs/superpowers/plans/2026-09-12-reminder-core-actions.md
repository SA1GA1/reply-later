# Reminder Core and Notification Actions Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the private local reminder engine that turns an explicitly selected companion-notification action into an encrypted, scheduled Reply Later item that survives process death and reboot.

**Architecture:** The existing listener remains a read-only classifier until it publishes a transient companion notification. A notification-action receiver decodes an OS-held synthetic payload, creates a reminder through a repository, and asks an idempotent AlarmManager scheduler to deliver it. Room stores metadata and AES-GCM ciphertext; Android Keystore retains the non-exportable key. A small application graph wires concrete Android implementations without adding a dependency-injection framework.

**Tech Stack:** Kotlin 2.3.21, Android Gradle Plugin 9.4.0, Room 2.8.5, KSP 2.3.12, Coroutines 1.10.2, Android Keystore AES-GCM, AlarmManager, NotificationCompat, JUnit 4, AndroidX Test 1.7.0.

**Spec:** `docs/superpowers/specs/2026-09-12-reply-later-android-mvp-design.md`

## Global Constraints

- Application ID and namespace remain `app.replylater.android`.
- Work and focused commits are published directly to `origin/main` as explicitly requested by the user.
- Minimum SDK remains API 26; target SDK remains API 36.
- Only direct messages from `org.telegram.messenger` and `com.whatsapp` may produce companion notifications.
- Observing a source notification must never create a database row.
- A reminder is created only after the user selects 30 minutes, 60 minutes, or confirms a custom date/time.
- Message text is nullable and encrypted with AES-GCM before Room receives it.
- The AES key is non-exportable and generated in Android Keystore; backup remains disabled.
- No message content is written to Logcat, analytics, validation documents, or exceptions.
- No `INTERNET` permission is declared.
- Exact alarms are used only when authorized; otherwise reminders use an inexact alarm and remain usable.
- Companion notifications use low importance with no sound or vibration; scheduled reminders use high importance.
- Group/channel rejection behavior and all existing tests must remain green.
- The untracked `design/` directory belongs to the user and is never staged.

---

## File map

```text
gradle/libs.versions.toml
build.gradle.kts
app/build.gradle.kts
                                             Room, KSP, coroutines, lifecycle dependencies
app/src/main/java/app/replylater/android/reminder/domain/Reminder.kt
app/src/main/java/app/replylater/android/reminder/domain/ReminderRepository.kt
app/src/main/java/app/replylater/android/reminder/domain/ReminderScheduler.kt
app/src/main/java/app/replylater/android/reminder/domain/CreateReminder.kt
                                             Android-free reminder contract and explicit-save use case
app/src/main/java/app/replylater/android/reminder/data/ReminderEntity.kt
app/src/main/java/app/replylater/android/reminder/data/ReminderDao.kt
app/src/main/java/app/replylater/android/reminder/data/ReplyLaterDatabase.kt
app/src/main/java/app/replylater/android/reminder/data/RoomReminderRepository.kt
                                             Room persistence and domain mapping
app/src/main/java/app/replylater/android/ReplyLaterApplication.kt
app/src/main/java/app/replylater/android/AppGraph.kt
                                             Process-wide concrete dependency graph
app/src/main/java/app/replylater/android/reminder/crypto/MessageCipher.kt
app/src/main/java/app/replylater/android/reminder/crypto/AndroidKeystoreMessageCipher.kt
                                             AES-GCM boundary and Android implementation
app/src/main/java/app/replylater/android/reminder/schedule/AlarmCapability.kt
app/src/main/java/app/replylater/android/reminder/schedule/AlarmPlan.kt
app/src/main/java/app/replylater/android/reminder/schedule/AlarmPlanSelector.kt
app/src/main/java/app/replylater/android/reminder/schedule/AlarmReminderScheduler.kt
app/src/main/java/app/replylater/android/reminder/schedule/ReminderReconciler.kt
app/src/main/java/app/replylater/android/reminder/schedule/ReminderAlarmReceiver.kt
app/src/main/java/app/replylater/android/reminder/schedule/ReminderReconcileReceiver.kt
                                             Alarm selection, delivery, and reconciliation
app/src/main/java/app/replylater/android/capture/companion/CapturePayload.kt
app/src/main/java/app/replylater/android/capture/companion/CapturePayloadCodec.kt
app/src/main/java/app/replylater/android/capture/companion/CaptureActionHandler.kt
app/src/main/java/app/replylater/android/capture/companion/CompanionNotificationPublisher.kt
app/src/main/java/app/replylater/android/capture/companion/CaptureActionReceiver.kt
app/src/main/java/app/replylater/android/capture/companion/SourceIntentRegistry.kt
                                             Explicit capture actions without pre-saving content
app/src/main/java/app/replylater/android/capture/framework/ReplyLaterNotificationListener.kt
                                             Publishes/cancels companion notifications
app/src/main/java/app/replylater/android/notification/NotificationChannels.kt
app/src/main/java/app/replylater/android/notification/ReminderNotificationPublisher.kt
app/src/main/java/app/replylater/android/notification/ReminderActionHandler.kt
app/src/main/java/app/replylater/android/notification/ReminderActionReceiver.kt
app/src/main/java/app/replylater/android/openchat/ChatOpener.kt
                                             Scheduled reminders and answered/later actions
app/src/main/java/app/replylater/android/permission/AccessStatus.kt
app/src/main/java/app/replylater/android/permission/AndroidAccessStatusReader.kt
                                             Listener, POST_NOTIFICATIONS, and exact-alarm status
app/src/main/AndroidManifest.xml
app/src/main/res/values/strings.xml
                                             Application, receivers, permissions, and user-facing copy
```

---

### Task 1: Reminder domain and explicit-save orchestration

**Files:**
- Create: `app/src/main/java/app/replylater/android/reminder/domain/Reminder.kt`
- Create: `app/src/main/java/app/replylater/android/reminder/domain/ReminderRepository.kt`
- Create: `app/src/main/java/app/replylater/android/reminder/domain/ReminderScheduler.kt`
- Create: `app/src/main/java/app/replylater/android/reminder/domain/CreateReminder.kt`
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Test: `app/src/test/java/app/replylater/android/reminder/domain/ReminderStatusTest.kt`
- Test: `app/src/test/java/app/replylater/android/reminder/domain/CreateReminderTest.kt`

**Interfaces:**
- Consumes: `DirectMessage` from the existing capture parser and an explicit target epoch millis.
- Produces: `Reminder`, `ReminderStatus`, `ReminderRepository`, `ReminderScheduler`, and `CreateReminder.invoke(message, remindAtEpochMillis)` for action receivers and later UI.

- [x] **Step 1: Add the coroutine test dependency and write failing status and creation tests**

Add `org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2` to unit tests, then cover these exact behaviors:

```kotlin
@Test fun futureUnansweredReminderIsPending() {
    assertEquals(ReminderStatus.PENDING, reminder(remindAt = 2_000L).statusAt(now = 1_000L))
}

@Test fun pastUnansweredReminderIsOverdue() {
    assertEquals(ReminderStatus.OVERDUE, reminder(remindAt = 999L).statusAt(now = 1_000L))
}

@Test fun answeredReminderStaysAnswered() {
    assertEquals(ReminderStatus.ANSWERED, reminder(remindAt = 999L, answeredAt = 500L).statusAt(1_000L))
}

@Test fun explicitCreationPersistsThenSchedules() = runTest {
    val id = createReminder(message, remindAtEpochMillis = 3_600_000L)
    assertEquals(listOf(id), repository.insertedIds)
    assertEquals(listOf(id), scheduler.scheduledIds)
}

@Test fun invalidTargetIsRejectedWithoutPersistence() = runTest {
    assertFailsWith<IllegalArgumentException> {
        createReminder(message, remindAtEpochMillis = 999L)
    }
    assertTrue(repository.insertedIds.isEmpty())
}
```

- [x] **Step 2: Run the focused tests and verify RED**

Run: `./gradlew testDebugUnitTest --tests '*ReminderStatusTest' --tests '*CreateReminderTest'`

Expected: Kotlin compilation fails because the reminder domain types do not exist.

- [x] **Step 3: Implement the minimal domain**

Define `Reminder` with string UUID, source package, hashed conversation key, contact name, nullable plaintext-at-domain-boundary message, received/remind/created/updated epoch millis, nullable answered time, and nullable source notification key. Define:

```kotlin
enum class ReminderStatus { PENDING, OVERDUE, ANSWERED }

fun Reminder.statusAt(now: Long): ReminderStatus = when {
    answeredAtEpochMillis != null -> ReminderStatus.ANSWERED
    remindAtEpochMillis <= now -> ReminderStatus.OVERDUE
    else -> ReminderStatus.PENDING
}

interface ReminderRepository {
    suspend fun upsert(reminder: Reminder)
    suspend fun get(id: String): Reminder?
    fun observeAll(): Flow<List<Reminder>>
    suspend fun unfinished(): List<Reminder>
    suspend fun markAnswered(id: String, answeredAtEpochMillis: Long)
    suspend fun reschedule(id: String, remindAtEpochMillis: Long, updatedAtEpochMillis: Long)
    suspend fun delete(id: String)
    suspend fun deleteAll()
}
```

`CreateReminder` receives repository, scheduler, clock lambda, and UUID lambda. It rejects targets not later than `now`, upserts first, schedules second, and returns the generated ID.

- [x] **Step 4: Run focused and full unit tests**

Run: `./gradlew testDebugUnitTest --tests '*ReminderStatusTest' --tests '*CreateReminderTest' && ./gradlew testDebugUnitTest`

Expected: all tests pass.

- [x] **Step 5: Commit and push**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts app/src/main/java/app/replylater/android/reminder/domain app/src/test/java/app/replylater/android/reminder/domain
git commit -m "feat: define reminder domain"
git push origin main
```

---

### Task 2: AES-GCM encrypted Room persistence

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `build.gradle.kts`
- Modify: `app/build.gradle.kts`
- Create: `app/src/main/java/app/replylater/android/reminder/crypto/MessageCipher.kt`
- Create: `app/src/main/java/app/replylater/android/reminder/crypto/AndroidKeystoreMessageCipher.kt`
- Create: `app/src/main/java/app/replylater/android/reminder/data/ReminderEntity.kt`
- Create: `app/src/main/java/app/replylater/android/reminder/data/ReminderDao.kt`
- Create: `app/src/main/java/app/replylater/android/reminder/data/ReplyLaterDatabase.kt`
- Create: `app/src/main/java/app/replylater/android/reminder/data/RoomReminderRepository.kt`
- Create: `app/src/main/java/app/replylater/android/ReplyLaterApplication.kt`
- Create: `app/src/main/java/app/replylater/android/AppGraph.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Test: `app/src/test/java/app/replylater/android/reminder/data/RoomReminderRepositoryTest.kt`
- Test: `app/src/androidTest/java/app/replylater/android/reminder/crypto/AndroidKeystoreMessageCipherTest.kt`
- Test: `app/src/androidTest/java/app/replylater/android/reminder/data/ReplyLaterDatabaseTest.kt`

**Interfaces:**
- Consumes: `ReminderRepository` and `Reminder` from Task 1.
- Produces: Room database version 1, a repository that encrypts/decrypts only at its boundary, and the process-wide application graph that later tasks extend.

- [x] **Step 1: Add processor and persistence dependencies**

Add KSP plugin `2.3.12` and Room `2.8.5`. Apply KSP to `app`; add `room-runtime`, `room-ktx`, `room-compiler` via `ksp`, and `room-testing` for androidTest. Configure Room schema output under `app/schemas` and commit generated version-1 JSON.

- [x] **Step 2: Write failing repository, database, and cipher tests**

The fake-cipher repository test must prove that plaintext passed to `upsert` becomes ciphertext/IV in `ReminderEntity` and is decrypted on `get`. The in-memory Room test must prove insert, observe, answer, reschedule, delete, and unfinished ordering. The device cipher test must prove a round trip and that encrypting the same text twice yields distinct IV/ciphertext pairs.

- [x] **Step 3: Run tests and verify RED**

Run: `./gradlew testDebugUnitTest --tests '*RoomReminderRepositoryTest'`

Expected: compilation fails because persistence and cipher types do not exist.

- [x] **Step 4: Implement cipher and Room schema**

Define:

```kotlin
data class EncryptedMessage(val ciphertext: ByteArray, val iv: ByteArray)

interface MessageCipher {
    fun encrypt(plaintext: String): EncryptedMessage
    fun decrypt(encrypted: EncryptedMessage): String
}
```

`AndroidKeystoreMessageCipher` uses alias `reply_later_message_key_v1`, `KeyProperties.KEY_ALGORITHM_AES`, GCM, no padding, randomized encryption required, and a fresh provider-generated 12-byte IV. `ReminderEntity` stores nullable `messageCiphertext` and `messageIv`; it never stores a plaintext message column. DAO unfinished queries select `answeredAtEpochMillis IS NULL ORDER BY remindAtEpochMillis ASC`.

Create `ReplyLaterApplication` and set `android:name` in the manifest. Its `AppGraph` initially exposes the database, cipher, and repository; later tasks add scheduler and notification services without changing consumers.

- [x] **Step 5: Verify JVM and physical-device persistence tests**

Run:

```bash
./gradlew testDebugUnitTest --tests '*RoomReminderRepositoryTest'
ANDROID_SERIAL="$(adb devices -l | awk '/model:RMX3938/{print $1; exit}')" ./gradlew connectedDebugAndroidTest
```

Expected: repository tests pass; device report includes the existing two pipeline tests plus Room/cipher tests with zero failures.

- [x] **Step 6: Verify ciphertext-at-rest**

Insert only synthetic `MESSAGE_1`, pull the debug database from the debuggable app, and search SQLite output for that exact token. Expected: the token is absent while repository retrieval returns `MESSAGE_1`. Delete the pulled temporary database after inspection.

- [x] **Step 7: Commit and push**

```bash
git add gradle/libs.versions.toml build.gradle.kts app/build.gradle.kts app/schemas app/src/main/AndroidManifest.xml app/src/main/java/app/replylater/android/ReplyLaterApplication.kt app/src/main/java/app/replylater/android/AppGraph.kt app/src/main/java/app/replylater/android/reminder app/src/test/java/app/replylater/android/reminder app/src/androidTest/java/app/replylater/android/reminder
git commit -m "feat: persist encrypted reminders"
git push origin main
```

---

### Task 3: Idempotent exact/inexact alarm scheduling

**Files:**
- Create: `app/src/main/java/app/replylater/android/reminder/schedule/AlarmCapability.kt`
- Create: `app/src/main/java/app/replylater/android/reminder/schedule/AlarmPlan.kt`
- Create: `app/src/main/java/app/replylater/android/reminder/schedule/AlarmPlanSelector.kt`
- Create: `app/src/main/java/app/replylater/android/reminder/schedule/AlarmReminderScheduler.kt`
- Create: `app/src/main/java/app/replylater/android/reminder/schedule/ReminderReconciler.kt`
- Modify: `app/src/main/java/app/replylater/android/AppGraph.kt`
- Test: `app/src/test/java/app/replylater/android/reminder/schedule/AlarmPlanSelectorTest.kt`
- Test: `app/src/test/java/app/replylater/android/reminder/schedule/ReminderReconcilerTest.kt`

**Interfaces:**
- Consumes: unfinished reminders from `ReminderRepository`.
- Produces: `ReminderScheduler.schedule(id, remindAt)`/`cancel(id)`, pure exact/inexact selection, and reusable reconciliation logic.

- [x] **Step 1: Write failing scheduler-policy tests**

Prove API 31+ with exact authorization selects exact; API 31+ without access selects inexact; API 26–30 selects exact; past targets select immediate inexact delivery; reconciliation schedules only unanswered future items and immediately publishes overdue items once without changing their stored target.

- [x] **Step 2: Run tests and verify RED**

Run: `./gradlew testDebugUnitTest --tests '*AlarmPlanSelectorTest' --tests '*ReminderReconcilerTest'`

Expected: compilation fails because scheduling types do not exist.

- [x] **Step 3: Implement policy and Android scheduler**

Use `setExactAndAllowWhileIdle` only when `AlarmManager.canScheduleExactAlarms()` is true on API 31+, otherwise `setAndAllowWhileIdle`. Build immutable/update-current broadcast PendingIntents keyed by a stable request code derived from reminder UUID and include only the reminder ID. Scheduling the same ID replaces its previous alarm.

- [x] **Step 4: Implement reusable reconciliation**

`ReminderReconciler` reads all unfinished reminders and schedules future targets idempotently. Past targets are returned as overdue delivery IDs so the Android receiver added later can publish them once. Extend `AppGraph` with the scheduler and reconciler.

- [x] **Step 5: Verify tests and manifest**

Run: `./gradlew testDebugUnitTest lintDebug assembleDebug`. Expected: tests/build/lint pass and the merged manifest still omits `INTERNET`.

- [x] **Step 6: Commit and push**

```bash
git add app/src/main/java/app/replylater/android/AppGraph.kt app/src/main/java/app/replylater/android/reminder/schedule app/src/test/java/app/replylater/android/reminder/schedule
git commit -m "feat: schedule durable reminders"
git push origin main
```

---

### Task 4: Explicit companion-notification capture actions

**Files:**
- Create: `app/src/main/java/app/replylater/android/capture/companion/CapturePayload.kt`
- Create: `app/src/main/java/app/replylater/android/capture/companion/CapturePayloadCodec.kt`
- Create: `app/src/main/java/app/replylater/android/capture/companion/CaptureActionHandler.kt`
- Create: `app/src/main/java/app/replylater/android/capture/companion/CompanionNotificationPublisher.kt`
- Create: `app/src/main/java/app/replylater/android/capture/companion/CaptureActionReceiver.kt`
- Create: `app/src/main/java/app/replylater/android/capture/companion/SourceIntentRegistry.kt`
- Create: `app/src/main/java/app/replylater/android/notification/NotificationChannels.kt`
- Modify: `app/src/main/java/app/replylater/android/capture/framework/ReplyLaterNotificationListener.kt`
- Modify: `app/src/main/java/app/replylater/android/AppGraph.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Test: `app/src/test/java/app/replylater/android/capture/companion/CapturePayloadCodecTest.kt`
- Test: `app/src/test/java/app/replylater/android/capture/companion/CaptureActionHandlerTest.kt`

**Interfaces:**
- Consumes: accepted `DirectMessage`, source notification content PendingIntent, and `CreateReminder`.
- Produces: quiet companion notification state machine: Reply later → 30 min / 60 min / More.

- [x] **Step 1: Write failing payload and action-handler tests**

Prove the codec round-trips only supported messenger values, hashed conversation key, synthetic contact/message, timestamps, and source key; rejects missing/invalid fields; never creates a reminder for initial publication or Reply later expansion; creates exactly one reminder for 30/60; and returns a custom-time navigation result for More.

- [x] **Step 2: Run tests and verify RED**

Run: `./gradlew testDebugUnitTest --tests '*CapturePayloadCodecTest' --tests '*CaptureActionHandlerTest'`

Expected: compilation fails because companion types do not exist.

- [x] **Step 3: Implement the pure handler and safe payload codec**

Use explicit action constants scoped to the package. Encode values as primitive Intent extras held by Android's PendingIntent; do not write them to app storage before a time action. Cap decoded contact/message lengths using the existing normalizer limit and reject unsupported package/messenger combinations.

- [x] **Step 4: Implement quiet companion notifications**

Create channel `capture_candidates_v1` at `IMPORTANCE_LOW`, vibration disabled, sound null, badge disabled. Initial notification has one `Ответить позже` action. Expansion republishes the same notification ID with `30 мин`, `60 мин`, and `Ещё`. IDs are stable per messenger plus conversation key; newer messages replace the same conversation notification.

- [x] **Step 5: Connect listener lifecycle**

On accepted notification, register its transient content PendingIntent and publish the companion. On source removal, cancel the companion and remove the transient intent unless the candidate has already been saved. Rejected and unsupported notifications remain inspector-only.

- [x] **Step 6: Verify tests and build variants**

Run: `./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease`. Expected: all pass; release still contains no content inspector behavior and no network permission.

- [x] **Step 7: Commit and push**

```bash
git add app/src/main/AndroidManifest.xml app/src/main/res/values/strings.xml app/src/main/java/app/replylater/android/capture app/src/main/java/app/replylater/android/notification app/src/test/java/app/replylater/android/capture
git commit -m "feat: add explicit reply-later actions"
git push origin main
```

---

### Task 5: Reminder delivery, open, answered, and later actions

**Files:**
- Create: `app/src/main/java/app/replylater/android/notification/ReminderNotificationPublisher.kt`
- Create: `app/src/main/java/app/replylater/android/notification/ReminderActionHandler.kt`
- Create: `app/src/main/java/app/replylater/android/notification/ReminderActionReceiver.kt`
- Create: `app/src/main/java/app/replylater/android/reminder/schedule/ReminderAlarmReceiver.kt`
- Create: `app/src/main/java/app/replylater/android/openchat/ChatOpener.kt`
- Modify: `app/src/main/java/app/replylater/android/AppGraph.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Test: `app/src/test/java/app/replylater/android/notification/ReminderActionHandlerTest.kt`
- Test: `app/src/test/java/app/replylater/android/openchat/ChatOpenerTest.kt`

**Interfaces:**
- Consumes: reminder ID alarm broadcasts and `ReminderRepository`.
- Produces: high-importance reminder notification with Open chat, Mark answered, and Later behavior.

- [ ] **Step 1: Write failing reminder-action tests**

Prove delivery ignores missing/answered IDs; Open first attempts a transient source PendingIntent then falls back to the supported messenger launch intent; Mark answered updates the repository, cancels its alarm, and dismisses its notification; Later republishes 30/60/More choices; 30/60 reschedule the same ID instead of inserting another row.

- [ ] **Step 2: Run tests and verify RED**

Run: `./gradlew testDebugUnitTest --tests '*ReminderActionHandlerTest' --tests '*ChatOpenerTest'`

Expected: compilation fails because reminder action and opener types do not exist.

- [ ] **Step 3: Implement reminder notifications and actions**

Create channel `scheduled_reminders_v1` at high importance. Show contact, nullable preview, and actions `Открыть чат`, `Отвечено`, `Позже`. Use notification ID derived from reminder UUID; use explicit immutable/update-current PendingIntents. Never mark answered after Open.

- [ ] **Step 4: Implement honest chat fallback**

`ChatOpener` catches `PendingIntent.CanceledException`; if no transient source intent succeeds, it calls `PackageManager.getLaunchIntentForPackage(sourcePackage)`. It returns `EXACT_CONVERSATION`, `MESSENGER_HOME`, or `UNAVAILABLE` so later UI can describe the result without promising a deep link.

- [ ] **Step 5: Verify full suite**

Run: `./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease`. Expected: all checks pass.

- [ ] **Step 6: Commit and push**

```bash
git add app/src/main/AndroidManifest.xml app/src/main/res/values/strings.xml app/src/main/java/app/replylater/android/notification app/src/main/java/app/replylater/android/openchat app/src/main/java/app/replylater/android/reminder/schedule app/src/test/java/app/replylater/android/notification app/src/test/java/app/replylater/android/openchat
git commit -m "feat: deliver actionable reminders"
git push origin main
```

---

### Task 6: Permission/degraded-state foundation and reconciliation receivers

**Files:**
- Modify: `app/src/main/java/app/replylater/android/ReplyLaterApplication.kt`
- Modify: `app/src/main/java/app/replylater/android/AppGraph.kt`
- Create: `app/src/main/java/app/replylater/android/reminder/schedule/ReminderReconcileReceiver.kt`
- Create: `app/src/main/java/app/replylater/android/permission/AccessStatus.kt`
- Create: `app/src/main/java/app/replylater/android/permission/AndroidAccessStatusReader.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/java/app/replylater/android/MainActivity.kt`
- Modify: `app/src/main/java/app/replylater/android/ui/ReplyLaterRoot.kt`
- Test: `app/src/test/java/app/replylater/android/permission/AccessStatusTest.kt`

**Interfaces:**
- Consumes: concrete database, repository, cipher, publishers, scheduler, and Android permission APIs.
- Produces: one initialized `AppGraph`, current access-state model, early permission flow, and usable fallback state for the later full UI.

- [ ] **Step 1: Write failing access-state tests**

Prove setup chooses notification listener first, POST_NOTIFICATIONS second only on API 33+, exact alarm third only on API 31+, and `COMPLETE_WITH_FALLBACK` after every relevant prompt has been presented even when exact access is denied. Prove listener or notification denial still yields a visible repair action instead of an onboarding loop.

- [ ] **Step 2: Run test and verify RED**

Run: `./gradlew testDebugUnitTest --tests '*AccessStatusTest'`

Expected: compilation fails because access-state types do not exist.

- [ ] **Step 3: Complete the application graph and reconciliation receiver**

Complete the lazily initialized graph with notification publishers, use cases, and action handlers once per process. Add `RECEIVE_BOOT_COMPLETED` and `SCHEDULE_EXACT_ALARM`; register a non-exported receiver for `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIME_SET`, and `TIMEZONE_CHANGED`. It launches coroutine work with `goAsync()` and always calls `finish()` in `finally`. Receivers and listener obtain dependencies from `applicationContext as ReplyLaterApplication`.

- [ ] **Step 4: Implement permission gateways and minimal setup UI**

Declare `POST_NOTIFICATIONS`. Check listener access through `NotificationManagerCompat.getEnabledListenerPackages`, notification permission through `ContextCompat.checkSelfPermission`, and exact-alarm capability through `AlarmManager.canScheduleExactAlarms`. MainActivity requests runtime notifications only after returning from listener settings, opens `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` when applicable, records presentation flags in saved preferences, and never automatically reopens a refused prompt.

- [ ] **Step 5: Run complete automated verification**

Run:

```bash
ANDROID_SERIAL="$(adb devices -l | awk '/model:RMX3938/{print $1; exit}')" ./gradlew connectedDebugAndroidTest
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
```

Expected: all device/unit tests pass, lint is clean, both APK variants build, and the merged manifest contains no `INTERNET` permission.

- [ ] **Step 6: Commit and push**

```bash
git add app/src/main/AndroidManifest.xml app/src/main/java/app/replylater/android app/src/test/java/app/replylater/android/permission
git commit -m "feat: wire reminder core and access state"
git push origin main
```

---

### Task 7: Physical-device core workflow checkpoint

**Files:**
- Create: `docs/validation/reminder-core-device-matrix.md`
- Modify: implementation or tests only after a failing regression test reproduces an observed defect.

**Interfaces:**
- Consumes: installed debug build and synthetic debug-only capture trigger or a real supported notification when available.
- Produces: evidence that explicit save, scheduling, delivery, rescheduling, process death, and degraded alarm behavior work before building the full inbox UI.

- [ ] **Step 1: Add a debug-only synthetic candidate control**

Expose a debug-only button that publishes a `CONTACT_A`/`MESSAGE_1` WhatsApp-shaped companion notification through the production publisher. Release source set provides no such control. This is test scaffolding, never a production feature.

- [ ] **Step 2: Verify explicit-save boundary**

Publish the synthetic candidate, inspect the database before tapping any time, and record zero rows. Tap Reply later → 30 min, then record exactly one encrypted row and one scheduled alarm.

- [ ] **Step 3: Verify lifecycle and actions**

Exercise process force-stop/relaunch, a short debug alarm target, reminder delivery, Later rescheduling, Open fallback to WhatsApp, and Mark answered. Confirm no duplicate row appears and answered state persists.

- [ ] **Step 4: Verify permission fallback**

Revoke exact-alarm access, create another synthetic reminder, and confirm an inexact alarm is selected while the saved item remains usable. Restore the user's previous permission state afterwards.

- [ ] **Step 5: Record sanitized evidence**

Document only device/API, booleans, row counts, timestamps rounded to minute, and synthetic constants. Do not include notification database ciphertext, user contacts, real message contents, or device identifiers beyond model/API.

- [ ] **Step 6: Run final core verification**

Run `ANDROID_SERIAL="$(adb devices -l | awk '/model:RMX3938/{print $1; exit}')" ./gradlew connectedDebugAndroidTest` and `./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease`. Confirm zero failures and no `INTERNET` permission.

- [ ] **Step 7: Commit and push**

```bash
git add docs/validation app/src/debug app/src/release
git commit -m "test: validate reminder core on device"
git push origin main
```

---

## Phase completion

After Task 7 passes, write and execute the second MVP plan for the approved Compose onboarding, custom date/time confirmation, inbox with filters/date navigation/sticky headers, reminder detail, completed archive, settings/data deletion, and final end-to-end release hardening.
