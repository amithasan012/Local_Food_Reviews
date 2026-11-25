# Local Food Reviews - A Firebase-Based Restaurant Review and Discovery App

<br>
<p align="center">
  <img src="docs/images/local-food-reviews-logo.png" alt="Local Food Reviews logo" width="18%">
</p>

<p align="center">
  <strong>Discover local restaurants, share food photos, and write honest reviews.</strong>
</p>

## Objective

The objective of **Local Food Reviews** is to provide a simple, secure mobile platform where people can discover local restaurants and share their own food experiences. Users can create an account, add one food photo with a restaurant review, search reviews by restaurant or cuisine, and manage only the reviews that they created.

## Introduction

Local Food Reviews is a Kotlin Android application developed in Android Studio with Firebase Authentication and Cloud Firestore. The application focuses on a clear student-project workflow rather than complex commercial features: a user signs in, browses food reviews, searches the feed, posts a review, and can later edit or delete their own review.

The UI uses a high-contrast blue design, black input text, selectable blue rating stars, and white button labels. The project does not use Firebase Realtime Database; it uses **Cloud Firestore**. It also avoids paid APIs. Because Firebase Cloud Storage requires a billing-enabled project under current Firebase policy, the app compresses a selected photo and stores a small data URI in the Firestore review document. This is suitable for a small demonstration project, not a high-volume production image service.

## Features

### Account Management

- Register using a unique username, email address, password, and password confirmation.
- Case-insensitive unique username validation using the `usernames/{lowercaseUsername}` collection.
- Email-and-password sign-in through Firebase Authentication.
- Remembered sign-in session: reopening the app returns the signed-in user to the Feed.
- Send a password-reset email from the Login page.
- Change password after confirming the current password.
- Sign out securely from the navigation drawer.

### Restaurant Review Management

- Add exactly one food photo for each review.
- Enter restaurant name, cuisine, rating from 1 to 5, and a 10–1024 character review.
- Display a photo preview before posting.
- Compress/resize the selected image before saving so it stays inside a Firestore-safe document size.
- View only the current user’s posts on **My Reviews**.
- Edit restaurant name, cuisine, rating, review text, or photo from an inline editor.
- Delete a review after a confirmation dialog.

### Restaurant Discovery

- Browse a live RecyclerView feed of restaurant review cards.
- Each review card includes the image, restaurant name, cuisine, rating, username, date, and review text.
- Search while typing by **restaurant name** or **cuisine**.
- Search is case-insensitive: `Thai`, `thai`, and `THAI` return the same matching results.
- Pull down to refresh the feed.

### Navigation and Security

- Navigation drawer: Feed, Add Review, My Reviews, Change Password, and Log Out.
- Drawer header displays the current user’s username and email.
- Firestore rules require authentication and enforce review ownership.
- A user can only update or delete a review where the stored `uid` matches their Firebase Auth UID.
- Clear validation, loading states, and network error messages are included.

## Workflow

### New User

1. Open the app and select **Create an account**.
2. Enter a unique username, valid email address, password, and matching confirmation password.
3. The app creates the Firebase account and reserves the lowercase username in Firestore.
4. The user is taken to the Food Reviews feed.

### Signed-In User

1. Browse recent restaurant reviews on the Feed.
2. Use the search box to find a restaurant or cuisine.
3. Open the drawer and select **Add review**.
4. Select one local JPG/PNG image, enter restaurant/cuisine details, choose a rating, and write a review.
5. Post the review; it appears in the Feed.
6. Open **My Reviews** to edit the review or delete it after confirmation.
7. Use **Change password** or **Log out** from the drawer when needed.

## Methodology

The application follows a standard Android architecture suitable for a small Kotlin/Firebase project:

- **Kotlin + XML:** Kotlin manages behaviour; XML provides the mobile layouts.
- **Single Activity + Fragments:** `MainActivity` hosts navigation and the drawer. Individual Fragments provide Login, Register, Feed, Add Review, My Reviews, and Change Password screens.
- **Repository pattern:** `FirebaseRepository` keeps Firebase calls separate from UI code.
- **Firebase Authentication:** Handles email/password account creation, sign-in, session persistence, password reset, reauthentication, and password updates.
- **Cloud Firestore:** Stores users, username reservations, and review documents. Snapshot listeners provide live Feed/My Reviews updates.
- **ImageCodec:** Uses Android’s Open Document picker, retains image read access, reduces image size, JPEG-compresses it, and converts it to a small data URI before posting.
- **Firestore Security Rules:** Protect users, username reservations, and review ownership on the server side.

