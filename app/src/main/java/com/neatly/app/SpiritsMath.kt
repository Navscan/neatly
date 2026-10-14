package com.neatly.app

/**
 * Spirits dilution math engine.
 *
 * Uses a third-degree 2D polynomial fit to empirical ethanol-water
 * contraction data. The underlying dataset covers source ABV from 39% to
 * 79% and target ABV from 34% to 51%. The polynomial reproduces the source
 * table to within 0.2 L per 100 L, and extrapolates to 96.4% ABV (practical
 * distillation ceiling) with physically accurate contraction behaviour.
 *
 * All internal math runs in ABV (% v/v) and litres, referenced to 20 C.
 * Unit toggles in the UI translate on the way in and out; they never
 * change the coefficients here.
 *
 * This is intentionally a self-contained object: no networking, no Android
 * dependencies. It can be tested on the JVM.
 */
object SpiritsMath {

    // Polynomial coefficients derived from the empirical dataset.
    private const val C0 = 61.223438
    private const val C1 = 6.696005
    private const val C2 = -10.971703
    private const val C3 = 0.008028
    private const val C4 = -0.169239
    private const val C5 = 0.260478
    private const val C6 = -0.000022
    private const val C7 = -0.000054
    private const val C8 = 0.001384
    private const val C9 = -0.002073

    // Litres per US gallon.
    const val LITERS_PER_US_GALLON = 3.785411784

    /**
     * Liters of water to add per 100 L of [sourceAbv] to reach [targetAbv],
     * accounting for ethanol-water contraction.
     */
    fun waterPer100L(sourceAbv: Double, targetAbv: Double): Double {
        val s = sourceAbv
        val t = targetAbv
        return C0 +
            C1 * s +
            C2 * t +
            C3 * s * s +
            C4 * s * t +
            C5 * t * t +
            C6 * s * s * s +
            C7 * s * s * t +
            C8 * s * t * t +
            C9 * t * t * t
    }

    data class Result(
        val waterToAddLiters: Double,
        val finalVolumeLiters: Double,
        val pureAlcoholLiters: Double,
        val contractionLiters: Double
    )

    /**
     * Full dilution calculation for a user-supplied volume, always in
     * litres and ABV at 20 C.
     *
     * @throws IllegalArgumentException when target >= source, or either is <= 0.
     */
    fun calculate(volumeLiters: Double, sourceAbv: Double, targetAbv: Double): Result {
        require(targetAbv < sourceAbv) { "Target strength must be lower than source strength." }
        require(targetAbv > 0.0 && sourceAbv > 0.0) { "Strength values must be greater than zero." }
        require(volumeLiters > 0.0) { "Volume must be greater than zero." }

        val waterPer100 = waterPer100L(sourceAbv, targetAbv)
        val waterToAdd = (volumeLiters / 100.0) * waterPer100
        val pureAlcohol = volumeLiters * (sourceAbv / 100.0)
        val finalVolume = pureAlcohol / (targetAbv / 100.0)
        val apparentVolume = volumeLiters + waterToAdd
        val contraction = apparentVolume - finalVolume

        return Result(
            waterToAddLiters = waterToAdd,
            finalVolumeLiters = finalVolume,
            pureAlcoholLiters = pureAlcohol,
            contractionLiters = contraction
        )
    }

    // ---------------------------------------------------------------
    // Unit conversions.
    // The math engine always runs in ABV and litres. These helpers
    // translate user-facing values on the way in and out.
    // ---------------------------------------------------------------

    /**
     * US Proof = 2 * ABV, at reference temperature 20 C. (TTB nominally
     * specifies 60 F; we stay at 20 C for consistency with the polynomial
     * and revisit temperature correction in a later version.)
     */
    fun abvToUsProof(abv: Double): Double = abv * 2.0

    /** Inverse of [abvToUsProof]. */
    fun usProofToAbv(proof: Double): Double = proof / 2.0

    /** Litres to US gallons. */
    fun litersToUsGallons(liters: Double): Double = liters / LITERS_PER_US_GALLON

    /** US gallons to litres. */
    fun usGallonsToLiters(gallons: Double): Double = gallons * LITERS_PER_US_GALLON
}
