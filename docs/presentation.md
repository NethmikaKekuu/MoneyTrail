# Presentation outline

A 15-minute engineering discussion with the panel, not a slide show. Keep slides few and spend most of the time in the app and the code. Rubric weights are in brackets.

## Slide 1: Title
MoneyTrail · SE3092 Assignment 01 · team names and IDs · one line: *"A money app for someone whose income doesn't arrive the same way twice."*

## Slide 2: The problem (Problem analysis 15%)
- Kavindu: LKR 1.6 million earned in a year, LKR 11,200 saved, can't say where it went.
- Four channels: salary with a bonus, freelance milestones, AdSense in USD, crypto.
- Four failed tools, one shared cause: too rigid, too manual, and a missed day felt unfixable.

## Slide 3: Requirements
- 26 functional requirements, each traced to a line of the scenario (show 4: confirmed vs expected, dependable vs variable income, catch-up, pay yourself first).
- Non-functional: offline, security, exact money, no crashes, API 26.
- Use case diagram ([Diagrams](diagrams.md#1-use-case-diagram)).

## Slide 4: Solution map
| Pain point | MoneyTrail answer |
| --- | --- |
| "Somewhere between 160 and 210k" | Income insights: confirmed vs expected, by source, dependable vs variable |
| Coffee and dining surprise | Committed vs discretionary; dining as days of goal saving |
| Goal "financially invisible" | Monthly saving needed; pay yourself first on pay day |
| Forgot invoices twice | Freelance milestones and overdue reminders |
| Sheet abandoned after a gap | Catch-up and balancing with "Not logged" |
| Can't reconcile | Monthly close, statement, CSV |

## Slide 5: Architecture (Architecture 15%)
MVVM diagram: Screen → ViewModel (StateFlow) → use case → repository interface → Firestore / Room / WorkManager. Hilt binds it. No logic in Composables.

## Slide 6: Firebase (Firebase 15%)
- Auth: email, Google, reset.
- Schema: everything under `users/{uid}`; entries with `status`, `bonus`, `usdCents`; plan in one document; closes by month.
- Indexing: every query is one field (`date` range, `status ==`), so no composite indexes.
- Multi-currency: LKR stored, USD kept beside it.
- Rules: ownership plus field validation; show `isValidEntry`.
- Real-time listeners feeding Room; offline writes.

## Slide 7: UI and UX (Compose UI 15%)
Calendar home, keypad entry, five tabs, Material 3 theme from the logo in light and dark, Sora and DM Sans, loading/empty/error everywhere.

## Slide 8: Edge cases and extras (Features 10%)
Pay day 31 in short months, salary with bonus, expected money that didn't happen, offline saves, first-time accounts in the monthly close, crypto losses kept out of planning, notifications that open the right place.

## Live demo (Demo 20%), about 7 minutes
1. Log in (Google). Calendar: month card, markers, today's entries.
2. **+** → 850 → Coffee → Save. It appears at once; Insights → Expenses shows the split and dining as days of saving.
3. Tap the expected salary → "Did it come in?" → 133,400 → Pay yourself first → **I've moved it** → Goal progress moves.
4. Insights → **Income**: by source, dependable 66% / variable 34%.
5. More → Freelance: Mark invoiced → Record payment → it's on the calendar.
6. Bell → a reminder → it opens the right place; the others stay.
7. More → Monthly close & statement → Close month → Books balanced → Export CSV.
8. Airplane mode → add an entry → offline banner → back online → show it in the Firestore console.
9. Open the code: `AddEntryViewModel` → `AddEntryUseCase` → `FirestoreEntryRepository` → `app/firestore.rules`.

## Questions to be ready for
| Question | Short answer |
| --- | --- |
| Where is the business logic? | Use cases and pure functions in `domain/`; Composables only draw state. |
| How does the dashboard update in real time? | Firestore snapshot listener → Room → `Flow` → `StateFlow` → recomposition. |
| What happens offline? | Reads from Room; writes queued by Firestore's persistent cache; banner shown. |
| Why `yyyy-MM-dd` strings? | Sort in order, one-field range query, no composite index, no time-zone shift. |
| How do you handle USD? | Store the LKR that arrived as `amount`; keep `usdCents`, `effectiveRate`, `feeLkr` beside it. |
| Why not count expected money? | It hasn't happened; counting it is how Kavindu overestimated his income. |
| How are rules enforced? | `isOwner(uid)` plus a validation function per collection; server timestamps checked. |
| How is the ViewModel created? | `hiltViewModel()`; Hilt injects repositories and `SavedStateHandle`. |
| What would you add next? | Transfers between own accounts, crypto losses, recurring bills. |
| How did you test it? | 172 unit tests on the domain rules, plus a device checklist. |

## Before the session
- [ ] Latest APK installed on the demo phone; signed in; notifications allowed.
- [ ] Demo data in place: salary expected, a project with an invoiced milestone, a few coffee entries, last month ready to close.
- [ ] Rules published; Firestore console open in a browser tab.
- [ ] Android Studio open on `AddEntryViewModel.kt` and `app/firestore.rules`.
- [ ] Phone charged, screen mirroring tested.
