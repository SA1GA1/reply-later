# Android Foundation and Notification Spike Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create a buildable Reply Later Android application and prove that current Telegram and WhatsApp direct-message notifications can be classified without accepting known group conversations.

**Architecture:** A single `app` module contains a small Compose shell and a notification-capture feature. Android notification objects are converted at the framework boundary into immutable `RawNotification` values, then messenger-specific pure Kotlin parsers return either a supported `DirectMessage` or an explicit rejection reason. A debug-only in-memory inspector displays sanitized parser decisions on-device without writing message content to Logcat or disk.

**Tech Stack:** Android Gradle Plugin 9.4.0 with built-in Kotlin 2.3.21, Gradle 9.6.0, JDK 17 toolchain, compileSdk 37, targetSdk 36, minSdk 26, Jetpack Compose BOM 2026.08.00, Material 3, JUnit 4.

**Spec:** `docs/superpowers/specs/2026-09-12-reply-later-android-mvp-design.md`

## Global Constraints

- Application ID and namespace are `app.replylater.android`.
- The Git branch is `main`; each task ends in a focused commit pushed to `origin/main` after verification.
- Only `org.telegram.messenger` and `com.whatsapp` are supported.
- Group chats, channels, bots, broadcast lists, communities, Telegram X, and WhatsApp Business are excluded.
- Ambiguous notifications are rejected rather than captured.
- No notification content may be written to logs, analytics, files, or persistent storage during this spike.
- No `INTERNET` permission is declared.
- Production UI strings are Russian; code identifiers are English.
- The existing `design/mockups` files are reference assets and must not be modified.

---

## File map

```text
settings.gradle.kts                         Plugin repositories and module inclusion
build.gradle.kts                            Root plugin declarations
gradle/libs.versions.toml                   Central dependency versions and aliases
gradle/wrapper/gradle-wrapper.properties    Gradle 9.6.0 distribution
gradle.properties                           AndroidX and build settings
.gitignore                                  Android Studio/Gradle generated files
app/build.gradle.kts                        Android application configuration
app/src/main/AndroidManifest.xml            Activity, notification listener, and permissions
app/src/main/res/values/strings.xml         Russian product copy
app/src/main/res/values/themes.xml          Platform launch theme
app/src/main/java/app/replylater/android/MainActivity.kt
                                             Compose activity entry point
app/src/main/java/app/replylater/android/ui/ReplyLaterRoot.kt
                                             Initial setup/debug navigation shell
app/src/main/java/app/replylater/android/ui/HomeDateFormatter.kt
                                             Russian home-header date formatting
app/src/main/java/app/replylater/android/ui/theme/Color.kt
app/src/main/java/app/replylater/android/ui/theme/Theme.kt
app/src/main/java/app/replylater/android/ui/theme/Type.kt
                                             Approved light visual foundation
app/src/main/java/app/replylater/android/capture/model/RawNotification.kt
                                             Android-free normalized notification input
app/src/main/java/app/replylater/android/capture/model/DirectMessage.kt
                                             Accepted message model
app/src/main/java/app/replylater/android/capture/parser/NotificationParser.kt
                                             Parser contract and result types
app/src/main/java/app/replylater/android/capture/parser/TelegramNotificationParser.kt
                                             Telegram-specific classification
app/src/main/java/app/replylater/android/capture/parser/WhatsAppNotificationParser.kt
                                             WhatsApp-specific classification
app/src/main/java/app/replylater/android/capture/parser/SupportedNotificationParser.kt
                                             Package-based parser router
app/src/main/java/app/replylater/android/capture/framework/NotificationNormalizer.kt
                                             StatusBarNotification to RawNotification adapter
app/src/main/java/app/replylater/android/capture/framework/ReplyLaterNotificationListener.kt
                                             NotificationListenerService boundary
app/src/debug/java/app/replylater/android/capture/debug/CaptureInspectorStore.kt
                                             Volatile bounded debug inspection state
app/src/debug/java/app/replylater/android/capture/debug/CaptureInspectorScreen.kt
                                             On-device sanitized decision viewer
app/src/release/java/app/replylater/android/capture/debug/CaptureInspectorStore.kt
                                             No-op release implementation
app/src/test/java/app/replylater/android/capture/parser/TelegramNotificationParserTest.kt
app/src/test/java/app/replylater/android/capture/parser/WhatsAppNotificationParserTest.kt
app/src/test/java/app/replylater/android/capture/parser/SupportedNotificationParserTest.kt
                                             Pure parser contract fixtures
```

