# Google Play Store SMS & Privacy Policy Compliance

Google Play has strict policy restrictions regarding `RECEIVE_SMS` and `READ_SMS` permissions. This document specifies the architectural paths and distribution options for **Ethiopian Finance Tracker**:

## Distribution Strategies

### Option A: Sideloaded / Direct Distribution (F-Droid, GitHub Releases, APK)
- Full native access to `RECEIVE_SMS` and `READ_SMS`.
- Users download and install the signed APK directly.
- Maximum privacy: 100% offline, zero network access, on-device parsing only.

### Option B: Google Play Financial SMS Exception
- Google Play Developer Policy allows SMS permissions for apps that qualify under specific permitted use exceptions (e.g., Financial Tracker / Personal Finance Management in countries where financial transactions primarily take place via SMS/telecom notifications, such as Ethiopia, Kenya, and India).
- The declaration form must explicitly demonstrate:
  1. The core functionality relies on reading bank transactions from registered financial entities (CBE, Telebirr, Awash, Dashen, etc.).
  2. No general personal SMS messages are read or stored.
  3. No network permission (`INTERNET`) exists in the manifest, eliminating any data leakage risk.
  4. Complete local on-device operation.

### Option C: Notification Listener Architecture (Play Store Alternative)
- The ingestion architecture in Ethiopian Finance Tracker is abstracted behind the `MessageSource` interface:
  ```kotlin
  interface MessageSource {
      suspend fun startListening(onMessageReceived: (source: String, body: String, timestamp: Long) -> Unit)
  }
  ```
- This abstraction allows plugging in an `Android NotificationListenerService` that reads banking push notifications instead of SMS without touching or rewriting any parser engine logic or database schema.
