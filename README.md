# huushalt

*huushalt* is Swiss German for *Haushalt* (household).

An Android app for keeping track of household chores in a **single-person household**.
It deliberately does one thing, chores, and does it without accounts, flatmates or finances.

- Chores with flexible or fixed schedules (every n days/weeks/months, weekdays, nth weekday of
  the month, specific dates), rooms, checklists, pausing and skipping
- Today's overview, reminders in the morning and evening, holiday mode with catching up
- History, streaks and statistics
- Export and import of all data as a file
- **Offline and private:** no account, no ads, no tracking. The app doesn't even request the
  internet permission; all data stays on the phone (and in Android's own device backup, if
  that is turned on).

The app is in **German only** (Swiss spelling, *ss* instead of *ß*). This documentation is in
English.

Feature list, scope and design decisions: [docs/MVP.md](docs/MVP.md)

## Install

huushalt isn't in the Play Store. Signed APKs are published as
[GitHub releases](https://github.com/Triamlipdho/huushalt/releases).

**Recommended: [Obtainium](https://github.com/ImranR98/Obtainium)**, which installs the app
from the GitHub releases and offers updates when a new version is published:

1. In Obtainium, tap *Add app*.
2. Enter the URL `https://github.com/Triamlipdho/huushalt` (source *GitHub* is detected
   automatically) and tap *Add*.
3. Install. Obtainium checks for new releases and offers updates.

No GitHub account or access token is needed.

**Or manually:** download `huushalt-<version>.apk` from the
[latest release](https://github.com/Triamlipdho/huushalt/releases/latest) and open it on the
phone. Android asks to allow installing from that app (e.g. the browser) once. Updates must be
installed the same way.

Requirements: Android 8.0 (API 26) or newer.

All releases are signed with the same key, so updates install over the existing app and keep
its data. A build you made yourself (debug or differently signed) can't be installed over a
release; uninstalling deletes the app's data, so export it first (*Einstellungen → Daten*).

## Home Assistant: today's chores when you get home

huushalt can show its overview of today's chores (everything due today or overdue) when another app
on the phone asks for it. The Home Assistant Companion app can do that when you arrive home. No
network access is needed: Home Assistant tells the Companion app, which passes it on to huushalt on
the phone.

1. In huushalt, turn on *Einstellungen → Erinnerungen → Auslösen durch andere Apps*. The package
   name and action shown there are the values for step 2.
2. In Home Assistant, add an automation (replace `person.me` and `mobile_app_my_phone`):

   ```yaml
   alias: huushalt – Aufgaben beim Heimkommen
   triggers:
     - trigger: state
       entity_id: person.me
       to: home
   actions:
     - action: notify.mobile_app_my_phone
       data:
         message: command_broadcast_intent
         data:
           intent_package_name: xyz.haselbach.huushalt
           intent_action: xyz.haselbach.huushalt.action.SHOW_TODAY
   ```

   For a debug build, use the package name `xyz.haselbach.huushalt.debug`.

Nothing is shown when no chore is open, during a holiday or when reminders are turned off. The
notification replaces the morning overview, so it never piles up. To be reminded only once per
day, add a condition to the automation (for example, only after 16:00).

## Build from source

Requirements: Android Studio, or JDK 21 + the Android SDK (platform 36). JDK 21 is needed for
the Robolectric UI tests. Without Android Studio, point Gradle to the SDK with `ANDROID_HOME`
or a `local.properties` file containing `sdk.dir=/path/to/android-sdk` (never committed).

```bash
git clone https://github.com/Triamlipdho/huushalt.git
cd huushalt
./gradlew assembleDebug
```

The debug APK is in `app/build/outputs/apk/debug/`. Debug builds use the package name
`xyz.haselbach.huushalt.debug`, so they can be installed next to a release.

### Checks

Before opening a pull request, run the same checks as CI locally (lint, all unit and UI tests,
debug build):

```bash
scripts/check.sh
```

`scripts/check.sh --release` also builds the (unsigned) release APK with R8 and resource
shrinking. The UI tests take screenshots of every screen (light and dark) to
`app/build/screenshots/`, which is the easiest way to see the app without a phone.

Single tasks:

```bash
./gradlew lintDebug testDebugUnitTest
```

### Tech stack

- Kotlin, Jetpack Compose, Material 3, Room, DataStore
- Android 8.0 (API 26) and newer, compiled against API 36
- Gradle with a version catalog (`gradle/libs.versions.toml`); Dependabot keeps versions up to date
- Unit and UI tests with JUnit and Robolectric
- Package name `xyz.haselbach.huushalt`

### GitHub Actions

GitHub only builds what is needed:

- **CI** (`.github/workflows/ci.yml`) runs once per pull request to `main`, not for every push,
  not for draft pull requests. It is required for merging into `main`. It can also be started
  by hand under *Actions → CI → Run workflow*. Develop and test locally with `scripts/check.sh`.
  Pull requests from forks run with a read-only token and without access to any secrets.
- **Release** (`.github/workflows/release.yml`) only builds and signs: the tests already passed in
  the pull request. The signing key exists only in the GitHub secrets, so signed releases are the
  one thing that is built on GitHub. Only maintainers can start it.
- **Dependabot** checks for updates once a month.

## Contributing and security

- Bug reports and ideas: [issues](https://github.com/Triamlipdho/huushalt/issues). See
  [CONTRIBUTING.md](CONTRIBUTING.md) before opening a pull request.
- Security issues: please report them privately, see [SECURITY.md](SECURITY.md).

## Releases (maintainers)

Releases are signed APKs published as GitHub releases by `.github/workflows/release.yml`.

### One-time setup: signing key

1. Create a keystore. Keep it and both passwords somewhere safe outside GitHub (e.g. a password
   manager). **If the key is lost, updates can no longer be installed over the existing app.**

   ```bash
   keytool -genkeypair -v -keystore huushalt-release.jks -alias huushalt -keyalg RSA -keysize 4096 -validity 10000
   ```

2. Encode it for GitHub:

   ```bash
   base64 -w 0 huushalt-release.jks > huushalt-release.jks.base64
   ```

3. In the repository on GitHub, go to *Settings → Secrets and variables → Actions*, tab
   **Secrets** (not *Variables*: variables are shown in plain text), and add these repository secrets:

   | Secret | Value |
   |---|---|
   | `HUUSHALT_KEYSTORE_BASE64` | Contents of `huushalt-release.jks.base64` |
   | `HUUSHALT_KEYSTORE_PASSWORD` | Keystore password |
   | `HUUSHALT_KEY_ALIAS` | `huushalt` (or the alias you chose) |
   | `HUUSHALT_KEY_PASSWORD` | Optional. Only needed if the key has its own password. keytool's default PKCS12 keystores use the keystore password for the key too, so leave it out |

4. Delete `huushalt-release.jks.base64`. Never commit the keystore; `.gitignore` excludes `*.jks`.

### Publishing a release

Either start it on GitHub (also works from the phone): *Actions → Release → Run workflow*, branch
`main`, enter the version, e.g. `0.2.0`. The workflow creates the tag `v0.2.0` on the current
commit of `main`.

Or tag a commit on the main branch and push the tag:

```bash
git tag v0.1.0
```

```bash
git push origin v0.1.0
```

`.github/workflows/release.yml` then:

1. turns the tag into the version: `v1.2.3` → versionName `1.2.3`, versionCode `10203`
   (minor and patch must stay below 100; versions must always increase),
2. builds and signs the release APK and checks the signature,
3. creates the GitHub release `v1.2.3` with `huushalt-1.2.3.apk` and generated release notes.

## License

huushalt is free software under the [GNU General Public License v3.0](LICENSE): you may use,
study, change and share it, and builds or forks you distribute must be under the GPL-3.0 as
well, with their source code.
