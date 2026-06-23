# Firestore rules tests

Security-rules unit tests for `../firestore.rules`, run against the Firebase
Local Emulator. These exercise the rules logic the Kotlin build can't — the
rules file is a separate (CEL-like) language the app compiler never reads.

## Run

From this directory:

```bash
npm install          # first time only
npm run emulate      # boots the Firestore emulator and runs the tests
```

`npm run emulate` wraps `firebase emulators:exec`, so it starts the emulator,
runs `npm test` (mocha) against it, and shuts it down. Requires the Firebase CLI
on PATH and **JDK 21+** for the emulator (current firebase-tools rejects older
Java). If your default `java` is older, point `JAVA_HOME` at a 21+ JDK first,
e.g. one of the JDKs Android Studio ships:

```bash
export JAVA_HOME="C:\\Users\\<you>\\.jdks\\openjdk-21"
export PATH="$JAVA_HOME/bin:$PATH"
```

## What's covered

`test/favorites.test.js` — the `favorites` map clause added for the
favorite-POIs feature: cap (≤3), add must be stamped to `request.time`
(no backdating), existing slots immutable, removal blocked until the 7-day
hold elapses. Add cases here as the rules grow.
