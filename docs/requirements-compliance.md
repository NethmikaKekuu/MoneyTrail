# Assignment requirements compliance

Each constraint and deliverable from the SE3092 Assignment 01 brief, and where MoneyTrail meets it.

## Implementation constraints (brief 4.3 and 5)

| Requirement | Status | Where |
| --- | --- | --- |
| Written entirely in Kotlin; Kotlin DSL for Gradle | Met | All of `app/src/main/java` is `.kt`; `build.gradle.kts` |
| All UI in Jetpack Compose, no XML layouts | Met | No layouts at all; the only XML file is `AndroidManifest.xml` |
| Material Design 3 | Met | `presentation/theme/` (light and dark schemes, type scale) |
| Strict MVVM, no ViewModel logic in Composables | Met | `XScreen` / `XViewModel` / `XState` in every feature; rules in `domain/` |
| Data layer not called from the UI layer | Met | Screens only call ViewModel functions; repositories are injected into ViewModels |
| StateFlow, no raw mutable state exposed | Met | `private MutableStateFlow` + `asStateFlow()` in every ViewModel |
| Coroutines in `viewModelScope` | Met | Every async call in ViewModels |
| Hilt, no manual ViewModel creation | Met | `@HiltViewModel` + `hiltViewModel()`; `di/` modules |
| Navigation Compose, single activity | Met | `MainActivity` + `presentation/navigation/MoneyTrailNavHost.kt` |
| Firebase Authentication, email/password (Google is bonus) | Met | `FirebaseAuthRepository`, `RegisterUseCase`, `SignInWithGoogleUseCase`, forgot password |
| No hard-coded user data | Met | All data under `users/{uid}`; sample data only in `@Preview` functions |
| Firestore as the primary store | Met | `data/repository/Firestore*Repository.kt` |
| Real-time listeners on the dashboard | Met | Snapshot listeners on entries for the calendar and insights |
| Security rules submitted | Met | `app/firestore.rules` |
| Room offline caching, UI not blocked offline | Met | `data/local/` entries cache, offline banner, `awaitServerBriefly` |
| Min SDK 26, target 34+ | Met | `minSdk 26`, `targetSdk 36` |
| All error states, no blank screens, no crashes on empty data | Met | `LoadingView`, `ErrorView` (with Retry), `EmptyView` on every screen |

## Analysis lenses (brief 4.1)

| Lens | Answered by |
| --- | --- |
| Income structure across four sources | Confirmed vs expected entries; salary base and bonus; USD fields; Income insights by source and dependability |
| Why four tools failed | Fast keypad entry, defaults, catch-up and balancing, multi-source and multi-currency model, cloud sync |
| Making goal progress real | Monthly saving needed; pay yourself first on pay day; dining as days of goal saving |
| Low-friction entry | Keypad-first New entry; expected money confirmed in one tap |
| Discretionary vs committed views | Committed vs discretionary bar; top categories; six-month chart |

## Deliverables (brief 6)

| Deliverable | Status | Where |
| --- | --- | --- |
| Private repository, iterative commit history | Met | GitHub, feature branches and pull requests |
| README: overview, Firebase setup, build, architecture | Met | `README.md` and `docs/` |
| Technical Design Document (PDF, 15+ pages) | Met | Separate PDF: requirements, Firestore schema, MVVM diagram, annotated wireframes, decisions |
| APK on a live Firebase project, Android 8.0+ | Met | GitHub release v1.1 |
| Live demo and Q&A | Prepared | [Presentation outline](presentation.md), [Cheat sheet](cheatsheet.md) |
| AI-use disclosure | Included | Technical Design Document, section 10 |

## Rubric evidence

| Criterion | Evidence in the repository |
| --- | --- |
| Problem analysis and requirements (15%) | Design document sections 2–3; [Decisions](decisions.md) |
| Architecture and code quality (15%) | [Architecture](architecture.md); `domain/` use cases; 172 unit tests |
| Compose UI and UX (15%) | 24 screens; [Diagrams](diagrams.md#navigation-map); theme |
| Firebase integration (15%) | [Firestore schema](firestore-schema.md); `app/firestore.rules`; listeners; offline |
| Features and innovation (10%) | Income dependability, catch-up, monthly close and CSV, freelance invoices, AdSense USD, notification centre |
| Documentation (10%) | `README.md`, `docs/`, design document |
| Demo (20%) | [Presentation outline](presentation.md) |
