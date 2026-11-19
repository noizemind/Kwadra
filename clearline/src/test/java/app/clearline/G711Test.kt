package app.clearline

import app.clearline.audio.G711
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class G711Test {
    @Test
    fun roundTripNearSilence() {
        val pcm = shortArrayOf(0, 40, -40, 120, -120)
        val ulaw = ByteArray(pcm.size)
        val out = ShortArray(pcm.size)
        G711.encode(pcm, ulaw)
        G711.decode(ulaw, out)
        for (i in pcm.indices) {
            assertTrue("sample $i drifted too far", abs(pcm[i] - out[i]) < 200)
        }
    }

    @Test
    fun knownUlawByte() {
        // µ-law of 0 is commonly 0xFF
        assertEquals(0xff, G711.linearToUlaw(0))
    }
}
