# Local Food Reviews — Kotlin + Firebase

A small, standard Android app where signed-in users can share restaurant reviews with one food photo, discover reviews, and search **restaurant name or cuisine**. It is deliberately designed for Firebase's no-payment-method **Spark** plan.

## Included pages / requirements

1. **Sign in** — email/password sign-in, field validation, visible errors, progress indicator, password-reset email, and direct entry to the Feed if Auth already has a signed-in user.
2. **Create account** — username, email, password, confirm password. Usernames are case-insensitive and atomically reserved in `usernames/{lowercaseUsername}`.
3. **Food review feed** — RecyclerView cards show photo, restaurant, cuisine, rating, username, date, and review text. Pull to refresh and live updates are included.
4. **Live search** — filters the loaded feed while typing, by restaurant **or** cuisine, ignoring uppercase/lowercase. Clearing the text shows all loaded reviews.
5. **Navigation drawer** — Feed, Add review, My reviews, Change password, and Log out.
6. **Change password** — current password + new password + confirmation. It re-authenticates before changing the password.
7. **Add review** — one required food photo, restaurant, cuisine, 1–5 rating, and a 10–1024 character review.
8. **My reviews** — shows only the signed-in user's reviews. Tapping **Edit review** opens an inline editor at the top of the same page; the user can replace the photo, edit all text/rating, save, or delete after confirmation.
9. **Firebase security rules** — `firebase/firestore.rules` ensures signed-in users can only create/update/delete their own reviews.

## Important: free Firebase / photos

As of the current Firebase policy, **Cloud Storage for Firebase requires the Blaze pay-as-you-go plan**, even where the usage may fall inside a no-cost allowance. Because you explicitly requested no paid service, this version **does not use Firebase Storage**.

Instead, before posting, each selected picture is resized and JPEG-compressed under approximately 330 KB, then stored as a small `photoBase64` data URI inside its Firestore review document. This means photos still work across devices while using only Firebase Authentication + Cloud Firestore Spark quota. It is appropriate for a compact classroom/demo app, not a large public photo product.

The app enforces a Firestore-safe image size and limits the feed to 100 recent reviews. Do not remove these limits on the Spark plan. For a production photo app, migrate `photoBase64` to Cloud Storage and update the model/rules after choosing a billing plan.

## Firebase data design

```text
users/{uid}
  uid, username, usernameLowercase, email, createdAt

usernames/{lowercaseUsername}
  uid, username

reviews/{reviewId}
  uid, username
  restaurantName, restaurantNameLowercase
  cuisine, cuisineLowercase
  rating, caption, photoBase64
  createdAt, updatedAt
```

## Open and run

### Prerequisites

- Android Studio Ladybug or newer (JDK 17)
- An Android emulator/device running Android 7.0 / API 24 or newer
- A Firebase project on the **Spark** plan

### 1. Create Firebase resources

1. Go to [Firebase Console](https://console.firebase.google.com/) and create a project.
2. Add an **Android app** with package name:
   ```
   com.localfoodreviews.app
   ```
   If you change `applicationId`, register that new package in Firebase instead.
3. Download the real `google-services.json` from Firebase and copy it to:
   ```
   LocalFoodReviews/app/google-services.json
   ```
   The project cannot ship your personal Firebase configuration file, so this is the one required local step.
4. In **Authentication → Sign-in method**, enable **Email/Password**.
5. In **Firestore Database**, create a database in **Production mode** (choose a nearby region; this cannot be changed later).
6. In **Firestore Database → Rules**, replace the generated rules with the contents of [`firebase/firestore.rules`](firebase/firestore.rules), then **Publish**.
7. Do **not** create Firebase Storage for this strict no-payment-method version. `firebase/storage.rules` is only a disabled reference file.

### 2. Run the Android app

1. Open the `LocalFoodReviews` folder in Android Studio.
2. Allow Gradle sync to finish.
3. Run the `app` configuration on a device/emulator.
4. Create two test accounts and post reviews to confirm that the live feed, search, and ownership checks work.

## Free-tier usage notes

- Firebase Auth remembers the sign-in automatically; no extra code or paid product is required.
- Firestore's Spark quota is sufficient for a small class demo. This app minimizes data by listening to only the latest 100 reviews and reading no user profile for every feed row.
- Photo data increases Firestore reads/downloads. Keep test images small and avoid using this pattern for many large images.
- No Cloud Functions, Cloud Storage, Maps, paid APIs, phone authentication, or external image host is used.

## Project structure

```text
app/src/main/java/com/localfoodreviews/app/
  data/        Firebase repository and models
  ui/          Activity, 7 fragments, and RecyclerView adapter
  util/        image compression/data-URI helper and view helpers
app/src/main/res/
  layout/      XML layouts
  navigation/  navigation graph
  menu/        navigation drawer
firebase/
  firestore.rules
  storage.rules (disabled documentation-only rule)
```

## Test checklist

- [ ] Invalid email, blank password, mismatch password, duplicate username, and network error show a clear message.
- [ ] Close/reopen the app after sign-in: it opens the Feed.
- [ ] A new review appears in the Feed with all expected fields.
- [ ] Search matches both `Thai` and `thai`, by restaurant and cuisine.
- [ ] Account A cannot see edit controls for Account B on **My reviews** (and Firestore rules also reject direct changes).
- [ ] Change password rejects an incorrect current password and accepts a different 6+ character new password.
- [ ] Logging out returns to Sign in; back cannot reopen authenticated pages.
