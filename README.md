# MoneyTrail 
Full feature list, in the order Kavindu meets each part of the app. Each feature is tagged by priority:

- **[M] Must:** build first. The app fails the scenario without it.
- **[S] Should:** build once all Musts work.
- **[C] Could:** only if time remains.

**1. Sign in, sign out and account**
- [M] Register with email and password (the brief makes this mandatory).
- [M] Sign in with email and password.
- [M] Stay signed in between app launches; go straight to the dashboard if already signed in.
- [M] Sign out from Settings.
- [M] Clear error messages for wrong password, existing email, weak password, empty fields and no internet.
- [S] Google sign-in (bonus credit in the brief).
- [S] Forgot password: send a reset email.
- [C] Delete account and all data.

**2. Onboarding (first launch after registering)**
- [M] Choose income sources: Salary, Freelance, AdSense, Crypto.
- [M] Enter salary day (25th) and usual amount.
- [M] Add rent and other fixed monthly costs.
- [M] Create the goal: name, target amount, target date, current savings.
- [S] Set AdSense currency to USD.
- [S] Pick a daily reminder time.
- [S] Skip any step and finish it later from the dashboard.

**3. Dashboard (home)**
- [M] This month's income received, total spent and net saved.
- [M] Goal progress: amount saved, percentage and required monthly saving.
- [M] Pending income cards (e.g. salary due), confirmed with one tap.
- [M] Recent transactions list.
- [M] Live updates through a real-time Firestore listener (required by the brief).
- [M] A friendly empty state for a brand-new user, never a blank screen.
- [S] Safe-to-spend today figure.
- [S] Offline indicator when there's no connection.

**4. Expenses**
- [M] Quick Add button on every main screen: amount, then category, then save.
- [M] Each expense stores amount, category, payment channel, date and an optional note.
- [M] Committed or discretionary type set automatically from the category, and changeable.
- [M] Default categories: Rent, Utilities, Food and dining, Transport, Subscriptions, Health and fitness, Shopping, Entertainment, Education, Other.
- [M] Default payment channels: Card, Cash, Secondary account, Bank transfer, App wallet.
- [M] Edit and delete an expense.
- [S] Remember the last used channel and show the most-used categories first.
- [S] Add custom categories.
- [S] "Nothing spent today" check-in.
- [C] Saved templates (e.g. "PickMe to office").
- [C] Expenses in a currency other than LKR.

**5. Income**
- [M] Log income from a source with amount, currency, exchange rate and LKR value. The LKR value is fixed at save time.
- [M] Income status: Pending or Received. Only Received counts toward monthly income.
- [M] Crypto entered as a realised gain or loss (plus or minus).
- [M] Edit and delete income.
- [S] Recurring income rules that create Pending entries automatically (salary monthly, AdSense monthly).
- [S] Freelance projects with milestones, each marked Received when paid.
- [S] Year-to-date net crypto position.
- [C] Reminder for unpaid invoices after 14 days.
- [C] Binance CSV import.

**6. Recurring bills and subscriptions**
- [M] Create recurring expenses (rent, gym, subscriptions) with amount, interval and due date.
- [M] Recurring entries created automatically on the due date.
- [M] Stop a recurring item.
- [S] Adjust variable bills such as utilities each month.
- [S] Subscriptions list showing monthly and yearly cost.

**7. Savings goal**
- [M] Create and edit the goal.
- [M] Add contributions.
- [M] Show saved amount, percentage and required monthly saving.
- [S] Projected completion date based on his actual saving pace.
- [C] Goal delay message, e.g. "dining this week = 4 days later."

**8. Insights**
- [M] Committed vs discretionary donut chart.
- [M] Spending by category bar chart.
- [M] Income by source for the month.
- [M] Month selector.
- [S] This week vs last week spending.
- [S] Tap a chart section to see its transactions.
- [C] 6-month income trend.

**9. History**
- [M] All transactions grouped by month.
- [S] Filter by category, type, channel or income source.
- [C] Search by note.

**10. Reminders and getting back on track**
- [S] Daily reminder, sent only if nothing was logged that day.
- [S] Catch-up card after 3 or more days without entries.
- [S] One lump "Unaccounted" entry to fill a gap quickly.
- [C] Notification buttons for Log now and Nothing spent.
- [C] "Start fresh from today", with the gap excluded from averages.
- [C] Escalating nudges on day 2, 7, 14 and 30.
- [C] Bank statement CSV import with a weekly review.

**11. Settings**
- [M] Sign out.
- [M] Manage income sources.
- [S] Manage categories.
- [S] Reminder time and notification on/off.
- [C] Dark mode toggle (follow the system setting by default).

**12. Built into everything (required by the brief, not optional)**
- [M] Works offline using Room and syncs when back online.
- [M] Loading, error and empty states on every screen; no crashes or blank screens.
- [M] Money stored as whole cents; amounts shown as "LKR 1,234.00".
- [M] Each user can only access their own data (security rules).
