# ADR-0003: Expected money is shown but never counted

**Status:** Accepted

## Context

He overestimated his income because he counted money that was promised (the salary bonus, freelance milestones) as money he had. A finance app that adds planned money into totals repeats the same mistake.

## Decision

- An entry is `CONFIRMED` (money moved) or `EXPECTED` (planned for a date). Expected entries appear on the calendar and in their own total, but never in income, expenses, insights, balances or the monthly close.
- On the day, the calendar asks "Did it come in?" (or "Did you pay it?"): confirm with the real amount, move it, or remove it.
- The salary, recurring bills and freelance milestones all become expected entries, so they share the same one-tap check.

## Consequences

- Real totals stay honest, and the gap between planned and real is visible.
- One confirm flow covers salary, bills and freelance, instead of three.
- An expected entry he forgets to check keeps showing until he answers it, and the daily reminder asks about it.
