# Testing

MoneyTrail has **203 unit tests in 39 classes**, all plain JUnit 4 on the JVM. They test the rules: the domain layer has no Android or Firebase imports, so its functions and use cases run without an emulator.

```
./gradlew testDebugUnitTest
```

Reports: `app/build/reports/tests/testDebugUnitTest/index.html`.

## Layout

```
app/src/test/java/lk/moneytrail/app/
├── data/           Room row conversion, reminder times, theme preferences
├── domain/         models and use cases: the bulk of the tests
└── presentation/   screen state classes and their derived values
```

## How the tests are written

- **Pure functions first.** Most rules live in `domain/model` as functions on plain data (`List<Entry>.dependability(month)`, `Account.balanceAtEndOf(day, entries)`), so a test is "make a list, call the function, check the number".
- **Fakes, not mocks.** A use case test defines tiny private fakes of the repositories it needs (`FakeEntryRepository` that keeps a list, `FakeCloseRepository` that remembers what was saved), then checks what was written.
- **Kavindu's numbers.** Tests use the scenario's figures: base salary 120,000 with a 13,400 bonus, rent 34,000, the MacBook goal of 490,000 with 11,200 saved (`kavinduNeeds39900AMonth`).
- **Fixed dates.** Tests pass `today` in; nothing reads the clock, so they never depend on the day they run.
- **`runBlocking`** for suspend use cases; flows are read with `first()`.
- **Names say the rule**, e.g. `dependability_isTheBaseSalaryOnly`, `csv_saysTheMonthAndDateAndItsEntriesAddUpToWhatWasKept`.

## What is covered

| Area | Test classes | Examples of what they check |
| --- | --- | --- |
| Entries | `AddEntryUseCaseTest`, `UpdateEntryUseCaseTest`, `EntryTotalsTest`, `ExpectedEntriesTest`, `EntryEntityTest` | Spending type from the category; zero amount refused; expected money kept out of totals; a confirmed salary keeps its bonus; Room rows round-trip |
| Categories and onboarding | `CategoriesTest`, `CompleteOnboardingUseCaseTest`, `OnboardingStateTest` | Own categories after built-ins; unique account slugs; every place needs a starting balance; nothing saved when accounts fail |
| Salary | `SalaryUseCasesTest`, `SalaryStateTest` | Salary expected on pay day; pay day 31 in a short month; changing the salary moves the expected entry and keeps its bonus |
| Goals | `SavingsGoalTest`, `GoalModelTest`, `GoalSavingsTest`, `GoalStateTest` | 39,900 a month for the MacBook; savings due after the salary is confirmed; "I've moved it" never passes the target; remind tomorrow |
| Insights and folders | `SpendingInsightsTest`, `IncomeInsightsTest`, `FolderTotalsTest`, `FoldersStateTest` | Committed vs discretionary; dining as days of saving; income by source; dependable = base salary; crypto kept apart |
| AdSense USD | `UsdConversionTest`, `UsdPayoutStateTest` | Effective rate and fee; parsing and showing two decimals; typed rupees override the rate |
| Catch-up and balancing | `CatchUpTest` | Offered after 3 days; "Not logged" for unlogged money; entries on the check day count once |
| Monthly close and statement | `MonthlyCloseTest` | Balance at the end of a month; opening from last month's close; the CSV's amounts add up to "Kept" |
| Freelance | `FreelanceModelTest`, `FreelanceViewModelTest`, `AddProjectViewModelTest`, `RecordFreelancePaymentTest`, `RecordFreelancePaymentViewModelTest`, `OverdueInvoicesTest` | Totals across projects; due today is not overdue; projects needing attention first; the price is only what was typed; split left and over; no milestones → one "Full payment"; a milestone can't go past the price; due order; the next unpaid milestone; overdue invoices oldest first |
| Reminders | `GetRemindersUseCaseTest`, `RefreshNotificationsUseCaseTest`, `ReminderScheduleTest` | Money due, pay yourself first and catch-up reminders; a done reminder is marked read; next 9:00 check |
| Input and calendar UI state | `AmountInputTest`, `AmountVisualTransformationTest`, `CalendarDaysTest`, `MoreStateTest`, `ThemePreferencesTest` | Keypad digits; commas without moving the cursor; Monday-to-Sunday grid with markers; theme saved and restored |

## What is tested by hand

Screens, navigation, Firebase and notifications need a device. They are covered by the **SRS Verification and Test Checklist** (69 function tests, 9 end-to-end flows, 14 non-functional checks), run on an Android 8.0 emulator and a recent phone. The security rules are tested automatically in `firestore-rules-tests/rules.test.js` on the Firestore emulator: 35 checks covering ownership, every collection's valid and invalid documents, server timestamps, and the catch-all rule. They run in CI whenever the rules change; see the [Developer guide](development.md#testing).

## Running part of the suite

```
./gradlew testDebugUnitTest --tests "lk.moneytrail.app.domain.MonthlyCloseTest"
./gradlew testDebugUnitTest --tests "*IncomeInsightsTest.dependability*"
```

## Adding a test

1. Put the rule in a pure function or a use case, not in a ViewModel or Composable.
2. Add the test in the matching folder, named `<Thing>Test`.
3. Use fixed dates and Kavindu's figures; write small private fakes for repositories.
4. Run the whole suite before pushing.
