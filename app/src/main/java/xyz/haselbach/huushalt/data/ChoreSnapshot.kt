package xyz.haselbach.huushalt.data

/**
 * Everything stored about some chores at one moment: the chores with their dates, checklists,
 * completions and missed periods. Taken before a change, it lets the change be undone, whatever
 * it was (skip, postpone, pause, delete, ...).
 */
data class ChoreSnapshot(
    val chores: List<ChoreEntity>,
    val dates: List<ChoreDateEntity>,
    val checklist: List<ChecklistItemEntity>,
    val completions: List<CompletionEntity>,
    val missedPeriods: List<MissedPeriodEntity>,
) {
    val choreIds: List<Long> get() = chores.map { it.id }
}
