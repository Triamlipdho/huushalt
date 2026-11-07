# huushalt MVP feature list

Status: draft, v0.2. More requirements will be added later (see [Decisions](#decisions) and [Open questions](#open-questions)).

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
if time allows, **Could** = nice to have, likely after the MVP.

### F1 Chore management

| ID | Feature | Priority |
|---|---|---|
| F1.1 | Create a chore with a name (required) and optional notes | Must |
| F1.2 | Edit and delete a chore (delete asks for confirmation) | Must |
| F1.3 | Pause / archive a chore without losing its history | Should |
| F1.4 | Assign a chore to a room or area (kitchen, bathroom, ...), with editable list of rooms | Should |
| F1.5 | Pick an icon or colour for a chore | Could |
| F1.6 | Estimated duration (e.g. 15 min), shown in the daily list | Could |
| F1.7 | Optional checklist of sub-steps per chore (e.g. "Bad putzen" = Lavabo, WC, Dusche, Spiegel). Items are ticked off while doing the chore and reset for the next occurrence | Should |
| F1.8 | Completing a chore with an unfinished checklist asks once whether to mark it done anyway | Should |
| F1.9 | Ticking the last checklist item marks the chore as done | Could |

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
| F2.7 | Seasonal chores (e.g. "clean windows every spring"). Plain yearly repeats are covered by F2.2 | Won't (after MVP) |
| F2.8 | Chores without a date ("whenever I find time") in a backlog | Could |

### F3 Daily overview (home screen)

| ID | Feature | Priority |
|---|---|---|
| F3.1 | Home screen shows chores grouped into **Overdue**, **Today**, **Upcoming** (next 7 days) | Must |
| F3.2 | Overdue chores show how many days late they are | Must |
| F3.3 | Empty state when nothing is due ("All done for today") | Must |
| F3.4 | List of all chores, sortable by name, next due date and room | Must |
| F3.5 | Filter the overview by room | Should |
| F3.6 | Calendar / week view of upcoming chores | Could |

### F4 Completing chores

| ID | Feature | Priority |
|---|---|---|
| F4.1 | Mark a chore as done with one tap; next due date is calculated immediately | Must |
| F4.2 | Undo the last completion (snackbar) | Must |
| F4.3 | Mark as done on an earlier date ("I did it yesterday") | Should |
| F4.4 | Postpone / snooze to tomorrow or a chosen date | Must |
| F4.5 | Skip one occurrence without counting it as done | Should |
| F4.6 | Add a short note when completing | Could |

### F5 Reminders

| ID | Feature | Priority |
|---|---|---|
| F5.1 | Daily summary notification at a time I choose (e.g. 08:00: "3 chores today, 1 overdue") | Must |
| F5.2 | Turn notifications on or off globally | Must |
| F5.3 | Tapping a notification opens the daily overview | Must |
| F5.4 | Mark as done directly from the notification | Should |
| F5.5 | Per-chore reminder at a specific time | Could |
| F5.6 | Quiet days (e.g. no reminders on Sunday) | Could |

### F6 History

| ID | Feature | Priority |
|---|---|---|
| F6.1 | Per-chore history: list of completion dates (Flatastic charges for this) | Must |
| F6.2 | Show "last done" date on each chore | Must |
| F6.3 | Global activity log of everything done, newest first | Should |

### F7 Motivation

Flatastic motivates through points compared between flatmates. With one person there is
nobody to compare against, so huushalt motivates by comparing you with yourself: progress,
streaks and a record of what you have achieved. Motivation must stay encouraging, never
nagging: no guilt messages, and a missed day doesn't wipe out everything.

| ID | Feature | Priority |
|---|---|---|
| F7.1 | Daily progress on the home screen (e.g. "2 von 5 erledigt" with a progress ring) | Must |
| F7.2 | "Alles erledigt" streak: number of days in a row with no overdue chores | Must |
| F7.3 | Per-chore streak: how many times in a row a chore was done on time | Should |
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

### F9 Data and settings

| ID | Feature | Priority |
|---|---|---|
| F9.1 | All data stored locally on the device | Must |
| F9.2 | Data is included in Android's automatic backup, so it survives a phone change. No other cloud backup | Must |
| F9.3 | Manual export / import of all data as a file (JSON) | Should |
| F9.4 | Settings: reminder time, first day of the week (default Monday), theme (system / light / dark) | Must |
| F9.5 | Language: German only, Swiss spelling (ss instead of ß, e.g. "erledigt", "Strasse", "grüssen"); Swiss date format (07.11.2026) | Must |
| F9.6 | Delete all data | Should |

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

1. **Now: signed APK via GitHub Releases, installed and updated with [Obtainium](https://github.com/ImranR98/Obtainium).**
   A GitHub Actions workflow builds and signs a release APK for every version tag. Obtainium
   watches the repository's releases and offers updates on the phone, so it feels like a store
   without one. No fees, no review, releases in minutes. If the repository stays private,
   Obtainium needs a GitHub access token with read access.
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
- Seasonal chores (F2.7)
- English or other languages

## Decisions

| # | Question | Decision |
|---|---|---|
| D1 | Home screen widget in the MVP? | No |
| D2 | Motivation features? | Yes, see [F7](#f7-motivation) |
| D3 | Distribution? | GitHub Releases + Obtainium, see [Distribution](#distribution) (recommendation, to confirm) |
| D4 | Languages? | German only, Swiss spelling (ss instead of ß) |
| D5 | Checklists / sub-steps? | Yes, optional per chore, see F1.7 to F1.9 |
| D6 | Cloud backup beyond Android's automatic backup? | No |
| D7 | Seasonal chores in the first release? | No |

## Open questions

1. Confirm distribution (D3): GitHub Releases + Obtainium, and should the repository be public or private?
2. Checklists: must every item be ticked before a chore counts as done, or is "done anyway" (F1.8) fine?
3. Motivation: which streak definition feels right, "no overdue chores" per day (F7.2) or something else?
