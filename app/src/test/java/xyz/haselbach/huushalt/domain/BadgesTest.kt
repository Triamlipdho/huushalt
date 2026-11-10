package xyz.haselbach.huushalt.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BadgesTest {
    private val day0 = LocalDate.parse("2026-01-01")

    /** [count] completions of [choreId], one a day, on time unless listed in [late]. */
    private fun done(count: Int, choreId: Long = 1, late: Set<Int> = emptySet()) = (0 until count).map { i ->
        val doneDate = day0.plusDays(i.toLong())
        CompletionRecord(choreId, doneDate, if (i in late) doneDate.minusDays(2) else doneDate)
    }

    private fun List<Badge>.byId(id: String) = single { it.id == id }

    @Test
    fun `nothing done earns nothing and every family has a next badge`() {
        val badges = Badges.of(emptyList(), bestStreak = 0)
        assertTrue(badges.none { it.earned })
        assertEquals(badges.map { it.id }.distinct().size, badges.size)
        assertEquals(listOf("done_1", "streak_7", "chore_streak_10"), Badges.next(badges).map { it.id })
    }

    @Test
    fun `completions count towards the done badges`() {
        val badges = Badges.of(done(37), bestStreak = 0)
        assertTrue(badges.byId("done_1").earned)
        assertTrue(badges.byId("done_10").earned)
        assertFalse(badges.byId("done_50").earned)
        assertEquals(37, badges.byId("done_50").progress)
        assertEquals(50, badges.byId("done_50").target)
        assertEquals("done_50", Badges.next(badges).first().id)
    }

    @Test
    fun `progress is capped at the target`() {
        val badges = Badges.of(done(12), bestStreak = 300)
        assertEquals(10, badges.byId("done_10").progress)
        assertTrue(badges.byId("streak_100").earned)
        assertEquals(100, badges.byId("streak_100").progress)
        assertTrue(Badges.next(badges).none { it.family == BadgeFamily.STREAK })
    }

    @Test
    fun `best streak counts, not the current one`() {
        val badges = Badges.of(emptyList(), bestStreak = 8)
        assertTrue(badges.byId("streak_7").earned)
        assertEquals(8, badges.byId("streak_28").progress)
    }

    @Test
    fun `best on time run of a single chore`() {
        // 12 completions, the 3rd late: runs of 2 and 9.
        assertEquals(9, Badges.bestOnTimeRun(done(12, late = setOf(2))))
        // Runs of different chores don't add up.
        assertEquals(6, Badges.bestOnTimeRun(done(6, choreId = 1) + done(6, choreId = 2)))
        assertEquals(0, Badges.bestOnTimeRun(emptyList()))
    }

    @Test
    fun `ten on time in a row earns the chore badge even when broken later`() {
        val badges = Badges.of(done(12, late = setOf(11)), bestStreak = 0)
        assertTrue(badges.byId("chore_streak_10").earned)
    }

    @Test
    fun `the first run remembers earned badges without announcing them`() {
        val badges = Badges.of(done(60), bestStreak = 10)
        val announcement = Badges.toAnnounce(badges, announced = null)
        assertEquals(emptyList<Badge>(), announcement.new)
        assertEquals(setOf("done_1", "done_10", "done_50", "streak_7", "chore_streak_10"), announcement.remember)
    }

    @Test
    fun `newly earned badges are announced once`() {
        val announcement = Badges.toAnnounce(Badges.of(done(1), bestStreak = 0), announced = emptySet())
        assertEquals(listOf("done_1"), announcement.new.map { it.id })
        assertEquals(setOf("done_1"), announcement.remember)
        val again = Badges.toAnnounce(Badges.of(done(2), bestStreak = 0), announcement.remember)
        assertEquals(emptyList<Badge>(), again.new)
    }

    @Test
    fun `a badge lost by deleting history is not announced again`() {
        // Undo of the first completion: nothing earned, but "done_1" stays remembered.
        val announcement = Badges.toAnnounce(Badges.of(emptyList(), bestStreak = 0), announced = setOf("done_1"))
        assertEquals(emptyList<Badge>(), announcement.new)
        assertEquals(setOf("done_1"), announcement.remember)
    }
}
