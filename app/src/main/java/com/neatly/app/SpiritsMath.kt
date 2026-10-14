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
 */
object SpiritsMath {

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

    const val LITERS_PER_US_GALLON = 3.785411784

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
     * Volume-mode dilution: dilute with water only, using the empirical
     * contraction polynomial.
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

    /**
     * On Hand mode: dilute source spirit with either water
     * (diluteWithAbv = 0) or another spirit at diluteWithAbv.
     *
     * When diluteWithAbv = 0, uses the same polynomial as [calculate].
     * When diluteWithAbv > 0, uses Pearson's square with a contraction
     * correction derived from the polynomial's water-dilution behaviour.
     */
    fun calculateOnHand(
        sourceLiters: Double,
        sourceAbv: Double,
        targetAbv: Double,
        diluteWithAbv: Double
    ): Result {
        require(targetAbv < sourceAbv) { "Target strength must be lower than source strength." }
        require(targetAbv > 0.0 && sourceAbv > 0.0) { "Strength values must be greater than zero." }
        require(sourceLiters > 0.0) { "Volume must be greater than zero." }
        require(diluteWithAbv >= 0.0) { "Dilution strength cannot be negative." }
        require(diluteWithAbv < targetAbv) { "Dilution strength must be below target strength." }

        // Water dilution — identical to Volume mode.
        if (diluteWithAbv <= 0.0) {
            return calculate(sourceLiters, sourceAbv, targetAbv)
        }

        // Spirit dilution. First, estimate the contraction using the
        // water-only case as a proxy, scaled by how "water-like" the
        // diluting spirit is.
        val waterOnlyWater = (sourceLiters / 100.0) * waterPer100L(sourceAbv, targetAbv)
        val waterOnlyPure = sourceLiters * (sourceAbv / 100.0)
        val waterOnlyFinal = waterOnlyPure / (targetAbv / 100.0)
        val waterOnlyContraction = sourceLiters + waterOnlyWater - waterOnlyFinal

        // A diluting spirit at C% ABV has (1 - C/T) as its water-equivalent
        // fraction relative to the target.
        val diluteWaterFraction = 1.0 - (diluteWithAbv / targetAbv)
        val estimatedContraction = waterOnlyContraction * diluteWaterFraction

        // Solve for W using the mass balance with the estimated contraction:
        // V_s + W - contraction = (V_s * S_s + W * C) / T
        // => W = [V_s * (S_s - T) + contraction * T] / (T - C)
        val diluteVolume = (sourceLiters * (sourceAbv - targetAbv) + estimatedContraction * targetAbv) /
                (targetAbv - diluteWithAbv)

        val pureAlcohol = sourceLiters * (sourceAbv / 100.0) + diluteVolume * (diluteWithAbv / 100.0)
        val finalVolume = pureAlcohol / (targetAbv / 100.0)
        val apparentTotal = sourceLiters + diluteVolume
        val contraction = apparentTotal - finalVolume

        return Result(
            waterToAddLiters = diluteVolume,
            finalVolumeLiters = finalVolume,
            pureAlcoholLiters = pureAlcohol,
            contractionLiters = contraction
        )
    }

    // ---------------------------------------------------------------
    // Unit conversions.
    // ---------------------------------------------------------------

    fun abvToUsProof(abv: Double): Double = abv * 2.0
    fun usProofToAbv(proof: Double): Double = proof / 2.0
    fun litersToUsGallons(liters: Double): Double = liters / LITERS_PER_US_GALLON
    fun usGallonsToLiters(gallons: Double): Double = gallons * LITERS_PER_US_GALLON
}
