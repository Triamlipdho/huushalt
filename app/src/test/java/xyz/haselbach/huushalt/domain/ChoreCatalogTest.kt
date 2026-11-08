package xyz.haselbach.huushalt.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ChoreCatalogTest {
    @Test
    fun `all templates are valid and unique`() {
        ChoreCatalog.templates.forEach { assertNull(it.name, it.schedule.validate()) }
        assertEquals(ChoreCatalog.templates.size, ChoreCatalog.templates.map { it.name }.toSet().size)
    }

    @Test
    fun `template texts use Swiss spelling`() {
        val texts = ChoreCatalog.templates.flatMap { listOf(it.name) + it.checklist + listOfNotNull(it.area) }
        assertTrue(texts.filter { 'ß' in it }.toString(), texts.none { 'ß' in it })
    }

    @Test
    fun `first due dates are spread but stay within one interval`() {
        val today = LocalDate.parse("2026-11-10")
        val plants = ChoreCatalog.templates.first { it.name == "Pflanzen giessen" } // every 4 days
        val vacuum = ChoreCatalog.templates.first { it.name == "Staubsaugen" }
        val dates = ChoreCatalog.firstDueDates(listOf(vacuum, vacuum, vacuum, vacuum, vacuum, plants), today)
        assertEquals(today, dates[0])
        assertEquals(today.plusDays(4), dates[4])
        assertEquals("Capped at interval - 1", today.plusDays(3), dates[5])
    }
}
