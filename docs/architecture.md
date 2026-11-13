# Architecture

## Overview

MoneyTrail is a single-activity Android app written in Kotlin with Jetpack Compose. It follows MVVM with a small domain layer of use cases, Hilt for dependency injection, Firebase Authentication for identity, Cloud Firestore as the main store, and Room as an on-phone copy so the screens work offline.

```
Compose screen  →  ViewModel  →  use case (when there's a rule)  →  repository interface  →  data source
 presentation      presentation          domain                         domain                    data
      ↑                 │
      └── StateFlow ────┘
```

## Layers

| Layer | Package | What lives there | May depend on |
| --- | --- | --- | --- |
| Presentation | `presentation/` | Screens (`XScreen`), ViewModels (`XViewModel`), screen state (`XState`), shared Composables, the theme | Domain |
| Domain | `domain/` | Models and pure functions (`model/`), use cases (`usecase/`), repository interfaces (`repository/`) | Nothing Android-specific |
| Data | `data/` | Firestore, Room, WorkManager, SharedPreferences, HTTP and Android implementations of the interfaces | Domain |
| DI | `di/` | Hilt modules that bind interfaces to implementations | All |

The domain layer has no Android or Firebase imports, so its rules are tested with plain JUnit.

## MVVM in MoneyTrail

- **Screen**: a `@Composable` that takes the state and lambdas. The public `XScreen` gets its ViewModel with `hiltViewModel()`, collects `uiState` with `collectAsStateWithLifecycle()` and passes it to a private `XContent`, which is what the previews use.
- **ViewModel**: `@HiltViewModel`, holds `private val _uiState = MutableStateFlow(XState())` and exposes `val uiState: StateFlow<XState> = _uiState.asStateFlow()`. Every event is a function (`onAmountChange`, `onSaveClick`) that updates state with `_uiState.update { it.copy(...) }` or launches work in `viewModelScope`.
- **State**: an immutable `data class`. Values worked out from other values are properties on it, for example `InsightsState.dependability`, so the screen never calculates anything.
- **Use case**: a class with `operator fun invoke`, used when a rule is more than a pass-through, for example `AddEntryUseCase` (validates the amount, picks committed or discretionary) or `CloseMonthUseCase` (adds "Not logged" entries and saves the close).
- **Repository**: an interface in `domain/repository` returning `Result<T>` for one-off calls and `Flow<Result<T>>` for live data. Failures are an `AppException` whose message can be shown as is.

## Data flow: adding an expense

1. The user types 850 on New entry and taps Save; `AddEntryScreen` calls `viewModel.onSaveClick()`.
2. `AddEntryViewModel` builds an `Entry` from its state and calls `AddEntryUseCase`.
3. The use case checks the amount and category, sets `spendingType`, and calls `EntryRepository.addEntry`.
4. `FirestoreEntryRepository` writes `users/{uid}/entries/{id}`. Firestore's persistent cache applies it on the phone at once; online, the call waits up to 3 seconds for the server so a rules error can be shown, offline it returns straight away.
5. The calendar's snapshot listener sees the new document and copies the month into Room (`EntryDao.replaceBetween`).
6. Room's `Flow` emits; `CalendarViewModel` updates its state; the calendar redraws with the entry and new totals.

## Confirmed and expected money

Every entry has a `status`: `CONFIRMED` (money that moved) or `EXPECTED` (planned for its date). Totals, folders and insights only count confirmed money. The calendar shows expected money with a dashed ring and asks "Did it come in?" on its day. `EnsureMonthlySalaryUseCase` adds the month's salary as expected on pay day; confirming it with a higher amount keeps the difference as its `bonus`.

## Offline and caching

- **Reads**: entries come from Room (`data/local/`), filled by Firestore listeners while a screen is open. Calendar, Folders and Insights open instantly with or without internet.
- **Writes**: always to Firestore, whose persistent cache (`di/FirebaseModule.kt`) queues them offline and uploads later.
- **Only entries are in Room**: they grow daily and every main screen reads them. The plan, accounts, categories, projects and closes are small and stay in Firestore's own cache.
- **Offline banner**: `AndroidConnectivityRepository` follows `ConnectivityManager`; the dashboard shows a banner while offline.
- Every Room row has the user's `uid`, and every query filters on it.

## Reminders and notifications

```
MoneyTrailApp ──books──▶ WorkManager daily job (9:00)
                               │
                         ReminderWorker ──▶ GetRemindersUseCase (same rules as the in-app cards)
                               │
                         ReminderNotifier ──▶ phone notification (one per kind)
                               └────────────▶ Room "notifications" table (history, 30 days)
```

- Four kinds: money due, pay yourself first, catch-up after 3+ days, freelance invoice overdue.
- The calendar shows a bell with the unread count and a "N reminders waiting" card. The Notifications screen lists the last month's reminders under **New** and **Earlier**.
- Tapping a reminder (in the list or on the phone) marks only that one read and opens its place through the dashboard (`openReminder` in `MoneyTrailNavHost`).
- `RefreshNotificationsUseCase` marks a kind read, and removes its phone notification, once what it asked for is done.

## Navigation

One `NavHost` in `presentation/navigation/MoneyTrailNavHost.kt`; route names in `Routes.kt`. Optional arguments are query parameters (`statement?month=2026-09`) read in the ViewModel through `SavedStateHandle`. The dashboard holds the five bottom tabs (Calendar, Folders, Goal, Insights, More) as state, not routes, so switching tabs doesn't grow the back stack. See [Diagrams](diagrams.md#navigation-map).

## Theme

`presentation/theme/` defines light and dark Material 3 colour schemes from the logo palette (Trail orange, Ember, Rust, Cream, Ink; Glow and Night in dark) and the type scale: **Sora** for headings and titles, **DM Sans** for body and labels. Screens use `MaterialTheme.colorScheme` roles and `MaterialTheme.typography` styles only, never raw colours or font sizes, so dark mode and the fonts stay consistent. Dynamic colour is off so the brand orange always shows. The theme mode (light, dark, system) is kept in `ThemePreferences`.

## Shared components (`presentation/components/`)

| Component | Use |
| --- | --- |
| `LoadingView`, `ErrorView`, `EmptyView` | The three states every screen shows before it has data |
| `MoneyTrailIcons` | All icons as Compose `ImageVector`s (no XML drawables) |
| `formatAmount`, `formatNet` | LKR with thousands separators; signed amounts |
| `AmountVisualTransformation` | Shows commas while the field holds plain digits, so the cursor never jumps |
| `OfflineBanner`, `AddCategoryDialog`, `AddEntryButton`, `MoneyTrailLogo`, `MoneyTrailDatePickerDialog` | Reused across screens |
