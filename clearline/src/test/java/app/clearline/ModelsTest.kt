package app.clearline

import app.clearline.data.CallDirection
import app.clearline.data.CallOutcome
import app.clearline.data.CallRecord
import app.clearline.data.Contact
import app.clearline.data.SipAccount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelsTest {
    @Test
    fun accountDefaultsToDemo() {
        val a = SipAccount()
        assertTrue(a.demoMode)
        assertEquals(5060, a.port)
        assertEquals("UDP", a.transport)
    }

    @Test
    fun contactKeepsUri() {
        val c = Contact(name = "Desk", uri = "1001@office.local")
        assertEquals("1001@office.local", c.uri)
    }

    @Test
    fun callRecordDuration() {
        val r = CallRecord(
            peer = "echo@demo",
            displayName = "Echo",
            direction = CallDirection.OUTGOING,
            outcome = CallOutcome.ANSWERED,
            durationSec = 42,
        )
        assertEquals(42, r.durationSec)
    }
}
