# Dependencies

Versions are kept in `gradle/libs.versions.toml`. Only what the app needs is added, and versions aren't changed in feature branches.

| Area | Library | Version | Used for |
| --- | --- | --- | --- |
| Build | Android Gradle Plugin | 9.3.3 | Building the app |
| Build | Kotlin (+ Compose compiler plugin) | 2.2.10 | Language and Compose |
| Build | KSP | 2.3.12 | Hilt and Room code generation |
| Build | Google Services plugin | 4.5.0 | Reads `google-services.json` |
| UI | Compose BOM | 2026.02.01 | Compose UI, Material 3, tooling, previews |
| UI | Activity Compose | 1.13.0 | `setContent`, permission and file-picker launchers |
| UI | Navigation Compose | 2.9.8 | The single navigation graph |
| UI | Lifecycle (runtime, ViewModel, runtime-compose) | 2.11.0 | `ViewModel`, `viewModelScope`, `collectAsStateWithLifecycle`, `LifecycleEventEffect` |
| Core | AndroidX Core KTX | 1.19.0 | `NotificationManagerCompat`, `ContextCompat` |
| DI | Hilt | 2.60.1 | Dependency injection |
| DI | AndroidX Hilt (navigation-compose) | 1.4.0 | `hiltViewModel()` |
| Firebase | Firebase BOM | 34.17.0 | Authentication and Firestore |
| Firebase | kotlinx-coroutines-play-services | 1.10.2 | `await()` on Firebase `Task`s |
| Sign-in | AndroidX Credentials (+ play-services-auth) | 1.6.0 | Google sign-in through Credential Manager |
| Sign-in | Google ID | 1.1.1 | Google ID token credential |
| Local | Room (runtime, ktx, compiler) | 2.8.5 | Entries cache and notification history |
| Background | WorkManager (runtime-ktx) | 2.11.2 | Daily 9:00 reminder check |
| Test | JUnit | 4.13.2 | Unit tests |
| Test | AndroidX JUnit, Espresso | 1.3.0, 3.7.0 | Instrumented test setup (from the template) |

## Not used on purpose

- No image loading, networking or JSON libraries: the USD rate is read with `HttpURLConnection` and `org.json`, which Android already has.
- No charting library: the insights bars are plain Compose boxes, which keeps them on the theme in light and dark.
- No Firebase Cloud Functions or Analytics: the project is on the free Spark plan.

## SDK levels

`minSdk 26` (Android 8.0), `targetSdk 36`, Java 11 bytecode. `java.time` (`LocalDate`, `YearMonth`) is available from API 26, so no desugaring is needed.
