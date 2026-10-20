package com.neatly.app

/**
 * Spirits dilution and blending math engine.
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
    const val KG_PER_US_POUND = 0.45359237

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

    data class SolveResult(
        val batchSizeLiters: Double,
        val sampleVolumeA: Double,
        val sampleVolumeB: Double,
        val pureAlcoholLiters: Double
    )

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

        val pureAlcohol = sampleVolumeA * (abvA / 100.0) + sampleVolumeB * (abvB / 100.0)

        return SolveResult(
            batchSizeLiters = batchSizeLiters,
            sampleVolumeA = sampleVolumeA,
            sampleVolumeB = sampleVolumeB,
            pureAlcoholLiters = pureAlcohol
        )
    }

    data class BottlerResult(
        val orderVolumeLiters: Double,
        val withWastageLiters: Double,
        val stockToChargeLiters: Double,
        val waterToAddLiters: Double,
        val pureAlcoholLiters: Double,
        val contractionLiters: Double,
        val angelsShareStartVolume: Double
    )

    fun calculateBottler(
        bottleVolumeMl: Double,
        packSize: Int,
        caseCount: Int,
        targetAbv: Double,
        stockAbv: Double,
        wastagePercent: Double,
        angelsShareEnabled: Boolean,
        yearsInBarrel: Double,
        annualLossPercent: Double
    ): BottlerResult {
        require(bottleVolumeMl > 0.0) { "Bottle size must be greater than zero." }
        require(packSize > 0) { "Pack size must be greater than zero." }
        require(caseCount > 0) { "Case count must be greater than zero." }
        require(targetAbv > 0.0) { "Target strength must be greater than zero." }
        require(stockAbv > targetAbv) { "Stock strength must be higher than target strength." }
        require(wastagePercent >= 0.0) { "Wastage cannot be negative." }

        val orderVolumeLiters = (bottleVolumeMl / 1000.0) * packSize * caseCount
        val wastageMultiplier = 1.0 + (wastagePercent / 100.0)
        val withWastage = orderVolumeLiters * wastageMultiplier
        val stockToCharge = withWastage * (targetAbv / stockAbv)
        val waterPer100 = waterPer100L(stockAbv, targetAbv)
        val waterToAdd = (stockToCharge / 100.0) * waterPer100
        val pureAlcohol = stockToCharge * (stockAbv / 100.0)
        val apparentVolume = stockToCharge + waterToAdd
        val contraction = apparentVolume - withWastage

        val angelsShareStartVolume = if (angelsShareEnabled && yearsInBarrel > 0.0 && annualLossPercent > 0.0) {
            val retention = Math.pow(1.0 - (annualLossPercent / 100.0), yearsInBarrel)
            if (retention > 0.0) stockToCharge / retention else stockToCharge
        } else {
            stockToCharge
        }

        return BottlerResult(
            orderVolumeLiters = orderVolumeLiters,
            withWastageLiters = withWastage,
            stockToChargeLiters = stockToCharge,
            waterToAddLiters = waterToAdd,
            pureAlcoholLiters = pureAlcohol,
            contractionLiters = contraction,
            angelsShareStartVolume = angelsShareStartVolume
        )
    }

    fun abvToUsProof(abv: Double): Double = abv * 2.0
    fun usProofToAbv(proof: Double): Double = proof / 2.0
    fun litersToUsGallons(liters: Double): Double = liters / LITERS_PER_US_GALLON
    fun usGallonsToLiters(gallons: Double): Double = gallons * LITERS_PER_US_GALLON

    enum class DoseUnit {
        ML_PER_L,
        G_PER_L,
        DROPS_PER_L
    }

    data class AdditiveInput(
        val name: String,
        val dose: Double,
        val unit: DoseUnit,
        val abv: Double = 0.0,
        val density: Double = 1.0
    )

    data class FlavourResult(
        val baseSpiritLiters: Double,
        val waterToAddLiters: Double,
        val finalVolumeLiters: Double,
        val pureAlcoholLiters: Double,
        val additiveVolumesLiters: List<Double>,
        val additiveMassesKg: List<Double>
    )

    fun calculateFlavour(
        finishedVolumeLiters: Double,
        targetAbv: Double,
        baseAbv: Double,
        additives: List<AdditiveInput>
    ): FlavourResult {
        require(finishedVolumeLiters > 0.0) { "Finished volume must be greater than zero." }
        require(targetAbv > 0.0 && baseAbv > 0.0) { "Strength values must be greater than zero." }
        require(targetAbv <= baseAbv) { "Target strength must be at or below base spirit strength." }

        val additiveVolumes = additives.map { a ->
            when (a.unit) {
                DoseUnit.ML_PER_L -> a.dose * finishedVolumeLiters / 1000.0
                DoseUnit.G_PER_L -> {
                    require(a.density > 0.0) { "Density must be greater than zero." }
                    a.dose * finishedVolumeLiters / (a.density * 1000.0)
                }
                DoseUnit.DROPS_PER_L -> a.dose * finishedVolumeLiters / 20.0 / 1000.0
            }
        }

        val additiveMassesKg = additives.mapIndexed { i, a ->
            when (a.unit) {
                DoseUnit.ML_PER_L -> additiveVolumes[i] * a.density
                DoseUnit.G_PER_L -> a.dose * finishedVolumeLiters / 1000.0
                DoseUnit.DROPS_PER_L -> additiveVolumes[i] * a.density
            }
        }

        val totalAdditiveVolume = additiveVolumes.sum()
        val totalAdditivePure = additives.mapIndexed { i, a ->
            additiveVolumes[i] * a.abv / 100.0
        }.sum()

        val targetPure = finishedVolumeLiters * targetAbv / 100.0
        val baseVolume = (targetPure - totalAdditivePure) * 100.0 / baseAbv

        require(baseVolume > 0.0) {
            "Additive alcohol exceeds the target. Lower additive ABV or raise the target."
        }

        val waterToAdd = finishedVolumeLiters - baseVolume - totalAdditiveVolume

        return FlavourResult(
            baseSpiritLiters = baseVolume,
            waterToAddLiters = waterToAdd,
            finalVolumeLiters = finishedVolumeLiters,
            pureAlcoholLiters = targetPure,
            additiveVolumesLiters = additiveVolumes,
            additiveMassesKg = additiveMassesKg
        )
    }

    fun calculateFlavourFromBase(
        baseVolumeLiters: Double,
        targetAbv: Double,
        baseAbv: Double,
        additives: List<AdditiveInput>
    ): FlavourResult {
        require(baseVolumeLiters > 0.0) { "Base volume must be greater than zero." }
        require(targetAbv > 0.0 && baseAbv > 0.0) { "Strength values must be greater than zero." }
        require(targetAbv <= baseAbv) { "Target strength must be at or below base spirit strength." }

        val coefficients = additives.map { a ->
            when (a.unit) {
                DoseUnit.ML_PER_L -> a.dose / 1000.0
                DoseUnit.G_PER_L -> {
                    require(a.density > 0.0) { "Density must be greater than zero." }
                    a.dose / (a.density * 1000.0)
                }
                DoseUnit.DROPS_PER_L -> a.dose / 20.0 / 1000.0
            }
        }

        val additiveAbvPerLitre = additives.mapIndexed { i, a ->
            coefficients[i] * a.abv
        }.sum()

        val effectiveTarget = targetAbv - additiveAbvPerLitre
        require(effectiveTarget > 0.0) {
            "Additive alcohol exceeds the target. Lower additive ABV or raise the target."
        }

        val finalVolume = baseVolumeLiters * baseAbv / effectiveTarget
        val additiveVolumes = coefficients.map { it * finalVolume }
        val totalAdditiveVolume = additiveVolumes.sum()

        val additiveMassesKg = additives.mapIndexed { i, a ->
            when (a.unit) {
                DoseUnit.ML_PER_L -> additiveVolumes[i] * a.density
                DoseUnit.G_PER_L -> a.dose * finalVolume / 1000.0
                DoseUnit.DROPS_PER_L -> additiveVolumes[i] * a.density
            }
        }

        val waterToAdd = finalVolume - baseVolumeLiters - totalAdditiveVolume
        val pureAlcohol = finalVolume * targetAbv / 100.0

        return FlavourResult(
            baseSpiritLiters = baseVolumeLiters,
            waterToAddLiters = waterToAdd,
            finalVolumeLiters = finalVolume,
            pureAlcoholLiters = pureAlcohol,
            additiveVolumesLiters = additiveVolumes,
            additiveMassesKg = additiveMassesKg
        )
    }

    private val DENSITY_TABLE: Array<Pair<Double, Double>> = arrayOf(
        0.0 to 0.9982,
        5.0 to 0.9917,
        10.0 to 0.9847,
        15.0 to 0.9791,
        20.0 to 0.9746,
        25.0 to 0.9686,
        30.0 to 0.9628,
        35.0 to 0.9554,
        40.0 to 0.9480,
        45.0 to 0.9392,
        50.0 to 0.9300,
        55.0 to 0.9197,
        60.0 to 0.9095,
        65.0 to 0.8976,
        70.0 to 0.8855,
        75.0 to 0.8727,
        80.0 to 0.8585,
        85.0 to 0.8445,
        90.0 to 0.8290,
        95.0 to 0.8122,
        100.0 to 0.7893
    )

    const val DENSITY_WATER = 0.9982
    const val DENSITY_PURE_ETHANOL = 0.7893

    fun densityAt(abv: Double): Double {
        val a = abv.coerceIn(0.0, 100.0)
        for (i in 0 until DENSITY_TABLE.size - 1) {
            val (a0, d0) = DENSITY_TABLE[i]
            val (a1, d1) = DENSITY_TABLE[i + 1]
            if (a <= a1) {
                val t = (a - a0) / (a1 - a0)
                return d0 + t * (d1 - d0)
            }
        }
        return DENSITY_TABLE.last().second
    }

    fun kgToLitres(kg: Double, abv: Double): Double = kg / densityAt(abv)
    fun litresToKg(litres: Double, abv: Double): Double = litres * densityAt(abv)
    fun kgToUsPounds(kg: Double): Double = kg / KG_PER_US_POUND
    fun usPoundsToKg(lb: Double): Double = lb * KG_PER_US_POUND
}
