# Firestore schema

## Conventions

- Everything a user owns sits under `users/{uid}`, so one ownership check protects it all and every query is already scoped to one person.
- **Amounts are whole rupees (LKR) as integers.** No floating point, so totals are exact.
- **Dates are `yyyy-MM-dd` strings** (months `yyyy-MM`). They sort in date order, a month is one range query, and there are no time-zone surprises.
- Server timestamps (`FieldValue.serverTimestamp()`) for `createdAt` and `updatedAt`; the rules check them against `request.time`.
- Enums are stored as their names (`INCOME`, `CONFIRMED`, `COMMITTED`…).
- Every collection has a validation function in `app/firestore.rules`.

## Collection map

```
users/{uid}                        profile
├── entries/{entryId}              one income or expense
├── accounts/{accountId}           a place money sits (main-bank, cash-wallet…)
├── categories/{categoryId}        a category the user added
├── settings/plan                  pay day, salary, income sources, goals
├── projects/{projectId}           a freelance project with its milestones
├── closes/{yyyy-MM}               a closed month's real balances
└── cryptoTrades/{tradeKey}        one trade from a Binance CSV import
```

On the phone only (Room, not Firestore): `entries` (a copy for offline reading) and `notifications` (reminder history, kept 30 days after being read).

## `users/{uid}`

| Field | Type | Notes |
| --- | --- | --- |
| `name` | string | ≤ 100; may be empty for a Google account with no display name |
| `email` | string | ≤ 254 |
| `createdAt` | timestamp | Server time on create; can't change |

## `users/{uid}/entries/{entryId}`

| Field | Type | Notes |
| --- | --- | --- |
| `type` | string | `INCOME` or `EXPENSE` |
| `status` | string | `CONFIRMED` or `EXPECTED` |
| `amount` | int | 1 to 999,999,999 LKR, always positive; `type` gives the direction |
| `category` | string | 1–50; a built-in one or one of the user's |
| `account` | string | 1–50; the account's name, e.g. `Main bank` |
| `date` | string | `yyyy-MM-dd` |
| `note` | string | ≤ 200, may be empty |
| `spendingType` | string or null | Expenses: `COMMITTED` or `DISCRETIONARY`; income: `null` |
| `bonus` | int | For a Salary income, the part of `amount` above the base pay; otherwise 0 |
| `usdCents` | int | For a USD payout (AdSense), the dollars earned in cents; otherwise 0 |
| `effectiveRate` | int | Rupees per dollar actually received, in hundredths; 0 unless `usdCents > 0` |
| `feeLkr` | int | Rupees lost to bank and conversion against the day's market rate; 0 unless USD |
| `createdAt` | timestamp | Server time; orders entries within a day and finds the last logged day |

### Multi-currency

`amount` is always the LKR that really arrived: that is what reached the bank and what the goal is measured in, so every total, chart and goal stays in one currency. A USD payout keeps the original dollars, the rate it got and the fee next to it. The market rate is fetched when the payout is recorded (`HttpUsdRateRepository`, with a fallback source). Converting at read time was rejected because past totals would change whenever the rate changed.

## `users/{uid}/accounts/{accountId}`

Id is a slug: `main-bank`, `secondary-bank`, `cash-wallet`, `savings`, `binance`, or a slug of a name the user typed (`sampath-bank`).

| Field | Type | Notes |
| --- | --- | --- |
| `name` | string | 1–50; stored on entries as `account` |
| `description` | string | ≤ 100 |
| `kind` | string | `BANK`, `CASH`, `SAVINGS`, `CRYPTO` |
| `checkedBalance` | int | Optional; what the account held at the start of `checkedOn` |
| `checkedOn` | string | Optional; `yyyy-MM-dd` of the last real balance |
| `createdAt` | timestamp | Server time |

The app's balance for an account is `checkedBalance` + confirmed income − confirmed expenses logged in it from `checkedOn` on. Onboarding, catch-up and the monthly close set the check.

## `users/{uid}/categories/{categoryId}`

| Field | Type | Notes |
| --- | --- | --- |
| `name` | string | 1–30, not the same as a built-in of that type |
| `type` | string | `INCOME` or `EXPENSE` |
| `spendingType` | string or null | Expense default `COMMITTED` / `DISCRETIONARY`; income `null` |
| `createdAt` | timestamp | Server time; categories can be created and deleted, not changed |

## `users/{uid}/settings/plan`

One document so every screen gets the whole plan in one read.

