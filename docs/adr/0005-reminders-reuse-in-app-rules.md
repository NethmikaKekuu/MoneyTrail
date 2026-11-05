# ADR-0005: Phone reminders use the same rules as the in-app cards

**Status:** Accepted

## Context

The calendar already shows cards for money due, pay yourself first, catch-up and overdue invoices. Reminders that worked out these things separately could disagree with what the app shows, for example a notification for an invoice the app says is not overdue.

## Decision

- `GetRemindersUseCase` decides what is due. The calendar cards and the daily `ReminderWorker` both call it.
- WorkManager runs the worker around 9:00 every day (`KEEP`, so the booked time doesn't move), even when the app is closed.
- Each reminder is saved to a Room history (kept 30 days) and posted as one notification per kind, at most once a day per kind.
- A notification carries its kind; tapping it opens the right screen through the same `openReminder` the in-app list uses.

## Consequences

- A rule change, such as "due today is not overdue", changes the app and the notifications together.
- The worker can't take constructor arguments from Hilt, so it gets the use case through a Hilt entry point.
- The reminders can be checked at any time from Settings with "Check reminders now".