## Application Benefits

- Gives food lovers a focused place to share local restaurant experiences.
- Makes food reviews easier to understand by including restaurant, cuisine, rating, image, date, and review text together.
- Supports direct, instant restaurant/cuisine discovery through live search.
- Gives authors control over their own content without allowing them to modify another user’s review.
- Uses Firebase services that are appropriate for a small classroom/demo application without requiring paid APIs.

## Screenshots

> **Note:** The images below are blue UI mock-ups created from the implemented XML layouts. Replace the files inside `docs/images/` with emulator screenshots before publishing if real runtime screenshots are required.

### Account Management

| Login | Create Account | Change Password |
|---|---|---|
| <img width="1080" height="2340" alt="image" src="https://github.com/user-attachments/assets/4edc931d-dabd-455e-adfd-6cd782b61489" />
 | <img src="docs/images/register.png" alt="Create account screen" width="220"> | <img src="docs/images/change_password.png" alt="Change password screen" width="220"> |

### Restaurant Exploration and Navigation

| Review Feed and Search | Navigation Drawer |
|---|---|
| <img src="docs/images/feed.png" alt="Review feed screen" width="250"> | <img src="docs/images/drawer.png" alt="Navigation drawer screen" width="250"> |

### Review Creation and Management

| Add Review | My Reviews / Inline Edit |
|---|---|
| <img src="docs/images/create_review.png" alt="Add review screen" width="250"> | <img src="docs/images/my_reviews.png" alt="My reviews screen" width="250"> |

## Firebase Data Design

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

## Getting Started

1. Download or clone this repository.
2. Open the outer **`LocalFoodReviews`** folder in Android Studio — do not open only the `app` folder.
3. Wait for Gradle sync to finish.
4. Create a Firebase project and register an Android app with this package name:

   ```text
   com.localfoodreviews.app
   ```

5. Download Firebase’s `google-services.json` file and place it here:

   ```text
   LocalFoodReviews/app/google-services.json
   ```

6. Enable **Email/Password** under Firebase Console → Authentication → Sign-in method.
7. Create a **Cloud Firestore** database in Production mode.
8. In Firebase Console → Firestore Database → Rules, publish the contents of:

   ```text
   firebase/firestore.rules
   ```

9. Select an emulator or Android device and click **Run ▶** in Android Studio.

> Do not paste `firebase/firestore.rules` into **Realtime Database → Rules**. This project uses Cloud Firestore, and the two rule formats are different.

## Requirements

- Android Studio with JDK 17 support
- Android SDK / emulator or a physical Android device running API 24 (Android 7.0) or newer
- Internet connection for Firebase services and Gradle dependency download
- Firebase project with Authentication and Cloud Firestore enabled
- `google-services.json` placed in `app/` (not included in the repository because it belongs to your Firebase project)

## Project Structure

```text
LocalFoodReviews/
├── app/
│   ├── src/main/java/com/localfoodreviews/app/
│   │   ├── data/       # Firebase repository and model classes
│   │   ├── ui/         # Activity, fragments, RecyclerView adapter
│   │   └── util/       # Image compression and view helpers
│   └── src/main/res/   # XML layouts, theme, navigation, menu, drawables
├── docs/images/        # README logo and UI mock-ups/screenshots
├── firebase/
│   ├── firestore.rules
│   └── storage.rules   # disabled reference; Cloud Storage is not used
├── README.md
└── build.gradle.kts
```

## Testing Checklist

- [ ] Invalid email, missing password, mismatched password, and duplicate username show clear feedback.
- [ ] A remembered signed-in user opens directly on the Feed after restarting the app.
- [ ] A review cannot be posted without one photo, a restaurant, cuisine, rating, and valid review text.
- [ ] Search matches restaurant and cuisine values without case sensitivity.
- [ ] A user can edit/delete only their own reviews.
- [ ] Incorrect current password is rejected during password change.
- [ ] Logging out returns to Login and does not reopen authenticated pages with Back.

## Contributions

This is a student project. Improvements and suggestions are welcome. Before adding a feature, keep the project’s free-plan requirement and Firebase security rules in mind. If you change the Firestore document structure, update `firebase/firestore.rules` at the same time.

## Contact

For project questions or feedback, update this section with your contact information:

```text
amithasanmr@gmail.com
```

## Author

**M.R. Amit Hasan**  
**2320405**  
*Department of CSE*  
*University of Brahmanbaria*  
**Year:** 2026
