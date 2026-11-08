package xyz.haselbach.huushalt.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ProgressTest {
    private fun d(text: String) = LocalDate.parse(text)

    // 2026-11-02 is a Monday.
    private val start = d("2026-11-02")

    @Test
    fun `late completion makes the days between due and done overdue`() {
        val days = Progress.overdueDays(
            completions = listOf(CompletionRecord(1, doneDate = d("2026-11-05"), dueDate = d("2026-11-03"))),
            currentDueDates = emptyList(),
            start = start,
            today = d("2026-11-10"),
        )
        assertEquals(setOf(d("2026-11-03"), d("2026-11-04")), days)
    }

    @Test
    fun `on time and early completions leave no overdue days`() {
        val days = Progress.overdueDays(
            listOf(
                CompletionRecord(1, d("2026-11-03"), d("2026-11-03")),
                CompletionRecord(1, d("2026-11-04"), d("2026-11-10")),
            ),
            emptyList(),
            start,
            d("2026-11-10"),
        )
        assertEquals(emptySet<LocalDate>(), days)
    }

    @Test
    fun `chore overdue now counts until yesterday, not before start`() {
        val days = Progress.overdueDays(emptyList(), listOf(d("2026-10-30"), d("2026-11-20")), start, d("2026-11-04"))
        assertEquals(setOf(d("2026-11-02"), d("2026-11-03")), days)
    }

    @Test
    fun `streak counts clean days and today only once it is clean`() {
        val today = d("2026-11-06")
        assertEquals(Streaks(current = 4, best = 4), Progress.streaks(emptySet(), start, today, todayClean = false))
        assertEquals(Streaks(current = 5, best = 5), Progress.streaks(emptySet(), start, today, todayClean = true))
    }

    @Test
    fun `one overdue day per week is forgiven (F7_6)`() {
        val today = d("2026-11-07")
        val oneBadDay = setOf(d("2026-11-04"))
        assertEquals(Streaks(5, 5), Progress.streaks(oneBadDay, start, today, todayClean = false))
        assertEquals(Streaks(2, 2), Progress.streaks(oneBadDay, start, today, todayClean = false, freeDayPerWeek = false))
    }

    @Test
    fun `second overdue day in the same week breaks the streak`() {
        val today = d("2026-11-09")
        val bad = setOf(d("2026-11-03"), d("2026-11-05"))
        // Mo ok, Tu free day, We ok (3), Th breaks; Fr, Sa, Su ok (3); today not clean yet.
        assertEquals(Streaks(current = 3, best = 3), Progress.streaks(bad, start, today, todayClean = false))
    }

    @Test
    fun `free day is available again in the next week`() {
        val today = d("2026-11-13")
        val bad = setOf(d("2026-11-04"), d("2026-11-11"))
        assertEquals(Streaks(11, 11), Progress.streaks(bad, start, today, todayClean = false))
    }

    @Test
    fun `no streak before the first chore`() {
        assertEquals(Streaks(0, 0), Progress.streaks(emptySet(), d("2026-11-10"), d("2026-11-10"), todayClean = false))
    }

    @Test
    fun `chore streak counts on time completions from the newest`() {
        val newestFirst = listOf(
            CompletionRecord(1, d("2026-11-10"), d("2026-11-10")),
            CompletionRecord(1, d("2026-11-02"), d("2026-11-03")),
            CompletionRecord(1, d("2026-10-30"), d("2026-10-27")),
            CompletionRecord(1, d("2026-10-20"), d("2026-10-20")),
        )
        assertEquals(2, Progress.choreStreak(newestFirst, overdueNow = false))
        assertEquals(0, Progress.choreStreak(newestFirst, overdueNow = true))
    }

    @Test
    fun `stats count weeks, months and punctuality`() {
        val today = d("2026-11-11") // Wednesday
        val completions = listOf(
            CompletionRecord(1, d("2026-11-10"), d("2026-11-10")),
            CompletionRecord(1, d("2026-11-09"), d("2026-11-08")),
            CompletionRecord(2, d("2026-11-04"), d("2026-11-04")),
            CompletionRecord(2, d("2026-10-28"), d("2026-10-28")),
            CompletionRecord(3, d("2026-08-01"), d("2026-08-01")),
        )
        val stats = Progress.stats(completions, today)
        assertEquals(2, stats.thisWeek)
        assertEquals(1, stats.lastWeek)
        assertEquals(3, stats.thisMonth)
        assertEquals(1, stats.lastMonth)
        assertEquals(75, stats.onTimePercent)
        assertEquals(8, stats.weeks.size)
        assertEquals(d("2026-11-09"), stats.weeks.last().weekStart)
        assertEquals(2, stats.weeks.last().count)
        assertNull(Progress.stats(emptyList(), today).onTimePercent)
    }
}
