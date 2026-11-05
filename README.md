<img width="2400" height="744" alt="Untitled design (12)" src="https://github.com/user-attachments/assets/8144d0ce-034f-4542-8737-3c95dae12409" />


# MoneyTrail — Personal Finance Management App

MoneyTrail is a modern Android personal finance management application built with Kotlin and Jetpack Compose. It helps users manage their income, expenses, financial commitments, savings goals, and account balances in one place.

The application supports multiple income sources, committed and discretionary spending, expected transactions, and cryptocurrency profit and loss calculations using Binance trade history exports.

MoneyTrail uses Firebase for authentication and cloud data storage, with Firestore persistent caching to support offline access and synchronization.

---

## Key Features

| Feature | Description |
|---|---|
| **User Authentication** | Secure registration and login using Email/Password and Google Sign-In. |
| **Onboarding & Budget Setup** | Configure payday, base salary, income sources, accounts, and savings goals. |
| **Income Tracking** | Record and manage salary, freelance, AdSense, investment, and cryptocurrency income. |
| **Expense Management** | Create, edit, delete, and categorize expenses. |
| **Spending Classification** | Separate committed expenses from discretionary spending. |
| **Expected Transactions** | Schedule future transactions and confirm them when they become due. |
| **Custom Categories** | Create and manage custom income and expense categories. |
| **Financial Dashboard** | View financial summaries, expense trends, and spending breakdowns. |
| **Savings Goals** | Set savings targets and monitor progress. |
| **Account Management** | Manage cash, bank, credit card, and savings accounts. |
| **Cryptocurrency PnL** | Import Binance CSV exports and calculate realized profits and losses using FIFO cost-basis calculations. |
| **Offline Support** | Access cached financial data and synchronize changes when connectivity returns. |
| **Settings** | Update salary information, modify budget configuration, and manage account sessions. |

## Technologies Used

The following technologies and libraries are used in the project:

| Technology | Purpose |
|---|---|
| Kotlin 2.2.10 | Primary programming language |
| Jetpack Compose | Modern declarative Android UI |
| Material Design 3 | UI components and design system |
| MVVM | Presentation architecture |
| Clean Architecture | Separation of application responsibilities |
| Navigation Compose 2.9.8 | Screen navigation |
| Firebase Authentication 34.17.0 | User authentication |
| Cloud Firestore 34.17.0 | Cloud database and synchronization |
| Android Credential Manager 1.6.0 | Google Sign-In integration |
| Hilt 2.60.1 | Dependency injection |
| Kotlin Coroutines 1.10.2 | Asynchronous operations |
| StateFlow | Observable UI state management |
| KSP 2.3.12 | Kotlin symbol processing |
| Gradle Kotlin DSL | Build configuration |
| JUnit 4 | Unit testing |
| Espresso 3.7.0 | Android UI testing |
| Compose UI Testing | Jetpack Compose UI testing |

## Architecture Overview

MoneyTrail follows **Clean Architecture combined with the Model-View-ViewModel (MVVM)** architectural pattern.

This architecture separates the presentation layer, business logic, and data management to improve maintainability, testability, and scalability.

<img width="2816" height="1536" alt="Gemini_Generated_Image_zfk153zfk153zfk1" src="https://github.com/user-attachments/assets/e7bfa00b-9cf2-4273-ac85-fe9129c90a61" />

### Architecture Layers

**1. Presentation Layer**

Built using Jetpack Compose and ViewModels.

Responsibilities:
- Display application screens and UI components.
- Handle user interactions.
- Communicate with domain use cases.
- Observe application state using StateFlow.
- Display loading, success, and error states.

**2. Domain Layer**

Contains the core business logic of the application.

Responsibilities:
- Define business entities and models.
- Define repository interfaces.
- Implement application use cases.
- Perform financial calculations independently of the UI and database.

Examples:
- `AddEntryUseCase`
- `UpdateEntryUseCase`
- `CompleteOnboardingUseCase`
- `ParseBinanceCsvUseCase`
- `CalculateRealisedPnlUseCase`

**3. Data Layer**

Handles data storage and retrieval.

Responsibilities:
- Implement repository interfaces.
- Communicate with Firebase Authentication.
- Read and write financial records using Cloud Firestore.
- Manage data synchronization and persistence.

**4. Dependency Injection Layer**

Uses Hilt to manage dependencies and provide required objects throughout the application.

### Project Structure

