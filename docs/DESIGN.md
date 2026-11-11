# Design system

huushalt is Android-only and follows **Material Design 3** (Jetpack Compose `material3`).
This page lists the tokens screens are built from. Use them instead of ad-hoc values. Code:
[`ui/theme/Theme.kt`](../app/src/main/java/xyz/haselbach/huushalt/ui/theme/Theme.kt) and
[`ui/theme/Tokens.kt`](../app/src/main/java/xyz/haselbach/huushalt/ui/theme/Tokens.kt).

## Colour

Own scheme: a fresh green (`primary`, ~10 % of the screen: section headers, due "erledigt"
buttons, progress) with a warm terracotta accent (`tertiary`: streaks, badges, the "+" button).
Surfaces have a hint of green instead of plain grey. Every Material role is set for light and
dark mode, so nothing falls back to Material's purple. Wallpaper colours (Android 12+) are
optional in the settings.

| Use | Token |
|---|---|
| Text on the background | `onSurface`; secondary text `onSurfaceVariant` |
| Section headers, text buttons | `primary` |
| Overdue ("2 Tage überfällig") | `error`, only on the due part, and always with text |
| Progress card | `primaryContainer` / `onPrimaryContainer` |
| Detail summary, statistics | `secondaryContainer` / `onSecondaryContainer` |
| Earned badges, "+" button, current week in the chart | `tertiaryContainer` / `onTertiaryContainer`, `tertiary` bar |
| Outlines of interactive elements | `outline` (≥ 3:1). `outlineVariant` only for dividers |
| Swipe right (done) / left (postpone) | `primaryContainer` / `errorContainer`, each with an icon |

Chore symbols have nine colours (`ChoreColor`): pastel circle with a dark symbol in light mode,
deep circle with a light symbol in dark mode.

**Contrast (WCAG 2.2 AA)** is checked by `ContrastTest`: 4.5:1 for every text/background pair
above and every symbol colour, 3:1 for outlines and the progress ring. Add new pairs there.

## Typography

Material 3 type scale with the system font (Roboto). No custom sizes; text scales with the
system font size (the screenshot tour has a `large` variant at 1.3×).

| Use | Style |
|---|---|
| Screen title | `titleLarge` (top app bar) |
| Empty states | `headlineSmall` |
| Card titles, progress | `titleMedium` |
| Section headers | `titleSmall`, `primary`, marked as heading for TalkBack |
| Chore name in lists | `bodyLarge` (list item headline) |
| Room · due date · checklist | `bodyMedium`, `onSurfaceVariant` |
| Stat values | `headlineSmall`; labels `labelMedium` |

## Spacing (`Spacing`)

4/8 dp grid.

| Token | dp | Use |
|---|---|---|
| `xs` | 4 | Value and label in dense cards |
| `sm` | 8 | Between chips and buttons (minimum gap between touch targets) |
| `md` | 12 | Inside small cards, between rows of a card |
| `lg` | 16 | Screen edge, card padding, list item padding |
| `xl` | 24 | Between sections |
| `xxl` / `xxxl` | 32 / 48 | Empty states |

## Shape (`HuushaltShapes`)

| Token | Radius | Use |
|---|---|---|
| `extraSmall` | 4 dp | Chart bars |
| `small` | 8 dp | Chips, text actions |
| `medium` | 16 dp | Cards |
| `large` | 20 dp | FAB |
| `extraLarge` | 28 dp | Dialogs, bottom sheets |
| `CircleShape` | – | Chore avatars, icon buttons, colour swatches |

## Elevation

Flat by default: cards are told apart by container colour, not shadows. Tonal elevation
(3 dp) only for the sticky bottom bar of the suggestions screen.

## Touch and accessibility

- Every touch target is at least **48 × 48 dp** (`Sizes.touchTarget`), with ≥ 8 dp between them.
- Primary actions are in the lower part of the screen: "+" FAB, "erledigt" buttons on the right
  of each row, the bottom navigation.
- Swipes are shortcuts only; the same actions exist as buttons and as TalkBack custom actions.
- State is never shown by colour alone: overdue has text, the selected colour swatch has a tick,
  selected chips have a check mark.
- Decorative icons (chore symbols) have no content description; the text next to them says it.
- Every change can be undone from the snackbar.

## Components

| Component | Where | Notes |
|---|---|---|
| `ChoreRow` | All lists | Avatar, name (2 lines max), room · due · checklist; filled tonal "erledigt" when due, outlined when upcoming |
| `ChoreAvatar` | Lists 40 dp, week view 32 dp, detail 56 dp | Symbol in a coloured circle |
| `SectionHeader(WithAction)` | Lists | Heading semantics; optional text action on the same baseline |
| `EmptyState` | Lists | Title, explanation, optional primary button |
| Cards | Progress, statistics, hints | Filled container colour, `medium` shape, `lg` padding |
| `ChoreSheet` | Tap on any chore | Bottom sheet: due date, schedule, checklist, Erledigt / Verschieben / Überspringen; Bearbeiten and Details one step further |
| `HuushaltSnackbarHost` | All screens | Floating, `large` shape, `inverseSurface`, icon of what happened, swipe to dismiss, "Rückgängig" in `inversePrimary` |
| Week strip | Alle Aufgaben → Woche | "KW 46", 7 day cells (min. 88 dp tall): dots per chore (number from 5), beach icon on holidays; today has a 2 dp `primary` ring, the selected day is filled `primaryContainer` |
| Top app bar | Tabs | `MediumTopAppBar`, collapses on scroll to a small bar; the title stays. Section headers are sticky |

Weeks are shown as calendar weeks ("KW 46", ISO 8601) in the week view and the Fortschritt
chart; the current week is highlighted by colour and a filled label.

## Checking a change

`./gradlew testDebugUnitTest --tests '*ScreenshotTour*'` writes every screen in light, dark and
large-font variants to `app/build/screenshots/`. Look at all three before opening a pull request.