---

### Task 1: Buildable Android foundation

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle/libs.versions.toml`
- Create: `gradle/wrapper/gradle-wrapper.properties`
- Create: `gradle/wrapper/gradle-wrapper.jar`
- Create: `gradlew`
- Create: `gradlew.bat`
- Create: `gradle.properties`
- Create: `.gitignore`
- Create: `app/build.gradle.kts`
- Create: `app/proguard-rules.pro`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values/themes.xml`
- Create: `app/src/main/java/app/replylater/android/MainActivity.kt`
- Create: `app/src/main/java/app/replylater/android/ui/ReplyLaterRoot.kt`
- Create: `app/src/main/java/app/replylater/android/ui/theme/Color.kt`
- Create: `app/src/main/java/app/replylater/android/ui/theme/Theme.kt`
- Create: `app/src/main/java/app/replylater/android/ui/theme/Type.kt`
- Test: `app/src/test/java/app/replylater/android/ui/HomeDateFormatterTest.kt`

**Interfaces:**
- Consumes: approved visual direction from `design/mockups` and application ID `app.replylater.android`.
- Produces: `MainActivity` and `@Composable fun ReplyLaterRoot()` as the executable application shell.

- [ ] **Step 1: Generate the Gradle wrapper and version catalog**

Use Android Studio's bundled JDK 17 and Gradle 9.6.0. Define these exact catalog entries:

```toml
[versions]
agp = "9.4.0"
kotlin = "2.3.21"
composeBom = "2026.08.00"
activityCompose = "1.13.0"
lifecycle = "2.11.0"
junit = "4.13.2"

[libraries]
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activityCompose" }
androidx-lifecycle-runtime = { module = "androidx.lifecycle:lifecycle-runtime-ktx", version.ref = "lifecycle" }
androidx-compose-bom = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { module = "androidx.compose.ui:ui" }
androidx-compose-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
androidx-compose-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
androidx-compose-material3 = { module = "androidx.compose.material3:material3" }
junit = { module = "junit:junit", version.ref = "junit" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

- [ ] **Step 2: Write the failing home-date test**

```kotlin
package app.replylater.android.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class HomeDateFormatterTest {
    @Test fun formatsDateForRussianInboxHeader() {
        val result = formatHomeDate(LocalDate.of(2026, 9, 12))

        assertEquals("суббота, 12 сентября", result)
    }
}
```

- [ ] **Step 3: Run the test and confirm the project is not yet buildable**

Run: `./gradlew testDebugUnitTest`  
Expected: compilation failure because `formatHomeDate` does not exist yet.

- [ ] **Step 4: Configure the app module and manifest**

Configure `namespace = "app.replylater.android"`, `compileSdk = 37`, `minSdk = 26`, `targetSdk = 36`, Java/Kotlin toolchain 17, AGP built-in Kotlin, Compose, `buildConfig = true`, and unit tests. The manifest must set `android:allowBackup="false"`, expose only `MainActivity`, and omit `android.permission.INTERNET`.

- [ ] **Step 5: Implement date formatting and the initial Compose shell**

Implement `fun formatHomeDate(date: LocalDate): String` with a fixed Russian locale, then render a light screen with the product name, the formatted current date, a neutral setup-state card, and a red-accent primary button. Use these foundation colors:

```kotlin
val ReplyRed = Color(0xFFE5484D)
val ReplyRedSoft = Color(0xFFFFE9EA)
val CanvasWhite = Color(0xFFF8F8F7)
val SurfaceWhite = Color(0xFFFFFFFF)
val TextPrimary = Color(0xFF202124)
val TextSecondary = Color(0xFF727272)
val DividerGray = Color(0xFFE8E8E6)
```

Dynamic color is disabled so the approved brand direction remains stable across devices.

- [ ] **Step 6: Verify the foundation**

Run: `./gradlew testDebugUnitTest lintDebug assembleDebug`  
Expected: all tasks succeed and `app/build/outputs/apk/debug/app-debug.apk` exists.

- [ ] **Step 7: Commit and publish**

```bash
git add .gitignore settings.gradle.kts build.gradle.kts gradle.properties gradle gradlew gradlew.bat app
git commit -m "build: bootstrap Reply Later Android app"
git push origin main
```

---

### Task 2: Parser contract and package routing

**Files:**
- Create: `app/src/main/java/app/replylater/android/capture/model/RawNotification.kt`
- Create: `app/src/main/java/app/replylater/android/capture/model/DirectMessage.kt`
- Create: `app/src/main/java/app/replylater/android/capture/parser/NotificationParser.kt`
- Create: `app/src/main/java/app/replylater/android/capture/parser/SupportedNotificationParser.kt`
- Test: `app/src/test/java/app/replylater/android/capture/parser/SupportedNotificationParserTest.kt`

**Interfaces:**
- Consumes: normalized strings and messaging-style participants from the future framework adapter.
- Produces: `NotificationParser.parse(RawNotification): ParseResult`, `ParseResult.Accepted`, and `ParseResult.Rejected` for messenger parsers and the listener.

- [ ] **Step 1: Write the failing router tests**

```kotlin
class SupportedNotificationParserTest {
    private val telegram = FakeParser(ParseResult.Rejected(RejectReason.AMBIGUOUS_CHAT))
    private val whatsapp = FakeParser(ParseResult.Rejected(RejectReason.GROUP_CHAT))
    private val router = SupportedNotificationParser(telegram, whatsapp)

