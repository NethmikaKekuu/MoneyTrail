<img width="2400" height="744" alt="Untitled design (12)" src="https://github.com/user-attachments/assets/8144d0ce-034f-4542-8737-3c95dae12409" />


# MoneyTrail — Personal Finance Management App

MoneyTrail is an Android personal finance app built with Kotlin and Jetpack Compose. It is made for someone whose income doesn't arrive the same way twice: a salary with a changing bonus, freelance projects paid by milestone, Google AdSense paid in USD, and crypto trading.

The app keeps planned money apart from real money, splits the salary into its dependable base and variable bonus, records AdSense in USD next to the rupees that actually landed, ties savings goals to pay day, and makes a missed week easy to catch up on.

MoneyTrail uses Firebase Authentication and Cloud Firestore, with a Room cache and Firestore's persistent cache so it keeps working offline, and WorkManager for daily reminders.

---

## Key Features

| Feature | Description |
|---|---|
| **User Authentication** | Email/password registration and login, Google Sign-In, and password reset. |
| **Onboarding** | Set pay day, base salary, income sources, accounts and a first savings goal. |
| **Calendar Home** | A month calendar with markers, the day's entries and the month's totals. |
| **Fast Entry** | Keypad-first income and expense entry; edit and delete any entry. |
| **Expected Transactions** | Plan income or bills for a date; on the day, confirm with the real amount, move or remove them. |
| **Salary and Bonus** | The expected salary appears on pay day; anything above the base is stored as the bonus. |
| **AdSense in USD** | Record the dollars and the rupees that arrived; the effective rate and fee are worked out. |
| **Pay Yourself First** | After the salary is confirmed, the app asks to move the month's saving to the goal. |
| **Savings Goals** | Several goals with target dates and the monthly saving needed to reach them. |
| **Expense Insights** | Committed vs discretionary spending, top categories, a six-month chart, dining as days of goal saving. |
| **Income Insights** | Income by source, and dependable vs variable income. |
| **Folders** | Totals grouped by category and account. |
| **Freelance Projects** | Projects with a price and milestones; mark invoiced, record payments, see overdue invoices. |
| **Recurring Bills** | Subscriptions and auto-debits that appear on the calendar every month, ready to mark as paid. |
| **Catch-up and Balancing** | After a few days away, balance each account; any difference is saved as "Not logged". |
| **Monthly Close and Statement** | Close a month with real balances, see the statement, and export it as CSV. |
| **Notifications** | A daily 9:00 check for money due, pay yourself first, catch-up and overdue invoices, with an in-app notification list. |
| **Custom Categories** | Add your own income and expense categories. |
| **Offline Support** | Cached data shows offline; changes sync when the connection returns. |
| **Light and Dark Theme** | Material 3 theme in light, dark or system mode. |

## Technologies Used

| Technology | Purpose |
|---|---|
| Kotlin 2.2.10 | Primary programming language |
| Jetpack Compose (BOM 2026.02.01) | Declarative Android UI |
| Material Design 3 | UI components and design system |
| MVVM | Presentation architecture |
| Clean Architecture | Separation of presentation, domain and data |
| Navigation Compose 2.9.8 | Single-activity screen navigation |
| Firebase Authentication (BOM 34.17.0) | User authentication |
| Cloud Firestore (BOM 34.17.0) | Cloud database and real-time listeners |
| Room 2.8.5 | Offline cache of entries and the notification history |
| WorkManager 2.11.2 | Daily background reminder check |
| Android Credential Manager 1.6.0 | Google Sign-In |
| Hilt 2.60.1 | Dependency injection |
| Kotlin Coroutines 1.10.2 | Asynchronous work |
| StateFlow | Observable UI state |
| KSP 2.3.12 | Annotation processing for Hilt and Room |
| Gradle Kotlin DSL (AGP 9.3.3) | Build configuration |
| JUnit 4 | Unit testing |
| Espresso 3.7.0 / Compose UI Testing | Android UI testing |

## Architecture Overview

MoneyTrail follows **Clean Architecture combined with MVVM**. Screens only draw state, ViewModels hold it, and the rules live in testable use cases and pure functions in the domain layer.

<img width="2816" height="1536" alt="Gemini_Generated_Image_zfk153zfk153zfk1" src="https://github.com/user-attachments/assets/e7bfa00b-9cf2-4273-ac85-fe9129c90a61" />

### Architecture Layers

**1. Presentation Layer**

Jetpack Compose screens, each with a `Screen`, a `ViewModel` and a `State` class.

- Displays screens and handles user actions.
- Exposes state as `StateFlow` (private `MutableStateFlow` + `asStateFlow()`).
- Shows loading, empty and error (with Retry) states on every screen.
- Contains no business logic; totals and percentages are properties on the state classes.

