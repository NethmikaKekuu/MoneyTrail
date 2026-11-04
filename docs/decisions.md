# Decisions

Why MoneyTrail is built the way it is. Each choice comes back to Kavindu: irregular income from four sources, impulsive spending, one clear goal, and four tools that failed him because they were too rigid, too manual or too unpleasant to open.

## Tech choices

| Choice | Instead of | Why |
| --- | --- | --- |
| Kotlin + Jetpack Compose | Java, XML layouts | Required by the brief; Compose state fits MVVM directly |
| Hilt | Koin, manual factories | Preferred by the brief; compile-time checked; `hiltViewModel()` means no ViewModel is made by hand |
| Firebase Auth (email + Google) | Custom auth | Required; Google sign-in is bonus credit and one tap for him |
| Cloud Firestore as the main store | Realtime Database, Room only | Required; live listeners and an offline write cache built in |
| Room for entries | Firestore cache alone | Reads that never wait on the network, and a queryable copy for the main screens |
| WorkManager for reminders | AlarmManager | Survives the app being closed and the phone restarting; respects battery rules |
| A free USD rate API over HTTP | Typing the rate by hand | He "never records what he actually receives"; filling the rate in removes a step |
| Spark (free) Firebase plan | Blaze with Cloud Functions | No billing; every rule runs on the phone or in security rules |

## Data choices

| Choice | Why |
| --- | --- |
| Whole rupees as `Long` / Firestore int | Exact totals; cents don't change his decisions |
| `yyyy-MM-dd` date strings | Sort in order; one range query per month; no time-zone shifts |
| `CONFIRMED` and `EXPECTED` entries | Planned money (salary, milestones, rent) is visible without inflating real totals, and confirming it is one tap |
| Bonus stored on the salary entry | Totals stay simple, while base (dependable) and bonus (variable) stay apart for Income insights |
| LKR as the stored amount, USD beside it | His bank and goal are in LKR; past totals don't move when rates change |
| One `settings/plan` document | Every screen gets pay day, salary, sources and goals in one read |
| Monthly saving calculated, not stored | Stays correct as the months pass |
| Accounts store a balance check, not a running balance | A running balance drifts whenever he forgets an entry; "last real balance + entries since" can always be corrected by balancing |
| "Not logged" entries when balancing | Accepts that he will forget things, yet the month still adds up |
| Crypto kept apart from income planning | Q1 gain, Q3 loss: it can't be planned on |
| One close per month, id = month | Closing again replaces it; no duplicates |

## UI / theme choices

- **Calendar as home.** He thinks in days and pay days; a calendar shows gaps, pay day and expected money at once, and gives logging a natural place.
- **Keypad first on New entry.** The amount is what he remembers; account, date and committed/discretionary are filled in.
- **Five bottom tabs** (Calendar, Folders, Goal, Insights, More), with notifications reached from a bell on the calendar and a row on More.
- **Palette from the logo** in light and dark; **Sora** for headings, **DM Sans** for text; both from our Figma frames.
- **Every screen uses theme roles and type styles only.** No hard-coded colours, so dark mode has no white boxes, and no raw font sizes, so all screens read the same.
- **Grouped settings** under small uppercase headings over their own cards, like the More page.
- **Cards only when they matter**: the reminders card and the pay-yourself-first card appear only while something is due.
- **No XML except the manifest**: icons are `ImageVector`s, the theme is Kotlin.

## Behaviour choices

- **Catch-up after 3 days away** instead of a streak or a guilt message: his Google Sheet died because a missed week felt too big to fix.
- **Pay yourself first after the salary is confirmed**, not a monthly budget cap: budgets failed him; tying saving to pay day makes the goal move when the money arrives.
- **Coffee and dining shown as days of goal saving**: links the LKR 24,000 surprise to the MacBook.
- **Reminders once a day per kind at 9:00**, and only when something is due.
- **Tapping a reminder opens its place and marks only that one read**; others stay as history for a month.
- **Statement CSV with LKR amounts, expenses negative**, so a spreadsheet `SUM` equals "Kept".

## Architecture rules

1. Screens draw state and send events; no business logic, no Firebase or Room calls in Composables.
2. ViewModels expose a read-only `StateFlow<XState>`; all async work in `viewModelScope`.
3. A rule that is more than a pass-through goes in a use case or a pure function in `domain/model`, so it can be unit tested and reused (the reminder worker and the in-app cards use the same rules).
4. Repositories are interfaces in `domain`, implementations in `data`, bound in `di/RepositoryModule.kt`; they return `Result` with an `AppException` message fit to show.
5. Every screen has loading, empty and error states.
6. Every Firestore collection has a validation function in `app/firestore.rules`.

## Reusable components: keep the bar high

Add something to `presentation/components/` only when two or more screens need it and it has no feature logic. Shared components take state and lambdas, use theme roles and type styles, and have a preview.
