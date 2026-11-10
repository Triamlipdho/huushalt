package xyz.haselbach.huushalt.domain

/** What a badge counts. */
enum class BadgeFamily {
    /** Chores done in total. */
    DONE,

    /** Best "Alles erledigt" streak in days (F7.2). */
    STREAK,

    /** Best run of one chore done on time in a row (F7.3). */
    CHORE_STREAK,
}

/**
 * A milestone (F7.7). [progress] is how far it is, capped at [target]; it's earned once the
 * target is reached. Titles and descriptions are in the resources, looked up by [id].
 */
data class Badge(val id: String, val emoji: String, val family: BadgeFamily, val target: Int, val progress: Int) {
    val earned: Boolean get() = progress >= target
}

/** Badges to announce now, and the ids to remember as announced afterwards. */
data class BadgeAnnouncement(val new: List<Badge>, val remember: Set<String>)

/**
 * Badges, all calculated from the history: no extra tracking, and they stay right when the
 * history is edited. Only bests count (best streak, best run per chore), so a missed day never
 * takes a badge away (F7).
 */
object Badges {

    private data class Definition(val id: String, val emoji: String, val family: BadgeFamily, val target: Int)

    private val definitions = listOf(
        Definition("done_1", "🌱", BadgeFamily.DONE, 1),
        Definition("done_10", "🧹", BadgeFamily.DONE, 10),
        Definition("done_50", "✨", BadgeFamily.DONE, 50),
        Definition("done_100", "💯", BadgeFamily.DONE, 100),
        Definition("done_250", "🏆", BadgeFamily.DONE, 250),
        Definition("done_500", "👑", BadgeFamily.DONE, 500),
        Definition("streak_7", "🔥", BadgeFamily.STREAK, 7),
        Definition("streak_28", "🌟", BadgeFamily.STREAK, 28),
        Definition("streak_100", "🏅", BadgeFamily.STREAK, 100),
        Definition("chore_streak_10", "🎯", BadgeFamily.CHORE_STREAK, 10),
    )

    /** All badges in a fixed order; [bestStreak] is the best "Alles erledigt" streak. */
    fun of(completions: List<CompletionRecord>, bestStreak: Int): List<Badge> {
        val values = mapOf(
            BadgeFamily.DONE to completions.size,
            BadgeFamily.STREAK to bestStreak,
            BadgeFamily.CHORE_STREAK to bestOnTimeRun(completions),
        )
        return definitions.map { d ->
            Badge(d.id, d.emoji, d.family, d.target, values.getValue(d.family).coerceIn(0, d.target))
        }
    }

    /** Per family, the first badge not earned yet: what to aim for next. */
    fun next(badges: List<Badge>): List<Badge> =
        badges.filterNot { it.earned }.groupBy { it.family }.values.map { it.first() }

    /** The longest run of on-time completions in a row of any single chore. */
    fun bestOnTimeRun(completions: List<CompletionRecord>): Int =
        completions.groupBy { it.choreId }.values.maxOfOrNull { list ->
            var run = 0
            var best = 0
            list.sortedBy { it.doneDate }.forEach { c ->
                run = if (c.onTime) run + 1 else 0
                best = maxOf(best, run)
            }
            best
        } ?: 0

    /**
     * Which earned badges to announce. With [announced] null (nothing remembered yet, e.g. the
     * first run after an update), everything already earned is remembered without announcing,
     * so a long-time user isn't flooded.
     */
    fun toAnnounce(badges: List<Badge>, announced: Set<String>?): BadgeAnnouncement {
        val earned = badges.filter { it.earned }
        if (announced == null) return BadgeAnnouncement(emptyList(), earned.map { it.id }.toSet())
        val new = earned.filter { it.id !in announced }
        return BadgeAnnouncement(new, announced + new.map { it.id })
    }
}
