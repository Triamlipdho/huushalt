package xyz.haselbach.huushalt.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

/** Upgrading keeps existing chores and completions (users already have data on their phones). */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {
    private val dbName = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), HuushaltDatabase::class.java)

    @Test
    fun `version 1 to the current version keeps chores and completions`() = runBlocking {
        val due = LocalDate.parse("2026-11-10")
        helper.createDatabase(dbName, 1).apply {
            execSQL(
                """
                INSERT INTO chores (id, name, notes, mode, unit, interval, weekdays, dayOfMonth, anchorDate, dueDate, createdAt)
                VALUES (1, 'Staubsaugen', 'Auch unter dem Sofa', 'FLEXIBLE', 'WEEKS', 1, 0, NULL, ${due.toEpochDay()}, ${due.toEpochDay()}, 0)
                """.trimIndent(),
            )
            execSQL(
                "INSERT INTO completions (id, choreId, doneDate, previousDueDate, createdAt) " +
                    "VALUES (1, 1, ${due.minusWeeks(1).toEpochDay()}, ${due.minusWeeks(1).toEpochDay()}, 0)",
            )
            close()
        }

        helper.runMigrationsAndValidate(dbName, 2, true)
        helper.runMigrationsAndValidate(dbName, 3, true)
        helper.runMigrationsAndValidate(dbName, 4, true)

        val db = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            HuushaltDatabase::class.java,
            dbName,
        ).build()
        try {
            val chore = db.choreDao().observeChores().first().single()
            assertEquals("Staubsaugen", chore.chore.name)
            assertEquals("Auch unter dem Sofa", chore.chore.notes)
            assertEquals(due, chore.chore.dueDate)
            assertEquals(due.minusWeeks(1), chore.lastDone)
            assertEquals(emptyList<HolidayEntity>(), db.holidayDao().getHolidays())
            assertEquals(null, chore.chore.areaId)
            assertEquals(false, chore.chore.paused)
            assertEquals(0, chore.checklistTotal)
            assertEquals(0, chore.chore.toleranceDays)
            assertEquals(null, chore.chore.weekOfMonth)
        } finally {
            db.close()
        }
    }
}
