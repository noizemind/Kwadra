package app.clearline.sip

import java.security.MessageDigest

object SipCrypto {
    fun md5Hex(s: String): String {
        val dig = MessageDigest.getInstance("MD5").digest(s.toByteArray())
        return dig.joinToString("") { "%02x".format(it) }
    }

    fun digestResponse(
        username: String,
        password: String,
        realm: String,
        nonce: String,
        method: String,
        uri: String,
    ): String {
        val ha1 = md5Hex("$username:$realm:$password")
        val ha2 = md5Hex("$method:$uri")
        return md5Hex("$ha1:$nonce:$ha2")
    }
}

data class SdpMedia(
    val host: String,
    val port: Int,
)

object SdpParse {
    fun media(sdpOrMessage: String): SdpMedia? {
        val body = if (sdpOrMessage.contains("\r\n\r\n")) {
            sdpOrMessage.substringAfter("\r\n\r\n")
        } else {
            sdpOrMessage
        }
        val host = Regex("""c=IN IP4 ([^\s]+)""").find(body)?.groupValues?.getOrNull(1) ?: return null
        val port = Regex("""m=audio (\d+)""").find(body)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return null
        return SdpMedia(host, port)
    }
}
