package com.metrolist.subsonic

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Parse MPEG1 Layer III MP3 frame header to extract bitrate (bps) and sample rate (Hz).
 * Returns nulls if header not found.
 */
object Mp3HeaderParser {
    // Bitrate tables: [version][layer][index]; we only need common Layer III values.
    // indices 1..14 valid; 0,15 reserved.
    private val bitrateTableLayer3 = arrayOf(
        // MPEG2/2.5 Layer III
        intArrayOf(0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160),
        // MPEG1 Layer III
        intArrayOf(0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320)
    )
    // Sample rate table by version index (00=MPEG2.5, 10=MPEG2, 11=MPEG1)
    private val sampleRateTable = mapOf(
        0 to intArrayOf(11025, 12000, 8000, 0), // MPEG2.5
        2 to intArrayOf(22050, 24000, 16000, 0), // MPEG2
        3 to intArrayOf(44100, 48000, 32000, 0) // MPEG1
    )

    data class ParseResult(
        val bitrate: Int?,      // first frame bitrate (bps)
        val sampleRate: Int?,   // Hz
        val avgBitrate: Int?,   // computed average bitrate (bps) if VBR header + content length
        val vbr: Boolean
    )

    suspend fun fetchAndParse(streamUrl: String, client: SubsonicClient): ParseResult = withContext(Dispatchers.IO) {
        val contentLength = headContentLength(streamUrl, client)
        val attemptRanges = listOf("bytes=0-131071", null)
        for (range in attemptRanges) {
            try {
                val response: HttpResponse = client.httpClient.get(streamUrl) { if (range != null) header("Range", range) }
                val bytes = response.readRawBytes()
                val parsed = parse(bytes, contentLength)
                if (parsed.bitrate != null || parsed.sampleRate != null) return@withContext parsed
            } catch (_: Exception) { }
        }
        ParseResult(null, null, null, vbr = false)
    }

    fun parse(bytes: ByteArray, contentLength: Long?): ParseResult {
        var offset = 0
        if (bytes.size >= 10 && bytes[0] == 'I'.code.toByte() && bytes[1] == 'D'.code.toByte() && bytes[2] == '3'.code.toByte()) {
            val sizeBytes = bytes.sliceArray(6..9)
            val tagSize = (sizeBytes[0].toInt() and 0x7F shl 21) or
                (sizeBytes[1].toInt() and 0x7F shl 14) or
                (sizeBytes[2].toInt() and 0x7F shl 7) or
                (sizeBytes[3].toInt() and 0x7F)
            offset = 10 + tagSize
            if (offset >= bytes.size) return ParseResult(null, null, null, false)
        }
        var frameHeaderIndex = -1
        var versionBitsFound: Int? = null
        var sampleRate: Int? = null
        var firstFrameBitrate: Int? = null
        for (i in offset until bytes.size - 4) {
            val b1 = bytes[i].toInt() and 0xFF
            val b2 = bytes[i + 1].toInt() and 0xFF
            if (b1 == 0xFF && (b2 and 0xE0) == 0xE0) {
                val versionBits = (b2 shr 3) and 0x03 // 00 MPEG2.5, 10 MPEG2, 11 MPEG1
                val layerBits = (b2 shr 1) and 0x03 // 01 Layer III
                val protectionBit = b2 and 0x01
                if (layerBits != 1) continue // only Layer III
                val b3 = bytes[i + 2].toInt() and 0xFF
                val bitrateIndex = (b3 shr 4) and 0x0F
                val sampleRateIndex = (b3 shr 2) and 0x03
                val mpegKey = when (versionBits) { 3 -> 3; 2 -> 2; 0 -> 0; else -> null }
                val sampleRateArr = mpegKey?.let { sampleRateTable[it] }
                sampleRate = sampleRateArr?.getOrNull(sampleRateIndex)
                val versionGroup = when (versionBits) { 3 -> 1; 2,0 -> 0; else -> null } // index into bitrateTableLayer3
                firstFrameBitrate = if (versionGroup != null && bitrateIndex in 1..14) bitrateTableLayer3[versionGroup][bitrateIndex] * 1000 else null
                frameHeaderIndex = i
                versionBitsFound = versionBits
                break
            }
        }
        if (frameHeaderIndex == -1) return ParseResult(null, null, null, false)

        // Attempt VBR header detection (Xing/Info/VBRI)
        var vbr = false
        var avgBitrate: Int? = null
        if (sampleRate != null && versionBitsFound != null) {
            val isMpeg1 = versionBitsFound == 3
            val samplesPerFrame = if (isMpeg1) 1152 else 576
            val xingOffsets = if (isMpeg1) intArrayOf(32, 36, 21, 13) else intArrayOf(17, 21, 13, 9)
            // Try possible side info offsets (stereo/mono differences). Check for 'Xing' or 'Info'
            var framesCount: Int? = null
            for (off in xingOffsets) {
                val p = frameHeaderIndex + off
                if (p + 16 < bytes.size) {
                    val tag = String(bytes, p, 4)
                    if (tag == "Xing" || tag == "Info") {
                        vbr = tag == "Xing"
                        val flags = bytes[p + 4].toInt() shl 24 or (bytes[p + 5].toInt() and 0xFF shl 16) or (bytes[p + 6].toInt() and 0xFF shl 8) or (bytes[p + 7].toInt() and 0xFF)
                        // frames flag bit 0x0001
                        if ((flags and 0x0001) != 0) {
                            framesCount = (bytes[p + 8].toInt() and 0xFF shl 24) or (bytes[p + 9].toInt() and 0xFF shl 16) or (bytes[p + 10].toInt() and 0xFF shl 8) or (bytes[p + 11].toInt() and 0xFF)
                        }
                        break
                    }
                }
            }
            // VBRI header at fixed offset 32 after frame header
            if (!vbr) {
                val vbriPos = frameHeaderIndex + 32
                if (vbriPos + 26 < bytes.size && String(bytes, vbriPos, 4) == "VBRI") {
                    vbr = true
                    // bytes 14..17 after 'VBRI' are frame count
                    val framesCountPos = vbriPos + 14
                    framesCount = (bytes[framesCountPos].toInt() and 0xFF shl 24) or (bytes[framesCountPos + 1].toInt() and 0xFF shl 16) or (bytes[framesCountPos + 2].toInt() and 0xFF shl 8) or (bytes[framesCountPos + 3].toInt() and 0xFF)
                }
            }
            if (framesCount != null && framesCount > 0 && contentLength != null && contentLength > 0) {
                val totalSamples = framesCount.toLong() * samplesPerFrame
                val durationSec = totalSamples / sampleRate.toLong()
                if (durationSec > 0) {
                    avgBitrate = ((contentLength * 8) / durationSec).toInt()
                }
            }
        }
        return ParseResult(firstFrameBitrate, sampleRate, avgBitrate, vbr)
    }

    suspend fun headContentLength(streamUrl: String, client: SubsonicClient): Long? = withContext(Dispatchers.IO) {
        try {
            val resp = client.httpClient.request(streamUrl) { method = HttpMethod.Head }
            resp.headers[HttpHeaders.ContentLength]?.toLongOrNull()
        } catch (_: Exception) { null }
    }
}
