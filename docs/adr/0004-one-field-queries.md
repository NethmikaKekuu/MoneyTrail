# ADR-0004: Only one-field queries, so no composite indexes

**Status:** Accepted

## Context

Composite indexes have to be created in the Firebase console or deployed. A query that needs one fails at runtime until it exists, which is easy to miss on a new Firebase project or a teammate's copy.

## Decision

- Dates are stored as `yyyy-MM-dd` strings, which sort in date order, so a month is one range query on `date`.
- Other reads are a whole small collection (accounts, categories, projects, bills) or a single document (`settings/plan`, `closes/{month}`).
- Anything more, such as filtering by account or category, is done in Kotlin on the month's entries.

## Consequences

- `firestore.indexes.json` is empty and stays empty; the app works on a fresh project with no setup.
- No time-zone shifts, because a date string has no time.
- Filtering in Kotlin reads a whole month of entries, which is a few hundred documents at most for one person.
