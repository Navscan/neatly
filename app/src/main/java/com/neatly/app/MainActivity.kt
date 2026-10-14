package com.neatly.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.RelativeSizeSpan
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private enum class Mode { BATCH, REDUCE, BLEND_COMBINE, BLEND_SOLVE }

    private lateinit var sourceVolume: EditText
    private lateinit var sourceAbv: EditText
    private lateinit var targetAbv: EditText
    private lateinit var compAVolume: EditText
    private lateinit var compAStrength: EditText
    private lateinit var compBVolume: EditText
    private lateinit var compBStrength: EditText
    private lateinit var solveAStrength: EditText
    private lateinit var solveBStrength: EditText
    private lateinit var solveBatchSize: EditText

    private lateinit var groupSingle: View
    private lateinit var groupCombine: View
    private lateinit var groupSolve: View
    private lateinit var groupTarget: View
    private lateinit var blendSubTabs: View
    private lateinit var targetHint: TextView

    private lateinit var labelVolume: TextView
    private lateinit var labelSourceStrength: TextView

    private lateinit var subBatch: TextView
    private lateinit var subReduce: TextView
    private lateinit var subBlend: TextView
    private lateinit var subCombine: TextView
    private lateinit var subSolve: TextView

    private lateinit var resultsCard: MaterialCardView
    private lateinit var resultsGroupDilution: View
    private lateinit var resultsGroupCombine: View
    private lateinit var resultsGroupSolve: View

    private lateinit var resRow1Label: TextView
    private lateinit var resRow2Label: TextView
    private lateinit var resWater: TextView
    private lateinit var resFinalVol: TextView
    private lateinit var resPureAlc: TextView
    private lateinit var resContraction: TextView

    private lateinit var combHeadline: TextView
    private lateinit var combPooledVol: TextView
    private lateinit var combPooledAbv: TextView
    private lateinit var combPureAlc: TextView
    private lateinit var combContraction: TextView

    private lateinit var solveHeadline: TextView
    private lateinit var solveSampleA: TextView
    private lateinit var solveSampleB: TextView
    private lateinit var solvePureAlc: TextView

    private lateinit var resWarning: TextView
    private lateinit var footerText: TextView
    private lateinit var wordmarkText: TextView
    private lateinit var updateBanner: MaterialCardView

    private lateinit var unitAbv: TextView
    private lateinit var unitProof: TextView
    private lateinit var unitMetric: TextView
    private lateinit var unitImperial: TextView

    private lateinit var unitVolume: TextView
    private lateinit var unitSourceStrength: TextView
    private lateinit var unitTargetStrength: TextView
    private lateinit var unitCompAVolume: TextView
    private lateinit var unitCompAStrength: TextView
    private lateinit var unitCompBVolume: TextView
    private lateinit var unitCompBStrength: TextView
    private lateinit var unitSolveAStrength: TextView
    private lateinit var unitSolveBStrength: TextView
    private lateinit var unitSolveBatchSize: TextView

    private lateinit var tabCut: TextView
    private lateinit var tab100l: TextView
    private lateinit var tabFlavour: TextView

    private var isProofMode = false
    private var isImperialMode = false
    private var mode = Mode.BATCH

    private val installPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Updater.canInstall(this)) {
            Updater.install(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindViews()
        buildWordmark()
        populateFooter()
        wireInputs()
        wireUnitToggles()
        wireMainTabs()
        wireSubTabs()
        wireUpdateBanner()
        updateToggleVisuals()
        updateUnitLabels()
        applyMode()
    }

    override fun onResume() {
        super.onResume()
        if (updateBanner.visibility != View.VISIBLE) {
            checkForUpdate()
        }
    }

    private fun bindViews() {
        sourceVolume = findViewById(R.id.sourceVolume)
        sourceAbv = findViewById(R.id.sourceAbv)
        targetAbv = findViewById(R.id.targetAbv)
        compAVolume = findViewById(R.id.compAVolume)
        compAStrength = findViewById(R.id.compAStrength)
        compBVolume = findViewById(R.id.compBVolume)
        compBStrength = findViewById(R.id.compBStrength)
        solveAStrength = findViewById(R.id.solveAStrength)
        solveBStrength = findViewById(R.id.solveBStrength)
        solveBatchSize = findViewById(R.id.solveBatchSize)

        groupSingle = findViewById(R.id.groupSingle)
        groupCombine = findViewById(R.id.groupCombine)
        groupSolve = findViewById(R.id.groupSolve)
        groupTarget = findViewById(R.id.groupTarget)
        blendSubTabs = findViewById(R.id.blendSubTabs)
        targetHint = findViewById(R.id.targetHint)

        labelVolume = findViewById(R.id.labelVolume)
        labelSourceStrength = findViewById(R.id.labelSourceStrength)

        subBatch = findViewById(R.id.subBatch)
        subReduce = findViewById(R.id.subReduce)
        subBlend = findViewById(R.id.subBlend)
        subCombine = findViewById(R.id.subCombine)
        subSolve = findViewById(R.id.subSolve)

        resultsCard = findViewById(R.id.resultsCard)
        resultsGroupDilution = findViewById(R.id.resultsGroupDilution)
        resultsGroupCombine = findViewById(R.id.resultsGroupCombine)
        resultsGroupSolve = findViewById(R.id.resultsGroupSolve)

        resRow1Label = findViewById(R.id.resRow1Label)
        resRow2Label = findViewById(R.id.resRow2Label)
        resWater = findViewById(R.id.resWater)
        resFinalVol = findViewById(R.id.resFinalVol)
        resPureAlc = findViewById(R.id.resPureAlc)
        resContraction = findViewById(R.id.resContraction)

        combHeadline = findViewById(R.id.combHeadline)
        combPooledVol = findViewById(R.id.combPooledVol)
        combPooledAbv = findViewById(R.id.combPooledAbv)
        combPureAlc = findViewById(R.id.combPureAlc)
        combContraction = findViewById(R.id.combContraction)

        solveHeadline = findViewById(R.id.solveHeadline)
        solveSampleA = findViewById(R.id.solveSampleA)
        solveSampleB = findViewById(R.id.solveSampleB)
        solvePureAlc = findViewById(R.id.solvePureAlc)

        resWarning = findViewById(R.id.resWarning)
        footerText = findViewById(R.id.footerText)
        wordmarkText = findViewById(R.id.wordmarkText)
        updateBanner = findViewById(R.id.updateBanner)

        unitAbv = findViewById(R.id.unitAbv)
        unitProof = findViewById(R.id.unitProof)
        unitMetric = findViewById(R.id.unitMetric)
        unitImperial = findViewById(R.id.unitImperial)

        unitVolume = findViewById(R.id.unitVolume)
        unitSourceStrength = findViewById(R.id.unitSourceStrength)
        unitTargetStrength = findViewById(R.id.unitTargetStrength)
        unitCompAVolume = findViewById(R.id.unitCompAVolume)
        unitCompAStrength = findViewById(R.id.unitCompAStrength)
        unitCompBVolume = findViewById(R.id.unitCompBVolume)
        unitCompBStrength = findViewById(R.id.unitCompBStrength)
        unitSolveAStrength = findViewById(R.id.unitSolveAStrength)
        unitSolveBStrength = findViewById(R.id.unitSolveBStrength)
        unitSolveBatchSize = findViewById(R.id.unitSolveBatchSize)

        tabCut = findViewById(R.id.tabCut)
        tab100l = findViewById(R.id.tab100l)
        tabFlavour = findViewById(R.id.tabFlavour)
    }

    private fun buildWordmark() {
        val cinzelTypeface = ResourcesCompat.getFont(this, R.font.cinzel_decorative_bold)
        val bodoniTypeface = ResourcesCompat.getFont(this, R.font.bodoni_moda)

        val nPart = getString(R.string.wordmark_n)
        val restPart = getString(R.string.wordmark_rest)
        val full = nPart + restPart

        val spanned = SpannableString(full)
        val nEnd = nPart.length

        if (cinzelTypeface != null) {
            spanned.setSpan(
                FontSpan(cinzelTypeface),
                0, nEnd,
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        if (bodoniTypeface != null) {
            spanned.setSpan(
                FontSpan(bodoniTypeface),
                nEnd, full.length,
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        spanned.setSpan(
            RelativeSizeSpan(0.92f),
            0, nEnd,
            SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        wordmarkText.text = spanned
    }

    private fun populateFooter() {
        val version = BuildConfig.VERSION_NAME
        val year = Calendar.getInstance().get(Calendar.YEAR)
        footerText.text = getString(R.string.footer_format, version, year)
    }

    private fun wireInputs() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                recalculate()
            }
        }
        listOf(
            sourceVolume, sourceAbv, targetAbv,
            compAVolume, compAStrength, compBVolume, compBStrength,
            solveAStrength, solveBStrength, solveBatchSize
        ).forEach { it.addTextChangedListener(watcher) }
    }

    private fun wireUnitToggles() {
        unitAbv.setOnClickListener { setStrengthMode(proof = false) }
        unitProof.setOnClickListener { setStrengthMode(proof = true) }
        unitMetric.setOnClickListener { setMeasurementMode(imperial = false) }
        unitImperial.setOnClickListener { setMeasurementMode(imperial = true) }
    }

    private fun setStrengthMode(proof: Boolean) {
        if (proof == isProofMode) return
        val factor = if (proof) 2.0 else 0.5
        scaleField(sourceAbv, factor)
        scaleField(targetAbv, factor)
        scaleField(compAStrength, factor)
        scaleField(compBStrength, factor)
        scaleField(solveAStrength, factor)
        scaleField(solveBStrength, factor)
        isProofMode = proof
        updateToggleVisuals()
        updateUnitLabels()
        recalculate()
    }

    private fun setMeasurementMode(imperial: Boolean) {
        if (imperial == isImperialMode) return
        val factor = if (imperial) 1.0 / SpiritsMath.LITERS_PER_US_GALLON
                     else SpiritsMath.LITERS_PER_US_GALLON
        scaleField(sourceVolume, factor)
        scaleField(compAVolume, factor)
        scaleField(compBVolume, factor)
        scaleField(solveBatchSize, factor)
        isImperialMode = imperial
        updateToggleVisuals()
        updateUnitLabels()
        recalculate()
    }

    private fun scaleField(field: EditText, factor: Double) {
        val raw = field.text?.toString().orEmpty()
        if (raw.isBlank()) return
        val value = raw.toDoubleOrNull() ?: return
        val scaled = value * factor
        field.setText(formatNumber(scaled))
        field.setSelection(field.text?.length ?: 0)
    }

    private fun formatNumber(value: Double): String {
        if (value == value.toLong().toDouble()) return value.toLong().toString()
        return String.format(Locale.US, "%.4f", value).trimEnd('0').trimEnd('.')
    }

    private fun updateToggleVisuals() {
        paintToggle(unitAbv, active = !isProofMode)
        paintToggle(unitProof, active = isProofMode)
        paintToggle(unitMetric, active = !isImperialMode)
        paintToggle(unitImperial, active = isImperialMode)
    }

    private fun paintToggle(view: TextView, active: Boolean) {
        if (active) {
            view.setBackgroundResource(R.drawable.pill_active)
            view.setTextColor(ContextCompat.getColor(this, R.color.on_accent))
        } else {
            view.setBackgroundResource(0)
            view.setTextColor(ContextCompat.getColor(this, R.color.ink_muted))
        }
    }

    private fun updateUnitLabels() {
        val strengthUnit = if (isProofMode) getString(R.string.unit_proof)
                           else getString(R.string.unit_abv)
        val volumeUnit = if (isImperialMode) getString(R.string.unit_gallons)
                         else getString(R.string.unit_liters)

        unitSourceStrength.text = strengthUnit
        unitTargetStrength.text = strengthUnit
        unitCompAStrength.text = strengthUnit
        unitCompBStrength.text = strengthUnit
        unitSolveAStrength.text = strengthUnit
        unitSolveBStrength.text = strengthUnit

        unitVolume.text = volumeUnit
        unitCompAVolume.text = volumeUnit
        unitCompBVolume.text = volumeUnit
        unitSolveBatchSize.text = volumeUnit
    }

    private fun wireSubTabs() {
        subBatch.setOnClickListener { setMode(Mode.BATCH) }
        subReduce.setOnClickListener { setMode(Mode.REDUCE) }
        subBlend.setOnClickListener { setMode(Mode.BLEND_COMBINE) }
        subCombine.setOnClickListener { setMode(Mode.BLEND_COMBINE) }
        subSolve.setOnClickListener { setMode(Mode.BLEND_SOLVE) }
    }

    private fun setMode(newMode: Mode) {
        mode = newMode
        applyMode()
        recalculate()
    }

    private fun applyMode() {
        paintToggle(subBatch, active = mode == Mode.BATCH)
        paintToggle(subReduce, active = mode == Mode.REDUCE)
        paintToggle(subBlend, active = mode == Mode.BLEND_COMBINE || mode == Mode.BLEND_SOLVE)

        val blendActive = mode == Mode.BLEND_COMBINE || mode == Mode.BLEND_SOLVE
        blendSubTabs.visibility = if (blendActive) View.VISIBLE else View.GONE
        paintToggle(subCombine, active = mode == Mode.BLEND_COMBINE)
        paintToggle(subSolve, active = mode == Mode.BLEND_SOLVE)

        groupSingle.visibility = if (mode == Mode.BATCH || mode == Mode.REDUCE) View.VISIBLE else View.GONE
        groupCombine.visibility = if (mode == Mode.BLEND_COMBINE) View.VISIBLE else View.GONE
        groupSolve.visibility = if (mode == Mode.BLEND_SOLVE) View.VISIBLE else View.GONE

        groupTarget.visibility = if (mode == Mode.BLEND_COMBINE) View.GONE else View.VISIBLE

        when (mode) {
            Mode.BATCH -> {
                labelVolume.text = getString(R.string.label_finished_volume)
                labelSourceStrength.text = getString(R.string.label_stock_strength)
            }
            Mode.REDUCE -> {
                labelVolume.text = getString(R.string.label_stock_volume)
                labelSourceStrength.text = getString(R.string.label_stock_strength)
            }
            else -> { }
        }

        if (mode != Mode.BLEND_SOLVE) {
            targetHint.visibility = View.GONE
        }
    }

    private fun readVolume(field: EditText): Double? {
        val raw = field.text?.toString().orEmpty()
        val value = raw.toDoubleOrNull() ?: return null
        return if (isImperialMode) SpiritsMath.usGallonsToLiters(value) else value
    }

    private fun readStrength(field: EditText): Double? {
        val raw = field.text?.toString().orEmpty()
        val value = raw.toDoubleOrNull() ?: return null
        return if (isProofMode) SpiritsMath.usProofToAbv(value) else value
    }

    private fun formatVolume(liters: Double): String {
        return if (isImperialMode) {
            String.format(Locale.US, "%.3f gal", SpiritsMath.litersToUsGallons(liters))
        } else {
            String.format(Locale.US, "%.3f L", liters)
        }
    }

    private fun formatStrength(abv: Double): String {
        return if (isProofMode) {
            String.format(Locale.US, "%.1f proof", SpiritsMath.abvToUsProof(abv))
        } else {
            String.format(Locale.US, "%.2f %% ABV", abv)
        }
    }

    private fun recalculate() {
        when (mode) {
            Mode.BATCH -> calculateBatch()
            Mode.REDUCE -> calculateReduce()
            Mode.BLEND_COMBINE -> calculateCombine()
            Mode.BLEND_SOLVE -> calculateSolve()
        }
    }

    // ---------------------------------------------------------------
    // Batch: input finished volume + stock strength + target
    // Output: stock to charge, water to add
    // ---------------------------------------------------------------

    private fun calculateBatch() {
        val finishedVolume = readVolume(sourceVolume)
        val stock = readStrength(sourceAbv)
        val target = readStrength(targetAbv)

        if (finishedVolume == null || stock == null || target == null ||
            target >= stock || target <= 0.0 || stock <= 0.0 || finishedVolume <= 0.0) {
            resultsCard.visibility = View.GONE
            return
        }

        try {
            val result = SpiritsMath.calculateBatch(finishedVolume, stock, target)
            showBatchResults(result)
        } catch (_: IllegalArgumentException) {
            resultsCard.visibility = View.GONE
        }
    }

    private fun showBatchResults(result: SpiritsMath.BatchResult) {
        resultsCard.visibility = View.VISIBLE
        resultsGroupDilution.visibility = View.VISIBLE
        resultsGroupCombine.visibility = View.GONE
        resultsGroupSolve.visibility = View.GONE

        resRow1Label.text = getString(R.string.result_stock_to_charge)
        resRow2Label.text = getString(R.string.result_water_to_add)

        resWater.text = formatVolume(result.stockToChargeLiters)
        resFinalVol.text = formatVolume(result.waterToAddLiters)
        resPureAlc.text = formatVolume(result.pureAlcoholLiters)
        resContraction.text = formatVolume(result.contractionLiters)
        resWarning.text = ""
    }

    // ---------------------------------------------------------------
    // Reduce: input stock volume + stock strength + target
    // Output: water to add, final volume
    // ---------------------------------------------------------------

    private fun calculateReduce() {
        val volume = readVolume(sourceVolume)
        val source = readStrength(sourceAbv)
        val target = readStrength(targetAbv)

        if (volume == null || source == null || target == null ||
            target >= source || target <= 0.0 || source <= 0.0 || volume <= 0.0) {
            resultsCard.visibility = View.GONE
            return
        }

        try {
            val result = SpiritsMath.calculate(volume, source, target)
            showReduceResults(result)
        } catch (_: IllegalArgumentException) {
            resultsCard.visibility = View.GONE
        }
    }

    private fun showReduceResults(result: SpiritsMath.Result) {
        resultsCard.visibility = View.VISIBLE
        resultsGroupDilution.visibility = View.VISIBLE
        resultsGroupCombine.visibility = View.GONE
        resultsGroupSolve.visibility = View.GONE

        resRow1Label.text = getString(R.string.result_water_to_add)
        resRow2Label.text = getString(R.string.result_final_volume)

        resWater.text = formatVolume(result.waterToAddLiters)
        resFinalVol.text = formatVolume(result.finalVolumeLiters)
        resPureAlc.text = formatVolume(result.pureAlcoholLiters)
        resContraction.text = formatVolume(result.contractionLiters)
        resWarning.text = ""
    }

    // ---------------------------------------------------------------
    // Combine: two known volumes + strengths → pooled result
    // ---------------------------------------------------------------

    private fun calculateCombine() {
        val volA = readVolume(compAVolume)
        val abvA = readStrength(compAStrength)
        val volB = readVolume(compBVolume)
        val abvB = readStrength(compBStrength)

        if (volA == null || abvA == null || volB == null || abvB == null ||
            volA <= 0.0 || volB <= 0.0 || abvA <= 0.0 || abvB <= 0.0) {
            resultsCard.visibility = View.GONE
            return
        }

        try {
            val result = SpiritsMath.blendCombine(volA, abvA, volB, abvB)
            showCombineResults(result)
        } catch (_: IllegalArgumentException) {
            resultsCard.visibility = View.GONE
        }
    }

    private fun showCombineResults(result: SpiritsMath.BlendResult) {
        resultsCard.visibility = View.VISIBLE
        resultsGroupDilution.visibility = View.GONE
        resultsGroupCombine.visibility = View.VISIBLE
        resultsGroupSolve.visibility = View.GONE

        combHeadline.text = formatVolume(result.pooledVolumeLiters) + " at " + formatStrength(result.pooledAbv)
        combPooledVol.text = formatVolume(result.pooledVolumeLiters)
        combPooledAbv.text = formatStrength(result.pooledAbv)
        combPureAlc.text = formatVolume(result.pureAlcoholLiters)
        combContraction.text = formatVolume(result.contractionLiters)
        resWarning.text = ""
    }

    // ---------------------------------------------------------------
    // Solve: two component strengths + target + batch size → ratio
    // ---------------------------------------------------------------

    private fun calculateSolve() {
        val abvA = readStrength(solveAStrength)
        val abvB = readStrength(solveBStrength)
        val target = readStrength(targetAbv)
        val batchSize = readVolume(solveBatchSize)

        // Always update the hint in Solve mode based on what's valid.
        updateSolveHint(abvA, abvB)

        if (abvA == null || abvB == null || target == null || batchSize == null) {
            resultsCard.visibility = View.GONE
            return
        }

        val low = minOf(abvA, abvB)
        val high = maxOf(abvA, abvB)
        if (target <= low || target >= high) {
            resultsCard.visibility = View.VISIBLE
            resultsGroupDilution.visibility = View.GONE
            resultsGroupCombine.visibility = View.GONE
            resultsGroupSolve.visibility = View.GONE
            resWarning.text = getString(R.string.msg_solve_out_of_range)
            return
        }

        if (batchSize <= 0.0) {
            resultsCard.visibility = View.GONE
            return
        }

        try {
            val result = SpiritsMath.blendSolve(abvA, abvB, target, batchSize)
            showSolveResults(result)
        } catch (_: IllegalArgumentException) {
            resultsCard.visibility = View.GONE
        }
    }

    private fun updateSolveHint(abvA: Double?, abvB: Double?) {
        if (mode != Mode.BLEND_SOLVE) {
            targetHint.visibility = View.GONE
            return
        }
        if (abvA == null || abvB == null) {
            targetHint.visibility = View.GONE
            return
        }
        val low = minOf(abvA, abvB)
        val high = maxOf(abvA, abvB)
        if (low <= 0.0 || high <= low) {
            targetHint.visibility = View.GONE
            return
        }
        targetHint.text = getString(
            R.string.msg_solve_range_format,
            formatStrength(low),
            formatStrength(high)
        )
        targetHint.visibility = View.VISIBLE
    }

    private fun showSolveResults(result: SpiritsMath.SolveResult) {
        resultsCard.visibility = View.VISIBLE
        resultsGroupDilution.visibility = View.GONE
        resultsGroupCombine.visibility = View.GONE
        resultsGroupSolve.visibility = View.VISIBLE

        solveHeadline.text = formatVolume(result.batchSizeLiters) + " batch"
        solveSampleA.text = formatVolume(result.sampleVolumeA)
        solveSampleB.text = formatVolume(result.sampleVolumeB)
        solvePureAlc.text = formatVolume(result.pureAlcoholLiters)
        resWarning.text = ""
    }

    private fun wireMainTabs() {
        tabCut.setOnClickListener { selectMainTab(tabCut) }
        tab100l.setOnClickListener { selectMainTab(tab100l) }
        tabFlavour.setOnClickListener { selectMainTab(tabFlavour) }
    }

    private fun selectMainTab(selected: TextView) {
        listOf(tabCut, tab100l, tabFlavour).forEach { tv ->
            paintToggle(tv, active = tv === selected)
        }
        if (selected !== tabCut) {
            resultsCard.visibility = View.VISIBLE
            resultsGroupDilution.visibility = View.GONE
            resultsGroupCombine.visibility = View.GONE
            resultsGroupSolve.visibility = View.GONE
            resWarning.text = getString(R.string.msg_coming_soon)
        } else {
            recalculate()
        }
    }

    private fun wireUpdateBanner() {
        updateBanner.setOnClickListener {
            if (Updater.canInstall(this)) {
                Updater.install(this)
            } else {
                val intent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:$packageName")
                )
                installPermissionLauncher.launch(intent)
            }
        }
    }

    private fun checkForUpdate() {
        lifecycleScope.launch {
            val status = Updater.checkForUpdate(this@MainActivity)
            if (status == Updater.UpdateStatus.UPDATE_READY) {
                updateBanner.visibility = View.VISIBLE
            } else {
                updateBanner.visibility = View.GONE
            }
        }
    }
}
