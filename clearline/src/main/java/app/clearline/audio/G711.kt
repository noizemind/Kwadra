package app.clearline.audio

/**
 * G.711 µ-law (PCMU) encode/decode — payload type 0 in Clearline SDP.
 */
object G711 {
    private val encodeTable = ShortArray(256)
    private val decodeTable = ShortArray(256)

    init {
        for (i in 0 until 256) {
            decodeTable[i] = ulawToLinear(i).toShort()
        }
    }

    fun encode(pcm: ShortArray, out: ByteArray, samples: Int = pcm.size) {
        require(out.size >= samples)
        for (i in 0 until samples) {
            out[i] = linearToUlaw(pcm[i].toInt()).toByte()
        }
    }

    fun decode(ulaw: ByteArray, out: ShortArray, samples: Int = ulaw.size) {
        require(out.size >= samples)
        for (i in 0 until samples) {
            out[i] = decodeTable[ulaw[i].toInt() and 0xff]
        }
    }

    fun linearToUlaw(sample: Int): Int {
        var pcm = sample
        val sign = if (pcm < 0) {
            pcm = -pcm
            0x80
        } else {
            0x00
        }
        if (pcm > 32635) pcm = 32635
        pcm += 0x84
        var exp = 7
        var mask = 0x4000
        while (exp > 0 && (pcm and mask) == 0) {
            exp--
            mask = mask shr 1
        }
        val mantissa = (pcm shr (exp + 3)) and 0x0f
        return (sign or (exp shl 4) or mantissa).inv() and 0xff
    }

    fun ulawToLinear(uVal: Int): Int {
        val u = uVal.inv() and 0xff
        val sign = u and 0x80
        val exp = (u shr 4) and 0x07
        val mantissa = u and 0x0f
        var sample = ((mantissa shl 3) + 0x84) shl exp
        sample -= 0x84
        return if (sign != 0) -sample else sample
    }
}
