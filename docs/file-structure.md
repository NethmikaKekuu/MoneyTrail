# File structure

```
MoneyTrail/
├── app/
│   ├── build.gradle.kts              app module: SDK levels, dependencies, Compose, Hilt, KSP
│   ├── firestore.rules               Firestore security rules (publish in the console)
│   ├── google-services.json          Firebase config (not committed)
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml   the only XML file in the app
│       │   ├── java/lk/moneytrail/app/
│       │   │   ├── MainActivity.kt   single activity; hands tapped notifications to navigation
│       │   │   ├── MoneyTrailApp.kt  @HiltAndroidApp; books the daily reminder check
│       │   │   ├── data/
│       │   │   ├── di/
│       │   │   ├── domain/
│       │   │   └── presentation/
│       │   └── res/                  fonts (sora.ttf, dm_sans.ttf) and the notification icon PNGs
│       └── test/java/lk/moneytrail/app/
│           ├── data/                 tests for data-layer helpers (EntryEntity, ThemePreferences, reminder times)
│           ├── domain/               use case and model tests
│           └── presentation/         screen state and ViewModel tests
├── docs/                             this documentation
├── gradle/libs.versions.toml         dependency versions
└── README.md
```

## `data/`

| Folder | Contents |
| --- | --- |
| `repository/` | `FirebaseAuthRepository`, `FirestoreUserRepository`, `FirestoreEntryRepository`, `FirestoreAccountRepository`, `FirestorePlanRepository`, `FirestoreCategoryRepository`, `FirestoreFreelanceRepository`, `FirestoreMonthCloseRepository`, `HttpUsdRateRepository`, `AndroidConnectivityRepository`; shared `FirestoreErrors.kt` (readable messages) and `FirestoreWrites.kt` (`awaitServerBriefly`) |
| `local/` | Room: `EntryEntity`, `EntryDao`, `MoneyTrailDatabase` (the entries copy) |
| `notifications/` | Room: `NotificationEntity`, `NotificationDao`, `NotificationDatabase`, `RoomNotificationLogRepository` (reminder history) |
| `reminders/` | `ReminderWorker`, `ReminderNotifier`, `WorkManagerReminderScheduler`, `ReminderIds` |
| `preferences/` | `ThemePreferences`, `ThemeMode`, `CatchUpPreferences` (SharedPreferences) |
| `files/` | `ContentResolverFileExportRepository` (writes the statement CSV to a file the user picked) |

## `di/`

`FirebaseModule` (Auth, Firestore with persistent cache), `DatabaseModule` (Room entries), `NotificationLogModule` (Room notifications), `RepositoryModule` (binds every interface), `ThemeModule`, and `ReminderEntryPoint` (how `ReminderWorker` reaches Hilt objects).

## `domain/`

| Folder | Contents |
| --- | --- |
| `model/` | `Entry`, `Account`, `Categories`, `CustomCategory`, `MoneyPlan`, `GoalModel`, `GoalSavings`, `Project`, `Reminder`, `AppNotification`, `CatchUp`, `ExpectedEntries`, `EntryTotals`, `FolderTotals`, `SpendingInsights`, `IncomeInsights`, `MonthlyClose`, `UsdConversion`, `UserProfile`, `AuthUser`, `AppException` |
| `usecase/` | `RegisterUseCase`, `SignInWithGoogleUseCase`, `CompleteOnboardingUseCase`, `AddEntryUseCase`, `UpdateEntryUseCase`, `AddCategoryUseCase`, `EnsureMonthlySalaryUseCase`, `UpdateSalaryUseCase`, `SaveGoalUseCase`, `DeleteGoalUseCase`, `MoveSavingsUseCase`, `RemindSavingsLaterUseCase`, `CheckCatchUpUseCase`, `BalanceAccountUseCase`, `CloseMonthUseCase`, `GetRemindersUseCase`, `RefreshNotificationsUseCase` |
| `repository/` | 14 interfaces: `Auth`, `User`, `Entry`, `Account`, `Plan`, `Category`, `Freelance`, `MonthClose`, `NotificationLog`, `UsdRate`, `Connectivity`, `CatchUp`, `FileExport` repositories and `ReminderScheduler` |

## `presentation/`

One folder per feature, each with `XScreen.kt`, `XViewModel.kt` and `XState.kt` (sometimes `XComponents.kt`).

| Folder | Screens |
| --- | --- |
| `splash/`, `welcome/`, `auth/` | Splash, Welcome, Login, Register, Forgot password; Google sign-in helper |
| `onboarding/` | Five-step onboarding |
| `dashboard/` | The bottom-bar home: Calendar, Folders, Goal, Insights, More |
| `calendar/` | Month grid, day list, "Did it come in?", reminders and savings cards |
| `entry/` | New entry (keypad) and Edit entry |
| `usd/` | AdSense payout in USD |
| `folders/`, `insights/` | Folders by category; Expense and Income insights |
| `goal/` | Goals and pay yourself first |
| `salary/`, `settings/`, `more/` | Salary, Settings, More |
| `catchup/`, `monthlyclose/`, `statement/` | Catch-up, Monthly close, Statement with CSV export |
| `freelance/` | Projects list (totals, overdue, project cards), Project detail (milestones and their actions), New project, milestone dialog, Record payment |
| `notifications/` | Notification centre |
| `navigation/` | `MoneyTrailNavHost`, `Routes` |
| `components/`, `theme/` | Shared Composables; colours, type, `MoneyTrailTheme` |

## Conventions

- Package `lk.moneytrail.app`; one public screen per file, named after it.
- `XScreen` (public, takes the ViewModel) wraps a private `XContent` (takes state and lambdas) so previews need no ViewModel.
- Every screen file ends with at least one `@Preview` using `MoneyTrailTheme`.
- Firestore repositories keep a `TAG` and their field names as `private const val` in a companion object.
- Comments say *why*, in plain words, and point at the Figma frame a screen follows (`// MoneyTrail-Figma-HTML/11-IncomeInsights.html`).
- No XML apart from `AndroidManifest.xml`: icons are `ImageVector`s, the theme is Kotlin.
