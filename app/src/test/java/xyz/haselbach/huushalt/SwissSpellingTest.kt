package xyz.haselbach.huushalt

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** UI text uses Swiss spelling: ss instead of ß (F9.5). */
class SwissSpellingTest {
    @Test
    fun `no sharp s in string resources`() {
        val offending = File("src/main/res").walkTopDown()
            .filter { it.isFile && it.extension == "xml" }
            .flatMap { file -> file.readLines().mapIndexedNotNull { i, line -> if ('ß' in line) "${file.path}:${i + 1}" else null } }
            .toList()
        assertTrue("Use ss instead of ß:\n" + offending.joinToString("\n"), offending.isEmpty())
    }
}
