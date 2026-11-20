# Engineering Assumptions & Architectural Decisions

This document outlines all technical assumptions made while building **Ethiopian Finance Tracker** autonomously:

## 1. Money Representation & Precision
- **Santim Minor Units**: All monetary values are strictly stored as `Long` integers representing Ethiopian *santim* (1 ETB = 100 santim). Floats and doubles are forbidden in calculations to eliminate IEEE-754 floating point inaccuracies.
- **Amounts Directionality**: `amountMinor` in `TransactionEntity` is always stored as a positive integer. The transaction `type` (`INCOME`, `EXPENSE`, `TRANSFER_IN`, `TRANSFER_OUT`, `FEE`, `CASH_IN`, `CASH_OUT`) defines the flow direction.
- **Negative Balances**: Overdrafts and negative cash entries are supported by using signed `Long` for account `currentBalanceMinor`.

## 2. Sender Registry & Bank SMS Patterns
- **Known Bank Senders**: Default recognized senders are:
  - Commercial Bank of Ethiopia: `"CBE"`, `"CBEBirr"`, `"CBE-Birr"`, `"CBE Birr"`
  - Telebirr (Ethio telecom): `"127"`, `"telebirr"`, `"Telebirr"`
  - Awash Bank: `"AwashBank"`, `"Awash"`
  - Dashen Bank: `"DashenBank"`, `"Dashen"`
  - Bank of Abyssinia: `"BOA"`, `"AbyssiniaBank"`, `"BankOfAbyssinia"`
- **Generic Fallback**: Any financial message matching transfer/debit/credit keywords with an amount pattern from an unregistered sender is caught by `GenericFallbackParser` with low confidence (0.4) and routed to the Review Queue (`needsReview = true`).
- **Sender Extensibility**: The registry is backed by a bundled JSON asset (`senders.json`) combined with user-defined overrides in Room database.

## 3. Account Resolution & Deduplication
- **Account Identity**: An account is matched by `(sender, maskedNumber)`. If a newly encountered masked account number appears in an incoming SMS, an `AccountEntity` is automatically created with an identifiable title (e.g., `CBE Account ****8018`).
- **Deduplication Hash**: `SHA-256(sender.trim().uppercase() + ":" + normalizedBody + ":" + (receivedAt / 60000))` is enforced via a unique Room database index on `rawSmsHash`.
- **Internal Transfer Pairing**: A debit on one tracked account and a credit on another tracked account with identical amount within a 10-minute window are classified as `INTERNAL_TRANSFER` and flagged `isExcludedFromReports = true` to prevent double-counting.

## 4. Privacy & Offline Architecture
- **Zero Internet**: The application requests no network permissions (`android.permission.INTERNET` is omitted from `AndroidManifest.xml`).
- **SMS Body Retention**: Storing full raw SMS text is disabled by default. If enabled by the user in Settings, it is stored locally on-device.

## 5. Calendar & Numeral Systems
- Both Gregorian and Ethiopian Ge'ez calendar dates are supported. Conversion utilizes the astronomical Julian Day Number (JDN) formula with leap year Pagumē 6 calculations.
