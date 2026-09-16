<div align="center">

# Footy Draft

**Spin legendary squads. Draft an XI. Win every match.**

A football draft game for Android. Build a starting XI from iconic Premier League, European and World Cup squads, then take it through a full league season, a Champions League campaign, a World Cup or an FA Cup run, and chase the perfect record.

![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white)
![Min SDK](https://img.shields.io/badge/min%20SDK-24-informational)
![Kotlin](https://img.shields.io/badge/Kotlin-2.3-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)

</div>

---

## Contents

- [Features](#features)
- [Download](#download)
- [Tech stack](#tech-stack)
- [Project structure](#project-structure)
- [Getting started](#getting-started)
- [Running tests](#running-tests)
- [Contributing](#contributing)
- [Releases and versioning](#releases-and-versioning)
- [Release signing (maintainers)](#release-signing-maintainers)
- [Disclaimer](#disclaimer)

## Features

### Four challenges

| Challenge | Squads you draft from | What you play | Perfect run |
| --- | --- | --- | --- |
| **Premier League** | Premier League sides, 1992/93 to 2024/25 | 38 league matches against today's top flight | 38-0 |
| **Champions League** | English and great European club sides | League phase, two-legged knockouts, one final | 17-0 |
| **World Cup** | Legendary national teams | Group stage, knockouts, the final | 8-0 |
| **FA Cup** | Premier League sides | Extra preliminary round all the way to Wembley | 14-0 |

148 hand-built squads in total, each with season-specific player ratings.

### The draft

- **12 formations**: 4-3-3, 4-4-2, 4-2-3-1, 4-5-1, 3-4-3, 3-5-2, 5-4-1, 4-1-2-1-2, 4-4-1-1, 5-3-2, 3-4-1-2 and 4-2-2-2.
- **Two draft modes**: *Squad first* (spin a squad, pick anyone, choose their position) or *Position first* (pick a slot, then spin for a squad that can fill it).
- **Difficulty**: Easy (3 re-spins), Normal (1 re-spin) or Hard (no re-spins, ratings hidden until the XI is complete).
- **Rating modes**: *Season* rates players as they were that season; *Prime* uses every player's career best.
- **Era filter**: limit spins to a range of seasons, from all-time down to the modern game.
- **Chemistry**: players from the same club or nation lift the whole team.

### The run

- **Managers**: appoint a gaffer once your XI is complete. Attacking, Defensive, Big-game, Man-manager and Tactician traits each change how the team plays.
- **January transfer window**: at the halfway point, gamble on a marquee signing, a loan, selling your star or a new fitness coach. It can help or hurt, and there is no undo.
- **European nights**: finish in the Premier League's top seven and your XI plays on in the Champions League, Europa League or Conference League.
- **Match simulation**: goals come from Poisson draws driven by your attack and defence against each opponent, so stronger teams win more often but never every time.
- **Records**: best record, perfect runs and trophies are saved per challenge.

## Download

Every push to `main` publishes a signed APK on the [Releases](../../releases) page.

1. Download `FootyDraft-vX.Y.apk` from the latest release on your Android device.
2. Open it and allow installs from that source when prompted.
3. Future releases install over the top and keep your records, since every build is signed with the same key.

Requires Android 7.0 (API 24) or newer.

## Tech stack

| Area | Choice |
| --- | --- |
| Language | Kotlin 2.3 |
| UI | Jetpack Compose with Material 3 (Compose BOM 2026.03) |
| Navigation | Navigation 3 |
| State | `ViewModel` exposing a single `StateFlow<GameUiState>` |
| Persistence | `SharedPreferences` (records and last-used settings per challenge) |
| Build | Android Gradle Plugin 9.0, Gradle 9.1 (wrapper), version catalog in `gradle/libs.versions.toml` |
| Tests | JUnit 4 and kotlinx-coroutines-test (unit), Compose UI test and Espresso (instrumented) |
| CI/CD | GitHub Actions: tests, version bump, signed APK, GitHub release |

## Project structure

```text
footygame/
├── .github/workflows/release.yml   # Release pipeline (see "Releases and versioning")
├── app/
│   ├── build.gradle.kts            # App module: SDK levels, version, signing
│   └── src/
│       ├── main/java/com/example/footygame/
│       │   ├── MainActivity.kt         # Single activity, edge-to-edge
│       │   ├── MainNavigation.kt       # Navigation 3 back stack and screen wiring
│       │   ├── data/                   # Squads, managers, opponents, SharedPreferences stores
│       │   │   └── squads/             # Squad data: English (early/late), European, national teams, kits
│       │   ├── game/                   # Pure Kotlin game logic
│       │   │   ├── DraftEngine.kt      #   Spins, picks, re-spins, manager offers
│       │   │   ├── SeasonSimulator.kt  #   Seeded league, cup and tournament simulation
│       │   │   └── TeamRatings.kt      #   Overall, attack, defence and chemistry
│       │   ├── models/                 # Immutable data models (draft, run, settings)
│       │   ├── theme/                  # Colours, typography, theme
│       │   ├── ui/                     # Menu, setup, draft and simulation screens
│       │   │   └── components/         # Pitch, stickers, shared controls
│       │   └── viewmodel/              # GameViewModel
│       ├── test/                       # JVM unit tests
│       └── androidTest/                # Instrumented Compose UI tests
├── gradle/libs.versions.toml       # Dependency versions
└── version.properties              # App versionName / versionCode (bumped by CI)
```

The game logic in `game/` and the data in `data/` have no Android dependencies, so they are fully covered by fast JVM unit tests. The simulator is seeded: the same seed always replays the same run, which is how a run resumes after the January transfer window.

## Getting started

### Prerequisites

- [Android Studio](https://developer.android.com/studio) (latest stable), which bundles a suitable JDK
- Android SDK Platform 36, installed through the Android Studio SDK Manager
- Git
- An Android device or emulator running Android 7.0+

Building from the command line only needs JDK 17 or newer and the Android SDK; the Gradle wrapper downloads Gradle itself.

### Set up on a new machine

1. **Clone the repository**

   ```bash
   git clone https://github.com/<owner>/<repo>.git
   cd <repo>
   ```

2. **Open it in Android Studio** with *File > Open* and select the project folder. Let the Gradle sync finish; Android Studio creates `local.properties` pointing at your SDK automatically.

   Building from the command line without Android Studio? Create `local.properties` yourself:

   ```properties
   # Windows
   sdk.dir=C\:/Users/you/AppData/Local/Android/Sdk
   # macOS: sdk.dir=/Users/you/Library/Android/sdk
   # Linux: sdk.dir=/home/you/Android/Sdk
   ```

   or set the `ANDROID_HOME` environment variable instead.

3. **Run the app**: pick a device or emulator and press *Run*, or from a terminal:

   ```bash
   ./gradlew installDebug        # macOS / Linux
   gradlew.bat installDebug      # Windows
   ```

Debug builds are signed with the standard Android debug key, so contributors do **not** need the release keystore.

### Useful Gradle tasks

| Task | What it does |
| --- | --- |
| `assembleDebug` | Build a debug APK into `app/build/outputs/apk/debug/` |
| `installDebug` | Build and install the debug APK on a connected device |
| `testDebugUnitTest` | Run the JVM unit tests |
| `connectedDebugAndroidTest` | Run the instrumented UI tests on a connected device or emulator |
| `assembleRelease` | Build a release APK (signed only if a signing key is configured) |

## Running tests

```bash
./gradlew testDebugUnitTest            # unit tests: draft engine, simulator, squads, records, view model
./gradlew connectedDebugAndroidTest    # UI tests: needs a running emulator or connected device
```

Unit test reports are written to `app/build/reports/tests/testDebugUnitTest/index.html`.

## Contributing

Contributions are welcome, whether that is a new squad, a new challenge, a balance tweak or a bug fix.

1. Fork the repository and create a branch from `main`:

   ```bash
   git checkout -b feature/short-description
   ```

2. Make your change, following the existing style:
   - Kotlin official code style (`kotlin.code.style=official`).
   - Keep game rules in `game/` free of Android APIs so they stay unit-testable.
   - Models are immutable `data class`es; state changes go through `GameViewModel`.
   - User-facing text belongs in `app/src/main/res/values/strings.xml`, not in code.
3. Add or update tests, and make sure `./gradlew testDebugUnitTest` passes.
4. Open a pull request against `main` describing what changed and why. Screenshots help for UI changes.

Do not edit `version.properties` in a pull request; the release workflow manages it.

### Adding a squad

Squads are plain Kotlin data in `app/src/main/java/com/example/footygame/data/squads/`:

1. If the club or nation is new, add its badge code and kit colours to `Kits.kt`.
2. Add the squad to the matching file (`EnglishSeasonsEarly.kt`, `EnglishSeasonsLate.kt`, `EuropeanSeasons.kt` or `NationalTeams.kt`). Each entry is a kit, the season's start year (or tournament year) and the players:

   ```kotlin
   english(Kits.NORWICH, 1992,
       gk("Bryan Gunn", 80),
       def("Ian Culverhouse", 78), def("Ian Butterworth", 77), def("John Polston", 77), def("Mark Bowen", 78),
       mid("Ian Crook", 80), mid("Gary Megson", 76), mid("David Phillips", 77), mid("Ruel Fox", 80),
       att("Mark Robins", 80), att("Chris Sutton", 80),
   ),
   ```

   An optional third argument sets the short name shown on the pitch, e.g. `gk("Peter Schmeichel", 88, "P. Schmeichel")`.

3. Use exactly the same spelling for a player who appears in several squads: the player id is derived from the full name, which stops the same person being drafted twice and powers *Prime* ratings.
4. Run `./gradlew testDebugUnitTest`; `ClubSeasonsTest` checks that every squad can fill every formation, that ids are well formed and that a player's name is spelled the same everywhere.

Ratings are game ratings chosen for balance, not official figures.

## Releases and versioning

Releases are fully automated by [`.github/workflows/release.yml`](.github/workflows/release.yml). On every push to `main` (or a manual *Run workflow*):

1. Unit tests run. A failing test stops the release.
2. `version.properties` is bumped: the minor version goes up by one (`1.4` becomes `1.5`) and `versionCode` goes up by one.
3. The bump is committed to `main` as `chore(release): vX.Y [skip ci]` and tagged `vX.Y`, both in one atomic push.
4. A signed release APK is built.
5. A GitHub release named **Footy Draft vX.Y** is published with the APK and auto-generated release notes.

Because each run claims the next unused tag, a release is never overwritten. Runs are queued, so rapid pushes each get their own version.

- **Pull after pushing.** The workflow adds a commit to `main`, so run `git pull --rebase` before your next push.
- **New major version.** Set `versionName=2.0` in `version.properties` by hand and push. A version that has no tag yet is released as is; the next push becomes `2.1`.
- **Branch protection.** If `main` requires pull requests or status checks, allow GitHub Actions to push to it, or the version bump push will be rejected.

## Release signing (maintainers)

Every release must be signed with the **same key**; Android refuses to update an installed app signed with a different one. Contributors never need this key.

### Files

| File | Purpose | In git? |
| --- | --- | --- |
| `footydraft-release.jks` | The release keystore (alias `footydraft`) | **No**, ignored |
| `keystore.properties` | Keystore path, alias and passwords for local release builds | **No**, ignored |

`keystore.properties` format:

```properties
storeFile=footydraft-release.jks
storePassword=...
keyAlias=footydraft
keyPassword=...
```

> **Back up both files together** in a password manager or other encrypted storage. If the keystore or its password is lost, installed copies of the app can no longer be updated.

### Set up signing on another machine

1. Clone the repository as described in [Getting started](#getting-started).
2. Copy `footydraft-release.jks` and `keystore.properties` from your backup into the project root, next to `settings.gradle.kts`.
3. Check the key and build a signed release:

   ```bash
   keytool -list -keystore footydraft-release.jks -alias footydraft
   ./gradlew assembleRelease
   ```

   The signed APK is `app/build/outputs/apk/release/app-release.apk`. An `app-release-unsigned.apk` means the keystore was not found.

### GitHub Actions secrets

The release workflow reads the key from repository secrets (*Settings > Secrets and variables > Actions > New repository secret*):

| Secret | Value |
| --- | --- |
| `SIGNING_KEYSTORE_BASE64` | The keystore file, base64-encoded |
| `SIGNING_STORE_PASSWORD` | `storePassword` from `keystore.properties` |
| `SIGNING_KEY_ALIAS` | `footydraft` |
| `SIGNING_KEY_PASSWORD` | `keyPassword` from `keystore.properties` |

Encode the keystore:

```powershell
# Windows (PowerShell): copies the value to the clipboard
[Convert]::ToBase64String([IO.File]::ReadAllBytes("footydraft-release.jks")) | Set-Clipboard
```

```bash
# macOS
base64 -i footydraft-release.jks | pbcopy
# Linux
base64 -w 0 footydraft-release.jks
```

With the [GitHub CLI](https://cli.github.com/) all four can be set from the project root:

```bash
base64 -w 0 footydraft-release.jks | gh secret set SIGNING_KEYSTORE_BASE64
gh secret set SIGNING_KEY_ALIAS --body footydraft
gh secret set SIGNING_STORE_PASSWORD     # paste the password when prompted
gh secret set SIGNING_KEY_PASSWORD       # paste the password when prompted
```

Local builds can use the same environment variables instead of `keystore.properties`: `SIGNING_STORE_FILE`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS` and `SIGNING_KEY_PASSWORD`.

## Disclaimer

Footy Draft is an unofficial fan project. It is not affiliated with, endorsed by or connected to the Premier League, UEFA, FIFA, The Football Association or any club, national team, player or manager. All names are used for identification only. Player ratings are invented for gameplay and are not official figures.
