package com.matth.scaleconnect.ble

import java.util.Calendar
import java.util.UUID

/**
 * Protocol reverse-engineered from the Senssun Body Monitor app (com.senssun.bodymonitor)
 * by decompiling its APK and cross-referencing against live BLE captures of the actual
 * scale. Every command is a byte array: 0xA5, function code, params..., checksum
 * (checksum = low byte of the sum of every byte after the 0xA5 header, including itself
 * being excluded).
 */
object ScaleProtocol {
    // Primary GATT service/characteristics (present on most scales in this OEM family).
    val SERVICE_UUID_PRIMARY: UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")
    val CHAR_NOTIFY_PRIMARY: UUID = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb")
    val CHAR_WRITE_PRIMARY: UUID = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb")
    val CHAR_NOTIFY_SECONDARY: UUID = UUID.fromString("0000fff3-0000-1000-8000-00805f9b34fb")

    // Fallback service seen on some units where the primary service isn't exposed.
    val SERVICE_UUID_FALLBACK: UUID = UUID.fromString("0000ffb0-0000-1000-8000-00805f9b34fb")
    val CHAR_FALLBACK: UUID = UUID.fromString("0000ffb2-0000-1000-8000-00805f9b34fb")

    val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private const val FUNCODE_DATE_SYNC = 0x30
    private const val FUNCODE_TIME_SYNC = 0x31
    private const val FUNCODE_PROFILE_SYNC = 0x10

    fun buildCommand(funCode: Int, params: IntArray = IntArray(0)): ByteArray {
        val body = mutableListOf(funCode)
        body.addAll(params.toList())
        val checksum = body.sum() and 0xFF
        val full = mutableListOf(0xA5)
        full.addAll(body)
        full.add(checksum)
        return full.map { it.toByte() }.toByteArray()
    }

    fun timeSyncCommand(calendar: Calendar = Calendar.getInstance()): ByteArray {
        return buildCommand(
            FUNCODE_TIME_SYNC,
            intArrayOf(
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND),
                0, 0, 0
            )
        )
    }

    fun dateSyncCommand(calendar: Calendar = Calendar.getInstance()): ByteArray {
        val yy = calendar.get(Calendar.YEAR) % 100
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        return buildCommand(
            FUNCODE_DATE_SYNC,
            intArrayOf(yy, (dayOfYear shr 8) and 0xFF, dayOfYear and 0xFF, 0, 0, 0)
        )
    }

    /**
     * userIndex: 1-based profile slot on the scale.
     * isMale: sex bit (male -> 8, female -> 0 in the high nibble). Confirmed against the
     * decompiled app (UserInfo.java: sex==1 maps to "img_female.png", not male as the
     * variable naming implies) and empirically: the original 0/8 assignment was inverted.
     * heightCm: height in whole centimeters.
     */
    fun profileSyncCommand(userIndex: Int, isMale: Boolean, age: Int, heightCm: Int): ByteArray {
        val sexBit = if (isMale) 8 else 0
        val userIndexSexByte = (sexBit shl 4) or (userIndex and 0x0F)
        return buildCommand(FUNCODE_PROFILE_SYNC, intArrayOf(userIndexSexByte, age, heightCm, 0, 0))
    }
}
