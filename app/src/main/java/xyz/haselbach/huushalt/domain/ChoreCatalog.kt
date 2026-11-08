package xyz.haselbach.huushalt.domain

import java.time.LocalDate

/** A common household chore with a suggested schedule, room and checklist (F8.2). */
data class ChoreTemplate(
    val name: String,
    val area: String?,
    val schedule: Schedule,
    val checklist: List<String> = emptyList(),
) {
    /** Rough length of one interval in days, used to spread first due dates. */
    val intervalDays: Int
        get() = when (schedule.unit) {
            RepeatUnit.DAYS -> schedule.interval
            RepeatUnit.WEEKS -> schedule.interval * 7
            RepeatUnit.MONTHS -> schedule.interval * 30
            else -> 1
        }
}

/**
 * Suggestions for a one-person household, used for the starter set (F8.2) and quick create.
 * All flexible ("Nach dem Erledigen"), since that is how most solo chores work.
 */
object ChoreCatalog {
    private fun every(interval: Int, unit: RepeatUnit) = Schedule(ScheduleMode.FLEXIBLE, unit, interval)

    private val days = RepeatUnit.DAYS
    private val weeks = RepeatUnit.WEEKS
    private val months = RepeatUnit.MONTHS

    val templates: List<ChoreTemplate> = listOf(
        // Ganze Wohnung
        ChoreTemplate("Staubsaugen", null, every(1, weeks)),
        ChoreTemplate("Boden feucht aufnehmen", null, every(2, weeks)),
        ChoreTemplate("Staub wischen", null, every(2, weeks)),
        ChoreTemplate("Pflanzen giessen", null, every(4, days)),
        ChoreTemplate("Fenster putzen", null, every(3, months)),
        ChoreTemplate("Kehricht rausbringen", null, every(1, weeks)),
        ChoreTemplate("Altpapier und Karton bereitstellen", null, every(2, weeks)),
        // Küche
        ChoreTemplate("Küche putzen", "Küche", every(1, weeks), listOf("Abwaschen", "Ablagen abwischen", "Herd reinigen", "Spülbecken putzen")),
        ChoreTemplate("Kühlschrank putzen", "Küche", every(1, months), listOf("Abgelaufenes entsorgen", "Fächer auswischen")),
        ChoreTemplate("Backofen reinigen", "Küche", every(3, months)),
        ChoreTemplate("Wasserkocher entkalken", "Küche", every(2, months)),
        ChoreTemplate("Geschirrspüler-Filter reinigen", "Küche", every(1, months)),
        ChoreTemplate("Dampfabzug-Filter reinigen", "Küche", every(3, months)),
        ChoreTemplate("Tiefkühler abtauen", "Küche", every(6, months)),
        // Bad
        ChoreTemplate("Bad putzen", "Bad", every(1, weeks), listOf("Lavabo", "WC", "Dusche oder Badewanne", "Spiegel", "Boden")),
        ChoreTemplate("Handtücher wechseln", "Bad", every(1, weeks)),
        ChoreTemplate("Abflüsse reinigen", "Bad", every(1, months)),
        // Schlafzimmer
        ChoreTemplate("Bettwäsche wechseln", "Schlafzimmer", every(2, weeks)),
        ChoreTemplate("Matratze wenden", "Schlafzimmer", every(3, months)),
        // Waschküche
        ChoreTemplate("Wäsche waschen", "Waschküche", every(1, weeks), listOf("Waschen", "Aufhängen", "Zusammenlegen")),
        ChoreTemplate("Waschmaschine reinigen", "Waschküche", every(2, months)),
        // Unterhalt
        ChoreTemplate("Rauchmelder testen", null, every(6, months)),
    )

    /**
     * First due dates for a batch of [selected] templates, so they don't all fall on the same day:
     * the n-th chore starts n days from [today], but never later than one interval away.
     */
    fun firstDueDates(selected: List<ChoreTemplate>, today: LocalDate): List<LocalDate> =
        selected.mapIndexed { index, template -> today.plusDays(minOf(index, template.intervalDays - 1).toLong()) }
}
