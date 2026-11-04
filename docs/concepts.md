# Concepts explained

Every idea the app relies on, in plain words, with where to find it in MoneyTrail.

## 1. Kotlin foundations

- **`data class`**: a class that only holds values, with `copy()` to make a changed version. Every model (`Entry`, `Account`) and every screen state (`CalendarState`) is one. Changing state is always `state.copy(isSaving = true)`, never changing a field in place.
- **`sealed interface`**: a closed set of cases the compiler knows. `Reminder` is `MoneyDue`, `PayYourselfFirst`, `CatchUp` or `InvoiceOverdue`, so a `when` over it must handle every case.
- **`enum class`**: a fixed list, e.g. `EntryType { INCOME, EXPENSE }`, `EntryStatus { CONFIRMED, EXPECTED }`. Stored in Firestore by name.
- **Extension functions**: `fun List<Entry>.dependability(month)` reads like a method on a list without changing the list type. Most insight rules are written this way.
- **Null safety**: `checkedBalance: Long?` means "may be missing"; `?.let { }` and `?:` handle it, so an account that was never balanced can't crash a calculation.
- **`Result<T>`**: a value or an error. Repositories return it, so callers write `.onSuccess { }.onFailure { }` instead of try/catch everywhere.

## 2. Coroutines

- A **coroutine** is work that can pause without blocking the thread, e.g. waiting for Firestore.
- **`suspend fun`** marks a function that can pause: `addEntry`, `saveClose`.
- **`viewModelScope.launch { }`** starts work tied to the ViewModel; it is cancelled when the screen goes away, so nothing leaks.
- **`withContext(Dispatchers.IO)`** moves blocking work (the USD rate request, writing the CSV) off the main thread.
- **`await()`** (kotlinx-coroutines-play-services) turns a Firebase `Task` into a suspend call.
- **`withTimeoutOrNull`**: the reminder worker gives up on a stuck check after 60 seconds.

## 3. Flow and StateFlow

- A **`Flow`** is a stream of values over time. Room queries return one, so a screen updates by itself when entries change.
- **`callbackFlow`** wraps a Firestore snapshot listener: each snapshot is sent into the flow; `awaitClose { registration.remove() }` stops listening when nobody collects.
- **`StateFlow`** always has a current value. Every ViewModel holds `MutableStateFlow` privately and exposes `StateFlow`, so only the ViewModel can change state.
- **`combine`** and **`flatMapLatest`**: `StatementViewModel` watches accounts, then (for those accounts) entries and two closes together; a new month cancels the old listeners.
- **`collectAsStateWithLifecycle()`** reads a `StateFlow` in Compose and stops when the app is in the background.

## 4. MVVM and layers

- **Model**: data and rules (`domain/model`).
- **View**: Composables that draw state.
- **ViewModel**: holds state, handles events, calls use cases and repositories.
- **Use case**: one rule with a name, e.g. `MoveSavingsUseCase` adds what was moved to each goal and records the month.
- **Why**: the screen never knows where data comes from, and the rules can be tested without a phone. See [Architecture](architecture.md).

## 5. Repository pattern

An interface in `domain/repository` says *what* can be done (`observeBetween(from, to)`, `addEntry(entry)`); a class in `data/` says *how* (Firestore query, Room copy). ViewModels only see the interface. Tests swap in a fake that keeps a list. Swapping a data source would only touch `data/` and `di/`.

## 6. Dependency injection with Hilt

- **`@HiltAndroidApp`** on `MoneyTrailApp` creates the object graph.
- **`@Inject constructor`** tells Hilt how to build a class (`AddEntryUseCase`, every repository implementation).
- **`@Module` + `@Binds`** in `RepositoryModule` connect each interface to its implementation; **`@Provides`** in `FirebaseModule` and `DatabaseModule` build library objects (Firestore with persistent cache, Room databases).
- **`@Singleton`**: one instance for the whole app (repositories, databases).
- **`@HiltViewModel`** + **`hiltViewModel()`**: ViewModels get their dependencies and their `SavedStateHandle` (route arguments) without being created by hand.
- **`@EntryPoint`** (`ReminderEntryPoint`): WorkManager builds `ReminderWorker` itself, so the worker asks Hilt for `GetRemindersUseCase` through an entry point.

## 7. Jetpack Compose

