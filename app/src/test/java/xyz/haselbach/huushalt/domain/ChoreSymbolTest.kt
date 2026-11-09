package xyz.haselbach.huushalt.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChoreSymbolTest {
    @Test
    fun `symbols are guessed from German names`() {
        val expected = mapOf(
            "Staubsaugen" to ChoreSymbol.VACUUM,
            "Staub wischen" to ChoreSymbol.DUSTING,
            "Bad putzen" to ChoreSymbol.BATH,
            "Kühlschrank putzen" to ChoreSymbol.FRIDGE,
            "Kehricht rausbringen" to ChoreSymbol.TRASH,
            "Altpapier und Karton bereitstellen" to ChoreSymbol.RECYCLING,
            "Pflanzen giessen" to ChoreSymbol.PLANTS,
            "Bettwäsche wechseln" to ChoreSymbol.BED,
            "Wäsche waschen" to ChoreSymbol.LAUNDRY,
            "Wasserkocher entkalken" to ChoreSymbol.KETTLE,
            "Rauchmelder testen" to ChoreSymbol.SMOKE_ALARM,
            "Steuererklärung" to ChoreSymbol.PAPERWORK,
            "Etwas ganz anderes" to ChoreSymbol.HOME,
        )
        expected.forEach { (name, symbol) -> assertEquals(name, symbol, ChoreSymbol.guess(name)) }
    }

    @Test
    fun `every catalogue chore gets a specific symbol`() {
        val generic = ChoreCatalog.templates.filter { ChoreSymbol.guess(it.name) == ChoreSymbol.HOME }.map { it.name }
        assertTrue("No symbol for: $generic", generic.isEmpty())
    }

    @Test
    fun `chosen symbol and colour win over the guess`() {
        assertEquals(ChoreLook(ChoreSymbol.BATH, ChoreColor.TEAL), ChoreLook.of("Bad putzen", null, null))
        assertEquals(ChoreLook(ChoreSymbol.PETS, ChoreColor.PINK), ChoreLook.of("Bad putzen", "PETS", "PINK"))
        assertEquals("Unknown keys fall back", ChoreLook(ChoreSymbol.BATH, ChoreColor.TEAL), ChoreLook.of("Bad putzen", "GONE", "GONE"))
    }
}
