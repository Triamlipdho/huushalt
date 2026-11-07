# huushalt MVP feature list

Status: draft, v0.1. More requirements will be added later (see [Open questions](#open-questions)).

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
| F2.7 | Yearly or seasonal chores (e.g. "clean windows every spring") | Should |
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
| F6.4 | Simple statistics (done this week / month, on-time rate) | Could |
| F6.5 | Streaks or other motivation features, replacing Flatastic's points | Could |

### F7 First start

| ID | Feature | Priority |
|---|---|---|
| F7.1 | App is usable immediately, no account or sign-up | Must |
| F7.2 | Optional starter set: pick from a list of common chores with suggested intervals (vacuum weekly, change bed linen every 2 weeks, clean fridge monthly, ...) | Should |
| F7.3 | Ask for notification permission (Android 13+) with a short explanation | Must |

### F8 Data and settings

| ID | Feature | Priority |
|---|---|---|
| F8.1 | All data stored locally on the device | Must |
| F8.2 | Data is included in Android's automatic backup, so it survives a phone change | Must |
| F8.3 | Manual export / import of all data as a file (JSON) | Should |
| F8.4 | Settings: reminder time, first day of the week, theme (system / light / dark) | Must |
| F8.5 | Languages: German and English, following the system language | Must |
| F8.6 | Delete all data | Should |

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

## Suggested tech stack (to confirm)

- Kotlin, Jetpack Compose, Material 3
- Room (SQLite) for local storage
- WorkManager / AlarmManager for reminders
- `java.time` for date logic, with unit tests for all recurrence rules
- Distribution: Google Play and/or direct APK (to decide)

## Explicitly not planned for the MVP

- Shopping list, expenses, budgets
- Multiple users, sharing, invites, chore rotation
- Accounts, cloud sync, backend
- iOS or web versions
- Home screen widget, Wear OS (good candidates for after the MVP)

## Open questions

To be worked out together before or during development:

1. Is a home screen widget ("today's chores") important enough to move into the MVP?
2. Should there be any motivation features (streaks, statistics), or should the app stay neutral?
3. Distribution: Google Play, F-Droid, or only a personal APK?
4. Languages: is German + English enough? Should UI text use Swiss German or Swiss Standard German spelling (ss instead of ß)?
5. Should chores support checklists / sub-steps (e.g. "clean bathroom" = sink, toilet, shower)?
6. Is cloud backup (e.g. to Google Drive) needed beyond Android's automatic backup?
7. Should seasonal chores (F2.7) be in the first release?