```text
lk.moneytrail.app/
│
├── domain/
│   ├── model/
│   │   ├── Entry
│   │   ├── Account
│   │   ├── MoneyPlan
│   │   ├── CryptoTrade
│   │   ├── CryptoRealisedPnl
│   │   └── UserProfile
│   │
│   ├── repository/
│   │   ├── EntryRepository
│   │   ├── AuthRepository
│   │   ├── UserRepository
│   │   ├── AccountRepository
│   │   ├── PlanRepository
│   │   └── CryptoRepository
│   │
│   └── usecase/
│       ├── AddEntryUseCase
│       ├── UpdateEntryUseCase
│       ├── CompleteOnboardingUseCase
│       ├── ParseBinanceCsvUseCase
│       └── CalculateRealisedPnlUseCase
│
├── data/
│   └── repository/
│       ├── FirestoreEntryRepository
│       ├── FirebaseAuthRepository
│       ├── FirestoreUserRepository
│       └── FirestoreAccountRepository
│
├── di/
│   ├── FirebaseModule.kt
│   └── RepositoryModule.kt
│
└── presentation/
    ├── auth/
    ├── calendar/
    ├── crypto/
    ├── dashboard/
    ├── entry/
    ├── navigation/
    ├── onboarding/
    └── settings/
```

### Application Data Flow

The application follows this data flow:

**User Actions:**

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

**UI Updates:**

```text
   Firestore
        ↓
   Repository
        ↓
   Use Case
        ↓
    ViewModel
        ↓
Jetpack Compose UI
```

This approach keeps business logic outside UI components and prevents the presentation layer from directly accessing Firebase.

## Database Structure

MoneyTrail uses Cloud Firestore as its primary database.

All financial records are stored under the authenticated user's Firebase UID to maintain user-specific data separation.

### Firestore Collection Structure

```text
users/
 └── {uid}/
      │
      ├── entries/
      │    └── {entryId}
      │
      ├── accounts/
      │    └── {accountId}
      │
      ├── categories/
      │    └── {categoryId}
      │
      └── settings/
           └── plan
```

### Collection Details

| Collection | Description |
|---|---|
| `users/{uid}` | Stores user profile information. |
| `users/{uid}/entries` | Stores income, expenses, and expected transactions. |
| `users/{uid}/accounts` | Stores user financial accounts. |
| `users/{uid}/categories` | Stores default and custom categories. |
| `users/{uid}/settings/plan` | Stores budget configuration and savings goals. |

### Important Data Fields

**Entries**

- `status` — CONFIRMED or EXPECTED
- `type` — INCOME or EXPENSE
- `amount` — Financial amount stored as an integer in minor units.
- `category` — Transaction category.
- `account` — Associated financial account.
- `date` — Transaction date.
- `note` — Additional transaction details.
- `spendingType` — COMMITTED or DISCRETIONARY.
- `bonus` — Salary bonus amount.
- `createdAt` — Creation timestamp.

**Accounts**

- `name`
- `description`
- `kind` — CASH, BANK, CREDIT_CARD, or SAVINGS.
- `createdAt`

**Budget Plan**

- `payDay`
- `baseSalary`
- `incomeSources`
- `goal` — Savings goal information, including name, target amount, saved amount, and target date.

Financial amounts are stored using integer minor units to reduce floating-point rounding errors.

## Authentication and Firebase Configuration

MoneyTrail uses Firebase Authentication for account registration and login.

### Supported Authentication Methods

- Email and Password Authentication.
- Google Sign-In using Android Credential Manager.

Additional authentication functionality includes password reset emails and Firebase session persistence.

### Firebase Configuration

The Firebase project must be configured before running the application.

| Setting | Configuration |
|---|---|
| Firebase Project | MoneyTrail Firebase project |
| Android Package Name | `lk.moneytrail.app` |
| Authentication | Firebase Authentication |
| Authentication Providers | Email/Password and Google Sign-In |
| Cloud Database | Cloud Firestore |
| Configuration File | `app/google-services.json` |

**Setup requirements:**

1. Create or access the Firebase project.
2. Register the Android application using the package name `lk.moneytrail.app`.
3. Download the `google-services.json` configuration file.
4. Place the file inside the application's `app/` directory.
5. Enable Email/Password authentication in Firebase Authentication.
6. Configure Google Sign-In and the required SHA fingerprints.
7. Ensure Cloud Firestore is enabled.

The Firebase configuration file is not included in the repository and must be supplied separately.

### User Data Isolation

Each user's financial records are stored under their authenticated Firebase UID.

This ensures that application queries are scoped to the currently authenticated user.

## Offline Support and Synchronization

MoneyTrail uses Cloud Firestore's persistent local cache to support offline access and data synchronization.

### Offline Functionality

- Previously cached financial records can be accessed without an internet connection.
- Firestore supports local reads and writes while offline.
- Snapshot listeners provide updates to the application when data changes.
- Pending changes are synchronized with the cloud when connectivity returns.

### Synchronization Flow

```text
User Creates or Updates a Record
              ↓
       Cloud Firestore
              ↓
       Local Cache Update
              ↓
      Internet Available?
          /       \
        No         Yes
        ↓           ↓
   Keep Pending   Cloud Sync
   Local Changes     ↓
                 Updated Data
                     ↓
                  UI Update
```

The application also includes a 15-second write timeout in `FirestoreUserRepository` for user profile creation to prevent indefinite UI waiting.

## Cryptocurrency Profit and Loss Calculation

MoneyTrail includes a cryptocurrency profit and loss calculation engine that processes Binance trade history CSV exports.

### Key Capabilities

