package xyz.haselbach.huushalt.domain

/** Colours for chore symbols (F1.5). The UI maps them to light and dark variants. */
enum class ChoreColor { BLUE, TEAL, GREEN, YELLOW, ORANGE, RED, PINK, PURPLE, GREY }

/**
 * Symbols for chores (F1.5). Each has a default colour and German keywords, so chores without a
 * chosen symbol still get a fitting one from their name. More specific symbols come first.
 */
enum class ChoreSymbol(val defaultColor: ChoreColor, vararg val keywords: String) {
    TRASH(ChoreColor.GREY, "kehricht", "abfall", "müll", "mist", "abfalleimer", "kübel"),
    RECYCLING(ChoreColor.GREEN, "papier", "karton", "altglas", "glas", "recycl", "pet", "kompost", "grüngut"),
    PLANTS(ChoreColor.GREEN, "pflanze", "blume", "giess", "garten", "balkon", "rasen"),
    WINDOW(ChoreColor.BLUE, "fenster", "storen", "vorhang"),
    FRIDGE(ChoreColor.ORANGE, "kühlschrank", "tiefkühl", "gefrier", "abtauen"),
    OVEN(ChoreColor.ORANGE, "backofen", "ofen", "herd", "mikrowelle"),
    KETTLE(ChoreColor.ORANGE, "wasserkocher", "kaffee", "entkalk"),
    DISHES(ChoreColor.ORANGE, "geschirr", "abwasch", "spül"),
    KITCHEN(ChoreColor.ORANGE, "küche", "kochen"),
    BED(ChoreColor.PURPLE, "bettwäsche", "bett", "matratze", "schlaf", "kissen"),
    LAUNDRY(ChoreColor.BLUE, "wäsche", "waschmaschine", "waschen", "tumbler"),
    IRONING(ChoreColor.PURPLE, "bügel"),
    TOWELS(ChoreColor.PINK, "handtuch", "handtücher", "kleider", "schrank"),
    SHOWER(ChoreColor.TEAL, "dusche"),
    BATH(ChoreColor.TEAL, "bad", "wc", "toilette", "lavabo"),
    DRAIN(ChoreColor.TEAL, "abfluss", "abflüsse", "rohr", "siphon"),
    AIR(ChoreColor.GREY, "filter", "dampfabzug", "lüft", "klima"),
    SMOKE_ALARM(ChoreColor.RED, "rauchmelder", "feuer"),
    VACUUM(ChoreColor.TEAL, "staubsaug", "saugen"),
    DUSTING(ChoreColor.YELLOW, "staub", "wischen", "boden", "putzen", "reinig"),
    PETS(ChoreColor.YELLOW, "katze", "hund", "tier", "futter", "aquarium"),
    SHOPPING(ChoreColor.GREEN, "einkauf", "einkaufen", "vorrat"),
    PAPERWORK(ChoreColor.BLUE, "steuer", "rechnung", "post", "brief", "versicherung"),
    REPAIR(ChoreColor.GREY, "reparier", "unterhalt", "werkzeug", "glühbirne", "lampe"),
    CAR(ChoreColor.BLUE, "auto", "velo", "fahrrad"),
    SOFA(ChoreColor.PURPLE, "sofa", "wohnzimmer", "teppich"),
    HOME(ChoreColor.BLUE),
    ;

    companion object {
        /** Symbol from a chore's name, [HOME] if nothing fits. */
        fun guess(name: String): ChoreSymbol {
            val text = name.lowercase()
            return entries.firstOrNull { symbol -> symbol.keywords.any { it in text } } ?: HOME
        }

        fun byKey(key: String?): ChoreSymbol? = entries.firstOrNull { it.name == key }
    }
}

/** The symbol and colour to show for a chore: chosen ones, or guessed from the name. */
data class ChoreLook(val symbol: ChoreSymbol, val color: ChoreColor) {
    companion object {
        fun of(name: String, symbolKey: String?, colorKey: String?): ChoreLook {
            val symbol = ChoreSymbol.byKey(symbolKey) ?: ChoreSymbol.guess(name)
            val color = ChoreColor.entries.firstOrNull { it.name == colorKey } ?: symbol.defaultColor
            return ChoreLook(symbol, color)
        }
    }
}
