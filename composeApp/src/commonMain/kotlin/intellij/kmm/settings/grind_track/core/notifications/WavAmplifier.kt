package intellij.kmm.settings.grind_track.core.notifications

import kotlin.math.abs
import kotlin.math.pow

object WavAmplifier {
    private const val TARGET_PEAK_LINEAR_F = 0.8912509f // -1 dBFS

    /**
     * Peak-normalizes the WAV to ~-1 dBFS and applies [additionalGainDb] on top of that,
     * hard-clipping at full scale. Returns the input unchanged on any parse failure or
     * unsupported format.
     *
     * Supported formats: PCM 8 (unsigned), 16/24/32-bit signed LE; IEEE float 32-bit.
     */
    fun amplify(wavBytes: ByteArray, additionalGainDb: Float): ByteArray = runCatching {
        val parsed = parse(wavBytes) ?: return@runCatching wavBytes
        val out = wavBytes.copyOf()
        applyGain(out, parsed, additionalGainDb) ?: wavBytes
    }.getOrElse { wavBytes }

    private data class Fmt(
        val audioFormat: Int,
        val numChannels: Int,
        val bitsPerSample: Int,
    )

    private data class Parsed(
        val dataOffset: Int,
        val dataLength: Int,
        val fmt: Fmt,
    )

    private fun parse(bytes: ByteArray): Parsed? {
        if (bytes.size < 44) return null
        if (readAscii(bytes, 0) != "RIFF") return null
        if (readAscii(bytes, 8) != "WAVE") return null

        var offset = 12
        var fmt: Fmt? = null

        while (offset + 8 <= bytes.size) {
            val chunkId = readAscii(bytes, offset)
            val chunkSize = readU32LE(bytes, offset + 4)
            val payloadOffset = offset + 8

            when (chunkId) {
                "fmt " -> {
                    if (payloadOffset + chunkSize > bytes.size) return null
                    fmt = readFmtChunk(bytes, payloadOffset, chunkSize) ?: return null
                }
                "data" -> {
                    val resolvedFmt = fmt ?: return null
                    val dataLen = minOf(chunkSize, bytes.size - payloadOffset)
                    return Parsed(payloadOffset, dataLen, resolvedFmt)
                }
            }
            offset = payloadOffset + chunkSize + (chunkSize and 1)
        }
        return null
    }

    private fun readFmtChunk(bytes: ByteArray, offset: Int, size: Int): Fmt? {
        if (size < 16) return null
        var format = readU16LE(bytes, offset)
        val channels = readU16LE(bytes, offset + 2)
        val bitsPerSample = readU16LE(bytes, offset + 14)
        if (format == 0xFFFE && size >= 40) {
            format = readU16LE(bytes, offset + 24)
        }
        val supported = when (format) {
            1 -> bitsPerSample in setOf(8, 16, 24, 32)
            3 -> bitsPerSample == 32
            else -> false
        }
        if (!supported || channels <= 0) return null
        return Fmt(format, channels, bitsPerSample)
    }

    private fun applyGain(out: ByteArray, p: Parsed, additionalGainDb: Float): ByteArray? {
        val extraGain = 10.0.pow(additionalGainDb / 20.0).toFloat()
        val peak = computePeakLinear(out, p)
        if (peak <= 0f) return null
        val totalGain = (TARGET_PEAK_LINEAR_F / peak) * extraGain
        if (totalGain in 0.999f..1.001f) return out
        scaleSamples(out, p, totalGain)
        return out
    }

    private fun computePeakLinear(bytes: ByteArray, p: Parsed): Float {
        val fmt = p.fmt
        val end = p.dataOffset + p.dataLength
        var peak = 0f
        var i = p.dataOffset
        when (fmt.audioFormat) {
            1 -> when (fmt.bitsPerSample) {
                8 -> {
                    while (i < end) {
                        val s = (bytes[i].toInt() and 0xFF) - 128
                        val v = abs(s) / 128f
                        if (v > peak) peak = v
                        i++
                    }
                }
                16 -> {
                    while (i + 1 < end) {
                        val raw = (bytes[i].toInt() and 0xFF) or
                            ((bytes[i + 1].toInt() and 0xFF) shl 8)
                        val signed = raw.toShort().toInt()
                        val v = abs(signed) / 32768f
                        if (v > peak) peak = v
                        i += 2
                    }
                }
                24 -> {
                    while (i + 2 < end) {
                        val b0 = bytes[i].toInt() and 0xFF
                        val b1 = bytes[i + 1].toInt() and 0xFF
                        val b2 = bytes[i + 2].toInt() and 0xFF
                        var s = b0 or (b1 shl 8) or (b2 shl 16)
                        if (s and 0x800000 != 0) s = s or -0x1000000
                        val v = abs(s) / 8388608f
                        if (v > peak) peak = v
                        i += 3
                    }
                }
                32 -> {
                    while (i + 3 < end) {
                        val s = readS32LE(bytes, i)
                        val v = (abs(s.toLong()) / 2147483648.0).toFloat()
                        if (v > peak) peak = v
                        i += 4
                    }
                }
            }
            3 -> {
                while (i + 3 < end) {
                    val bits = readS32LE(bytes, i)
                    val f = Float.fromBits(bits)
                    val v = abs(f)
                    if (v > peak) peak = v
                    i += 4
                }
            }
        }
        return peak
    }

