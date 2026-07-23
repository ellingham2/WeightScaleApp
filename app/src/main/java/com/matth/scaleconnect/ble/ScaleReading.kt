package com.matth.scaleconnect.ble

/**
 * A single decoded "FF A5 b2 b3 b4 b5 channel checksum" frame from the scale.
 *
 * Reverse-engineered from live captures and cross-checked against the scale's own
 * on-device display (which cycles Weight -> Fat% -> Hydration% -> Muscle% -> Bone% ->
 * Calories per the product manual). While idle the scale streams
 * "FF A5 00 00 00 00 A0 A0" (channel 0xA0). Once a stable weigh-in is reached, it
 * cycles through a fixed set of channels once per ~90ms, repeating:
 *
 *   0xAA: impedance (ohms, pairHi) + weight (tenths of a lb, pairLo)
 *   0xB0: body fat % (tenths, pairHi) + hydration % (tenths, pairLo)
 *   0xC0: muscle % (tenths, pairHi) + bone % (tenths, single byte b4)
 *   0xD0: BMR in kcal (pairHi) - second half unidentified
 *   0xE0/0xE1: echo of the synced profile (age/height) - not a measurement
 *
 * The scale doesn't transmit AMR at all - that's a client-side calculation
 * (BMR x activity factor) in the original app, not something read from the device.
 */
data class ScaleFrame(val channel: Int, val b2: Int, val b3: Int, val b4: Int, val b5: Int) {
    val pairHi: Int get() = (b2 shl 8) or b3
    val pairLo: Int get() = (b4 shl 8) or b5
}

object ScaleFrameParser {
    const val CHANNEL_IDLE = 0xA0
    const val CHANNEL_WEIGHT_IMPEDANCE = 0xAA
    const val CHANNEL_FAT_HYDRATION = 0xB0
    const val CHANNEL_MUSCLE_BONE = 0xC0
    const val CHANNEL_BMR = 0xD0
    const val CHANNEL_AGE_ECHO = 0xE0
    const val CHANNEL_HEIGHT_ECHO = 0xE1

    fun parse(value: ByteArray): ScaleFrame? {
        if (value.size < 8) return null
        if (value[0] != 0xFF.toByte() || value[1] != 0xA5.toByte()) return null
        val b2 = value[2].toInt() and 0xFF
        val b3 = value[3].toInt() and 0xFF
        val b4 = value[4].toInt() and 0xFF
        val b5 = value[5].toInt() and 0xFF
        val channel = value[6].toInt() and 0xFF
        val checksum = value[7].toInt() and 0xFF
        val computed = (b2 + b3 + b4 + b5 + channel) and 0xFF
        if (computed != checksum) return null
        return ScaleFrame(channel, b2, b3, b4, b5)
    }
}

data class WeighInResult(
    val weightLb: Double? = null,
    val impedanceRaw: Int? = null,
    val bodyFatPercent: Double? = null,
    val hydrationPercent: Double? = null,
    val musclePercent: Double? = null,
    val bonePercent: Double? = null,
    val bmrKcal: Int? = null
) {
    fun merge(frame: ScaleFrame): WeighInResult = when (frame.channel) {
        ScaleFrameParser.CHANNEL_WEIGHT_IMPEDANCE -> copy(
            impedanceRaw = frame.pairHi,
            weightLb = frame.pairLo / 10.0
        )
        ScaleFrameParser.CHANNEL_FAT_HYDRATION -> copy(
            bodyFatPercent = frame.pairHi / 10.0,
            hydrationPercent = frame.pairLo / 10.0
        )
        ScaleFrameParser.CHANNEL_MUSCLE_BONE -> copy(
            musclePercent = frame.pairHi / 10.0,
            bonePercent = frame.b4 / 10.0
        )
        ScaleFrameParser.CHANNEL_BMR -> copy(bmrKcal = frame.pairHi)
        else -> this
    }
}
