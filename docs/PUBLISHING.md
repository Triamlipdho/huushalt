# Making the repository public: checklist

This checklist was prepared together with the pull request that readied the repository for
publishing (branch `claude/prepare-public`). Work through it **in order**. Items marked
**✅ done in the PR** need no action beyond merging; everything else is a setting or a decision
only the owner can make.

## Audit summary (2026-11-09)

Checked: all 44 commits on all 5 branches and 12 tags (`v0.1.0` to `v1.3.1`), every blob that
ever existed (301), the 21 pull requests and their descriptions, issues (none) and the current
files.

| Area | Result |
|---|---|
| Secrets in git history | **Nothing found.** No keystore, `local.properties`, `.env`, private key, token or password was ever committed. Signing is read only from environment variables (`app/build.gradle.kts`); the workflow takes them from the Actions secrets `HUUSHALT_KEYSTORE_BASE64`, `HUUSHALT_KEYSTORE_PASSWORD`, `HUUSHALT_KEY_ALIAS`, `HUUSHALT_KEY_PASSWORD`. The only long hex strings are Gradle distribution checksums, Room schema hashes and Action commit SHAs. The only binary is `gradle/wrapper/gradle-wrapper.jar` (validated by CI). **No key rotation and no history rewrite are needed.** |
| Commit authors | `Triamlipdho <6876738+Triamlipdho@users.noreply.github.com>` (16 commits, GitHub noreply), `Claude <noreply@anthropic.com>` (27), `dependabot[bot]` (1). Committer is `GitHub <noreply@github.com>` or Claude. **No private email address is exposed.** |
| Personal data in files | No real name, email, hostname or local path. Test data uses made-up chores. The package name `xyz.haselbach.huushalt` contains a surname (see decision 2 below). |
| Commit messages and PR descriptions | 26 commit messages and the Claude-authored PR descriptions contain `Claude-Session: https://claude.ai/code/session_…` links. These sessions are private: only the owner's account can open them; to everyone else they are dead links that reveal nothing except that Claude Code was used. |
| Internal-only content | README and `docs/MVP.md` described the repository as private and Obtainium as needing a token: **✅ updated in the PR.** No TODOs, no screenshots or other large files in the repository. |
| Workflows | CI runs on `pull_request` (never `pull_request_target`), uses no secrets, token is read-only. Release runs only on tag pushes and manual runs (both need write access to the repository). All actions are pinned to commit SHAs. Hardened further: **✅ done in the PR** (see step 4). |
| Branches | `ccr-0a2f02d4-01pb6f`, `claude/optimistic-hawking-4qjtew` and `claude/security-hardening` are fully merged into `main` and can be deleted. `dependabot/gradle/gradle-dependencies-3e0fa7d597` is the open Dependabot PR #9. |

Note on tooling: gitleaks could not be executed in the preparation environment, so the history
was scanned with targeted searches over every blob (keystore/certificate file names, `password`
/ `secret` / `token` assignments, private key headers, GitHub/AWS/Google/Slack/OpenAI key
formats, high-entropy base64 and hex strings, email addresses, URLs, IPs, local paths). If you
want a second opinion, GitHub's secret scanning (step 6) scans the full history automatically
once it is turned on.

## 1. Choose and add a license

Without a `LICENSE` file the code is **"all rights reserved"**: people may look at it on
GitHub, but not legally copy, modify or redistribute it. Options:

| License | Trade-off |
|---|---|
| **GPL-3.0** | Forks and redistributed builds must stay open source under the GPL; keeps improvements public, but companies avoid it. Common for F-Droid apps. |
| **MIT** | Shortest and most permissive; anyone may do anything, including closed-source forks, as long as the notice is kept. |
| **Apache-2.0** | Permissive like MIT, plus an explicit patent grant and change notices; the usual choice in the Android ecosystem. |
| No license | Source-visible only, "all rights reserved"; contributions are legally murky. |

Add it on GitHub: *Code → Add file → Create new file*, name it `LICENSE`, click *Choose a
license template*. Then mention it at the end of `README.md`.

## 2. Decide on personal data exposure

- **Commit emails:** already the GitHub noreply address, nothing to do. To keep it that way,
  leave *Settings (your profile) → Emails → Keep my email addresses private* and *Block command
  line pushes that expose my email* turned on, and use
  `6876738+Triamlipdho@users.noreply.github.com` as `user.email` for local commits.
- **Package name `xyz.haselbach.huushalt`:** contains a surname and appears in the source, the
  APK and the README. It can't be changed without breaking updates (Android treats a new
  package name as a different app; data would have to be exported and imported). Decide
  whether that is acceptable; nothing else in the repository names a person.
- **Claude session links** in commit messages: harmless (private links), and removing them would
  need a history rewrite (`git filter-repo`) and a force push of `main` and all tags, which
  changes every commit SHA. Not recommended.

## 3. Delete merged branches (optional)

*Code → Branches*: delete `ccr-0a2f02d4-01pb6f`, `claude/optimistic-hawking-4qjtew`,
`claude/security-hardening` and, after merging, `claude/prepare-public`. Optionally turn on
*Settings → General → Pull Requests → Automatically delete head branches*.

## 4. Workflow hardening

