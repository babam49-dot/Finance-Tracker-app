# Changelog - Ethiopian Finance Tracker

All notable changes and phase completions are documented here.

## [Phase 0 - Setup & Foundations] - Completed
- Initialized Gradle 8.11 Kotlin DSL build configuration with Version Catalog (`libs.versions.toml`).
- Configured Android SDK 35 (`compileSdk 35`, `targetSdk 35`, `minSdk 24`) and JDK 23 compatibility.
- Set up Android App namespace `com.example.ethfinance`.
- Configured Hilt Dependency Injection, Room Database with KSP annotation processor, Jetpack DataStore, and Compose Material 3.
- Implemented core offline permissions in `AndroidManifest.xml` (`RECEIVE_SMS`, `READ_SMS`, `POST_NOTIFICATIONS`, `USE_BIOMETRIC`, with NO INTERNET permission).
- Established multi-locale string resources for 6 languages (`en`, `am`, `om`, `ti`, `so`, `aa`) and `locales_config.xml`.
- Created foundational engineering documentation: `ASSUMPTIONS.md`, `PLAY_POLICY.md`, and `TRANSLATION_REVIEW.md`.

## [Phase 1 - Data Layer & Domain Models] - Completed
- Implemented Room database entities (`AccountEntity`, `TransactionEntity`, `CategoryEntity`, `UserRuleEntity`, `BudgetEntity`, `SyncStateEntity`), DAOs, and type converters.
- Implemented `MoneyFormatter` working strictly with santim integer units (1 ETB = 100 santim).
- Defined Domain models, Repository contracts, and Category seeding.

## [Phase 2 - Demo Mode & Financial Overview Home Screen] - Completed
- Built `DemoSeeder` populating sample accounts and deterministic transactions:
  - CBE Main Account: 6,194.34 ETB
  - Telebirr Wallet: 1,840.50 ETB
  - Cash Wallet: 2,800.00 ETB
  - Total Tracked = 10,834.84 ETB (Digital = 8,034.84 ETB, Cash = 2,800.00 ETB)
  - This month's cash flow matching exact screenshot values: Income +4,000.00 ETB, Expenses -676.26 ETB, Net Flow +3,323.74 ETB.
  - Review queue initialized with 2 items.
- Built `HomeScreen` with total tracked money card, review banner with count badge, 3-column cash flow card, mini insight, account cards with gold "SAMPLE" tags, and "Log Cash" floating action button.

## [Phase 3 - Transactions & Cash Logging] - Completed
- Built `TransactionsScreen` with real-time search, period chips (`7 days`, `30 days`, `This month`, `Last month`, `Custom`), summary strip (In, Out, Count), and grouped daily transactions with headers.
- Built `AddCashScreen` ("Log Cash") bottom sheet with amount input, quick amount chips (+10, +50, +100), type chips, category selector, and note editor.
- Built `TransactionDetailScreen` with parsed fields (balance after, reference, fee), exclusion toggle, and rule creation options.

## [Phase 4 - Reports & Custom Canvas Charts] - Completed
- Built `ReportsScreen` with custom Compose Canvas donut category chart (with center totals) and trend area chart with peak marker.
- Implemented local CSV export and PDF report generation via Android `PdfDocument` and `FileProvider` sharing.

## [Phase 5 - Review Queue & Rule Learning] - Completed
- Built `ReviewScreen` with Suggestions and Needs Category tabs, one-tap approval, ignore action, confidence indicators, and live bottom bar badge.

## [Phase 6 - Settings & 6 Languages Localization] - Completed
- Built `SettingsScreen` with demo/live mode switcher dialog, typography scaling (slider and bold numbers), calendar toggle (Gregorian vs. Ethiopian Ge'ez), currency symbol selection, and data clearing.
- Verified dynamic language switching across English, አማርኛ, Afaan Oromoo, ትግርኛ, Soomaali, and Qafaraf.

## [Phase 7 - Live Mode & Bank Parser Engine] - Completed
- Implemented `ParserEngine` with bank-specific regex parsers: `CbeParser`, `TelebirrParser`, `AwashParser`, `DashenParser`, `BoaParser`, and `GenericFallbackParser`.
- Implemented `SmsReceiver` with SHA-256 deduplication and account resolution.

## [Phase 8 - Polish, Unit Tests & Verification] - Completed
- Created launcher vector and adaptive icons.
- Added comprehensive unit tests in `MoneyFormatterTest`, `EthiopianCalendarTest`, and `BankParserTest`.
- All unit tests verified: `BUILD SUCCESSFUL`.
- Debug APK successfully compiled and packaged at: `app/build/outputs/apk/debug/app-debug.apk`.
