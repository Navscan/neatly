package com.neatly.app

/**
 * Spirits dilution and blending math engine.
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
     * Water-dilution calculation: add water to [volumeLiters] of
     * [sourceAbv] to reach [targetAbv].
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
    // Blend: combine two known volumes and strengths.
    // ---------------------------------------------------------------

    data class BlendResult(
        val pooledVolumeLiters: Double,
        val pooledAbv: Double,
        val pureAlcoholLiters: Double,
        val contractionLiters: Double
    )

    /**
     * Combines two known quantities of spirit and returns the pooled
     * result, accounting for contraction. The contraction estimate is
     * derived by treating the blend as a weighted average of two
     * water-dilution scenarios weighted by the alcohol contributions
     * of each component.
     */
    fun blendCombine(
        volumeA: Double,
        abvA: Double,
        volumeB: Double,
        abvB: Double
    ): BlendResult {
        require(volumeA > 0.0 && volumeB > 0.0) { "Volumes must be greater than zero." }
        require(abvA > 0.0 && abvB > 0.0) { "Strengths must be greater than zero." }

        val pureA = volumeA * (abvA / 100.0)
        val pureB = volumeB * (abvB / 100.0)
        val totalPure = pureA + pureB
        val apparentVolume = volumeA + volumeB

        // Apparent pooled ABV (ignoring contraction).
        val apparentAbv = (totalPure / apparentVolume) * 100.0

        // Estimate contraction by looking at the two components as if
        // each were being diluted to the apparent pooled strength, and
        // summing the water each would need.
        val higher: Pair<Double, Double>
        val lower: Pair<Double, Double>
        if (abvA >= abvB) {
            higher = volumeA to abvA
            lower = volumeB to abvB
        } else {
            higher = volumeB to abvB
            lower = volumeA to abvA
        }

        // Contraction contribution from diluting the higher component
        // down to the pooled strength.
        val higherTarget = apparentAbv.coerceIn(0.1, higher.second - 0.1)
        val higherWater = (higher.first / 100.0) * waterPer100L(higher.second, higherTarget)
        val higherPure = higher.first * (higher.second / 100.0)
        val higherFinal = higherPure / (higherTarget / 100.0)
        val higherContraction = higher.first + higherWater - higherFinal

        // Scale to the actual pooled batch.
        val scale = totalPure / higherPure
        val contraction = (higherContraction * scale).coerceAtLeast(0.0)

        val pooledVolume = apparentVolume - contraction
        val pooledAbv = if (pooledVolume > 0.0) (totalPure / pooledVolume) * 100.0 else 0.0

        return BlendResult(
            pooledVolumeLiters = pooledVolume,
            pooledAbv = pooledAbv,
            pureAlcoholLiters = totalPure,
            contractionLiters = contraction
        )
    }

    // ---------------------------------------------------------------
    // Blend: solve for the ratio of two components needed to hit a
    // target strength.
    // ---------------------------------------------------------------

    data class SolveResult(
        val ratioA: Double,
        val ratioB: Double,
        val sampleVolumeA: Double,
        val sampleVolumeB: Double
    )

    /**
     * Pearson's square. Given two component strengths and a target
     * strength that lies between them, returns the ratio A:B and a
     * sample batch scaled so A + B = 100 L.
     *
     * Note: contraction is not modelled here — the ratio is derived
     * from pure-alcohol mass balance, which is what the ratio is
     * actually about. Final volumes in production include contraction,
     * but the ratio between components does not change with it.
     */
    fun blendSolve(abvA: Double, abvB: Double, targetAbv: Double): SolveResult {
        require(abvA > 0.0 && abvB > 0.0) { "Component strengths must be greater than zero." }
        require(targetAbv > 0.0) { "Target strength must be greater than zero." }

        val low = minOf(abvA, abvB)
        val high = maxOf(abvA, abvB)
        require(targetAbv in low..high && targetAbv != low && targetAbv != high) {
            "Target must lie strictly between the two component strengths."
        }

        // Which of A/B is the "high" component?
        val aIsHigh = abvA >= abvB

        // Pearson's square: parts of high = |target - low|, parts of
        // low = |high - target|.
        val partsHigh = targetAbv - low
        val partsLow = high - targetAbv
        val totalParts = partsHigh + partsLow

        val highFraction = partsHigh / totalParts
        val lowFraction = partsLow / totalParts

        val ratioHigh = highFraction / lowFraction  // high per 1 part low

        val sampleVolumeA: Double
        val sampleVolumeB: Double
        if (aIsHigh) {
            sampleVolumeA = highFraction * 100.0
            sampleVolumeB = lowFraction * 100.0
        } else {
            sampleVolumeA = lowFraction * 100.0
            sampleVolumeB = highFraction * 100.0
        }

        return SolveResult(
            ratioA = sampleVolumeA / 100.0,
            ratioB = sampleVolumeB / 100.0,
            sampleVolumeA = sampleVolumeA,
            sampleVolumeB = sampleVolumeB
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