- UI is a function of state: when `uiState` changes, Compose redraws (recomposes) only what read the changed value.
- **`remember` / `rememberSaveable`**: keep UI-only values (the selected tab survives rotation with `rememberSaveable`).
- **`LaunchedEffect(key)`**: run a one-off action when a value changes, e.g. go to the statement once `isClosed` is true, or open the reminder's place.
- **`LifecycleEventEffect(ON_RESUME)`**: the dashboard and notification list refresh reminders when shown again.
- **Material 3**: `Scaffold`, `NavigationBar`, `OutlinedTextField`, `ModalBottomSheet`, `Badge`; colours and type from `MaterialTheme`.
- **State hoisting**: `XContent(state, onClick…)` takes everything as parameters, which is what makes previews possible.
- **`VisualTransformation`**: `AmountVisualTransformation` shows "48,320" while the field holds "48320", so the cursor never jumps.

## 8. Navigation Compose

- One `NavHost`, one route per screen (`Routes.kt`).
- Optional arguments as query parameters (`statement?month=2026-09`), read through `SavedStateHandle` in the ViewModel.
- **Results back** through the previous entry's `savedStateHandle`: the notification list hands the dashboard the reminder kind to open.
- `popBackStack(route, inclusive = true)` closes a screen; `navigateAndClearBackStack` after sign-in so Back doesn't return to Login.

## 9. Room

- **Entity** = table row (`EntryEntity`), **DAO** = queries (`EntryDao`), **Database** = the file (`MoneyTrailDatabase`).
- Queries returning `Flow` emit again whenever the table changes.
- **`@Transaction`**: `replaceBetween` deletes a month's rows and inserts the new ones as one step, so a screen never sees half a month.
- The entries table is only a copy of Firestore, so a schema change drops it (`fallbackToDestructiveMigration`) and the listeners refill it. The notifications table is the only copy of the history, so it has no destructive fallback.

## 10. Firebase Authentication and Firestore

- **Auth** gives each user a `uid`; email/password and Google (through Credential Manager and a Google ID token).
- **Firestore** stores documents in collections. Everything sits under `users/{uid}`.
- **Snapshot listeners** push changes as they happen: that's the "real-time" in the brief.
- **Persistent cache**: writes made offline are applied locally and sent later.
- **Security rules** run on Google's servers for every read and write; ours check ownership and validate every field. See [Firestore schema](firestore-schema.md).
- **Server timestamps**: `createdAt` is set by the server, and the rules check it equals `request.time`, so the phone's clock can't fake it.

## 11. Offline-first

Read from the phone, write to Firestore, let listeners keep the phone's copy up to date. The user never waits for the network to see data, and saves work in airplane mode. Online saves wait briefly for the server so a rules error can still be shown; offline saves return at once (`awaitServerBriefly`).

## 12. WorkManager

Runs background work Android guarantees to run, even after the app is closed or the phone restarts. `WorkManagerReminderScheduler` books a unique periodic job every 24 hours starting at the next 9:00 (`KEEP`, so reopening the app doesn't move it), and a one-off forced job for "Check reminders now".

## 13. Notifications

- A **channel** ("Reminders") is created before posting; users can mute it in Android settings.
- **Android 13+** needs the `POST_NOTIFICATIONS` permission, asked on the dashboard.
- Each kind has a fixed id (1–4), so a new one replaces the old one instead of piling up; `cancel(id)` removes it once done.
- The **`PendingIntent`** carries the reminder kind; `MainActivity` reads it in `onCreate` / `onNewIntent` and navigation opens the right place.

## 14. Money rules

- **Committed vs discretionary**: what had to be paid (rent, bills) vs what was a choice (coffee, rides). Set from the category, changeable per entry.
- **Confirmed vs expected**: money that moved vs money planned for its date. Only confirmed money counts in totals.
- **Dependable vs variable income**: the base salary vs everything else (bonus, freelance, AdSense). Crypto is shown on its own.
- **Balancing**: real balance − calculated balance; the difference becomes a "Not logged" entry.
- **Pay yourself first**: move the goal's monthly saving as soon as the salary arrives.

## 15. Testing

Unit tests check rules on plain data with fixed dates; fakes stand in for repositories. Screens and Firebase are checked by hand on a device. See [Testing](testing.md).

## 16. Glossary

| Term | Meaning |
| --- | --- |
| BOM | "Bill of materials": one version that sets matching versions for a family of libraries (Compose, Firebase) |
| DAO | Data access object: the Room interface with the queries |
| DI | Dependency injection: classes receive what they need instead of creating it |
| KSP | Kotlin Symbol Processing: generates Hilt and Room code at build time |
| MoSCoW | Must, Should, Could, Won't: requirement priorities |
| Recomposition | Compose calling a Composable again because state it read changed |
| Slug | A short id made from a name, e.g. `main-bank` |
| Snapshot listener | A Firestore callback that fires with the current data and again on every change |
| StateFlow | A flow that always holds a current value |
