# Developer guide

## Prerequisites

- Android Studio (latest stable) with its bundled JDK
- An emulator or phone on Android 8.0 (API 26) or newer
- Access to the Firebase project `moneytrail-se3092`, or your own project

## Setup

1. Clone the repository and open it in Android Studio.
2. Put `google-services.json` in `app/` (never commit it).
3. Add your debug SHA-1 (`./gradlew signingReport`) to the Firebase Android app for Google sign-in.
4. In Firebase: Email/Password and Google sign-in enabled; Firestore created; `app/firestore.rules` published.

### Using your own Firebase project

1. Create a project, add an Android app with package `lk.moneytrail.app` and your SHA-1.
2. Download its `google-services.json` into `app/`.
3. Enable Email/Password and Google sign-in, create Firestore, publish `app/firestore.rules`.

The Gradle plugin turns `google-services.json` into `R.string.default_web_client_id`, which Google sign-in needs.

## Run

| Task | Command |
| --- | --- |
| Build a debug APK | `./gradlew assembleDebug` |
| Install on a connected device | `./gradlew installDebug` |
| Run the unit tests | `./gradlew testDebugUnitTest` |
| Both, before a pull request | `./gradlew assembleDebug testDebugUnitTest` |

Look at the Room tables while the app runs: **App Inspection → Database Inspector**. Run the reminder check at once: **Settings → Check reminders now**.

## Testing

- Unit tests live in `app/src/test`, mirroring `data`, `domain` and `presentation`. See [Testing](testing.md).
- New rules go into pure functions or use cases with tests; repositories are replaced by small fakes in the test file.
- Screens and flows are checked by hand on a device; the checklist is in the SRS Verification and Test Checklist document.

## Git workflow

1. Branch from `develop`: `feature/…`, `fix/…`, `docs/…`.
2. Keep commits few and focused, with plain-English messages ("Add income insights by source").
3. Build and run the tests before pushing.
4. Open a pull request into `develop`; merge `develop` into `main` for a release. `main` only accepts pull requests.
5. A release: raise `versionCode` and `versionName` in `app/build.gradle.kts`, build the APK, and attach it to a GitHub release tagged `vX.Y` on `main`.
6. If a pull request changes `app/firestore.rules`, publish the rules once it's merged.

## Code rules

- Kotlin only; UI only in Compose; no XML except `AndroidManifest.xml`.
- MVVM: no business logic or data calls in Composables; ViewModels expose `StateFlow`; use `hiltViewModel()`.
- Colours from `MaterialTheme.colorScheme`, text from `MaterialTheme.typography`; never hard-coded.
- Amounts in whole LKR (`Long`), dates as `LocalDate` in Kotlin and `yyyy-MM-dd` in Firestore.
- Every screen: loading, empty and error states; every list key stable; every icon a content description or `null` when decorative.
- No hard-coded user data or sample data outside previews.
- No dependency version bumps in a feature branch.