**✅ done in the PR:**

- `ci.yml`: explicit `permissions: contents: read` on the workflow and the job;
  `actions/checkout` with `persist-credentials: false`.
- `release.yml`: workflow default `permissions: {}`, only the release job gets
  `contents: write`; checkout with `persist-credentials: false` so the write token is not
  stored in `.git/config` while Gradle and its plugins run; the "tag already exists" check now
  uses the GitHub API instead of an authenticated `git ls-remote`; step outputs are passed
  through `env` instead of being expanded inside scripts.
- `.gitignore`: also ignores `*.jks.base64` (the intermediate file from the README's signing
  instructions), `*.p12`, `*.pfx`, `*.apk`, `*.aab`, `.env`.

Still to do in the settings:

1. *Settings → Actions → General*
   - **Actions permissions:** keep *Allow all actions and reusable workflows*, or choose *Allow
     Triamlipdho, and select non-Triamlipdho, actions and reusable workflows* and allow
     `actions/*`, `gradle/actions/*`.
   - **Fork pull request workflows from outside collaborators:** *Require approval for all
     outside collaborators*.
   - **Workflow permissions:** *Read repository contents and packages permissions* (read-only
     default), and leave *Allow GitHub Actions to create and approve pull requests* unchecked.
     The release job requests `contents: write` itself, so releases keep working.
2. Optional, extra protection for the signing key: *Settings → Environments → New environment*
   `release` with *Deployment branches and tags* limited to `main` and tags `v*`, move the four
   `HUUSHALT_*` secrets there, and add `environment: release` to the `release` job in
   `release.yml`. Only worth it if other people ever get write access.

## 5. Protect `main`

*Settings → Rules → Rulesets → New ruleset → New branch ruleset*:

- Name `main`, *Enforcement status* **Active**, *Target branches* → *Include default branch*.
- Bypass list: empty, or yourself (*Repository admin*) if you want to push small fixes directly.
- Check **Restrict deletions**, **Block force pushes**.
- Check **Require a pull request before merging** (required approvals 0 is fine for a
  one-person project).
- Check **Require status checks to pass**, add the check **`build`** (the CI job). Note: CI
  skips documentation-only pull requests (`paths-ignore`), so such pull requests would wait
  forever for `build`. Either merge those via the bypass, or start CI by hand
  (*Actions → CI → Run workflow* doesn't report to the PR), or remove `paths-ignore` from
  `ci.yml` (costs nothing on a public repository, see step 7).

Optionally a tag ruleset (*New tag ruleset*, target `v*`, *Restrict creations*, *Restrict
updates*, *Restrict deletions*, yourself in the bypass list) so that only you can create
release tags.

## 6. Code security

*Settings → Advanced Security* (called *Code security* on some accounts):

- **Private vulnerability reporting:** Enable (`SECURITY.md` points to it — **✅ done in the
  PR**).
- **Dependabot alerts:** Enable. *Dependabot security updates:* Enable. (Version updates are
  already configured in `.github/dependabot.yml`.)
- **Secret Protection → Secret scanning:** Enable, and **Push protection:** Enable. Free for
  public repositories; scans the whole history after enabling. Check *Security → Secret
  scanning* a few minutes later; it should show no alerts.
- Optional: **Code scanning → CodeQL analysis → Default setup** (Kotlin and GitHub Actions).
  Free for public repositories; runs on pushes to `main` and on pull requests.

## 7. Change the visibility

*Settings → General → Danger Zone → Change repository visibility → Make public*.

What becomes public at that moment: all code and history, all branches and tags, the 12
releases with their APKs, the 21 pull requests with descriptions and comments, Actions runs
with their logs and artifacts (debug APKs and lint reports, kept for 14 days). The Actions
secrets stay secret.

Side effects: GitHub Actions minutes on standard runners are free and unlimited for public
repositories, so the minute-saving measures (CI only on pull requests, monthly Dependabot) are
no longer necessary but do no harm. Forks of a public repository stay public even if you make
it private again later.

## 8. Update Obtainium

In Obtainium: open *Settings → Source-specific → GitHub* and delete the personal access token
(or the app's own override). Then open huushalt in Obtainium and tap *Check for updates*; it
should still find the latest release. Afterwards revoke the token on GitHub: *Settings
(profile) → Developer settings → Personal access tokens → Fine-grained tokens*.

## 9. Verify a release

1. Merge a small pull request (CI `build` must pass).
2. *Actions → Release → Run workflow* on `main` with the next version (e.g. `1.3.2`).
3. Check: the run is green, the release `v1.3.2` has `huushalt-1.3.2.apk`, the *Verify
   signature* step shows the same certificate digest as earlier releases, and Obtainium (without
   token) offers the update and installs it over the existing app.

## 10. Optional polish

- Repository description, website and topics (*Code → ⚙ next to About*), e.g. `android`,
  `kotlin`, `jetpack-compose`, `chores`, `household`.
- Screenshots in the README: run `scripts/check.sh`, pick images from `app/build/screenshots/`,
  commit a few to e.g. `docs/screenshots/` and link them in `README.md`.
- *Settings → General → Features*: turn off *Wikis* and *Projects* if unused; turn on
  *Discussions* only if you want them.
