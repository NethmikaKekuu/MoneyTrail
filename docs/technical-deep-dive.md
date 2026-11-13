# Technical deep dive

How the main mechanisms work, step by step, with the files to open.

## 1. App start

1. Android creates `MoneyTrailApp` (`@HiltAndroidApp`). Hilt builds the singleton graph: Firebase Auth, Firestore with `PersistentCacheSettings` (`di/FirebaseModule.kt`), the two Room databases, every repository.
2. `MoneyTrailApp.onCreate` calls `reminderScheduler.scheduleDaily()`, which books the daily reminder job with WorkManager (`KEEP`, so the time already booked doesn't move).
3. `MainActivity` (`@AndroidEntryPoint`) reads the theme mode from `ThemePreferences`, reads a tapped notification's kind from the launch intent, and sets the content: `MoneyTrailTheme { MoneyTrailNavHost(...) }`.
4. The start route is `splash`. `SplashViewModel` waits 900 ms so the brand screen is seen, then picks `dashboard` if `authRepository.currentUserId` is set (Firebase restores the session from disk), else `welcome`.
5. On the dashboard, `DashboardViewModel` checks for a catch-up, listens to the unread notification count and to connectivity, and the dashboard asks for the notification permission on Android 13+.

## 2. How Hilt builds a ViewModel

`CalendarScreen(viewModel: CalendarViewModel = hiltViewModel())` asks Hilt for a ViewModel scoped to that navigation entry. Hilt sees `@HiltViewModel class CalendarViewModel @Inject constructor(entryRepository: EntryRepository, …)`, looks up each parameter, finds `@Binds bindEntryRepository(impl: FirestoreEntryRepository): EntryRepository` in `RepositoryModule`, and builds `FirestoreEntryRepository` from its own `@Inject constructor(firestore, auth, entryDao, connectivity)`, which come from `FirebaseModule` and `DatabaseModule`. All of this is generated at build time by KSP; a missing binding is a build error, not a crash.

## 3. Reading entries: Firestore → Room → screen

`FirestoreEntryRepository.observeBetween(from, to)` returns a `channelFlow` (see `observeCached`):

1. It launches a child coroutine that runs a snapshot listener (`callbackFlow` around `addSnapshotListener`) for `date >= from && date <= to`.
2. Each snapshot is turned into Room rows and saved with `EntryDao.replaceBetween(uid, from, to, rows)`, a `@Transaction` that deletes the range and inserts the new rows, so deletions on another phone disappear too. A `synced` flag is set.
3. At the same time it collects Room's `observeBetween(uid, from, to)` flow, combined with `synced`.
4. It sends rows when Room has some, or once the first snapshot has arrived. That avoids flashing an empty screen for a month never opened on this phone, while a month already cached shows instantly, even offline.
5. A listener error (e.g. permission denied) is sent as `Result.failure` with a readable message; the screen shows its error view with Retry.

When the screen leaves, `viewModelScope` is cancelled, the channel closes, and `awaitClose` removes the Firestore listener.

## 4. Writing an entry

1. `AddEntryUseCase` checks the amount (> 0) and category, trims the note, and sets `spendingType` from the category (`Categories.defaultSpending`) unless the user chose one.
2. `FirestoreEntryRepository.addEntry` writes the document with `createdAt = FieldValue.serverTimestamp()`.
3. `awaitServerBriefly` (`data/repository/FirestoreWrites.kt`): if offline, return at once (the write is already in Firestore's cache and will sync); if online, wait up to 3 s for the server so a rules rejection can be shown; after 3 s stop waiting and let it sync.
4. Firestore applies the write locally immediately, so the open snapshot listener fires and Room updates (step 3 of section 3): the entry appears before the server has answered.

## 5. StateFlow and recomposition

`CalendarViewModel` collects the repository flow and calls `_uiState.update { it.copy(entries = …) }`. `update` is atomic, so two coroutines can't lose each other's change. The screen's `collectAsStateWithLifecycle()` turns the `StateFlow` into Compose `State`; Compose only re-runs the Composables that read a value that changed. Totals and the grid are properties on `CalendarState`, worked out from `entries`, so the screen has no calculations to repeat.

## 6. Pay day: the salary as expected income

`EnsureMonthlySalaryUseCase` runs when the calendar opens:

1. Read the plan; stop if there's no salary or `SALARY` isn't an income source.
2. Pay day for this month: `payDay.coerceAtMost(month.lengthOfMonth())`, so 31 falls on the 30th or 28th.
3. Stop if pay day has passed (a salary removed with "Didn't happen" doesn't come back) or this month already has a Salary entry.
4. Add an `EXPECTED` Salary entry of `baseSalary` on pay day.

On the day, "Did it come in?" confirms it with the real amount; anything above the base is stored as `bonus` (`Entry.confirmedSalary`). Once confirmed, `MoneyPlan.isSavingsDue` becomes true and the calendar shows the pay-yourself-first card.

## 7. Balancing an account

An account remembers `checkedBalance` (what it held at the *start* of `checkedOn`). `Account.calculatedBalance(entries)` = `checkedBalance` + confirmed income − confirmed expenses in that account, from `checkedOn` on.

`BalanceAccountUseCase(account, realBalance, today)`:

1. Work out the calculated balance; `difference = real − calculated`.
2. If not zero, add a "Not logged" entry on today (expense if money left unlogged, income if it came in).
3. Save the new check as the start-of-day balance: `real − today's entries' change`. Today's entries are counted again from the check day on, so anything logged today, before or after balancing, counts exactly once.

## 8. Closing a month

`CloseMonthUseCase(month, accounts, realBalances, today)`:

1. Read entries from the earliest `checkedOn` to the month's last day.
2. For each account: `calculated = balanceAtEndOf(lastDay)`, or null if it was first checked after that month ("First check").
3. Add `notLoggedEntry(account, real − calculated, lastDay)` where they differ.
4. Save each real balance as the account's check for the 1st of next month (unless it was already checked later).
5. Save `closes/{yyyy-MM}` with the balances.

The statement reads the month's entries and this and last month's close. Opening balance = last month's close if there is one, else `balanceAtEndOf(day before the month)`. The CSV lists every confirmed entry with expenses negative, so `SUM(amount)` equals income − expenses − unaccounted = "Kept".

## 9. AdSense in USD

`UsdPayoutViewModel` asks `UsdRateRepository.rateFor(date)` for that day's USD→LKR market rate. `HttpUsdRateRepository` reads a dated file from the free currency API on jsDelivr, falling back to open.er-api.com's latest rate, on `Dispatchers.IO` with `HttpURLConnection` and `org.json`. The user types the dollars and, if known, the rupees that arrived. `UsdConversion` works out `effectiveRate = rupees × 100 / dollars` (in hundredths) and `feeLkr = max(0, dollars × marketRate − rupees)`. The entry stores the rupees as `amount` and the rest beside it.

## 10. The daily reminder check

1. WorkManager runs `ReminderWorker` (a `CoroutineWorker`) around 9:00, app open or not.
2. The worker can't take constructor parameters from Hilt, so it gets `GetRemindersUseCase`, `AuthRepository` and the notification log through `EntryPointAccessors.fromApplication(…, ReminderEntryPoint::class.java)`.
3. Signed out → nothing to do. Otherwise `GetRemindersUseCase(today)` (timeout 60 s) returns the reminders by the same rules as the in-app cards: expected money due, pay yourself first, catch-up after 3 days, invoices overdue.
4. `ReminderNotifier.show` saves each to the Room history, then posts one phone notification per kind (fixed ids 1–4), at most once a day per kind unless forced by "Check reminders now".
5. Each notification's `PendingIntent` carries its kind (`MainActivity.EXTRA_NOTIFICATION_KIND`), with its own request code so extras don't mix.

## 11. Tapping a notification

1. `MainActivity` reads the kind in `onCreate` (fresh start) or `onNewIntent` (already open) into Compose state.
2. `MoneyTrailNavHost` has a `LaunchedEffect` on that kind and the current route. Once the route is `dashboard` (after splash and sign-in if needed), it calls `openReminder(kind)`.
3. `openReminder` puts the kind on the dashboard entry's `savedStateHandle` and pops back to the dashboard.
4. `DashboardViewModel` collects that key, marks the kind read in the history, and `DashboardScreen` acts: Money due → Calendar with the check open; Pay yourself first → Goal; Catch-up → Catch-up; Invoice overdue → Freelance.

The in-app list uses the same `openReminder`, and marks the tapped row read by its id, so the others stay as they are.

## 12. Freelance projects

1. **New project** (`AddProjectViewModel`): name, client and the **price** are required. The price box holds only what was typed (`totalBudgetDigits`). `AddProjectUiState.leftToSplit` = price − milestones, shown as "Split 40,000 of 60,000 · 20,000 left". The milestone dialog starts on what's left, and `milestoneError` stops a milestone going past it. On Save: milestones over the price block saving; milestones short of it ask *Add the rest as a milestone* or *Save anyway*; no milestones saves one "Full payment". The id is a UUID made on the phone, so the app opens the new project at once.
2. **Projects list** (`FreelanceUiState`): totals across every project (outstanding = invoiced, paid, to invoice), the overdue invoices from `overdueInvoices(today)` (the same function the daily reminder uses, so "due today" is never late), and project cards sorted overdue → invoiced → the rest → completed.
3. **Project detail** (`ProjectDetailViewModel`, route `freelance/project/{projectId}`): milestones in due order. Planned: *Edit* / *Mark invoiced*. Invoiced: *Not invoiced* / *Record payment*. Paid: no actions, it is already income. Each change is saved to Firestore with Undo; on failure the old project is put back.
4. **Record payment** opens on the milestone it came from (`milestoneId` argument), saves the income entry, then marks the milestone paid.

## 13. Auth state and navigation

Sign-in screens call the auth use cases, then navigate with `navigateAndClearBackStack(DASHBOARD)` or `(ONBOARDING)` for a new account, so Back can't return to Login. Sign out from More calls `authRepository.signOut()` and clears the stack back to Login. Every repository reads `auth.currentUser?.uid` per call and returns a "signed out" failure when it's null, and every Room query filters by uid, so another account on the same phone never sees the previous user's data.
