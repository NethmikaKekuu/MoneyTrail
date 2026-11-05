# ADR-0002: Screens read Room; Firestore keeps Room up to date

**Status:** Accepted

## Context

Kavindu opens the app on the bus and in places with poor signal. Waiting for Firestore before drawing the calendar would show empty screens, and an empty month looks like he spent nothing.

## Decision

- Entries are copied into Room. `FirestoreEntryRepository.observeCached` runs a Firestore snapshot listener that replaces the month's rows in Room in one `@Transaction`, and the screen collects Room's `Flow`.
- A month already cached shows at once; a month never opened waits for the first snapshot instead of flashing empty.
- Writes go to Firestore, whose persistent cache applies them locally straight away and uploads them later. `awaitServerBriefly` waits up to 3 s online, so a rules rejection can still be shown.
- Only entries are in Room. The plan, accounts, categories, projects, bills and closes are small and use Firestore's own cache.

## Consequences

- Screens work offline, with an offline banner.
- Deletions on another phone disappear too, because the whole range is replaced.
- Every Room query filters by uid, so another account on the same phone never sees old data.
- Two copies of entries means one more place to keep in step; the listener is the only writer to Room.
