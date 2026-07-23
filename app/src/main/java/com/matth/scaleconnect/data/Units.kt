package com.matth.scaleconnect.data

enum class WeightUnit { LB, KG }
enum class HeightUnit { CM, FT_IN }

/** All weights are stored internally in lb (matching the scale's decoded protocol
 * values) and all heights in cm - these convert only at the display/input boundary. */
fun Double.lbToKg(): Double = this * 0.45359237
fun Double.kgToLb(): Double = this / 0.45359237

fun Int.cmToFeetInches(): Pair<Int, Int> {
    val totalInches = (this / 2.54).let { Math.round(it).toInt() }
    return totalInches / 12 to totalInches % 12
}

fun feetInchesToCm(feet: Int, inches: Int): Int =
    Math.round((feet * 12 + inches) * 2.54).toInt()

fun formatWeight(lb: Double, unit: WeightUnit): String = when (unit) {
    WeightUnit.LB -> "%.1f lb".format(lb)
    WeightUnit.KG -> "%.1f kg".format(lb.lbToKg())
}
