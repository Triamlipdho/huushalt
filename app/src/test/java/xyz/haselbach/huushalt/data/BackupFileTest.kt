package xyz.haselbach.huushalt.data

import java.io.ByteArrayInputStream
import java.io.InputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Import files are read with a size limit (F9.3). */
class BackupFileTest {
    @Test
    fun `reads files up to the limit`() {
        val text = "ä".repeat(50) // 100 bytes in UTF-8
        assertEquals(text, Backup.readText(ByteArrayInputStream(text.toByteArray()), maxBytes = 100))
    }

    @Test
    fun `refuses files above the limit`() {
        assertThrows(Backup.TooLargeException::class.java) {
            Backup.readText(ByteArrayInputStream(ByteArray(101)), maxBytes = 100)
        }
    }

    @Test
    fun `stops reading an endless stream at the limit`() {
        var read = 0L
        val endless = object : InputStream() {
            override fun read(): Int = 'x'.code.also { read++ }
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                b.fill('x'.code.toByte(), off, off + len)
                read += len
                return len
            }
        }
        assertThrows(Backup.TooLargeException::class.java) { Backup.readText(endless) }
        assertTrue("read $read bytes", read <= Backup.MAX_FILE_BYTES + 64 * 1024)
    }
}