**2. Domain Layer**

The business rules, independent of Android, Firebase and the UI.

- Models such as `Entry`, `Account`, `MoneyPlan`, `Project`, `RecurringBill` and `MonthClose`.
- Repository interfaces.
- Use cases, for example:
  - `AddEntryUseCase`, `UpdateEntryUseCase`
  - `CompleteOnboardingUseCase`, `EnsureMonthlySalaryUseCase`
  - `BalanceAccountUseCase`, `CloseMonthUseCase`
  - `EnsureRecurringBillsUseCase`, `GetRemindersUseCase`

**3. Data Layer**

Implements the repository interfaces.

- Firebase Authentication and Cloud Firestore repositories.
- Room database for the entries cache and the notification history.
- WorkManager reminder scheduling and notifications.
- USD exchange-rate lookup and CSV file export.

**4. Dependency Injection Layer**

Hilt modules bind each repository interface to its implementation and provide Firebase, Room and WorkManager.

### Project Structure

```text
lk.moneytrail.app/
│
├── domain/
│   ├── model/        Entry, Account, MoneyPlan, GoalModel, Project, RecurringBill,
│   │                 MonthlyClose, IncomeInsights, SpendingInsights, UsdConversion, Reminder…
│   ├── repository/   Auth, User, Entry, Account, Plan, Category, Freelance,
│   │                 RecurringBill, MonthClose, NotificationLog, UsdRate, Connectivity…
│   └── usecase/      AddEntry, UpdateEntry, CompleteOnboarding, EnsureMonthlySalary,
│                     BalanceAccount, CloseMonth, EnsureRecurringBills, GetReminders…
│
├── data/
│   ├── repository/   Firestore* repositories, FirebaseAuthRepository, HttpUsdRateRepository
│   ├── local/        Room entries cache
│   ├── notifications/ Room notification history
│   ├── reminders/    ReminderWorker, ReminderNotifier, WorkManager scheduler
│   ├── files/        CSV export through the Storage Access Framework
│   ├── preferences/  Theme preference
│   └── remote/       Exchange-rate API
│
├── di/               FirebaseModule, DatabaseModule, RepositoryModule, …
│
└── presentation/
    ├── auth/  welcome/  splash/  onboarding/
    ├── dashboard/  calendar/  entry/  usd/
    ├── insights/  folders/  goal/  salary/
    ├── freelance/  bills/  catchup/
    ├── monthlyclose/  statement/  notifications/
    ├── more/  settings/  navigation/
    ├── components/   shared top bar, chips, state views, icons
    └── theme/
```

### Application Data Flow

**User actions:**

```text
Jetpack Compose UI
        ↓
    ViewModel
        ↓
    Use Case
        ↓
   Repository
        ↓
   Firestore
```

**UI updates:**

```text
   Firestore (snapshot listener)
        ↓
   Room cache
        ↓
   Repository (Flow)
        ↓
    ViewModel (StateFlow)
        ↓
Jetpack Compose UI
```

The presentation layer never calls Firebase directly.

## Database Structure

Cloud Firestore is the primary database. Every record is stored under the signed-in user's Firebase UID.

### Firestore Collection Structure

```text
users/
 └── {uid}/                    profile: name, email, createdAt
      ├── entries/{entryId}         one income or expense
      ├── accounts/{accountId}      a place money sits (main-bank, cash-wallet…)
      ├── categories/{categoryId}   a category the user added
      ├── settings/plan             pay day, salary, income sources, goals
      ├── projects/{projectId}      a freelance project with its milestones
      ├── recurringBills/{billId}   a bill due on the same day every month
      └── closes/{yyyy-MM}          a closed month's real balances
```

### Collection Details

| Collection | Description |
|---|---|
| `users/{uid}` | User profile. |
| `users/{uid}/entries` | Income, expenses and expected transactions. |
| `users/{uid}/accounts` | The user's accounts and their last checked balance. |
| `users/{uid}/categories` | Custom categories (built-in ones live in the app). |
| `users/{uid}/settings/plan` | Pay day, base salary, income sources and savings goals. |
| `users/{uid}/projects` | Freelance projects with their price and milestones. |
| `users/{uid}/recurringBills` | Subscriptions and auto-debits. |
| `users/{uid}/closes` | Closed months with each account's real balance. |

### Important Data Fields

**Entries**

- `status` — `CONFIRMED` or `EXPECTED`
- `type` — `INCOME` or `EXPENSE`
- `amount` — whole rupees (LKR), always positive
- `bonus` — the part of a salary above the base pay (0 otherwise)
- `category`, `account`, `note`
- `date` — `yyyy-MM-dd`
- `spendingType` — `COMMITTED` or `DISCRETIONARY` for expenses
- `usdCents`, `effectiveRate`, `feeLkr` — for AdSense payouts in USD
- `createdAt` — server timestamp

