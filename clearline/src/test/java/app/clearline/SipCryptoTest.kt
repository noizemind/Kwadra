package app.clearline

import app.clearline.sip.SdpParse
import app.clearline.sip.SipCrypto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SipCryptoTest {
    @Test
    fun digestMatchesRfcExampleShape() {
        // Known MD5 of empty string for sanity on helper
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", SipCrypto.md5Hex(""))
        val resp = SipCrypto.digestResponse(
            username = "alice",
            password = "secret",
            realm = "atlanta.com",
            nonce = "dcd98b7102dd2f0e8b11d0f600bfb0c093",
            method = "REGISTER",
            uri = "sip:atlanta.com",
        )
        assertEquals(32, resp.length)
    }

    @Test
    fun parsesSdpMedia() {
        val sdp = """
            v=0
            o=- 0 0 IN IP4 10.0.0.5
            s=Clearline
            c=IN IP4 10.0.0.5
            t=0 0
            m=audio 4004 RTP/AVP 0
        """.trimIndent().replace("\n", "\r\n")
        val media = SdpParse.media(sdp)
        assertNotNull(media)
        assertEquals("10.0.0.5", media!!.host)
        assertEquals(4004, media.port)
    }
}
