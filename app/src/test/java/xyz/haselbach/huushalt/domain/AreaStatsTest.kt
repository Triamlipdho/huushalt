package xyz.haselbach.huushalt.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class AreaStatsTest {
    private val today = LocalDate.parse("2026-11-10")

    private fun done(choreId: Long, daysAgo: Long) = CompletionRecord(choreId, today.minusDays(daysAgo), null)

    // Chores 1 and 2 in the kitchen (10), 3 in the bath (20), 4 without a room.
    private val areaOf = mapOf(1L to 10L, 2L to 10L, 3L to 20L, 4L to null)

    @Test
    fun `counts the rolling 30 days per room, most first`() {
        val counts = AreaStats.last30Days(
            listOf(
                done(1, 0), done(2, 3), done(1, 29),
                done(1, 30), // 31st day back: outside
                done(3, 5),
                done(4, 1), done(4, 2),
                done(1, -1), // in the future: not counted
            ),
            areaOf,
            today,
        )
        assertEquals(listOf(AreaCount(10, 3), AreaCount(null, 2), AreaCount(20, 1)), counts)
    }

    @Test
    fun `rooms without completions are listed with 0, ties keep the room order`() {
        val counts = AreaStats.last30Days(emptyList(), areaOf, today, areaOrder = listOf(20L, 10L))
        assertEquals(listOf(AreaCount(20, 0), AreaCount(10, 0), AreaCount(null, 0)), counts)
    }

    @Test
    fun `no chore without a room means no Ohne Raum row`() {
        val counts = AreaStats.last30Days(listOf(done(3, 0)), mapOf(3L to 20L), today)
        assertEquals(listOf(AreaCount(20, 1)), counts)
    }

    @Test
    fun `completions of unknown chores are ignored`() {
        assertEquals(listOf(AreaCount(20, 0)), AreaStats.last30Days(listOf(done(99, 0)), mapOf(3L to 20L), today))
    }
}
