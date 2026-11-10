package xyz.haselbach.huushalt.domain

import java.time.LocalDate

/** Completions in a room ([areaId] null: "Ohne Raum") in the last 30 days (F7.8). */
data class AreaCount(val areaId: Long?, val count: Int)

object AreaStats {

    /**
     * Completions per room in the rolling 30 days ending [today], like [Stats.last30Days].
     * [areaOf] maps each chore to its room; every room with a chore is listed, also with 0, most
     * first. Ties keep the order of [areaOrder] (e.g. the room list), "Ohne Raum" last.
     */
    fun last30Days(
        completions: List<CompletionRecord>,
        areaOf: Map<Long, Long?>,
        today: LocalDate,
        areaOrder: List<Long> = emptyList(),
    ): List<AreaCount> {
        val from = today.minusDays(DAYS)
        val counts = completions
            .filter { it.doneDate > from && it.doneDate <= today && it.choreId in areaOf }
            .groupingBy { areaOf[it.choreId] }
            .eachCount()
        val rank = areaOrder.withIndex().associate { (i, id) -> id to i }
        return (areaOf.values.toSet() + counts.keys)
            .sortedWith(compareBy<Long?> { if (it == null) 1 else 0 }.thenBy { rank[it] ?: Int.MAX_VALUE }.thenBy { it })
            .map { AreaCount(it, counts[it] ?: 0) }
            .sortedByDescending { it.count }
    }

    const val DAYS = 30L
}