| Field | Type | Notes |
| --- | --- | --- |
| `payDay` | int | 1–31; 31 falls on the last day of shorter months |
| `baseSalary` | int | LKR before bonuses; 0 when there's no salary |
| `incomeSources` | array | Any of `SALARY`, `FREELANCE`, `ADSENSE`, `CRYPTO`, `OTHER` |
| `goal` | map | The first goal: `id`, `name`, `targetAmount`, `savedAmount` (≤ target), `targetDate`, `transferDay`, `allowEmergencyWithdrawals` |
| `goals` | array of maps | Every goal, same fields; up to 20 |
| `savingsMovedMonth` | string | `yyyy-MM` of the last "I've moved it" |
| `savingsRemindOn` | string | `yyyy-MM-dd` set by "Remind me tomorrow" |
| `updatedAt` | timestamp | Server time on every save |

The monthly saving needed is calculated (`SavingsGoal.monthlySavingNeeded`), not stored, so it stays right as months pass.

## `users/{uid}/projects/{projectId}`

| Field | Type | Notes |
| --- | --- | --- |
| `name`, `client` | string | ≤ 100 |
| `totalBudget` | int | LKR; the agreed project price, typed when the project is made. Milestones can add up to less (the rest is "not split yet") but not more; adding a milestone past the price raises it |
| `milestones` | array of maps | `id`, `title`, `amount`, `status` (`planned`, `invoiced`, `paid`), `dueTimestamp` (start of the due day, ms); up to 50. A project saved without milestones gets one "Full payment" milestone for the whole price |
| `updatedAt` | timestamp | Server time |

Milestones are a list inside the project because a project has only a few and they are always shown together. Recording a payment writes a confirmed `Freelance` income entry first, then marks the milestone paid, so a milestone is never paid without the money being on the calendar. The project id is made on the phone (a UUID), so the app can open the new project straight after saving, even offline.

## `users/{uid}/cryptoTrades/{tradeKey}`

| Field | Type | Notes |
| --- | --- | --- |
| `tradeId` | string | Binance's trade id, empty if the export has none |
| `date` | string | `yyyy-MM-ddTHH:mm:ss`, as in the export (UTC) |
| `pair`, `side` | string | e.g. `BTCUSDT`; `BUY` or `SELL` |
| `price`, `amount`, `total`, `fee` | number | In the pair's quote coin (usually USDT); `amount` > 0 |
| `feeCoin` | string | The coin the fee was paid in |
| `costBasis` | number or null | Only if the export has it |
| `importedAt` | timestamp | Server time |

The id is `id_<tradeId>`, or a UUID made from the date, pair, side, amount and price when there's no trade id, so importing the same file twice overwrites rather than doubles. Trades are not entries: realised profit or loss is worked out from them (first in, first out, after fees) and shown on the Crypto screen only, so crypto never counts as income or in the plan.

## `users/{uid}/closes/{yyyy-MM}`

| Field | Type | Notes |
| --- | --- | --- |
| `month` | string | Must equal the document id |
| `balances` | map | Account id → real balance (LKR) at the end of the month |
| `closedOn` | string | `yyyy-MM-dd` |

The month as the id guarantees one close per month; closing again replaces it.

## Queries and indexes

| Query | Used by | Index |
| --- | --- | --- |
| `entries` where `date >= start` and `date <= end` | Calendar, Folders, Insights, Statement, Catch-up | Automatic single-field |
| `entries` where `status == "EXPECTED"` | "Did it come in?", reminders | Automatic single-field |
| `entries` order by `createdAt` desc, limit 1 | Last logged day for catch-up | Automatic single-field |
| `projects`, `categories`, `accounts` (whole collection) | Freelance, entry screens | None |
| `settings/plan`, `closes/{month}` (single document) | Everywhere / Statement | None |

No composite index is needed: insights and folders read months by date range and group in memory instead of querying by type and date together.

## Security rules

`app/firestore.rules` is part of the code. Publish it whenever it changes: **Firestore Database → Rules**, replace everything with the file, **Publish**.

- `isOwner(uid)`: `request.auth.uid == uid` on every path.
- One validation function per collection (`isValidEntry`, `isValidAccount`, `isValidPlan`, `isValidGoal`, `isValidProject`, `isValidCategory`, `isValidClose`, `isValidCryptoTrade`): allowed keys only, types, ranges, fixed lists, date formats and server timestamps.
- Cross-field checks: a bonus only on a Salary income and below the amount; USD fields only on income; a goal's saved amount ≤ its target; a close's `month` equals its id.
- A catch-all allows other owner-only collections but excludes the validated ones, so their checks can't be skipped.

## Adding a new collection

1. Add the model in `domain/model/` and the interface in `domain/repository/`.
2. Write the Firestore implementation in `data/repository/` with a `TAG`, field-name constants and `toModel()` that returns null for unreadable documents.
3. Bind it in `di/RepositoryModule.kt`.
4. Add an `isValidX` function and a `match` block to `app/firestore.rules`, add the collection to the catch-all's exclusion list, and publish the rules.
5. Document it in this file.
