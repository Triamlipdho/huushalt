package xyz.haselbach.huushalt.domain

import java.time.DateTimeException
import java.time.LocalDate

/**
 * Reads dates from pasted text, e.g. a collection calendar copied from the Gemeinde's website
 * (F2.11): "Di 12.01.2027, Di 26.01.27; 9.2.2027". Accepts d.M.yyyy and d.M.yy, ignores
 * everything else and returns the valid dates sorted, without duplicates.
 */
object DateListParser {
    private val pattern = Regex("""\b(\d{1,2})\.(\d{1,2})\.(\d{4}|\d{2})\b""")

    fun parse(text: String): List<LocalDate> = pattern.findAll(text)
        .mapNotNull { match ->
            val (day, month, year) = match.destructured
            val fullYear = if (year.length == 2) 2000 + year.toInt() else year.toInt()
            try {
                LocalDate.of(fullYear, month.toInt(), day.toInt())
            } catch (e: DateTimeException) {
                null
            }
        }
        .distinct()
        .sorted()
        .toList()
}
