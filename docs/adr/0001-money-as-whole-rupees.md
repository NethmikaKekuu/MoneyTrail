# ADR-0001: Store money as whole rupees, with USD beside it

**Status:** Accepted

## Context

Kavindu's money is in LKR, except AdSense, which is paid in USD and lands in his bank at a rate and fee he never records. Floating-point amounts would make totals drift by cents, and storing USD alone would make past months change whenever the rate changes.

## Decision

- Every amount is a whole number of rupees (`Long` in Kotlin, `int` in Firestore). `amount` is always positive; `type` says which way the money went.
- An AdSense entry stores the rupees that arrived as `amount`, and keeps `usdCents`, `effectiveRate` (hundredths of a rupee) and `feeLkr` next to it.
- The day's market rate only fills the form in; what the bank credited is what is saved.

## Consequences

- Totals are exact and the monthly close CSV adds up to the rupee.
- Old months never move when the rate changes.
- The rules check each money field is an `int` in range, so a fractional or negative amount is rejected by the server too.
- Cents are lost, which doesn't change any decision he makes.
