# Architecture decision records

Short records of decisions that shape MoneyTrail: the situation, what was decided, and what follows from it. The wider list of choices is in [Decisions](../decisions.md).

| ADR | Decision |
| --- | --- |
| [ADR-0001](0001-money-as-whole-rupees.md) | Store money as whole rupees, with USD beside it |
| [ADR-0002](0002-screens-read-room.md) | Screens read Room; Firestore keeps Room up to date |
| [ADR-0003](0003-expected-is-not-counted.md) | Expected money is shown but never counted |
| [ADR-0004](0004-one-field-queries.md) | Only one-field queries, so no composite indexes |
| [ADR-0005](0005-reminders-reuse-in-app-rules.md) | Phone reminders use the same rules as the in-app cards |

## Adding one

Copy the shape of an existing record, give it the next number, and set **Status** to Proposed until the team agrees, then Accepted. A decision that is replaced keeps its file and says *Superseded by ADR-xxxx*.
