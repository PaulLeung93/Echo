# 📍 Echo

**Echo** is a location-based social networking app built with **Kotlin** and **Jetpack Compose**.  
It fosters real-world connections by letting people share posts, discover local events, and interact with others nearby — creating a true sense of community.

---

## 🎯 Overview

Echo connects people through **proximity and shared interests**.  
Posts are visible to users within a defined local radius, making conversations relevant and community-driven.  
The app delivers real-time updates, location-based filtering, and globally seeded Points of Interest (POIs) that act as community hubs.  
It’s built with **clean architecture** principles, prioritizing performance, scalability, and a seamless user experience.

---

## 🌟 Key Features

- **Feed Screen**
  - Browse posts from users in your local area, sorted by recency or popularity.
  - Filter content by tags (e.g., `events`, `alerts`, `offers`).
  - Real-time updates ensure you see the latest activity around you.

- **Map Screen**
  - Explore posts and POIs displayed on an interactive map.
  - Filter markers by type for a customized view.
  - Location-aware: markers update based on your current position.

- **Post Details**
  - Consistent `PostCard` design showing post content, likes, comments, and tags.
  - Commenting is **proximity-based** — only available if you’re within 5km of the post’s location.

- **Points of Interest (POIs)**
  - Pre-seeded locations such as colleges, landmarks, and parks.
  - Visible to all users, with interaction limited to nearby users.

- **Post Creation**
  - Compose posts with optional map visibility.
  - Tag posts for targeted discovery in your local community.

---

## 🛠️ Tech Stack

**Frontend**
- [Kotlin](https://kotlinlang.org/) + [Jetpack Compose](https://developer.android.com/jetpack/compose)  
- [Material 3](https://m3.material.io/) for UI styling  
- [Google Maps Compose](https://developers.google.com/maps/documentation/android-sdk/compose)  
- [Coil](https://coil-kt.github.io/coil/) for image loading

**Backend**
- [Firebase Authentication](https://firebase.google.com/docs/auth) (Email & Google Sign-In)  
- [Cloud Firestore](https://firebase.google.com/docs/firestore) for data storage  
- [Firebase Storage](https://firebase.google.com/docs/storage) for image hosting  
- [GeoFire for Firestore](https://github.com/MichaelSolati/geofirestore-android) for geolocation queries

**Utilities**
- **POI Seeding Script** — Python + Firebase Admin SDK to batch insert global POIs into Firestore without per-user Places API billing.

---

## 📸 Screenshots & GIFs

### Feed Screen
![Feed Screenshot](docs/gifs/feed-screen.gif)

### Map Screen
![Map Screenshot](docs/gifs/map-screen.gif)

### Post Details
![Post Details Screenshot](docs/gifs/post-details.gif)

### Create Post Flow (GIF)
![Create Post GIF](docs/gifs/create-post.gif)

### POI Interaction (GIF)
![POI Interaction GIF](docs/gifs/poi-interaction.gif)

---

## 📄 License

This project is not licensed for use, modification, or redistribution.
