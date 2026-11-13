# Quick start

Get MoneyTrail running on an emulator or phone in about ten minutes.

1. Install **Android Studio** (latest stable) with an Android SDK and an emulator image of API 26 or newer.
2. Clone the repository:
   ```
   git clone https://github.com/NethmikaKekuu/MoneyTrail.git
   cd MoneyTrail
   ```
3. Put the team's **`google-services.json`** in `app/`. It is not committed; ask a teammate for it, or make your own Firebase project (see [Developer guide](development.md#using-your-own-firebase-project)).
4. Add your debug **SHA-1** to the Firebase Android app, so Google sign-in works on your machine:
   ```
   ./gradlew signingReport
   ```
   Copy the `SHA1` of the `debug` variant into **Firebase console → Project settings → Your apps → Add fingerprint**.
5. Open the project in Android Studio, let Gradle sync, and press **Run** on an emulator or a phone.
6. Register with an email and password, finish the five onboarding steps, and the calendar opens.

To check everything builds and the tests pass without Android Studio:

```
./gradlew assembleDebug testDebugUnitTest
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## If something goes wrong

| Problem | Fix |
| --- | --- |
| `File google-services.json is missing` | Step 3: the file must be at `app/google-services.json`. |
| Google sign-in says "No credentials available" | Step 4: your debug SHA-1 isn't in Firebase yet, or the phone has no Google account. |
| "Missing or insufficient permissions" when saving | The Firestore rules in the console are older than `app/firestore.rules`. Publish the file (see [Firestore schema](firestore-schema.md#security-rules)). |
| No reminder notifications | Allow notifications for MoneyTrail (Android 13+ asks on first open), then use **Settings → Check reminders now**. |
