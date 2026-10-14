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
import android.widget.CheckBox
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

    private enum class TopTab { CUT, BOTTLER, FLAVOUR }
    private enum class Mode { BATCH, REDUCE, BLEND_COMBINE, BLEND_SOLVE }

    // Cut inputs
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

    private lateinit var cutCard: MaterialCardView
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

    // Bottler inputs
    private lateinit var bottlerCard: MaterialCardView
    private lateinit var bottlerPresetGlobal: TextView
    private lateinit var bottlerPresetEu: TextView
    private lateinit var bottlerPresetUs: TextView
    private lateinit var bottlerPresetRsa: TextView
    private lateinit var bottlerUnitMl: TextView
    private lateinit var bottlerUnitCl: TextView
    private lateinit var bottlerBottleSize: EditText
    private lateinit var bottlerUnitBottleSize: TextView
    private lateinit var bottlerPackSize: EditText
    private lateinit var bottlerCases: EditText
    private lateinit var bottlerTargetAbv: EditText
    private lateinit var bottlerUnitTargetStrength: TextView
    private lateinit var bottlerStockAbv: EditText
    private lateinit var bottlerUnitStockStrength: TextView
    private lateinit var bottlerAvailableStock: EditText
    private lateinit var bottlerUnitAvailableStock: TextView
    private lateinit var bottlerWastage: EditText
    private lateinit var bottlerAngelsCheckbox: CheckBox
    private lateinit var bottlerAngelsGroup: View
    private lateinit var bottlerYearsBarrel: EditText
    private lateinit var bottlerAnnualLoss: EditText

    // Results
    private lateinit var resultsCard: MaterialCardView
    private lateinit var resultsGroupDilution: View
    private lateinit var resultsGroupCombine: View
    private lateinit var resultsGroupSolve: View
    private lateinit var resultsGroupBottler: View

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

    private lateinit var bottlerHeadline: TextView
    private lateinit var bottlerOrderVolume: TextView
    private lateinit var bottlerWithWastage: TextView
    private lateinit var bottlerStockCharge: TextView
    private lateinit var bottlerWaterAdd: TextView
    private lateinit var bottlerContraction: TextView

    private lateinit var resWarning: TextView
    private lateinit var footerText: TextView
    private lateinit var wordmarkText: TextView
    private lateinit var updateBanner: MaterialCardView

    // Header pills
    private lateinit var unitAbv: TextView
    private lateinit var unitProof: TextView
    private lateinit var unitMetric: TextView
    private lateinit var unitImperial: TextView

    // Cut unit labels (dynamic)
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

    // Top tabs
    private lateinit var tabCut: TextView
    private lateinit var tabBottler: TextView
    private lateinit var tabFlavour: TextView

    private var topTab = TopTab.CUT
    private var mode = Mode.BATCH
    private var isProofMode = false
    private var isImperialMode = false
    private var bottlerInCl = false

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
        wireTopTabs()
        wireSubTabs()
        wireBottlerPresets()
        wireBottlerUnitToggle()
        wireBottlerAngelsCheckbox()
        wireUpdateBanner()

        updateToggleVisuals()
        updateUnitLabels()
        applyMode()
        applyTopTab()
    }

    override fun onResume() {
        super.onResume()
        if (updateBanner.visibility != View.VISIBLE) {
            checkForUpdate()
        }
    }

    private fun bindViews() {
        // Cut
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

        cutCard = findViewById(R.id.cutCard)
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

        // Bottler
        bottlerCard = findViewById(R.id.bottlerCard)
        bottlerPresetGlobal = findViewById(R.id.bottlerPresetGlobal)
        bottlerPresetEu = findViewById(R.id.bottlerPresetEu)
        bottlerPresetUs = findViewById(R.id.bottlerPresetUs)
        bottlerPresetRsa = findViewById(R.id.bottlerPresetRsa)
        bottlerUnitMl = findViewById(R.id.bottlerUnitMl)
        bottlerUnitCl = findViewById(R.id.bottlerUnitCl)
        bottlerBottleSize = findViewById(R.id.bottlerBottleSize)
        bottlerUnitBottleSize = findViewById(R.id.bottlerUnitBottleSize)
        bottlerPackSize = findViewById(R.id.bottlerPackSize)
        bottlerCases = findViewById(R.id.bottlerCases)
        bottlerTargetAbv = findViewById(R.id.bottlerTargetAbv)
        bottlerUnitTargetStrength = findViewById(R.id.bottlerUnitTargetStrength)
        bottlerStockAbv = findViewById(R.id.bottlerStockAbv)
        bottlerUnitStockStrength = findViewById(R.id.bottlerUnitStockStrength)
        bottlerAvailableStock = findViewById(R.id.bottlerAvailableStock)
        bottlerUnitAvailableStock = findViewById(R.id.bottlerUnitAvailableStock)
        bottlerWastage = findViewById(R.id.bottlerWastage)
        bottlerAngelsCheckbox = findViewById(R.id.bottlerAngelsCheckbox)
        bottlerAngelsGroup = findViewById(R.id.bottlerAngelsGroup)
        bottlerYearsBarrel = findViewById(R.id.bottlerYearsBarrel)
        bottlerAnnualLoss = findViewById(R.id.bottlerAnnualLoss)

        // Results
        resultsCard = findViewById(R.id.resultsCard)
        resultsGroupDilution = findViewById(R.id.resultsGroupDilution)
        resultsGroupCombine = findViewById(R.id.resultsGroupCombine)
        resultsGroupSolve = findViewById(R.id.resultsGroupSolve)
        resultsGroupBottler = findViewById(R.id.resultsGroupBottler)

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

        bottlerHeadline = findViewById(R.id.bottlerHeadline)
        bottlerOrderVolume = findViewById(R.id.bottlerOrderVolume)
        bottlerWithWastage = findViewById(R.id.bottlerWithWastage)
        bottlerStockCharge = findViewById(R.id.bottlerStockCharge)
        bottlerWaterAdd = findViewById(R.id.bottlerWaterAdd)
        bottlerContraction = findViewById(R.id.bottlerContraction)

        resWarning = findViewById(R.id.resWarning)
        footerText = findViewById(R.id.footerText)
        wordmarkText = findViewById(R.id.wordmarkText)
        updateBanner = findViewById(R.id.updateBanner)

        // Header toggles
        unitAbv = findViewById(R.id.unitAbv)
        unitProof = findViewById(R.id.unitProof)
        unitMetric = findViewById(R.id.unitMetric)
        unitImperial = findViewById(R.id.unitImperial)

        // Cut unit labels
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
        tabBottler = findViewById(R.id.tabBottler)
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
            solveAStrength, solveBStrength, solveBatchSize,
            bottlerBottleSize, bottlerPackSize, bottlerCases,
            bottlerTargetAbv, bottlerStockAbv, bottlerAvailableStock,
            bottlerWastage, bottlerYearsBarrel, bottlerAnnualLoss
        ).forEach { it.addTextChangedListener(watcher) }
    }

    // ---------------------------------------------------------------
    // Header toggles
    // ---------------------------------------------------------------

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
        scaleField(bottlerTargetAbv, factor)
        scaleField(bottlerStockAbv, factor)
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
        scaleField(bottlerAvailableStock, factor)
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
        val bottleUnit = if (bottlerInCl) getString(R.string.bottler_unit_cl)
                         else getString(R.string.bottler_unit_ml)

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

        bottlerUnitBottleSize.text = bottleUnit
        bottlerUnitTargetStrength.text = strengthUnit
        bottlerUnitStockStrength.text = strengthUnit
        bottlerUnitAvailableStock.text = volumeUnit
    }

    // ---------------------------------------------------------------
    // Top tab routing
    // ---------------------------------------------------------------

    private fun wireTopTabs() {
        tabCut.setOnClickListener { setTopTab(TopTab.CUT) }
        tabBottler.setOnClickListener { setTopTab(TopTab.BOTTLER) }
        tabFlavour.setOnClickListener { setTopTab(TopTab.FLAVOUR) }
    }

    private fun setTopTab(tab: TopTab) {
        topTab = tab
        applyTopTab()
        recalculate()
    }

    private fun applyTopTab() {
        paintToggle(tabCut, active = topTab == TopTab.CUT)
        paintToggle(tabBottler, active = topTab == TopTab.BOTTLER)
        paintToggle(tabFlavour, active = topTab == TopTab.FLAVOUR)

        cutCard.visibility = if (topTab == TopTab.CUT) View.VISIBLE else View.GONE
        bottlerCard.visibility = if (topTab == TopTab.BOTTLER) View.VISIBLE else View.GONE
    }

    // ---------------------------------------------------------------
    // Cut sub-tabs
    // ---------------------------------------------------------------

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

    // ---------------------------------------------------------------
    // Bottler: presets, unit toggle, angels' share
    // ---------------------------------------------------------------

    private fun wireBottlerPresets() {
        bottlerPresetGlobal.setOnClickListener { applyBottlerPreset(750.0, "12", bottlerPresetGlobal) }
        bottlerPresetEu.setOnClickListener { applyBottlerPreset(700.0, "6", bottlerPresetEu) }
        bottlerPresetUs.setOnClickListener { applyBottlerPreset(750.0, "12", bottlerPresetUs) }
        bottlerPresetRsa.setOnClickListener { applyBottlerPreset(750.0, "12", bottlerPresetRsa) }
    }

    private fun applyBottlerPreset(bottleMl: Double, packSize: String, selected: TextView) {
        listOf(bottlerPresetGlobal, bottlerPresetEu, bottlerPresetUs, bottlerPresetRsa).forEach {
            paintToggle(it, active = it === selected)
        }
        val displayed = if (bottlerInCl) bottleMl / 10.0 else bottleMl
        bottlerBottleSize.setText(formatNumber(displayed))
        bottlerPackSize.setText(packSize)
        recalculate()
    }

    private fun wireBottlerUnitToggle() {
        bottlerUnitMl.setOnClickListener { setBottlerUnit(inCl = false) }
        bottlerUnitCl.setOnClickListener { setBottlerUnit(inCl = true) }
    }

    private fun setBottlerUnit(inCl: Boolean) {
        if (inCl == bottlerInCl) return
        val raw = bottlerBottleSize.text?.toString().orEmpty()
        val value = raw.toDoubleOrNull()
        if (value != null) {
            val converted = if (inCl) value / 10.0 else value * 10.0
            bottlerBottleSize.setText(formatNumber(converted))
            bottlerBottleSize.setSelection(bottlerBottleSize.text?.length ?: 0)
        }
        bottlerInCl = inCl
        paintToggle(bottlerUnitMl, active = !inCl)
        paintToggle(bottlerUnitCl, active = inCl)
        updateUnitLabels()
    }

    private fun wireBottlerAngelsCheckbox() {
        bottlerAngelsCheckbox.setOnCheckedChangeListener { _, checked ->
            bottlerAngelsGroup.visibility = if (checked) View.VISIBLE else View.GONE
            recalculate()
        }
    }

    // ---------------------------------------------------------------
    // Reading helpers
    // ---------------------------------------------------------------

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

    // ---------------------------------------------------------------
    // Calculation dispatcher
    // ---------------------------------------------------------------

    private fun recalculate() {
        when (topTab) {
            TopTab.CUT -> when (mode) {
                Mode.BATCH -> calculateBatch()
                Mode.REDUCE -> calculateReduce()
                Mode.BLEND_COMBINE -> calculateCombine()
                Mode.BLEND_SOLVE -> calculateSolve()
            }
            TopTab.BOTTLER -> calculateBottler()
            TopTab.FLAVOUR -> showComingSoon()
        }
    }

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
        resultsGroupBottler.visibility = View.GONE

        resRow1Label.text = getString(R.string.result_stock_to_charge)
        resRow2Label.text = getString(R.string.result_water_to_add)

        resWater.text = formatVolume(result.stockToChargeLiters)
        resFinalVol.text = formatVolume(result.waterToAddLiters)
        resPureAlc.text = formatVolume(result.pureAlcoholLiters)
        resContraction.text = formatVolume(result.contractionLiters)
        resWarning.text = ""
    }

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
        resultsGroupBottler.visibility = View.GONE

        resRow1Label.text = getString(R.string.result_water_to_add)
        resRow2Label.text = getString(R.string.result_final_volume)

        resWater.text = formatVolume(result.waterToAddLiters)
        resFinalVol.text = formatVolume(result.finalVolumeLiters)
        resPureAlc.text = formatVolume(result.pureAlcoholLiters)
        resContraction.text = formatVolume(result.contractionLiters)
        resWarning.text = ""
    }

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
        resultsGroupBottler.visibility = View.GONE

        combHeadline.text = formatVolume(result.pooledVolumeLiters) + " at " + formatStrength(result.pooledAbv)
        combPooledVol.text = formatVolume(result.pooledVolumeLiters)
        combPooledAbv.text = formatStrength(result.pooledAbv)
        combPureAlc.text = formatVolume(result.pureAlcoholLiters)
        combContraction.text = formatVolume(result.contractionLiters)
        resWarning.text = ""
    }

    private fun calculateSolve() {
        val abvA = readStrength(solveAStrength)
        val abvB = readStrength(solveBStrength)
        val target = readStrength(targetAbv)
        val batchSize = readVolume(solveBatchSize)

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
            resultsGroupBottler.visibility = View.GONE
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
        if (topTab != TopTab.CUT || mode != Mode.BLEND_SOLVE) {
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
        resultsGroupBottler.visibility = View.GONE

        solveHeadline.text = formatVolume(result.batchSizeLiters) + " batch"
        solveSampleA.text = formatVolume(result.sampleVolumeA)
        solveSampleB.text = formatVolume(result.sampleVolumeB)
        solvePureAlc.text = formatVolume(result.pureAlcoholLiters)
        resWarning.text = ""
    }

    // ---------------------------------------------------------------
    // Bottler calculation
    // ---------------------------------------------------------------

    private fun calculateBottler() {
        val bottleRaw = bottlerBottleSize.text?.toString()?.toDoubleOrNull()
        val packRaw = bottlerPackSize.text?.toString()?.toIntOrNull()
        val casesRaw = bottlerCases.text?.toString()?.toIntOrNull()
        val target = readStrength(bottlerTargetAbv)
        val stock = readStrength(bottlerStockAbv)
        val wastage = bottlerWastage.text?.toString()?.toDoubleOrNull() ?: 0.0

        if (bottleRaw == null || packRaw == null || casesRaw == null ||
            target == null || stock == null) {
            resultsCard.visibility = View.GONE
            return
        }

        val bottleMl = if (bottlerInCl) bottleRaw * 10.0 else bottleRaw

        if (bottleMl <= 0.0 || packRaw <= 0 || casesRaw <= 0 ||
            target <= 0.0 || stock <= target) {
            resultsCard.visibility = View.GONE
            return
        }

        val angelsEnabled = bottlerAngelsCheckbox.isChecked
        val years = bottlerYearsBarrel.text?.toString()?.toDoubleOrNull() ?: 0.0
        val annualLoss = bottlerAnnualLoss.text?.toString()?.toDoubleOrNull() ?: 0.0

        try {
            val result = SpiritsMath.calculateBottler(
                bottleVolumeMl = bottleMl,
                packSize = packRaw,
                caseCount = casesRaw,
                targetAbv = target,
                stockAbv = stock,
                wastagePercent = wastage,
                angelsShareEnabled = angelsEnabled,
                yearsInBarrel = years,
                annualLossPercent = annualLoss
            )
            showBottlerResults(result, stock)
        } catch (_: IllegalArgumentException) {
            resultsCard.visibility = View.GONE
        }
    }

    private fun showBottlerResults(result: SpiritsMath.BottlerResult, stockAbv: Double) {
        resultsCard.visibility = View.VISIBLE
        resultsGroupDilution.visibility = View.GONE
        resultsGroupCombine.visibility = View.GONE
        resultsGroupSolve.visibility = View.GONE
        resultsGroupBottler.visibility = View.VISIBLE

        val headline = formatVolume(result.stockToChargeLiters) + " of " +
                formatStrength(stockAbv) + " stock"
        bottlerHeadline.text = headline

        bottlerOrderVolume.text = formatVolume(result.orderVolumeLiters)
        bottlerWithWastage.text = formatVolume(result.withWastageLiters)
        bottlerStockCharge.text = formatVolume(result.stockToChargeLiters)
        bottlerWaterAdd.text = formatVolume(result.waterToAddLiters)
        bottlerContraction.text = formatVolume(result.contractionLiters)

        // Available stock check
        val available = readVolume(bottlerAvailableStock)
        resWarning.text = when {
            available == null || available <= 0.0 -> ""
            available >= result.stockToChargeLiters -> {
                val surplus = available - result.stockToChargeLiters
                getString(R.string.bottler_msg_surplus, formatVolume(surplus))
            }
            else -> {
                val shortfall = result.stockToChargeLiters - available
                getString(R.string.bottler_msg_shortfall, formatVolume(shortfall))
            }
        }
    }

    // ---------------------------------------------------------------
    // Coming soon
    // ---------------------------------------------------------------

    private fun showComingSoon() {
        resultsCard.visibility = View.VISIBLE
        resultsGroupDilution.visibility = View.GONE
        resultsGroupCombine.visibility = View.GONE
        resultsGroupSolve.visibility = View.GONE
        resultsGroupBottler.visibility = View.GONE
        resWarning.text = getString(R.string.msg_coming_soon)
    }

    // ---------------------------------------------------------------
    // Updater
    // ---------------------------------------------------------------

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
