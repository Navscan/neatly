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

    data class BatchResult(
        val stockToChargeLiters: Double,
        val waterToAddLiters: Double,
        val finalVolumeLiters: Double,
        val pureAlcoholLiters: Double,
        val contractionLiters: Double
    )

    /**
     * Reduce mode: add water to a known volume of stock to reach a
     * target strength.
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
     * Batch mode: charge a tank to finish at exactly [finishedVolumeLiters]
     * at [targetAbv], given stock spirit at [stockAbv].
     *
     * Returns how much stock to charge and how much water to add.
     *
     * The math:
     *   pure alcohol needed    = finishedVolume * targetAbv
     *   pure alcohol from stock = stockVolume * stockAbv
     *   => stockVolume = finishedVolume * targetAbv / stockAbv
     *   water to add    = stockVolume * waterPer100L(stockAbv, targetAbv) / 100
     */
    fun calculateBatch(
        finishedVolumeLiters: Double,
        stockAbv: Double,
        targetAbv: Double
    ): BatchResult {
        require(targetAbv < stockAbv) { "Target strength must be lower than stock strength." }
        require(targetAbv > 0.0 && stockAbv > 0.0) { "Strength values must be greater than zero." }
        require(finishedVolumeLiters > 0.0) { "Finished volume must be greater than zero." }

        val stockToCharge = finishedVolumeLiters * (targetAbv / stockAbv)
        val waterPer100 = waterPer100L(stockAbv, targetAbv)
        val waterToAdd = (stockToCharge / 100.0) * waterPer100
        val pureAlcohol = stockToCharge * (stockAbv / 100.0)
        val apparentVolume = stockToCharge + waterToAdd
        val contraction = apparentVolume - finishedVolumeLiters

        return BatchResult(
            stockToChargeLiters = stockToCharge,
            waterToAddLiters = waterToAdd,
            finalVolumeLiters = finishedVolumeLiters,
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
        val apparentAbv = (totalPure / apparentVolume) * 100.0

        val higher: Pair<Double, Double>
        if (abvA >= abvB) {
            higher = volumeA to abvA
        } else {
            higher = volumeB to abvB
        }

        val higherTarget = apparentAbv.coerceIn(0.1, higher.second - 0.1)
        val higherWater = (higher.first / 100.0) * waterPer100L(higher.second, higherTarget)
        val higherPure = higher.first * (higher.second / 100.0)
        val higherFinal = higherPure / (higherTarget / 100.0)
        val higherContraction = higher.first + higherWater - higherFinal

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
    // target strength, scaled to a specified batch size.
    // ---------------------------------------------------------------

    data class SolveResult(
        val batchSizeLiters: Double,
        val sampleVolumeA: Double,
        val sampleVolumeB: Double,
        val pureAlcoholLiters: Double
    )

    /**
     * Pearson's square. Given two component strengths and a target
     * strength that lies strictly between them, returns the ratio A:B
     * and the litre quantities of each component needed for a
     * [batchSizeLiters] batch.
     */
    fun blendSolve(
        abvA: Double,
        abvB: Double,
        targetAbv: Double,
        batchSizeLiters: Double
    ): SolveResult {
        require(abvA > 0.0 && abvB > 0.0) { "Component strengths must be greater than zero." }
        require(targetAbv > 0.0) { "Target strength must be greater than zero." }
        require(batchSizeLiters > 0.0) { "Batch size must be greater than zero." }

        val low = minOf(abvA, abvB)
        val high = maxOf(abvA, abvB)
        require(targetAbv > low && targetAbv < high) {
            "Target must lie strictly between the two component strengths."
        }

        val aIsHigh = abvA >= abvB

        val partsHigh = targetAbv - low
        val partsLow = high - targetAbv
        val totalParts = partsHigh + partsLow

        val highFraction = partsHigh / totalParts
        val lowFraction = partsLow / totalParts

        val sampleVolumeA: Double
        val sampleVolumeB: Double
        if (aIsHigh) {
            sampleVolumeA = highFraction * batchSizeLiters
            sampleVolumeB = lowFraction * batchSizeLiters
        } else {
            sampleVolumeA = lowFraction * batchSizeLiters
            sampleVolumeB = highFraction * batchSizeLiters
        }

        // Pure alcohol for the batch = sum of the two components.
        val pureAlcohol = sampleVolumeA * (abvA / 100.0) + sampleVolumeB * (abvB / 100.0)

        return SolveResult(
            batchSizeLiters = batchSizeLiters,
            sampleVolumeA = sampleVolumeA,
            sampleVolumeB = sampleVolumeB,
            pureAlcoholLiters = pureAlcohol
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