**Accounts**

- `name`, `description`
- `kind` — `BANK`, `CASH`, `SAVINGS` or `CRYPTO`
- `checkedBalance`, `checkedOn` — the real balance from the last check
- `createdAt`

**Budget Plan**

- `payDay`, `baseSalary`, `incomeSources`
- `goal` / `goals` — name, target amount, saved amount, target date
- `savingsMovedMonth`, `savingsRemindOn` — pay yourself first

Amounts are stored as whole rupees in integers, so there are no floating-point rounding errors. USD is kept in cents beside the rupees that arrived.

### Security Rules

`app/firestore.rules` lets each user read and write only their own data, and validates every collection's fields (types, ranges, allowed keys and server timestamps). The rules are tested on the Firestore emulator in `firestore-rules-tests/` and deployed with `npx firebase-tools deploy --only firestore:rules`.

## Authentication and Firebase Configuration

### Supported Authentication Methods

- Email and password, with password reset emails.
- Google Sign-In through Android Credential Manager.

Firebase keeps the session, so returning users go straight to the dashboard.

### Firebase Configuration

| Setting | Configuration |
|---|---|
| Firebase Project | `moneytrail-se3092` |
| Android Package Name | `lk.moneytrail.app` |
| Authentication Providers | Email/Password and Google |
| Cloud Database | Cloud Firestore |
| Configuration File | `app/google-services.json` (included in the repository) |

**To use your own Firebase project:**

1. Create a Firebase project and add an Android app with package `lk.moneytrail.app`.
2. Add your SHA-1 fingerprint (`./gradlew signingReport`) for Google Sign-In.
3. Download `google-services.json` and replace `app/google-services.json`.
4. Enable Email/Password and Google sign-in in Firebase Authentication.
5. Create a Cloud Firestore database.
6. Publish the rules from `app/firestore.rules`.

### User Data Isolation

All data is stored under `users/{uid}`, every Room query is filtered by uid, and the security rules allow access only to the owner, so another account on the same phone never sees the previous user's data.

## Offline Support and Synchronization

- Entries are cached in **Room**; screens read Room, and Firestore snapshot listeners keep it up to date.
- Months already opened show instantly, even offline.
- Writes go to Firestore's persistent cache and sync when the connection returns.
- An offline banner tells the user when they are offline; saves don't wait for the server.
- Profile creation has a 15-second write timeout so the UI never waits indefinitely.

### Synchronization Flow

```text
User creates or updates a record
              ↓
 Firestore local cache (applied at once)
              ↓
 Snapshot listener → Room → UI updates
              ↓
      Internet available?
          /       \
        No         Yes
        ↓           ↓
   Kept pending   Synced to the cloud
   until online
```

## Reminders and Notifications

- WorkManager runs a check every day at 9:00, even when the app is closed.
- Reminders: expected money due today, pay yourself first, catch-up after 3+ days away, and overdue freelance invoices.
- Each reminder is saved to an in-app notification list (kept 30 days) with a badge for unread ones.
- Tapping a notification opens the right place in the app.

## Testing

### Unit Tests

Located in:

```text
app/src/test/java/lk/moneytrail/app/
```

MoneyTrail has about 200 plain JUnit 4 unit tests on the domain rules and UI state, covering:

- Adding and updating entries, categories and onboarding.
- Entry totals, expected entries, salary and bonus.
- Savings goals and pay yourself first.
- Expense and income insights, folders.
- AdSense USD conversion.
- Catch-up, balancing and the monthly close (the CSV adds up).
- Freelance projects, milestones, payments and overdue invoices.
- Recurring bills.
- Reminders and notifications.
- Amount input, calendar grid and other screen state.

### Important Test Classes

```text
AddEntryUseCaseTest          UpdateEntryUseCaseTest
CategoriesTest               CompleteOnboardingUseCaseTest
EntryTotalsTest              ExpectedEntriesTest
SalaryUseCasesTest           SavingsGoalTest
SpendingInsightsTest         IncomeInsightsTest
UsdConversionTest            CatchUpTest
MonthlyCloseTest             RecurringBillsTest
FreelanceModelTest           OverdueInvoicesTest
GetRemindersUseCaseTest      RefreshNotificationsUseCaseTest
AddProjectViewModelTest      FreelanceViewModelTest
RecurringBillsStateTest      CalendarDaysTest
AmountInputTest              FoldersStateTest
```

### Instrumentation Tests

Located in:

```text
app/src/androidTest/java/lk/moneytrail/app/
```

### Firestore Rules Tests

