# Diagrams

All diagrams are Mermaid, so GitHub draws them in place.

## 1. Use case diagram

```mermaid
flowchart LR
    K((Kavindu))
    S((Daily scheduler<br/>WorkManager 9:00))
    F((Firebase<br/>Auth + Firestore))
    subgraph MoneyTrail
        UC1([Register / log in])
        UC2([Complete onboarding])
        UC3([Log income or expense])
        UC4([Confirm expected money])
        UC5([Record AdSense payout in USD])
        UC6([View folders and insights])
        UC7([Track goal: pay yourself first])
        UC8([Manage freelance projects])
        UC9([Record freelance payment])
        UC10([Catch up after a break])
        UC11([Balance an account])
        UC12([Close a month])
        UC13([View statement and export CSV])
        UC14([Open notifications])
        UC15([Get daily reminders])
        UC16([Change theme and categories])
    end
    K --- UC1 & UC2 & UC3 & UC4 & UC5 & UC6 & UC7 & UC8 & UC10 & UC12 & UC13 & UC14 & UC16
    S --- UC15
    F --- UC1
    UC9 -. extends .-> UC8
    UC10 -. includes .-> UC11
    UC12 -. includes .-> UC11
```

## 2. Architecture (MVVM, layered)

```mermaid
flowchart LR
    subgraph Presentation
        Screen[Compose screen<br/>XScreen / XContent]
        VM[ViewModel<br/>StateFlow of XState]
    end
    subgraph Domain
        UC[Use cases]
        Model[Models and pure functions]
        Repo[Repository interfaces]
    end
    subgraph Data
        FS[Firestore repositories]
        Room[(Room: entries,<br/>notifications)]
        WM[WorkManager<br/>reminders]
        Http[USD rate API]
        Prefs[SharedPreferences]
    end
    Screen -- events --> VM
    VM -- state --> Screen
    VM --> UC
    VM --> Repo
    UC --> Model
    UC --> Repo
    Repo -. bound by Hilt .-> FS & Room & WM & Http & Prefs
    FS --> Cloud[(Cloud Firestore)]
    FS --> Auth[Firebase Auth]
```

## 3. Write path: logging an expense

```mermaid
sequenceDiagram
    actor K as Kavindu
    participant S as AddEntryScreen
    participant V as AddEntryViewModel
    participant U as AddEntryUseCase
    participant R as FirestoreEntryRepository
    participant F as Firestore (with local cache)
    participant D as Room
    participant C as CalendarViewModel
    K->>S: types 850, picks Coffee, Save
    S->>V: onSaveClick()
    V->>U: invoke(entry)
    U->>U: check amount, set spendingType
    U->>R: addEntry(entry)
    R->>F: set users/{uid}/entries/{id}
    F-->>R: applied locally (server ack ≤ 3 s if online)
    R-->>V: Result.success
    V-->>S: close the screen
    F-->>R: snapshot listener: month changed
    R->>D: replaceBetween(month)
    D-->>C: Flow emits new entries
    C-->>K: calendar shows the entry and new totals
```

## 4. Reading offline

```mermaid
flowchart LR
    FS[(Firestore)] -- snapshot listener<br/>while the screen is open --> L[FirestoreEntryRepository]
    L -- replaceBetween --> R[(Room entries)]
    R -- Flow --> VM[ViewModel] --> UI[Screen]
    W[Add / edit / delete] --> FS
    N[ConnectivityRepository] -- offline --> B[Offline banner]
```

## 5. Expected money and pay day

```mermaid
stateDiagram-v2
    [*] --> Expected: salary added on pay day<br/>or planned by the user
    Expected --> Confirmed: "Did it come in?"<br/>real amount (bonus kept)
    Expected --> [*]: "Didn't happen"
    Confirmed --> PayYourselfFirst: salary confirmed
    PayYourselfFirst --> Moved: "I've moved it"
    PayYourselfFirst --> Snoozed: "Remind me tomorrow"
    Snoozed --> PayYourselfFirst: next day
    Moved --> [*]
```

## 6. Freelance milestone

```mermaid
stateDiagram-v2
    [*] --> Planned: added with the project
    Planned --> Invoiced: Mark invoiced
    Invoiced --> Overdue: due date passes
    Overdue --> Paid: Record payment
    Invoiced --> Paid: Record payment
    Paid --> [*]: Freelance income on the calendar
    note right of Overdue: daily reminder<br/>"invoice overdue"
```

## 7. Reminders and notifications

```mermaid
sequenceDiagram
    participant WM as WorkManager (9:00)
    participant W as ReminderWorker
    participant G as GetRemindersUseCase
    participant N as ReminderNotifier
    participant L as Room notifications
    actor K as Kavindu
    WM->>W: doWork()
    W->>G: what is due today?
    G-->>W: money due, pay yourself first, catch-up, invoices
    W->>N: show(reminders)
    N->>L: add to history (unread)
    N-->>K: phone notification per kind
    K->>N: taps a notification
    N->>K: app opens on that place
    Note over L: only that kind is marked read;<br/>the rest stay in the list
```

## 8. Monthly close

```mermaid
flowchart TD
    A[More → Monthly close & statement] --> B[Close month]
    B --> C[Calculated balance per account<br/>last check + entries to the last day]
    C --> D[Type each real balance]
    D --> E{Real = calculated?}
    E -- yes --> G[Save the close]
    E -- no --> F[Add 'Not logged' entry<br/>on the last day]
    F --> G
    G --> H[Real balance = start of next month]
    H --> I[Statement: Books balanced]
    I --> J[Export CSV]
```

## Navigation map

```mermaid
flowchart TD
    Splash --> Welcome & Dashboard
    Welcome --> Login & Register
    Login --> ForgotPassword
    Login & Register --> Onboarding --> Dashboard
    subgraph Dashboard[Dashboard: bottom tabs]
        Calendar
        Folders
        Goal
        Insights
        More
    end
    Calendar --> AddEntry --> UsdPayout
    Calendar --> EditEntry
    Calendar --> Notifications
    More --> Notifications & Freelance & Salary & CatchUp & Statement & Settings
    Freelance --> AddProject & RecordPayment
    Statement --> MonthlyClose
    Notifications -. opens the place .-> Calendar & Goal & CatchUp & Freelance
```
