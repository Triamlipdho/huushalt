# huushalt

*huushalt* is Swiss German for *Haushalt* (household).

An Android app for keeping track of household chores in a **single-person household**.
It deliberately does one thing, chores, and does it without accounts, flatmates or finances.

- MVP scope and feature list: [docs/MVP.md](docs/MVP.md)

## Tech stack

- Kotlin, Jetpack Compose, Material 3
- Android 8.0 (API 26) and newer, compiled against API 36
- Gradle with a version catalog (`gradle/libs.versions.toml`); Dependabot keeps versions up to date
- Package name `xyz.haselbach.huushalt` (debug builds: `xyz.haselbach.huushalt.debug`, so both can be installed side by side)

## Development

Requirements: Android Studio (or JDK 21 + Android SDK). JDK 21 is needed for the Robolectric UI tests.

```bash
./gradlew assembleDebug
```

```bash
./gradlew lintDebug testDebugUnitTest
```

Before pushing, run the same checks as CI locally (lint, all unit and UI tests, debug build):

```bash
scripts/check.sh
```

`scripts/check.sh --release` also builds the (unsigned) release APK.

### GitHub Actions minutes

To keep within the free minutes of a private repository, GitHub only builds what is needed:

- **CI** (`.github/workflows/ci.yml`) runs once per pull request to `main`, not for every push,
  not for draft pull requests and not for changes to documentation only. It can also be started
  by hand under *Actions → CI → Run workflow*. Develop and test locally with `scripts/check.sh`.
- **Release** (`.github/workflows/release.yml`) only builds and signs: the tests already passed in
  the pull request. The signing key exists only in the GitHub secrets, so signed releases are the
  one thing that is built on GitHub.
- **Dependabot** checks for updates once a month.

## Releases

Releases are signed APKs published as GitHub releases and installed with
[Obtainium](https://github.com/ImranR98/Obtainium).

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

### Installing on the phone with Obtainium

1. Create a fine-grained GitHub personal access token: *Settings → Developer settings → Personal
   access tokens → Fine-grained tokens*, repository access *only this repository*, permission
   *Contents: Read-only*.
2. In Obtainium, add an app with source *GitHub* and the URL `https://github.com/<owner>/huushalt`.
3. In Obtainium's settings for GitHub, enter the access token (needed because the repository is private).
4. Install. Obtainium checks for new releases and offers updates.
