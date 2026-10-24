# MoneyTrail

**Follow every rupee.** MoneyTrail is an Android personal finance app for people with irregular, multi-source income. Log income and spending in a few taps on a calendar, see where the money went, and track progress towards a savings goal.

Built for SE3092 Platform Based Development, Assignment 01 (2026), with Kotlin, Jetpack Compose, MVVM, Hilt and Firebase.

## Contents

1. [The problem it solves](#the-problem-it-solves)
2. [Features](#features)
3. [Tech stack](#tech-stack)
4. [Getting started](#getting-started)
5. [Firebase setup](#firebase-setup)
6. [Firestore data model](#firestore-data-model)
7. [Security rules](#security-rules)
8. [Architecture](#architecture)
9. [Project rules](#project-rules)
10. [Adding a new feature](#adding-a-new-feature)
11. [Git workflow](#git-workflow)
12. [Build, run and test](#build-run-and-test)
13. [Troubleshooting](#troubleshooting)

## The problem it solves

The app is designed around **Kavindu Silva**, a 25-year-old junior software engineer in Colombo from the assignment scenario:

- His income comes from four places: a salary that changes with bonuses (LKR 120,000–140,000), freelance projects paid by milestone, Google AdSense paid in USD, and crypto trading.
- His spending is spread across cards, cash, rides, food delivery and subscriptions, and he can't say where it goes.
- He wants a MacBook Pro M4 (about LKR 490,000) within 12 months, but has only LKR 11,200 saved.
- Every tool he tried failed within two weeks: too much typing, USD only, no sync, or one income source only.

So MoneyTrail keeps logging fast (a calendar, a keypad and category chips), sorts spending into **committed** (rent, bills) and **discretionary** (coffee, rides) by itself, and ties everything back to the goal.

## Features

**Built**

| Area | What it does | Figma screen |
| --- | --- | --- |
| Splash and welcome | Brand splash, then a 3-slide welcome for signed-out users | 01, 02 |
| Sign up and log in | Email and password, with Firestore user profiles | 03, 04 |
| Google sign-in | Credential Manager account picker, then Firebase Auth | 03 |
| Forgot password | Firebase reset email, then a "check your email" screen | 05 |
| Onboarding | Four steps after sign-up: money places, pay day and base salary, income sources, and a savings goal showing the amount to save each month. Places and ways of earning that aren't listed can be added | 06 (steps 2–4 follow its style) |
| Calendar | Month grid with income, expense and expected markers, Income / Expenses / Expected totals, the selected day's entries, updated live from Firestore | 07 |
| New entry | Expense or income, amount keypad, category, account (from onboarding), date and note. **+ New** adds a category of your own | 08 |
| Edit entry | Tap an entry to change its amount, category, account, committed or discretionary, date and note, or delete it after a confirm dialog | 24 |
| Folders | Expenses and Income for a month, each category's total with a bar against the biggest, and the six earlier months to switch to; updated live | 09 |
| Expected entries | Plan income or bills for a date (e.g. a freelance payment due on the 14th). On the day, the calendar asks "Did it come in?": confirm with the real amount, move it with Not yet, or remove it. Overdue ones keep showing until checked | 07, 08, 09 |
| Salary on the calendar | Each month the base salary is added as an expected Salary entry on pay day. The salary is a fixed base (changed in Settings) plus a bonus that varies: confirming it asks for the base received and this month's bonus, and opening the salary later lets you add or change the bonus. The calendar shows "incl. 15,000 bonus" | 07, 24 |
| Expense insights | This month's spending, the committed vs discretionary split, coffee and dining out as days of goal saving, spending by category, and a six-month chart | 10 |
| More | Profile, Settings and sign-out | 12 (first part) |
| Settings | Change the pay day and base salary (this month's expected salary follows), and list, add or delete your own categories | Not in Figma; follows the onboarding style |

**Next, in order:** goal progress, AdSense in USD (multi-currency), income insights, recurring bills, then freelance projects, monthly close and crypto import if time allows.

## Tech stack

| Concern | Choice |
| --- | --- |
| Language | Kotlin only (Kotlin DSL for Gradle) |
| UI | Jetpack Compose + Material 3. **No XML** except `AndroidManifest.xml` |
| Architecture | MVVM with a domain layer (use cases and repository interfaces) |
| State | `StateFlow` exposed read-only from each ViewModel |
| Async | Kotlin coroutines, `viewModelScope` |
| Dependency injection | Hilt (with KSP) |
| Navigation | Navigation Compose, single activity |
| Auth | Firebase Authentication: email/password and Google (Credential Manager) |
| Database | Cloud Firestore with its persistent offline cache |
| Local cache | Room: a copy of the entries on the phone, kept up to date by Firestore listeners |
| Min / target SDK | 26 (Android 8.0) / 36; compiled against SDK 37 |
| Build | Gradle 9.5, Android Gradle Plugin 9.3, Kotlin 2.2, JDK 21 toolchain |

Library versions live in `gradle/libs.versions.toml`.

## Getting started

### 1. Install

- **Android Studio**, a recent version that supports Android Gradle Plugin 9.3. An older version will ask you to update when you open the project.
- **Android SDK Platform 37** (Android Studio offers to install it on first sync).
- **JDK 21**: Gradle downloads it automatically through the toolchain settings. Nothing to install by hand.
- An **emulator with Google Play** (API 26 or higher), or an Android phone. Google sign-in needs a Google account signed in on the device.

### 2. Get the code

```bash
git clone https://github.com/NethmikaKekuu/MoneyTrail.git
cd MoneyTrail
git switch develop
```

Open the `MoneyTrail` folder in Android Studio (**File → Open**), trust the project and wait for the Gradle sync to finish.

### 3. Get Firebase access

The app already points at the shared Firebase project **`moneytrail-se3092`**; `app/google-services.json` is in the repository, so the app builds and email login works straight away.

Ask the project owner to:

1. Add you in the Firebase console under **Project settings → Users and permissions**, so you can see the database and rules.
2. Add **your** SHA-1 fingerprint (next section), otherwise Google sign-in fails on your machine.

### 4. Add your SHA-1 for Google sign-in

Every computer has its own debug signing key, and Google sign-in only works for keys Firebase knows about. Get yours with:

```bash
# Windows PowerShell
.\gradlew signingReport

# macOS, Linux or Git Bash
./gradlew signingReport
```

Copy the `SHA1` line under `Variant: debug`. You can also read it straight from the debug keystore:

```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

(On Windows the keystore is at `%USERPROFILE%\.android\debug.keystore`.)

Then in the Firebase console: **Project settings → General → Your apps → `lk.moneytrail.app` → Add fingerprint**, paste it and save. If Firebase offers an updated `google-services.json` afterwards, download it into `app/` and commit it.

### 5. Run

Pick the emulator or phone and press **Run**. The app opens on the splash screen, then Welcome. Create an account or use **Continue with Google**.

## Firebase setup

This is what the shared project already has. Use it to check the setup, or to make your own Firebase project for testing.

| Firebase console section | Setting |
| --- | --- |
| Project | `moneytrail-se3092` |
| Android app | package name `lk.moneytrail.app`, every developer's debug SHA-1 added |
| Authentication → Sign-in method | **Email/Password** enabled, **Google** enabled (with a support email) |
| Firestore Database | Created in production mode |
| Firestore → Rules | The contents of `app/firestore.rules`, published |
| Firestore → Indexes | None needed yet (see below) |

### Publishing the security rules

The rules live in **`app/firestore.rules`** and are part of the code. Change them in the file, then publish:

1. Open **Firestore Database → Rules** in the Firebase console.
2. Select everything in the editor and paste the whole contents of `app/firestore.rules`.
3. Click **Publish**. Rules take about a minute to apply.

Whenever a pull request changes `app/firestore.rules`, publish the new version once it is merged.

### Indexes

Current queries filter and sort on one field (`date`), which Firestore indexes automatically. A query that filters on one field and sorts on another (for example `type` and `date` for insights) needs a **composite index**. Running the query once prints a link in Logcat that creates it. Add the index, then note it in the data model section below.

### Using your own Firebase project

1. Create a Firebase project and add an Android app with package name `lk.moneytrail.app` and your SHA-1.
2. Download its `google-services.json` into `app/`. Don't commit it, or you'll replace the team's file.
3. Enable Email/Password and Google sign-in, create a Firestore database, and publish `app/firestore.rules`.

The Gradle plugin turns `google-services.json` into `R.string.default_web_client_id`, which Google sign-in uses. Google sign-in only works if the file contains a **Web client** (OAuth client type 3), which Firebase creates when the Google provider is enabled.

## Firestore data model

Everything a user owns sits under their own document, so one rule (`request.auth.uid == uid`) protects it all.

```
users/{uid}                       profile
├── entries/{entryId}             one income or expense on the calendar
├── accounts/{accountId}          a place money sits, picked in onboarding
├── categories/{categoryId}       a category the user added, e.g. "Pet care"
└── settings/plan                 pay day, salary, income sources and the savings goal
```

### `users/{uid}`

Written once when the account is made: on email sign-up, or the first Google sign-in.

| Field | Type | Notes |
| --- | --- | --- |
| `name` | string | Up to 100 characters. May be empty for a Google account without a display name |
| `email` | string | Up to 254 characters |
| `createdAt` | timestamp | Set by the server (`FieldValue.serverTimestamp()`), never changed |

### `users/{uid}/entries/{entryId}`

| Field | Type | Notes |
| --- | --- | --- |
| `status` | string | `CONFIRMED` (money that moved) or `EXPECTED` (planned for its date). Entries saved before this field existed are read as confirmed |
| `type` | string | `INCOME` or `EXPENSE` |
| `amount` | integer | Whole rupees (LKR), 1 to 999,999,999. Always positive; `type` gives the direction |
| `category` | string | 1–50 characters: a built-in one from `domain/model/Categories.kt` or one of the user's own |
| `account` | string | 1–50 characters, e.g. `Main bank`, `Cash wallet` |
| `date` | string | `yyyy-MM-dd`, the day the money moved |
| `note` | string | Up to 200 characters, may be empty |
| `spendingType` | string or null | Expenses: `COMMITTED` or `DISCRETIONARY`, set from the category. Income: `null` |
| `bonus` | integer | For a Salary income: the part of `amount` that was a bonus on top of the base pay. Always less than `amount`; 0 for every other entry. Entries saved before this field existed are read as 0 |
| `createdAt` | timestamp | Set by the server; orders entries within a day |

**Why it is shaped this way**

- **The date is a string.** `yyyy-MM-dd` strings sort in date order, so a whole month is one range query (`date >= "2026-10-01"` and `date <= "2026-10-31"`) on one field, with no time-zone surprises and no composite index. Folders uses the same query over seven months.
- **Amounts are whole rupees as integers**, so there are no floating-point rounding errors. Multi-currency fields (`currency`, `originalAmount`, `rate`) will be added for AdSense income in USD.
- **Expected money is kept apart.** Totals, folders and insights count only `CONFIRMED` entries, so planned money never makes the real numbers look better. The calendar finds expected entries with one equality query on `status`, which needs no composite index.
- **A salary's bonus lives on the salary.** `amount` is always the whole sum, so totals, folders and insights need nothing extra, while `bonus` keeps the base and the extra told apart (`Entry.baseAmount`).
- **`spendingType` is stored, not worked out each time**, so Edit entry can override the category's default (a Rent payment marked discretionary stays that way).
- **Live and offline.** Snapshot listeners copy entries into Room, and the screens read Room, so new entries appear at once and the calendar opens without internet. Firestore's persistent cache is turned on in `di/FirebaseModule.kt`, so entries also save without internet and sync later. See [Offline cache](#offline-cache).

### `users/{uid}/accounts/{accountId}`

Written at the end of onboarding, one document per place the user ticked, all in one batch. The id is a short slug: `main-bank`, `secondary-bank`, `cash-wallet`, `savings`, `binance`. A place the user added gets a slug of its name, such as `sampath-bank` (`AccountTemplates.custom`).

| Field | Type | Notes |
| --- | --- | --- |
| `name` | string | 1–50 characters, e.g. `Main bank`. Entries store this name in their `account` field |
| `description` | string | Up to 100 characters, e.g. `Card and salary account` |
| `kind` | string | `BANK`, `CASH`, `SAVINGS` or `CRYPTO` |
| `createdAt` | timestamp | Set by the server |

### `users/{uid}/categories/{categoryId}`

Categories the user added on top of the built-in ones: from **+ New** on the new entry screen, from Settings, or as extra ways of earning in onboarding step 3. They're offered after the built-in ones on New entry and Edit entry (`Categories.namesFor(type, custom)`). Deleting one doesn't change entries already saved with it.

| Field | Type | Notes |
| --- | --- | --- |
| `name` | string | 1–30 characters, not the same as a built-in or another of the user's categories of that type (checked in `AddCategoryUseCase`) |
| `type` | string | `INCOME` or `EXPENSE` |
| `spendingType` | string or null | Expenses: `COMMITTED` or `DISCRETIONARY`, the default for new entries in it. Income: `null` |
| `createdAt` | timestamp | Set by the server |

### `users/{uid}/settings/plan`

One document, written at the end of onboarding and changed from Settings. The goal is a map inside it, so one read gives a screen everything it needs about the plan.

| Field | Type | Notes |
| --- | --- | --- |
| `payDay` | integer | Day of the month the salary arrives, 1–31 |
| `baseSalary` | integer | The fixed part of the salary in LKR, before bonuses; 0 when there is no salary |
| `incomeSources` | array of strings | Any of `SALARY`, `FREELANCE`, `ADSENSE`, `CRYPTO`, `OTHER` |
| `goal.name` | string | 1–60 characters, e.g. `MacBook Pro M4` |
| `goal.targetAmount` | integer | LKR, 1 to 999,999,999 |
| `goal.savedAmount` | integer | LKR already saved before using the app; never more than the target |
| `goal.targetDate` | string | `yyyy-MM-dd` |
| `updatedAt` | timestamp | Set by the server on every save |

The monthly amount to save is worked out when needed (`SavingsGoal.monthlySavingNeeded`), not stored, so it stays right as the months pass.

**Salary as expected income.** When the calendar opens, `EnsureMonthlySalaryUseCase` adds this month's salary as an `EXPECTED` Salary entry on pay day (a pay day of 31 falls on the last day of shorter months). It does nothing if there's no salary, if this month already has a Salary entry, or once pay day has passed, so a salary removed with "Didn't happen" doesn't come back. Changing the salary in Settings (`UpdateSalaryUseCase`) moves a still-expected salary to the new base and day, keeping any bonus already on it. A bonus is added to the salary entry itself: in the "Did it come in?" dialog (`Entry.confirmedSalary`) or later on Edit entry, which shows Base salary and Bonus for a Salary income.

## Security rules

`app/firestore.rules` enforces:

- A user can only read and write data under their own `users/{uid}`.
- The profile may only hold `name`, `email` and `createdAt`; `createdAt` must be the server time on create and can't change later.
- Every entry is checked field by field: allowed keys only, `status`, `type` and `spendingType` from fixed lists, amount range, a `bonus` below the amount and only on a Salary income, `yyyy-MM-dd` date, note length, server `createdAt`.
- Each account must have a name, a description and a `kind` from a fixed list, with a server `createdAt`.
- The plan must have a pay day of 1–31, a salary of 0 or more, income sources from a fixed list, and a goal whose saved amount is no more than its target.
- Each category must have a 1–30 character name and a `type`; expense categories need a `spendingType`, income ones must have `null`. Categories can be created and deleted, not changed.
- Any other collection under a user is owner-only. Entries, accounts, settings and categories are excluded from that catch-all, so their checks can't be skipped.

**Every new collection needs its own validation function and match block**, in the same style as `isValidEntry`.

## Architecture

```
Compose screen  →  ViewModel  →  use case (optional)  →  repository interface  →  Firebase implementation
 (presentation)   (presentation)       (domain)                (domain)                   (data)
     ↑                 │
     └── StateFlow ────┘
```

- **Screens** only draw state and pass user actions to the ViewModel. No business logic or Firebase calls in composables.
- **ViewModels** (`@HiltViewModel`) hold a private `MutableStateFlow<XState>` and expose it as a read-only `StateFlow`. All async work runs in `viewModelScope`.
- **Use cases** hold rules that are more than a pass-through, e.g. `AddEntryUseCase` validates the amount and picks committed or discretionary; `RegisterUseCase` makes the account and profile together.
- **Repositories**: interfaces in `domain/repository`, Firebase code in `data/repository`, bound in `di/RepositoryModule.kt`. They return `Result<T>`; a failure is an `AppException` whose message can be shown to the user as-is.

### Offline cache

```
add / edit / delete ──▶ Firestore (queues writes made offline)
                            │ snapshot listener
                            ▼
                     Room "entries" table ──Flow──▶ ViewModel ──▶ screen
```

- Entries are read from **Room** (`data/local/`), the database on the phone, so Calendar, Folders and Insights open at once with the last copy, with or without internet. Room's `Flow` queries emit again whenever the table changes.
- While a screen is open, a Firestore snapshot listener copies its dates into Room (`EntryDao.replaceBetween`), so entries added, changed or deleted on another phone show up too.
- Writes still go to Firestore. Its persistent cache applies them on the phone at once and uploads them when the connection is back. The listener sees them straight away, so they reach Room the same way.
- Only entries are in Room: they grow every day and every main screen reads them. Accounts, categories and the plan are small and stay on Firestore's cache.
- Room only holds a copy, so after a schema change, raise the version in `MoneyTrailDatabase`; Room drops the table (`fallbackToDestructiveMigration` in `di/DatabaseModule.kt`) and the listeners fill it again.
- Each row has the user's `uid` and every query filters on it, so another account on the same phone never sees it.
- To look at the table while the app runs, open **App Inspection → Database Inspector** in Android Studio.

### Packages

```
app/src/main/java/lk/moneytrail/app/
├── MainActivity.kt, MoneyTrailApp.kt
├── data/
│   ├── local/            Room: EntryEntity (the entries table), EntryDao, MoneyTrailDatabase
│   └── repository/       Firebase implementations + shared Firestore error messages and write helper
├── di/                   Hilt modules: FirebaseModule, DatabaseModule, RepositoryModule
├── domain/
│   ├── model/            Entry, Categories, CustomCategory, Account, MoneyPlan, EntryTotals, FolderTotals, SpendingInsights, UserProfile, AuthUser, AppException
│   ├── repository/       Auth, User, Entry, Account, Plan and Category repositories (interfaces)
│   └── usecase/          RegisterUseCase, SignInWithGoogleUseCase, AddEntryUseCase, UpdateEntryUseCase,
│                         CompleteOnboardingUseCase, AddCategoryUseCase, EnsureMonthlySalaryUseCase, UpdateSalaryUseCase
└── presentation/
    ├── auth/             Login, Register, Forgot password, Google sign-in helper
    ├── calendar/         Calendar screen, month grid logic
    ├── components/       Logo, icons (MoneyTrailIcons), money formatting, loading/error/empty views, AddCategoryDialog
    ├── dashboard/        Bottom-bar home: Calendar, Folders, Insights, More
    ├── entry/            New entry (with keypad) and Edit entry screens
    ├── folders/          Expenses and Income folders by category and month
    ├── insights/         Expense insights (income insights to come)
    ├── more/             Profile, Settings link and sign-out
    ├── navigation/       Routes and the NavHost
    ├── onboarding/       Four-step onboarding: money places, salary, income sources, goal
    ├── settings/         Salary and pay day, the user's own categories
    ├── splash/, welcome/
    └── theme/            Colours, Sora and DM Sans typography, MoneyTrailTheme
```

### Navigation

| Route | Screen |
| --- | --- |
| `splash` | Start screen; sends signed-in users to `dashboard`, others to `welcome` |
| `welcome` | 3-slide intro with Create account / Log in |
| `login`, `register`, `forgot_password` | Auth screens |
| `onboarding` | After the first sign-up or first Google sign-in; Skip goes straight to the dashboard |
| `dashboard` | Bottom-bar home (calendar first) |
| `add_entry?date={yyyy-MM-dd}` | New entry, starting on the given day |
| `edit_entry/{entryId}` | Edit or delete one entry, opened by tapping it on the calendar |
| `settings` | Salary and categories, opened from More. For someone who skipped onboarding it links to `onboarding` |

Signing in or out clears the back stack, so Back can't cross between signed-in and signed-out screens.

## Project rules

Follow these on every change. Reviewers check them in pull requests.

1. **Layer-first packages.** Code goes into `data/`, `domain/`, `presentation/` or `di/`. Don't create new top-level packages.
2. **`domain/` is plain Kotlin.** No Android, Firebase or Compose imports there.
3. **One folder per feature in `presentation/`**, holding `XScreen.kt`, `XViewModel.kt` and `XState.kt`.
4. **Screen structure.** `XScreen` gets the ViewModel with `hiltViewModel()` and collects state with `collectAsStateWithLifecycle()`. It calls a private, stateless `XContent(uiState, callbacks…)`, which has a `@Preview`.
5. **No XML.** Icons are `ImageVector`s in `presentation/components/MoneyTrailIcons.kt`; colours and fonts are in `presentation/theme/`; text is in Kotlin. `AndroidManifest.xml` is the only XML file.
6. **Use the theme**, `MaterialTheme.colorScheme` and `MaterialTheme.typography`, not hard-coded colours, so dark mode works.
7. **Every screen handles loading, empty and error states.** No blank screens on network failure and no crashes on empty data. Reuse `LoadingView`, `ErrorView` and `EmptyView`.
8. **User-facing errors are `AppException` messages**, written as plain sentences.
9. **Firestore changes come with rules.** A new collection or field means updating `app/firestore.rules` and the data model section of this README in the same pull request.
10. **Dependencies:** add them only when a feature needs them, through `gradle/libs.versions.toml`, and don't bump versions casually. For example, `googleid` stays at **1.1.1**, because 1.2.x is built with Kotlin 2.4 and the project uses Kotlin 2.2.
11. **No `!!`.** Handle nulls explicitly.
12. **Dates and money:** use `LocalDate` and `YearMonth`, `Locale.ENGLISH` for date text, and `formatAmount` / `formatNet` for rupees.

## Adding a new feature

A worked order, using "Folders" as the example:

1. **Branch** from the latest `develop`: `git switch develop`, `git pull`, `git switch -c feature/folders`.
2. **Domain:** add models in `domain/model/`, add functions to a repository interface (or a new one) in `domain/repository/`, and add a use case in `domain/usecase/` if there are rules beyond loading data.
3. **Data:** implement the repository in `data/repository/` (use `toFirestoreMessage(...)` for errors), and bind it in `di/RepositoryModule.kt`.
4. **Rules:** if Firestore changes, update `app/firestore.rules` and the data model section above.
5. **Presentation:** create `presentation/folders/` with `FoldersScreen.kt`, `FoldersState.kt` and `FoldersViewModel.kt`, following the Figma screen.
6. **Navigation:** add a route in `Routes.kt` and a `composable(...)` in `MoneyTrailNavHost.kt`, or a tab in `DashboardScreen.kt`.
7. **Tests:** unit-test the logic in `app/src/test/` (see `CalendarDaysTest`, `AddEntryUseCaseTest`).
8. **Check:** build and run the tests (next sections), and try the screen on an emulator.
9. **Commit in small steps** and open a pull request into `develop`.

## Git workflow

| Branch | Purpose |
| --- | --- |
| `main` | Submitted, working versions. Protected: changes only through pull requests |
| `develop` | Integration branch. Every feature merges here first |
| `feature/<name>` | One feature, e.g. `feature/folders` |
| `fix/<name>`, `docs/<name>` | Bug fixes and documentation |

- **Commits:** small, one step each, with an imperative message, e.g. `Add folder totals to the domain layer`. The assignment checks for a real, iterative history.
- **Pull requests:** `feature/*` → `develop`, then `develop` → `main` when a set of features is ready. Merge with **Create a merge commit**, not Squash or Rebase, so the commits stay visible.
- **Stay up to date:** `git switch develop` and `git pull` before starting a new branch.

## Build, run and test

Use `.\gradlew` in Windows PowerShell and `./gradlew` in macOS, Linux or Git Bash.

| Task | Command |
| --- | --- |
| Build the debug APK | `./gradlew assembleDebug` |
| Run unit tests | `./gradlew testDebugUnitTest` |
| Build the instrumented test APK | `./gradlew assembleDebugAndroidTest` |
| Show signing keys (SHA-1) | `./gradlew signingReport` |

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Before submitting, install it on an **Android 8.0 (API 26)** emulator and check the main flows: sign up, add entries, see them on the calendar, sign out.

Unit tests cover entry totals, adding and editing entries, expected entries, folder totals, spending insights, the calendar month grid, the keypad rules, the onboarding checks (including added places and income), the user's own categories, the monthly salary, salary changes and salary bonuses, the monthly saving needed for the goal, and how entries are saved to and read from Room.

## Troubleshooting

| You see | Fix |
| --- | --- |
| `File google-services.json is missing` | You opened the wrong project, or `app/google-services.json` is missing. Pull `develop` again |
| "Google sign-in isn't set up for this build. Add its SHA-1 fingerprint in the Firebase console." | Add your debug SHA-1 ([step 4](#4-add-your-sha-1-for-google-sign-in)) |
| "No Google account found on this phone." | Add a Google account in the device's Settings → Accounts. Emulators need a Google Play system image |
| "Google sign-in isn't turned on in the Firebase console." / "Email/Password sign-in isn't turned on…" | Enable the provider under Authentication → Sign-in method |
| "Firestore blocked this request. Check the Rules tab…" | The published rules don't allow the write. Publish `app/firestore.rules`, and make sure new fields are allowed in it |
| "The Firestore database hasn't been created yet…" | Create the Firestore database in the console |
| "Can't reach Firestore. Check your internet connection…" | The device is offline and the data isn't cached yet |
| Build fails with "compiled with an incompatible version of Kotlin" | A library was upgraded past what Kotlin 2.2 can read. Go back to the version in `libs.versions.toml` (e.g. `googleid` 1.1.1) |
| Gradle sync fails on a fresh machine | Let Android Studio install SDK Platform 37 and the build tools it asks for, then sync again |

## Maintainer

Kekulanthale K. M. N. Y (IT23657496)
