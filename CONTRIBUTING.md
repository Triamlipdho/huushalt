# Contributing

Thanks for your interest in huushalt! It is a small personal project with a deliberately narrow
scope: chores for a **single-person household**, offline, in German. Please read the guiding
principles and the "not planned" list in [docs/MVP.md](docs/MVP.md) before suggesting larger
features, and open an issue to discuss them before writing code.

## Bugs and ideas

Use the [issue templates](https://github.com/Triamlipdho/huushalt/issues/new/choose).
Security problems: see [SECURITY.md](SECURITY.md), not a public issue.

## Pull requests

1. Fork the repository and create a branch from `main`.
2. Keep changes focused; one topic per pull request.
3. **Run the checks locally first:**

   ```bash
   scripts/check.sh
   ```

   This runs lint, all unit and UI tests (Robolectric) and the debug build. It needs JDK 21 and
   the Android SDK (see [README.md](README.md#build-from-source)).
4. Open the pull request against `main`.

**CI runs only on pull requests** (not on pushes to your branch), and only once a pull request
is ready for review. Pull requests from first-time contributors need a maintainer's approval
before CI starts, so please don't rely on CI to find problems: run `scripts/check.sh` first.

## Conventions

- App texts are **German with Swiss spelling** (*ss*, never *ß*). `SwissSpellingTest` checks this.
- All user-visible strings go into `app/src/main/res/values/strings.xml`.
- New behaviour comes with tests (unit tests for logic, Robolectric tests for screens).
- Database changes need a Room migration and an exported schema in `app/schemas/`.
- Don't add dependencies, permissions (especially internet) or tracking without discussing it
  first.