    @Test fun routesTelegramPackage() {
        val result = router.parse(raw(packageName = "org.telegram.messenger"))
        assertEquals(RejectReason.AMBIGUOUS_CHAT, (result as ParseResult.Rejected).reason)
    }

    @Test fun routesWhatsAppPackage() {
        val result = router.parse(raw(packageName = "com.whatsapp"))
        assertEquals(RejectReason.GROUP_CHAT, (result as ParseResult.Rejected).reason)
    }

    @Test fun rejectsUnsupportedPackage() {
        val result = router.parse(raw(packageName = "com.example.chat"))
        assertEquals(RejectReason.UNSUPPORTED_PACKAGE, (result as ParseResult.Rejected).reason)
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew testDebugUnitTest --tests '*SupportedNotificationParserTest'`  
Expected: compilation failure because capture model and parser types do not exist.

- [ ] **Step 3: Implement immutable input and result types**

Define:

```kotlin
data class RawNotification(
    val packageName: String,
    val notificationKey: String,
    val postedAtEpochMillis: Long,
    val title: String?,
    val text: String?,
    val subText: String?,
    val conversationTitle: String?,
    val category: String?,
    val isGroupConversation: Boolean?,
    val messages: List<RawMessage>,
)

data class RawMessage(
    val sender: String?,
    val text: String?,
    val timestampEpochMillis: Long,
)

enum class Messenger { TELEGRAM, WHATSAPP }

data class DirectMessage(
    val messenger: Messenger,
    val notificationKey: String,
    val conversationKey: String,
    val contactDisplayName: String,
    val messageText: String?,
    val receivedAtEpochMillis: Long,
)

sealed interface ParseResult {
    data class Accepted(val message: DirectMessage) : ParseResult
    data class Rejected(val reason: RejectReason) : ParseResult
}

enum class RejectReason {
    UNSUPPORTED_PACKAGE,
    NOT_A_MESSAGE,
    GROUP_CHAT,
    CHANNEL_OR_BOT,
    MISSING_CONTACT,
    AMBIGUOUS_CHAT,
}

fun interface NotificationParser {
    fun parse(notification: RawNotification): ParseResult
}
```

The `conversationKey` is a deterministic SHA-256 digest of package name plus stable normalized conversation input; raw identifiers are not persisted by this spike.

- [ ] **Step 4: Implement strict package routing**

`SupportedNotificationParser` delegates only the two exact package names and returns `UNSUPPORTED_PACKAGE` for everything else.

- [ ] **Step 5: Run parser routing tests**

Run: `./gradlew testDebugUnitTest --tests '*SupportedNotificationParserTest'`  
Expected: all three tests pass.

- [ ] **Step 6: Commit and publish**

```bash
git add app/src/main/java/app/replylater/android/capture app/src/test/java/app/replylater/android/capture
git commit -m "feat: define notification parser boundary"
git push origin main
```

---

### Task 3: Telegram direct-message classifier

**Files:**
- Create: `app/src/main/java/app/replylater/android/capture/parser/TelegramNotificationParser.kt`
- Test: `app/src/test/java/app/replylater/android/capture/parser/TelegramNotificationParserTest.kt`

**Interfaces:**
- Consumes: `RawNotification` from Task 2.
- Produces: `TelegramNotificationParser.parse(RawNotification): ParseResult` for the package router.

- [ ] **Step 1: Write failing direct/group/ambiguous tests**

Create fixtures covering:

```kotlin
@Test fun acceptsDirectMessagingStyleNotification() {
    val result = parser.parse(rawTelegram(
        title = "Илья",
        text = "Скинь, пожалуйста, ссылку",
        conversationTitle = null,
        isGroupConversation = false,
        messages = listOf(RawMessage("Илья", "Скинь, пожалуйста, ссылку", 1_000L)),
    ))
    val message = (result as ParseResult.Accepted).message
    assertEquals("Илья", message.contactDisplayName)
    assertEquals("Скинь, пожалуйста, ссылку", message.messageText)
}

@Test fun rejectsExplicitGroupConversation() {
    val result = parser.parse(rawTelegram(
        title = "Команда",
        conversationTitle = "Команда",
        isGroupConversation = true,
    ))
    assertEquals(RejectReason.GROUP_CHAT, (result as ParseResult.Rejected).reason)
}

@Test fun rejectsChannelStyleTitle() {
    val result = parser.parse(rawTelegram(title = "Новости", subText = "канал"))
    assertEquals(RejectReason.CHANNEL_OR_BOT, (result as ParseResult.Rejected).reason)
}

@Test fun rejectsConflictingConversationSignals() {
    val result = parser.parse(rawTelegram(
        title = "Илья",
        conversationTitle = "Команда",
        isGroupConversation = null,
    ))
    assertEquals(RejectReason.AMBIGUOUS_CHAT, (result as ParseResult.Rejected).reason)
}
```

- [ ] **Step 2: Run the Telegram tests to verify failure**

Run: `./gradlew testDebugUnitTest --tests '*TelegramNotificationParserTest'`  
Expected: compilation failure because `TelegramNotificationParser` does not exist.

- [ ] **Step 3: Implement conservative Telegram rules**

Reject when `isGroupConversation == true`, when a nonblank `conversationTitle` conflicts with the direct contact title, or when inspected current-device fixtures contain a confirmed channel/bot marker. Accept only a message-category notification with a nonblank contact and either `isGroupConversation == false` or one unambiguous sender matching the title. Prefer the newest messaging-style entry over summary text.

- [ ] **Step 4: Run Telegram parser tests**

Run: `./gradlew testDebugUnitTest --tests '*TelegramNotificationParserTest'`  
Expected: all Telegram cases pass.

- [ ] **Step 5: Commit and publish**

```bash
git add app/src/main/java/app/replylater/android/capture/parser/TelegramNotificationParser.kt app/src/test/java/app/replylater/android/capture/parser/TelegramNotificationParserTest.kt
git commit -m "feat: classify Telegram direct messages"
git push origin main
```

---

### Task 4: WhatsApp direct-message classifier

**Files:**
- Create: `app/src/main/java/app/replylater/android/capture/parser/WhatsAppNotificationParser.kt`
- Test: `app/src/test/java/app/replylater/android/capture/parser/WhatsAppNotificationParserTest.kt`

**Interfaces:**
- Consumes: `RawNotification` from Task 2.
- Produces: `WhatsAppNotificationParser.parse(RawNotification): ParseResult` for the package router.

- [ ] **Step 1: Write failing direct/group/summary tests**

Create fixtures covering:

```kotlin
@Test fun acceptsDirectMessagingStyleNotification() {
    val result = parser.parse(rawWhatsApp(
        title = "Анна",
        text = "Будешь сегодня?",
        isGroupConversation = false,
        messages = listOf(RawMessage("Анна", "Будешь сегодня?", 2_000L)),
    ))
    val message = (result as ParseResult.Accepted).message
    assertEquals(Messenger.WHATSAPP, message.messenger)
    assertEquals("Анна", message.contactDisplayName)
}

@Test fun rejectsExplicitGroupConversation() {
    val result = parser.parse(rawWhatsApp(
        title = "Семья",
        conversationTitle = "Семья",
        isGroupConversation = true,
    ))
    assertEquals(RejectReason.GROUP_CHAT, (result as ParseResult.Rejected).reason)
}

@Test fun rejectsBundledSummary() {
    val result = parser.parse(rawWhatsApp(
        title = "WhatsApp",
        text = "3 новых сообщения из 2 чатов",
        messages = emptyList(),
    ))
    assertEquals(RejectReason.AMBIGUOUS_CHAT, (result as ParseResult.Rejected).reason)
}
```

- [ ] **Step 2: Run the WhatsApp tests to verify failure**

Run: `./gradlew testDebugUnitTest --tests '*WhatsAppNotificationParserTest'`  
Expected: compilation failure because `WhatsAppNotificationParser` does not exist.

- [ ] **Step 3: Implement conservative WhatsApp rules**

Reject explicit groups, bundled summaries, and notifications without one identifiable direct contact. Prefer the newest messaging-style entry and require direct-conversation evidence equivalent to the Telegram contract. Never match localized summary text as the sole group-detection mechanism.

- [ ] **Step 4: Run all parser tests**

Run: `./gradlew testDebugUnitTest --tests '*NotificationParserTest'`  
Expected: Telegram, WhatsApp, and routing suites pass.

- [ ] **Step 5: Commit and publish**

```bash
git add app/src/main/java/app/replylater/android/capture/parser/WhatsAppNotificationParser.kt app/src/test/java/app/replylater/android/capture/parser/WhatsAppNotificationParserTest.kt
git commit -m "feat: classify WhatsApp direct messages"
git push origin main
```

---

### Task 5: Android notification listener and safe debug inspector

**Files:**
- Create: `app/src/main/java/app/replylater/android/capture/framework/NotificationNormalizer.kt`
- Create: `app/src/main/java/app/replylater/android/capture/framework/ReplyLaterNotificationListener.kt`
- Create: `app/src/debug/java/app/replylater/android/capture/debug/CaptureInspectorStore.kt`
- Create: `app/src/debug/java/app/replylater/android/capture/debug/CaptureInspectorScreen.kt`
- Create: `app/src/release/java/app/replylater/android/capture/debug/CaptureInspectorStore.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/java/app/replylater/android/ui/ReplyLaterRoot.kt`
- Test: `app/src/test/java/app/replylater/android/capture/framework/NotificationNormalizerPolicyTest.kt`

**Interfaces:**
- Consumes: Android `StatusBarNotification`, `SupportedNotificationParser`, and `ParseResult`.
- Produces: notification-listener lifecycle integration and sanitized in-memory `InspectorEntry` state for physical-device validation.

- [ ] **Step 1: Write failing normalization-policy tests**

Extract Android-independent policy helpers and test that whitespace is normalized, blank contacts become null, message arrays remain chronological, and unsupported packages are rejected before extras are copied.

```kotlin
@Test fun normalizeText_collapsesWhitespace() {
    assertEquals("Привет мир", normalizeText("  Привет\n  мир "))
}

@Test fun supportedPackage_isStrict() {
    assertTrue(isSupportedPackage("org.telegram.messenger"))
    assertTrue(isSupportedPackage("com.whatsapp"))
    assertFalse(isSupportedPackage("org.thunderdog.challegram"))
}
```

- [ ] **Step 2: Run the policy tests to verify failure**

Run: `./gradlew testDebugUnitTest --tests '*NotificationNormalizerPolicyTest'`  
Expected: compilation failure because the helpers do not exist.

- [ ] **Step 3: Implement `NotificationNormalizer`**

Read only documented notification fields: package, key, post time, category, `EXTRA_TITLE`, `EXTRA_TEXT`, `EXTRA_SUB_TEXT`, `EXTRA_CONVERSATION_TITLE`, `EXTRA_IS_GROUP_CONVERSATION`, and `Notification.MessagingStyle.Message` bundles. Convert spans to plain strings immediately and cap all text fields at 4,096 characters in memory.

- [ ] **Step 4: Implement the listener service**

Declare the service with `android.permission.BIND_NOTIFICATION_LISTENER_SERVICE`, `android:exported="true"`, and the notification-listener intent filter. In `onNotificationPosted`, return immediately for the app's own package and unsupported packages, normalize, parse, and forward only a sanitized decision object to the debug store. Do not call `Log.*` with payload fields.

- [ ] **Step 5: Implement the debug-only inspector**

Keep at most 50 volatile entries containing timestamp, messenger, accepted/rejected state, rejection reason, and booleans describing which fields existed. Show contact and message preview only behind an explicit “Показывать содержимое в этой сессии” switch that defaults off and resets after process death. The release source set provides a no-op store and no inspector destination.

- [ ] **Step 6: Verify build variants and manifest**

Run: `./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease`  
Expected: tests and lint pass; both APK variants build; `aapt dump permissions` confirms there is no `INTERNET` permission.

- [ ] **Step 7: Commit and publish**

```bash
git add app/src/main app/src/debug app/src/release app/src/test
git commit -m "feat: inspect supported messenger notifications"
git push origin main
```

---

### Task 6: Physical-device notification matrix

**Files:**
- Create: `docs/validation/notification-spike-matrix.md`
- Modify: `app/src/test/java/app/replylater/android/capture/parser/TelegramNotificationParserTest.kt`
- Modify: `app/src/test/java/app/replylater/android/capture/parser/WhatsAppNotificationParserTest.kt`
- Modify: parser implementation files only when observed fields justify a stricter rule.

**Interfaces:**
- Consumes: debug inspector from Task 5 and current Telegram/WhatsApp versions installed on a physical Android device.
- Produces: an evidence table and sanitized regression fixtures that decide whether the capture approach is viable for the full MVP.

- [ ] **Step 1: Install the debug build on a physical device**

Run: `./gradlew installDebug`  
Expected: Reply Later appears on the device and opens the initial screen.

- [ ] **Step 2: Grant notification-listener access**

Open Reply Later, navigate to Android notification-access settings, enable Reply Later, return to the app, and confirm the UI reports the listener as connected.

- [ ] **Step 3: Exercise the acceptance/rejection matrix**

Record pass/fail and field-presence booleans for:

```text
Telegram: one direct message; multiple messages from same contact; group; channel; hidden preview
WhatsApp: one direct message; multiple messages from same contact; group; bundled multi-chat summary; hidden preview
Lifecycle: source dismissed; app process killed; phone locked; listener toggled off/on
```

No real contact name or message text may be copied into the document or committed fixture. Replace them with deterministic values such as `CONTACT_A` and `MESSAGE_1`.

- [ ] **Step 4: Add sanitized regression fixtures before changing rules**

For every misclassification, first add a test reproducing the exact structural field combination with synthetic strings, run it to observe failure, then adjust the relevant parser with the narrowest rule that passes without weakening existing group rejection.

- [ ] **Step 5: Run the complete verification suite**

Run: `./gradlew testDebugUnitTest lintDebug assembleDebug`  
Expected: all tasks pass and every matrix row is either PASS or explicitly UNSUPPORTED because Android exposed insufficient information.

- [ ] **Step 6: Decide the spike gate**

The phase passes only if both messengers accept ordinary direct messages and reject tested groups without relying solely on localized strings. If either messenger cannot satisfy that condition, stop before persistence work and revise the product scope or capture interaction with the user.

- [ ] **Step 7: Commit and publish evidence**

```bash
git add docs/validation app/src/main/java/app/replylater/android/capture/parser app/src/test/java/app/replylater/android/capture/parser
git commit -m "test: validate messenger notification classification"
git push origin main
```

---

## Phase completion

After Task 6 passes, write the next implementation plan for encrypted Room persistence, explicit notification actions, exact/inexact scheduling, and reboot reconciliation. UI feature plans follow only after the end-to-end reminder domain is proven.