- Import Binance CSV files using Android's Storage Access Framework.
- Validate CSV headers and input data.
- Detect duplicate trades using trade IDs or date, pair, and amount combinations.
- Calculate realized profit and loss using the FIFO (First-In, First-Out) cost-basis method.
- Deduct applicable trading fees.
- Group realized profit and loss by month.
- Calculate year-to-date (YTD) totals.

The calculations are implemented using Kotlin business logic, keeping financial computation separate from the presentation layer.

## Testing

MoneyTrail includes unit tests and Android instrumentation tests.

### Unit Testing

Tests are located in:

```text
app/src/test/java/lk/moneytrail/app/
```

The test suite covers:

- Income and expense use cases.
- Category management.
- Onboarding configuration.
- Entry totals and financial calculations.
- Expected transactions.
- Salary calculations.
- Savings goal calculations.
- Spending insights.
- Binance CSV parsing.
- Cryptocurrency realized PnL calculations.
- ViewModel state and presentation logic.

### Important Test Classes

```text
AddEntryUseCaseTest
CategoriesTest
CompleteOnboardingUseCaseTest
EntryTotalsTest
ExpectedEntriesTest
FolderTotalsTest
SalaryUseCasesTest
SavingsGoalTest
SpendingInsightsTest
UpdateEntryUseCaseTest
ParseBinanceCsvUseCaseTest
CalculateRealisedPnlUseCaseTest
AmountInputTest
CalendarDaysTest
FoldersStateTest
CryptoViewModelTest
```

### Instrumentation Testing

Located in:

```text
app/src/androidTest/java/lk/moneytrail/app/
```

Includes Android instrumentation testing using AndroidX Test and Espresso.

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

Ensure the following tools are installed:

- Android Studio.
- Android SDK.
- Compatible JDK for the project's Gradle configuration.
- Git.
- Firebase project access.
- Android emulator or physical Android device.

### 1. Clone the Repository

```bash
git clone https://github.com/NethmikaKekuu/MoneyTrail.git
```

Navigate to the project directory:

```bash
cd MoneyTrail
```

### 2. Configure Firebase

Download the Firebase Android configuration file:

```text
google-services.json
```

Place it inside:

```text
app/google-services.json
```

Ensure that Firebase Authentication and Cloud Firestore are configured correctly.

### 3. Open the Project

1. Open Android Studio.
2. Select **Open**.
3. Choose the cloned MoneyTrail project directory.
4. Allow Gradle to synchronize the project.
5. Resolve any missing SDK or dependency requirements.

### 4. Build the Application

On Windows:

```powershell
.\gradlew.bat build
```

On macOS or Linux:

```bash
./gradlew build
```

### 5. Run the Application

1. Start an Android emulator or connect an Android device.
2. Select the MoneyTrail application configuration.
3. Click **Run** in Android Studio.

### 6. Run Unit Tests

On Windows:

```powershell
.\gradlew.bat testDebugUnitTest
```

On macOS or Linux:

```bash
./gradlew testDebugUnitTest
```

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
| Backend | Firebase |
| Database | Cloud Firestore |

## Application Navigation

MoneyTrail uses Navigation Compose to manage navigation between application screens.

### Main Navigation Flow

```text
Splash Screen
     ↓
Welcome Screen
     ↓
 Login / Register
     ↓
  Onboarding
     ↓
  Dashboard
     │
     ├── Add Entry
     │
     ├── Edit Entry
     │
     └── Settings
            │
            ├── Update Budget
            │
            └── Sign Out
```

### Main Application Screens

| Screen | Purpose |
|---|---|
| Splash | Initial application screen and session handling. |
| Welcome | Entry point for new and returning users. |
| Login | User authentication. |
| Register | New account registration. |
| Forgot Password | Password reset functionality. |
| Onboarding | Initial financial and budget configuration. |
| Dashboard | Financial overview and insights. |
| Calendar | View scheduled and expected transactions. |
| Add Entry | Create income or expense records. |
| Edit Entry | Modify existing transactions. |
| Crypto | Import and calculate cryptocurrency PnL. |
| Settings | Manage salary, budget configuration, and sessions. |

## GitHub Repository Information

| Item | Details |
|---|---|
| Project Name | MoneyTrail |
| Repository | [MoneyTrail on GitHub](https://github.com/NethmikaKekuu/MoneyTrail) |
| Main Branch | `main` |
| Latest Commit | `4914b7ff933e181cc0d566d4f2fbbd46be58e356` |
| Latest Commit Message | Merge pull request #32 from NethmikaKekuu/develop |

## Future Improvements

Potential areas for future development include:

- Additional financial reports and analytics.
- More advanced budgeting and spending insights.
- Enhanced cryptocurrency portfolio tracking.
- Additional currency conversion capabilities.
- Improved synchronization monitoring and conflict handling.
- Automated CI/CD testing and build workflows.

These are potential improvements, not claims about currently implemented functionality.

---

## License

License information has not yet been specified for this repository.

---

**MoneyTrail — Take control of your money, track your progress, and build better financial habits.**