```bash
cd firestore-rules-tests
npm install
npm test
```

### Continuous Integration

GitHub Actions (`.github/workflows/`) runs the unit tests and builds the APK on every push and pull request, runs the rules tests when the rules change, and publishes a release with the APK when a `v*` tag is pushed.

### Run Tests

```bash
./gradlew testDebugUnitTest
```

On Windows:

```powershell
.\gradlew.bat testDebugUnitTest
```

## Build and Installation Instructions

### Prerequisites

- Android Studio (a version that supports Android Gradle Plugin 9.3).
- Android SDK with API 37.
- JDK 17 or newer (Android Studio's bundled JDK works).
- Git.
- An Android emulator or a phone running Android 8.0 or later.

### 1. Clone the Repository

```bash
git clone https://github.com/NethmikaKekuu/MoneyTrail.git
cd MoneyTrail
```

### 2. Firebase

`app/google-services.json` for the shared project is already in the repository. To use your own project, follow the steps under [Firebase Configuration](#firebase-configuration).

### 3. Open the Project

1. Open Android Studio and choose **Open**.
2. Select the `MoneyTrail` folder.
3. Wait for the Gradle sync to finish.

### 4. Build the Application

On Windows:

```powershell
.\gradlew.bat assembleDebug
```

On macOS or Linux:

```bash
./gradlew assembleDebug
```

### 5. Run the Application

1. Start an emulator or connect a phone.
2. Select the `app` configuration.
3. Click **Run**.

## Platform Requirements

| Requirement | Configuration |
|---|---|
| Platform | Android |
| Minimum Android Version | Android 8.0 Oreo |
| Minimum SDK | API 26 |
| Compile SDK | API 37 |
| Target SDK | API 36 |
| Programming Language | Kotlin |
| UI Framework | Jetpack Compose |
| Design System | Material Design 3 |
| Application ID | `lk.moneytrail.app` |
| Backend | Firebase Authentication, Cloud Firestore |
| Local Storage | Room |

## Application Navigation

MoneyTrail is a single-activity app using Navigation Compose.

### Main Navigation Flow

```text
Splash
  ↓
Welcome → Login / Register / Forgot password
  ↓
Onboarding (new accounts)
  ↓
Dashboard (bottom tabs)
  ├── Calendar ── Add entry ── AdSense (USD)
  │      ├── Edit entry
  │      └── Notifications
  ├── Folders
  ├── Goal (Pay yourself first)
  ├── Insights (Expenses / Income)
  └── More
         ├── Notifications
         ├── Freelance projects ── Project ── Record payment
         │        └── New project
         ├── Recurring bills
         ├── Salary
         ├── Catch-up
         ├── Statement ── Monthly close
         ├── Settings
         └── Sign out
```

### Main Application Screens

| Screen | Purpose |
|---|---|
| Splash | Brand screen and session check. |
| Welcome | Entry point for new and returning users. |
| Login / Register / Forgot Password | Authentication. |
| Onboarding | Pay day, salary, income sources, accounts and first goal. |
| Calendar | The month, its markers, the day's entries and totals. |
| Add Entry / Edit Entry | Create, change or delete income and expenses. |
| AdSense (USD) | Record a payout in USD with the rupees received. |
| Folders | Totals by category and account. |
| Goal | Savings goals and pay yourself first. |
| Insights | Expense and income insights. |
| Freelance | Projects list with totals and overdue invoices. |
| Project | Milestones: mark invoiced and record payments. |
| New Project | Price, client and milestones. |
| Record Payment | Save a freelance payment to an account and date. |
| Recurring Bills | Subscriptions and auto-debits. |
| Salary | Pay day and base salary. |
| Catch-up | Balance accounts after a break. |
| Monthly Close / Statement | Close a month and export its statement as CSV. |
| Notifications | Reminder history; tap to open the right place. |
| Settings | Theme and account settings. |

## GitHub Repository Information

| Item | Details |
|---|---|
| Project Name | MoneyTrail |
| Repository | [MoneyTrail on GitHub](https://github.com/NethmikaKekuu/MoneyTrail) |
| Main Branch | `main` |
| Development Branch | `develop` |
| Latest Commit | `075130f32b634f85505ef399a061b98a6265ed02` |
| Latest Commit Message | Merge pull request #113 from NethmikaKekuu/develop |
| More Documentation | [`docs/`](docs/README.md) |

## Future Improvements

Possible next steps (not implemented yet):

- Crypto trade import with realised profit and loss (in progress on the `feature/Crypto` branch).
- Transfers between the user's own accounts.
- More reports and analytics.
- More currencies besides LKR and USD.

---

## License

License information has not yet been specified for this repository.

---

**MoneyTrail — Take control of your money, track your progress, and build better financial habits.**
