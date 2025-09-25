# Schmotz Calender (Android)

A couples' calendar + shared links app built with Kotlin & Jetpack Compose.

**Features**
- Email/password registration and login (persists across restarts)
- Shared **access code**: both of you enter the same code to see the same calendar & links
- Month view (Kizitonwose), add/edit/delete events, search, upcoming list
- Person filters (via search by creator name), Settings to change access code & account names
- Remember this device (Firebase Auth persistence), and Sign out
- Android "Share" target: save links (with thumbnail via OpenGraph) into a separate list with categories

## One‑time setup (about 10–15 minutes)
1. Install **Android Studio** (latest).
2. Create a free **Firebase** project at https://console.firebase.google.com
   - Enable **Authentication → Email/Password**
   - Enable **Firestore Database**
3. Add an Android app with package name `com.schmotz.calendar`.
   - Download the `google-services.json` and put it into `app/src/google-services.json` (replace placeholder).
4. Open this project in Android Studio and let Gradle sync.
5. Run on your phone or **Build → Generate Signed Bundle/APK…** to produce an APK for release.

## Firestore Structure
- `users/{uid}` → `UserProfile(uid, email, displayName, householdCode)`
- `households/{householdCode}/events/{eventId}` → `Event(...)`
- `households/{householdCode}/links/{linkId}` → `SharedLink(...)`

Add security rules to restrict access; simplest approach is to check that a user can only read/write documents where their `householdCode` matches.

## Notes
- Thumbnails come from Open Graph tags; some platforms block scraping. The link still saves even without a thumbnail.
- You can change the shared access code anytime in **Settings**.
- The app label is **Schmotz Calender**.
