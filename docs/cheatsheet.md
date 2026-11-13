# Viva cheat sheet

## 30-second pitch

Kavindu earns from four places that never arrive the same way twice, and every tool he tried either ignored that or punished him for missing a day. MoneyTrail keeps planned money apart from real money, splits his salary into the dependable base and the variable bonus, records AdSense in USD with the rupees that really landed, and ties his MacBook goal to pay day. When he forgets things, catch-up and balancing still make the month add up. It's Kotlin and Compose with MVVM and Hilt, Firebase Auth and Firestore with validated rules, and Room so it works offline.

## Where to point in the code

| Concept | File |
| --- | --- |
| Screen → ViewModel | `presentation/entry/AddEntryScreen.kt`, `AddEntryViewModel.kt` |
| StateFlow state | `presentation/insights/InsightsState.kt` |
| Use case | `domain/usecase/AddEntryUseCase.kt`, `CloseMonthUseCase.kt` |
| Pure rules | `domain/model/IncomeInsights.kt`, `SpendingInsights.kt`, `MonthlyClose.kt` |
| Repository interface / implementation | `domain/repository/EntryRepository.kt` / `data/repository/FirestoreEntryRepository.kt` |
| Real-time listener + Room | `FirestoreEntryRepository.observeCached`, `data/local/EntryDao.kt` |
| Hilt | `di/RepositoryModule.kt`, `di/FirebaseModule.kt` |
| Navigation | `presentation/navigation/MoneyTrailNavHost.kt`, `Routes.kt` |
| Security rules | `app/firestore.rules` (`isValidEntry`) |
| Freelance | `presentation/freelance/ProjectDetailViewModel.kt`, `domain/model/Project.kt` (`overdueInvoices`, `unsplitAmount`) |
| Background reminders | `data/reminders/ReminderWorker.kt`, `ReminderNotifier.kt` |
| Theme | `presentation/theme/Theme.kt`, `Type.kt` |
| Tests | `app/src/test/.../domain/` |

## Likely questions

- **Why MVVM?** The screen only draws state; the ViewModel survives rotation and holds the state; rules sit in testable use cases.
- **Where's the real-time part?** Firestore snapshot listeners on the entries; the calendar updates the moment an entry is saved anywhere.
- **Offline?** Room for reads, Firestore's persistent cache for writes, a banner, and no waiting for the server when offline.
- **Multi-currency?** LKR as `amount`; `usdCents`, `effectiveRate`, `feeLkr` beside it; market rate fetched automatically.
- **Indexes?** None custom: every query is on one field.
- **Rules?** Owner only; one validation function per collection; server timestamps checked; bonus only on Salary.
- **No logic in Composables?** Totals and percentages are properties on the state class or functions in `domain/model`.
- **Error handling?** Repositories return `Result` with readable `AppException`s; every screen has loading, empty and error with Retry.
- **How do reminders work when the app is closed?** WorkManager job at 9:00; worker uses the same use case as the in-app cards.
- **What's innovative?** Dependable vs variable income; dining as days of goal saving; catch-up with "Not logged"; monthly close with a CSV that adds up.

## Six sentences to remember

1. Expected money is shown but never counted until it's confirmed.
2. The salary's bonus lives on the salary, so totals stay simple and the dependable part stays visible.
3. Every amount is whole rupees; USD is kept beside the rupees that arrived.
4. Screens read Room, Firestore keeps Room fresh, so nothing waits for the network.
5. One ownership rule plus field validation protects every collection.
6. A missed week costs two minutes in catch-up, not the whole habit.

## Glossary

StateFlow (state that always has a value) · Flow (a stream) · `viewModelScope` (coroutines tied to the ViewModel) · Hilt `@Binds` (interface → implementation) · snapshot listener (live Firestore query) · `@Transaction` (all-or-nothing Room change) · WorkManager (guaranteed background work) · committed vs discretionary · confirmed vs expected · dependable vs variable.

## Demo order

Log in → calendar → add coffee → Expense insights → confirm salary → pay yourself first → Income insights → freelance payment → bell → monthly close → CSV → airplane mode → code walk.
