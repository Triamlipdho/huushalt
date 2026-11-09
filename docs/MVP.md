# huushalt MVP feature list

Status: draft, v0.4. Includes the comparison with Tody, Sweepy, Home Routines and Grocy
(see [Decisions](#decisions)). More requirements will be added later.

## Product vision

huushalt is a chore planner for people who live alone. It reminds you what needs doing
around the home, when, and keeps track of what you have done.

### Why not Flatastic?

Flatastic is built around a shared household. It assumes several members, needs an account,
rotates chores between people and bundles shopping lists and expense splitting. For one person,
most of that is overhead, and the core experience doesn't work well. huushalt keeps only the
chores part and designs it for one person.

### Guiding principles

1. **One person, no ceremony.** No sign-up, no household to create, no members to invite.
2. **Chores only.** No shopping list, no expenses, no chat.
3. **Offline and private.** All data stays on the device. No ads, no tracking.
4. **Fast daily use.** Open the app, see what's due, tick it off, done.

## Scope

| In scope (MVP) | Out of scope (MVP) |
|---|---|
| Android phone app | iOS, web, tablets optimised layouts, Wear OS |
| Single user, single household | Multiple users, sharing, chore rotation |
| Chores, schedules, reminders, history | Shopping list, expenses, budgets, chat, pin board |
| Local storage on the device | Accounts, cloud sync, server backend |

## Feature list

Priorities use MoSCoW: **Must** = required for the MVP release, **Should** = planned for the MVP
if time allows, **Could** = nice to have, not yet decided, **Later** = decided for after the MVP
(see [After the MVP](#after-the-mvp)).

### F1 Chore management

| ID | Feature | Priority |
|---|---|---|
| F1.1 | Create a chore with a name (required) and optional notes | Must |
| F1.2 | Edit and delete a chore (delete asks for confirmation) | Must |
| F1.3 | Pause / archive a chore without losing its history | Should |
| F1.4 | Assign a chore to a room or area (kitchen, bathroom, ...), with editable list of rooms | Should |
| F1.5 | Pick an icon or colour for a chore (done in v1.2.0: guessed from the name, changeable) | Could |
| F1.6 | Estimated duration (e.g. 15 min), shown in the daily list | Could |
| F1.7 | Optional checklist of sub-steps per chore (e.g. "Bad putzen" = Lavabo, WC, Dusche, Spiegel). Items are ticked off while doing the chore and reset for the next occurrence | Should |
| F1.8 | Completing a chore with an unfinished checklist asks once whether to mark it done anyway. Unticked items are not stored as done | Must (if F1.7 is built) |
| F1.9 | Ticking the last checklist item marks the chore as done | Could |
| F1.10 | Duplicate a chore (copy with all settings and checklist, without history) | Later |

### F2 Scheduling

Two scheduling modes are needed, because chores behave differently:

- **Fixed schedule**: due on set dates regardless of when it was last done
  (e.g. "take out the recycling every Tuesday").
- **Flexible, after completion**: next due date is calculated from the day it was actually done
  (e.g. "descale the kettle 4 weeks after last time"). Flatastic doesn't model this well, and it
  is how most solo chores work in practice.

| ID | Feature | Priority |
|---|---|---|
| F2.1 | One-off chore with a due date | Must |
| F2.2 | Repeat every N days / weeks / months | Must |
| F2.3 | Weekly on selected weekdays (e.g. Mon + Thu) | Must |
| F2.4 | Monthly on a day of the month (with sensible handling of the 29th to 31st) | Must |
| F2.5 | Choose fixed or flexible (after completion) mode per chore | Must |
| F2.6 | Set the start / first due date | Must |
| F2.7 | Seasonal chores (e.g. "clean windows every spring"). Plain yearly repeats are covered by F2.2 | Later |
| F2.8 | Chores without a date ("whenever I find time") in a backlog | Could |
| F2.9 | Every N weeks on selected weekdays, e.g. the Waschküche slot every 2nd Wednesday (combines F2.2 and F2.3) | Must |
| F2.10 | Monthly on the nth weekday, e.g. "first Monday" or "last Friday of the month" | Should |
| F2.11 | List of specific dates, e.g. paper and cardboard collection days from the Gemeinde's Abfallkalender. Dates can be added in bulk; the app reminds when the list is about to run out | Should |
| F2.12 | Tolerance window per chore, e.g. "every 7 days, ±2 days is fine". Within the window the chore shows as "fällig", only after it as overdue. Default: no tolerance | Should |
| F2.13 | Interval suggestion based on real behaviour, e.g. "You clean the fridge about every 3 weeks instead of every 4. Change it?" | Later |

### F3 Daily overview (home screen)

| ID | Feature | Priority |
|---|---|---|
| F3.1 | Home screen shows chores grouped into **Overdue**, **Today**, **Upcoming** (next 7 days) | Must |
| F3.2 | Overdue chores show how many days late they are | Must |
| F3.3 | Empty state when nothing is due ("All done for today") | Must |
| F3.4 | List of all chores, sortable by name, next due date and room | Must |
| F3.5 | Filter the overview by room | Should |
| F3.6 | Calendar / week view of upcoming chores | Could |
| F3.7 | Search chores by name, notes and checklist items | Later |
| F3.8 | Sort chores manually (drag and drop) | Later |

### F4 Completing chores

| ID | Feature | Priority |
|---|---|---|
| F4.1 | Mark a chore as done with one tap; next due date is calculated immediately | Must |
| F4.2 | Undo the last completion (snackbar) | Must |
| F4.3 | Mark as done on an earlier date ("I did it yesterday") | Should |
| F4.4 | Postpone / snooze to tomorrow or a chosen date | Must |
| F4.5 | Skip one occurrence without counting it as done | Should |
| F4.6 | Add a short note when completing | Could |
| F4.7 | Mark any chore as done at any time from the list of all chores (F3.4), also before it is due or when done spontaneously. Flexible chores: next due date counts from that day. Fixed chores: it counts for the next open occurrence | Must |

### F5 Reminders

| ID | Feature | Priority |
|---|---|---|
| F5.1 | Daily summary notification at a time I choose (e.g. 08:00: "3 chores today, 1 overdue") | Must |
| F5.2 | Turn notifications on or off globally | Must |
| F5.3 | Tapping a notification opens the daily overview | Must |
| F5.4 | Mark as done directly from the notification | Should |
| F5.5 | Per-chore reminder at a specific time | Could |
| F5.6 | Quiet days (e.g. no reminders on Sunday) | Could |
| F5.7 | Evening reminder at a second time I choose (e.g. 19:00), only if chores due today are still open. Can be turned off | Should |
| F5.8 | Other apps on the phone, e.g. the Home Assistant Companion app when I arrive home, can ask for the overview of today's chores. Off by default, no network needed | Could |

### F6 History

| ID | Feature | Priority |
|---|---|---|
| F6.1 | Per-chore history: list of completion dates (Flatastic charges for this) | Must |
| F6.2 | Show "last done" date on each chore | Must |
| F6.3 | Global activity log of everything done, newest first | Should |
| F6.4 | Edit the date of a past completion or delete it. Due dates, statistics and streaks are recalculated | Should |

### F7 Motivation

Flatastic motivates through points compared between flatmates. With one person there is
nobody to compare against, so huushalt motivates by comparing you with yourself: progress,
streaks and a record of what you have achieved. Motivation must stay encouraging, never
nagging: no guilt messages, and a missed day doesn't wipe out everything.

| ID | Feature | Priority |
|---|---|---|
| F7.1 | Daily progress on the home screen (e.g. "2 von 5 erledigt" with a progress ring) | Must |
| F7.2 | "Alles erledigt" streak: number of days in a row where no chore is overdue at the end of the day. Days with nothing due count too | Must |
| F7.3 | Per-chore streak: how many times in a row a chore was done on time ("on time" = by the due date plus its tolerance, F2.12) | Should |
| F7.4 | Statistics screen: chores done this week / month, on-time rate, trend compared with the previous period | Must |
| F7.5 | Small celebration when the last chore of the day is done (animation / message) | Should |
| F7.6 | Streak protection: postponing a chore (F4.4) before it is due does not break a streak; one "free day" per week | Should |
| F7.7 | Milestones / badges (e.g. "100 chores done", "4 weeks without overdue chores") | Could |
| F7.8 | Statistics per room (where do I spend my effort?) | Could |
| F7.9 | Motivation features can be turned off in settings | Should |

### F8 First start

| ID | Feature | Priority |
|---|---|---|
| F8.1 | App is usable immediately, no account or sign-up | Must |
| F8.2 | Optional starter set: pick from a list of common chores with suggested intervals (staubsaugen weekly, Bettwäsche wechseln every 2 weeks, Kühlschrank putzen monthly, ...) | Should |
| F8.3 | Ask for notification permission (Android 13+) with a short explanation | Must |
| F8.4 | Upkeep chores with long intervals in the starter set: Wasserkocher entkalken, Geschirrspüler-Filter reinigen, Waschmaschine reinigen, Dampfabzug-Filter, Rauchmelder testen, Tiefkühler abtauen | Later |

### F9 Data and settings

| ID | Feature | Priority |
|---|---|---|
| F9.1 | All data stored locally on the device | Must |
| F9.2 | Data is included in Android's automatic backup, so it survives a phone change. No other cloud backup | Must |
| F9.3 | Manual export / import of all data as a file (JSON) | Should |
| F9.4 | Settings: reminder time, first day of the week (default Monday), theme (system / light / dark) | Must |
| F9.5 | Language: German only, Swiss spelling (ss instead of ß, e.g. "erledigt", "Strasse", "grüssen"); Swiss date format (07.11.2026) | Must |
| F9.6 | Delete all data | Should |
| F9.7 | App shortcut: long-press the app icon to get "Neue Aufgabe" and "Heute" | Later |

### F10 Holidays and catching up

In a shared flat someone else does the chores while you're away. Alone, nobody does, and
coming back to a wall of overdue chores is the fastest way to give up on the app.

| ID | Feature | Priority |
|---|---|---|
| F10.1 | Holiday mode: pause everything from date A to date B. No reminders during that time | Must |
| F10.2 | After a holiday, flexible chores are shifted by the length of the holiday. Occurrences of fixed chores that fell inside the holiday are dropped; the chore continues with its next regular date | Must |
| F10.3 | Holiday days don't break the "Alles erledigt" streak (F7.2) or per-chore streaks (F7.3); they are neutral | Must |
| F10.4 | End a holiday early or extend it | Must |
| F10.5 | A fixed chore that was missed several times shows up once as overdue (with "3× verpasst"), not as three separate entries | Must |
| F10.6 | "Neu planen ab heute": one action for all overdue chores. Flexible chores get their next due date counted from today, fixed chores jump to their next regular date. Nothing is logged as done | Must |

### F11 Planning aids (after the MVP)

| ID | Feature | Priority |
|---|---|---|
| F11.1 | Effort per chore (leicht / mittel / streng) and a daily effort budget. Flexible chores are spread so no day is overloaded | Later |
| F11.2 | Fixed rest days (e.g. Sunday) on which flexible chores are not scheduled | Later |
| F11.3 | Timer: "15 Minuten putzen", optionally with a chore or room | Later |
| F11.4 | "Nur eine Sache" mode: show only the next chore, nothing else | Later |

## Implementation status

| Milestone | Features | Status |
|---|---|---|
| 0 Project setup | Android project, CI, signed releases via GitHub Releases + Obtainium | Done (v0.1.0) |
| 1 Chore basics | F1.1, F1.2, F2.1 to F2.6, F2.9, F3.1 to F3.4, F4.1, F4.2, F4.4, F4.7, F6.1, F6.2, F7.1, F8.1, F9.1, F9.2, F9.5, F10.5 | Done |
| 2 Reminders | F5.1 to F5.4, F5.7, F8.3, N8 | Done |
| 3 Motivation and history | F6.3, F6.4, F7.1 to F7.6, F7.9 | Done (F7.7 badges and F7.8 room statistics later) |
| 4 Holidays and catching up | F10.1 to F10.4, F10.6 | Done |
| 5a Rooms, pausing, checklists | F1.3, F1.4, F1.7, F1.8, F3.5 | Done (F1.9 later) |
| 5b More schedules and completion options | F2.10 to F2.12, F4.3, F4.5 | Done |
| 6 Settings and data | F8.2, F9.3, F9.4, F9.6 | Done (v1.0.0: MVP complete) |
| 7 Visual review, quick create | Rework after a screenshot review; quick create from suggestions, F1.10 duplicate, F3.7 search, F9.7 app shortcuts, themed icon | Done |
| 8 Home Assistant | F5.8 | Done |
| Security hardening | Import validates the whole file (schedules, date range 1900 to 2200, references) and refuses it without changes; import files limited to 5 MB; GitHub Actions pinned to commit SHAs | Done |

## Non-functional requirements

| ID | Requirement |
|---|---|
| N1 | Works fully offline; no network permission needed for the MVP |
| N2 | No ads, no analytics or tracking SDKs |
| N3 | Supports Android 8.0 (API 26) and newer; targets the current Android API level |
| N4 | Home screen opens in under 1 second on a mid-range phone |
| N5 | Follows Material 3 design, supports dark mode and dynamic colour (Android 12+) |
| N6 | Accessible: works with TalkBack, supports large font sizes, touch targets at least 48 dp |
| N7 | Due dates handle time zones and daylight saving time changes correctly (date-based, not timestamp-based) |
| N8 | Reminders still arrive after a reboot and with battery optimisation on |
| N9 | All UI text lives in string resources, so another language can be added later without code changes |

## Suggested tech stack (to confirm)

- Kotlin, Jetpack Compose, Material 3
- Room (SQLite) for local storage
- WorkManager / AlarmManager for reminders
- `java.time` for date logic, with unit tests for all recurrence and streak rules

## Distribution

The app is for personal use. Recommendation:

Decided: **signed APK via GitHub Releases, installed and updated with [Obtainium](https://github.com/ImranR98/Obtainium).**
The other options are kept below for reference.

1. **Chosen: signed APK via GitHub Releases + Obtainium.**
   A GitHub Actions workflow builds and signs a release APK for every version tag. Obtainium
   watches the repository's releases and offers updates on the phone, so it feels like a store
   without one. No fees, no review, releases in minutes. The repository is public, so
   Obtainium needs no GitHub access token.
   - The signing key and its passwords are stored as GitHub Actions secrets and backed up
     outside GitHub. Losing the key means updates can no longer be installed over the
     existing app, so data would have to be exported and imported (F9.3).
2. **Later, only if other people should use it: Google Play.** It costs a one-off USD 25, and new
   personal developer accounts must run a closed test with at least 12 testers for 14 days
   before publishing. That's a lot of overhead for an app with one user.
3. **Not recommended: F-Droid.** It requires the app to be open source, builds the app on its own
   servers and updates arrive slowly. It's a good fit for public open-source apps, not for a
   personal one.

Note: Google is introducing developer verification for apps installed outside the Play Store
on certified Android phones, starting with a few countries in 2026 and rolling out globally
from 2027. Installing via `adb` stays possible, and Google has announced a free option for
hobby developers. Check the current state before the first release; it may mean registering
once as a developer even for sideloading.

## Explicitly not planned for the MVP

- Shopping list, expenses, budgets
- Multiple users, sharing, invites, chore rotation
- Accounts, cloud sync, cloud backup, backend
- iOS or web versions
- Home screen widget, Wear OS
- English or other languages
- Dirtiness levels per room and a mascot (as in Tody): more complexity, little benefit
- Photos, e.g. before / after
- Calendar export (ICS): maybe later
- Shopping list for cleaning supplies: that's a shopping list

## After the MVP

Decided for a later release (priority **Later** in the tables above):

- F1.10 Duplicate a chore
- F2.7 Seasonal chores
- F2.13 Interval suggestions based on real behaviour
- F3.7 Search, F3.8 manual sorting
- F8.4 Upkeep chores in the starter set
- F9.7 App shortcut
- F11 Planning aids: effort budget, rest days, timer, "Nur eine Sache" mode

## Decisions

| # | Question | Decision |
|---|---|---|
| D1 | Home screen widget in the MVP? | No |
| D2 | Motivation features? | Yes, see [F7](#f7-motivation) |
| D3 | Distribution? | GitHub Releases + Obtainium, see [Distribution](#distribution) |
| D3a | Public or private repository? | Public (was private during development; see [PUBLISHING.md](PUBLISHING.md)) |
| D4 | Languages? | German only, Swiss spelling (ss instead of ß) |
| D5 | Checklists / sub-steps? | Yes, optional per chore, see F1.7 to F1.9 |
| D6 | Cloud backup beyond Android's automatic backup? | No |
| D7 | Seasonal chores in the first release? | No |
| D8 | Must every checklist item be ticked before a chore counts as done? | No. The app asks once and the chore can be marked done anyway (F1.8, now Must) |
| D9 | Streak definition | A day counts towards the streak when no chore is overdue at the end of the day (F7.2) |
| D10 | Additions to the MVP from the comparison with Tody, Sweepy, Home Routines and Grocy | Holiday mode and catching up (F10), completing any chore at any time (F4.7), new repeat options (F2.9 to F2.11), tolerance window (F2.12), correcting past completions (F6.4), evening reminder (F5.7) |
| D11 | Additions for after the MVP | See [After the MVP](#after-the-mvp) |

## Open questions

None right now. New requirements will be added here as they come up.