    private fun scaleSamples(bytes: ByteArray, p: Parsed, gain: Float) {
        val fmt = p.fmt
        val end = p.dataOffset + p.dataLength
        var i = p.dataOffset
        when (fmt.audioFormat) {
            1 -> when (fmt.bitsPerSample) {
                8 -> {
                    while (i < end) {
                        val s = (bytes[i].toInt() and 0xFF) - 128
                        val clipped = softClip(s * gain / 128f)
                        var scaled = (clipped * 128f).toInt()
                        if (scaled > 127) scaled = 127 else if (scaled < -128) scaled = -128
                        bytes[i] = ((scaled + 128) and 0xFF).toByte()
                        i++
                    }
                }
                16 -> {
                    while (i + 1 < end) {
                        val raw = (bytes[i].toInt() and 0xFF) or
                            ((bytes[i + 1].toInt() and 0xFF) shl 8)
                        val signed = raw.toShort().toInt()
                        val clipped = softClip(signed * gain / 32768f)
                        var scaled = (clipped * 32768f).toInt()
                        if (scaled > 32767) scaled = 32767
                        else if (scaled < -32768) scaled = -32768
                        bytes[i] = (scaled and 0xFF).toByte()
                        bytes[i + 1] = ((scaled ushr 8) and 0xFF).toByte()
                        i += 2
                    }
                }
                24 -> {
                    while (i + 2 < end) {
                        val b0 = bytes[i].toInt() and 0xFF
                        val b1 = bytes[i + 1].toInt() and 0xFF
                        val b2 = bytes[i + 2].toInt() and 0xFF
                        var s = b0 or (b1 shl 8) or (b2 shl 16)
                        if (s and 0x800000 != 0) s = s or -0x1000000
                        val clipped = softClip(s * gain / 8388608f)
                        var scaled = (clipped * 8388608f).toInt()
                        if (scaled > 8388607) scaled = 8388607
                        else if (scaled < -8388608) scaled = -8388608
                        bytes[i] = (scaled and 0xFF).toByte()
                        bytes[i + 1] = ((scaled ushr 8) and 0xFF).toByte()
                        bytes[i + 2] = ((scaled ushr 16) and 0xFF).toByte()
                        i += 3
                    }
                }
                32 -> {
                    while (i + 3 < end) {
                        val s = readS32LE(bytes, i).toLong()
                        val clipped = softClip((s * gain.toDouble() / 2147483648.0).toFloat())
                        var scaled = (clipped.toDouble() * 2147483648.0).toLong()
                        if (scaled > Int.MAX_VALUE.toLong()) scaled = Int.MAX_VALUE.toLong()
                        else if (scaled < Int.MIN_VALUE.toLong()) scaled = Int.MIN_VALUE.toLong()
                        writeS32LE(bytes, i, scaled.toInt())
                        i += 4
                    }
                }
            }
            3 -> {
                while (i + 3 < end) {
                    val bits = readS32LE(bytes, i)
                    val clipped = softClip(Float.fromBits(bits) * gain)
                    writeS32LE(bytes, i, clipped.toRawBits())
                    i += 4
                }
            }
        }
    }

    // Cubic soft clipper: y = x - (4/27)x³ on [-1.5, 1.5], saturating to ±1 beyond.
    // Linear at small amplitudes (f'(0)=1); zero slope at ±1.5 for a smooth ceiling.
    private fun softClip(x: Float): Float {
        if (x >= 1.5f) return 1f
        if (x <= -1.5f) return -1f
        return x - (4f / 27f) * x * x * x
    }

    private fun readAscii(b: ByteArray, offset: Int): String {
        val sb = StringBuilder(4)
        for (n in 0 until 4) sb.append((b[offset + n].toInt() and 0xFF).toChar())
        return sb.toString()
    }

    private fun readU16LE(b: ByteArray, o: Int): Int =
        (b[o].toInt() and 0xFF) or ((b[o + 1].toInt() and 0xFF) shl 8)

    private fun readU32LE(b: ByteArray, o: Int): Int =
        (b[o].toInt() and 0xFF) or
            ((b[o + 1].toInt() and 0xFF) shl 8) or
            ((b[o + 2].toInt() and 0xFF) shl 16) or
            ((b[o + 3].toInt() and 0xFF) shl 24)

    private fun readS32LE(b: ByteArray, o: Int): Int =
        (b[o].toInt() and 0xFF) or
            ((b[o + 1].toInt() and 0xFF) shl 8) or
            ((b[o + 2].toInt() and 0xFF) shl 16) or
            ((b[o + 3].toInt() and 0xFF) shl 24)

    private fun writeS32LE(b: ByteArray, o: Int, v: Int) {
        b[o] = (v and 0xFF).toByte()
        b[o + 1] = ((v ushr 8) and 0xFF).toByte()
        b[o + 2] = ((v ushr 16) and 0xFF).toByte()
        b[o + 3] = ((v ushr 24) and 0xFF).toByte()
    }
}
