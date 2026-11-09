# Security policy

## Supported versions

Only the [latest release](https://github.com/Triamlipdho/huushalt/releases/latest) gets fixes.
Please check that a problem still occurs with it before reporting.

## Reporting a vulnerability

Please **don't open a public issue** for security problems. Report them privately via GitHub:
*Security → Report a vulnerability* in this repository
([direct link](https://github.com/Triamlipdho/huushalt/security/advisories/new)).

Please include the app version, the Android version and steps to reproduce. This is a hobby
project maintained in spare time, so there is no guaranteed response time, but reports are
taken seriously and you'll get an answer.

## Scope

huushalt has no server, no account and no internet permission; all data stays on the phone.
Relevant are, for example:

- other apps on the phone triggering actions in huushalt or reading its data
  (exported activities and broadcast receivers, notifications),
- import files that crash the app or corrupt data,
- problems in the build and release process (GitHub Actions, signing, dependencies).
