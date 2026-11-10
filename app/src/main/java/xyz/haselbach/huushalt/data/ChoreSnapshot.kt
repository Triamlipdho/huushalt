package xyz.haselbach.huushalt.data

/**
 * Everything stored about some chores at one moment: the chores with their dates, checklists,
 * completions and missed periods. Lists are sorted by id, so two snapshots of unchanged chores
 * are equal.
 */
data class ChoreSnapshot(
    val chores: List<ChoreEntity>,
    val dates: List<ChoreDateEntity>,
    val checklist: List<ChecklistItemEntity>,
    val completions: List<CompletionEntity>,
    val missedPeriods: List<MissedPeriodEntity>,
)

/**
 * A change that can be undone: the chores before and right after it. Undo puts [before] back,
 * but only while the chores still look like [after]; anything changed since (an edit, a
 * completion from the notification, an import, a holiday ending) is never overwritten.
 */
data class UndoToken(val choreIds: List<Long>, val before: ChoreSnapshot, val after: ChoreSnapshot)
