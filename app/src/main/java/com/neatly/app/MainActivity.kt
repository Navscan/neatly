package com.neatly.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
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
import kotlin.math.floor

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
    private lateinit var modeHint: TextView

    private lateinit var labelVolume: TextView
    private lateinit var labelSourceStrength: TextView

    private lateinit var subBatch: TextView
    private lateinit var subReduce: TextView
    private lateinit var subBlend: TextView
    private lateinit var subCombine: TextView
    private lateinit var subSolve: TextView

    // Bottler inputs
    private lateinit var bottlerCard: MaterialCardView
    private lateinit var bottlerModeCases: TextView
    private lateinit var bottlerModeVolume: TextView
    private lateinit var bottlerPresetGlobal: TextView
    private lateinit var bottlerPresetEu: TextView
    private lateinit var bottlerPresetUs: TextView
    private lateinit var bottlerPresetRsa: TextView
    private lateinit var bottlerUnitMl: TextView
    private lateinit var bottlerUnitCl: TextView
    private lateinit var bottlerBottleSize: EditText
    private lateinit var bottlerUnitBottleSize: TextView
    private lateinit var bottlerPackSize: EditText
    private lateinit var bottlerCasesGroup: View
    private lateinit var bottlerCases: EditText
    private lateinit var bottlerVolumeGroup: View
    private lateinit var bottlerFinishedVolume: EditText
    private lateinit var bottlerUnitFinishedVolume: TextView
    private lateinit var bottlerTargetAbv: EditText
    private lateinit var bottlerUnitTargetStrength: TextView
    private lateinit var bottlerStockAbv: EditText
    private lateinit var bottlerUnitStockStrength: TextView
    private lateinit var bottlerAvailableGroup: View
    private lateinit var bottlerAvailableStock: EditText
    private lateinit var bottlerUnitAvailableStock: TextView
    private lateinit var bottlerWastage: EditText
    private lateinit var bottlerAngelsCheckbox: CheckBox
    private lateinit var bottlerAngelsGroup: View
    private lateinit var bottlerYearsBarrel: EditText
    private lateinit var bottlerAnnualLoss: EditText

    // Enhancing inputs
    private lateinit var flavourCard: MaterialCardView
    private lateinit var enhModeFinished: TextView
    private lateinit var enhModeBase: TextView
    private lateinit var enhModeHint: TextView
    private lateinit var enhFirstLabel: TextView
    private lateinit var flavourFinishedVolume: EditText
    private lateinit var flavourTargetAbv: EditText
    private lateinit var flavourBaseAbv: EditText
    private lateinit var flavourUnitVolume: TextView
    private lateinit var flavourUnitTargetStrength: TextView
    private lateinit var flavourUnitBaseStrength: TextView

    private lateinit var flavourAddName: Array<EditText>
    private lateinit var flavourAddDose: Array<EditText>
    private lateinit var flavourAddAbv: Array<EditText>
    private lateinit var flavourAddDensity: Array<EditText>
    private lateinit var flavourAddUnitMl: Array<TextView>
    private lateinit var flavourAddUnitG: Array<TextView>
    private lateinit var flavourAddUnitDrops: Array<TextView>

    private val flavourAddUnit: Array<SpiritsMath.DoseUnit> = arrayOf(
        SpiritsMath.DoseUnit.ML_PER_L,
        SpiritsMath.DoseUnit.ML_PER_L,
        SpiritsMath.DoseUnit.ML_PER_L
    )

    // Results
    private lateinit var resultsCard: MaterialCardView
    private lateinit var resultsGroupDilution: View
    private lateinit var resultsGroupCombine: View
    private lateinit var resultsGroupSolve: View
    private lateinit var resultsGroupBottler: View
    private lateinit var resultsGroupFlavour: View
    private lateinit var contractionRowDilution: View
    private lateinit var contractionRowCombine: View
    private lateinit var contractionRowBottler: View
    private lateinit var bottlerCasesResultsGroup: View
    private lateinit var bottlerVolumeResultsGroup: View

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
    private lateinit var bottlerWholeCases: TextView
    private lateinit var bottlerWholeBottles: TextView
    private lateinit var bottlerResidual: TextView

    private lateinit var flavHeadline: TextView
    private lateinit var flavBaseSpirit: TextView
    private lateinit var flavWaterAdd: TextView
    private lateinit var flavAdd1VolLabel: TextView
    private lateinit var flavAdd1Vol: TextView
    private lateinit var flavAdd2VolLabel: TextView
    private lateinit var flavAdd2Vol: TextView
    private lateinit var flavAdd3VolLabel: TextView
    private lateinit var flavAdd3Vol: TextView
    private lateinit var flavPureAlc: TextView

    // Push-forward chevrons
    private lateinit var pushDiluteFinalToEnhVol: TextView
    private lateinit var pushDiluteFinalToBottlerVol: TextView
    private lateinit var pushCombineVolToEnhVol: TextView
    private lateinit var pushCombineVolToBottlerVol: TextView
    private lateinit var pushCombineAbvToEnhStrength: TextView
    private lateinit var pushBottlerWastageToEnhVol: TextView
    private lateinit var pushBottlerStockToDiluteVol: TextView
    private lateinit var pushFlavBaseToDiluteVol: TextView
    private lateinit var pushFlavHeadlineToBottlerVol: TextView

    private lateinit var resWarning: TextView
    private lateinit var footerText: TextView
    private lateinit var wordmarkText: TextView
    private lateinit var updateBanner: MaterialCardView

    // Header pills
    private lateinit var unitAbv: TextView
    private lateinit var unitProof: TextView
    private lateinit var unitMetric: TextView
    private lateinit var unitImperial: TextView
    private lateinit var unitVol: TextView
    private lateinit var unitMass: TextView

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
    private var isMassMode = false
    private var bottlerInCl = false
    private var bottlerVolumeMode = false
    private var enhBaseMode = false
    private var suppressSync = false

    private var lastCombinePooledAbv: Double? = null

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
        wireSharedStrengths()
        wireUnitToggles()
        wireTopTabs()
        wireSubTabs()
        wireBottlerMode()
        wireBottlerPresets()
        wireBottlerUnitToggle()
        wireBottlerAngelsCheckbox()
        wireFlavourUnitPills()
        wireEnhancingMode()
        wirePushButtons()
        wireUpdateBanner()

        updateToggleVisuals()
        updateUnitLabels()
        applyMassVisibility()
        applyEnhancingMode()
        applyBottlerMode()
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
        modeHint = findViewById(R.id.modeHint)

        labelVolume = findViewById(R.id.labelVolume)
        labelSourceStrength = findViewById(R.id.labelSourceStrength)

        subBatch = findViewById(R.id.subBatch)
        subReduce = findViewById(R.id.subReduce)
        subBlend = findViewById(R.id.subBlend)
        subCombine = findViewById(R.id.subCombine)
        subSolve = findViewById(R.id.subSolve)

        // Bottler
        bottlerCard = findViewById(R.id.bottlerCard)
        bottlerModeCases = findViewById(R.id.bottlerModeCases)
        bottlerModeVolume = findViewById(R.id.bottlerModeVolume)
        bottlerPresetGlobal = findViewById(R.id.bottlerPresetGlobal)
        bottlerPresetEu = findViewById(R.id.bottlerPresetEu)
        bottlerPresetUs = findViewById(R.id.bottlerPresetUs)
        bottlerPresetRsa = findViewById(R.id.bottlerPresetRsa)
        bottlerUnitMl = findViewById(R.id.bottlerUnitMl)
        bottlerUnitCl = findViewById(R.id.bottlerUnitCl)
        bottlerBottleSize = findViewById(R.id.bottlerBottleSize)
        bottlerUnitBottleSize = findViewById(R.id.bottlerUnitBottleSize)
        bottlerPackSize = findViewById(R.id.bottlerPackSize)
        bottlerCasesGroup = findViewById(R.id.bottlerCasesGroup)
        bottlerCases = findViewById(R.id.bottlerCases)
        bottlerVolumeGroup = findViewById(R.id.bottlerVolumeGroup)
        bottlerFinishedVolume = findViewById(R.id.bottlerFinishedVolume)
        bottlerUnitFinishedVolume = findViewById(R.id.bottlerUnitFinishedVolume)
        bottlerTargetAbv = findViewById(R.id.bottlerTargetAbv)
        bottlerUnitTargetStrength = findViewById(R.id.bottlerUnitTargetStrength)
        bottlerStockAbv = findViewById(R.id.bottlerStockAbv)
        bottlerUnitStockStrength = findViewById(R.id.bottlerUnitStockStrength)
        bottlerAvailableGroup = findViewById(R.id.bottlerAvailableGroup)
        bottlerAvailableStock = findViewById(R.id.bottlerAvailableStock)
        bottlerUnitAvailableStock = findViewById(R.id.bottlerUnitAvailableStock)
        bottlerWastage = findViewById(R.id.bottlerWastage)
        bottlerAngelsCheckbox = findViewById(R.id.bottlerAngelsCheckbox)
        bottlerAngelsGroup = findViewById(R.id.bottlerAngelsGroup)
        bottlerYearsBarrel = findViewById(R.id.bottlerYearsBarrel)
        bottlerAnnualLoss = findViewById(R.id.bottlerAnnualLoss)

        // Enhancing
        flavourCard = findViewById(R.id.flavourCard)
        enhModeFinished = findViewById(R.id.enhModeFinished)
        enhModeBase = findViewById(R.id.enhModeBase)
        enhModeHint = findViewById(R.id.enhModeHint)
        enhFirstLabel = findViewById(R.id.enhFirstLabel)
        flavourFinishedVolume = findViewById(R.id.flavourFinishedVolume)
        flavourTargetAbv = findViewById(R.id.flavourTargetAbv)
        flavourBaseAbv = findViewById(R.id.flavourBaseAbv)
        flavourUnitVolume = findViewById(R.id.flavourUnitVolume)
        flavourUnitTargetStrength = findViewById(R.id.flavourUnitTargetStrength)
        flavourUnitBaseStrength = findViewById(R.id.flavourUnitBaseStrength)

        flavourAddName = arrayOf(
            findViewById(R.id.flavourAdd1Name),
            findViewById(R.id.flavourAdd2Name),
            findViewById(R.id.flavourAdd3Name)
        )
        flavourAddDose = arrayOf(
            findViewById(R.id.flavourAdd1Dose),
            findViewById(R.id.flavourAdd2Dose),
            findViewById(R.id.flavourAdd3Dose)
        )
        flavourAddAbv = arrayOf(
            findViewById(R.id.flavourAdd1Abv),
            findViewById(R.id.flavourAdd2Abv),
            findViewById(R.id.flavourAdd3Abv)
        )
        flavourAddDensity = arrayOf(
            findViewById(R.id.flavourAdd1Density),
            findViewById(R.id.flavourAdd2Density),
            findViewById(R.id.flavourAdd3Density)
        )
        flavourAddUnitMl = arrayOf(
            findViewById(R.id.flavourAdd1UnitMl),
            findViewById(R.id.flavourAdd2UnitMl),
            findViewById(R.id.flavourAdd3UnitMl)
        )
        flavourAddUnitG = arrayOf(
            findViewById(R.id.flavourAdd1UnitG),
            findViewById(R.id.flavourAdd2UnitG),
            findViewById(R.id.flavourAdd3UnitG)
        )
        flavourAddUnitDrops = arrayOf(
            findViewById(R.id.flavourAdd1UnitDrops),
            findViewById(R.id.flavourAdd2UnitDrops),
            findViewById(R.id.flavourAdd3UnitDrops)
        )

        // Results
        resultsCard = findViewById(R.id.resultsCard)
        resultsGroupDilution = findViewById(R.id.resultsGroupDilution)
        resultsGroupCombine = findViewById(R.id.resultsGroupCombine)
        resultsGroupSolve = findViewById(R.id.resultsGroupSolve)
        resultsGroupBottler = findViewById(R.id.resultsGroupBottler)
        resultsGroupFlavour = findViewById(R.id.resultsGroupFlavour)
        contractionRowDilution = findViewById(R.id.contractionRowDilution)
        contractionRowCombine = findViewById(R.id.contractionRowCombine)
        contractionRowBottler = findViewById(R.id.contractionRowBottler)
        bottlerCasesResultsGroup = findViewById(R.id.bottlerCasesResultsGroup)
        bottlerVolumeResultsGroup = findViewById(R.id.bottlerVolumeResultsGroup)

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
        bottlerWholeCases = findViewById(R.id.bottlerWholeCases)
        bottlerWholeBottles = findViewById(R.id.bottlerWholeBottles)
        bottlerResidual = findViewById(R.id.bottlerResidual)

        flavHeadline = findViewById(R.id.flavHeadline)
        flavBaseSpirit = findViewById(R.id.flavBaseSpirit)
        flavWaterAdd = findViewById(R.id.flavWaterAdd)
        flavAdd1VolLabel = findViewById(R.id.flavAdd1VolLabel)
        flavAdd1Vol = findViewById(R.id.flavAdd1Vol)
        flavAdd2VolLabel = findViewById(R.id.flavAdd2VolLabel)
        flavAdd2Vol = findViewById(R.id.flavAdd2Vol)
        flavAdd3VolLabel = findViewById(R.id.flavAdd3VolLabel)
        flavAdd3Vol = findViewById(R.id.flavAdd3Vol)
        flavPureAlc = findViewById(R.id.flavPureAlc)

        pushDiluteFinalToEnhVol = findViewById(R.id.pushDiluteFinalToEnhVol)
        pushDiluteFinalToBottlerVol = findViewById(R.id.pushDiluteFinalToBottlerVol)
        pushCombineVolToEnhVol = findViewById(R.id.pushCombineVolToEnhVol)
        pushCombineVolToBottlerVol = findViewById(R.id.pushCombineVolToBottlerVol)
        pushCombineAbvToEnhStrength = findViewById(R.id.pushCombineAbvToEnhStrength)
        pushBottlerWastageToEnhVol = findViewById(R.id.pushBottlerWastageToEnhVol)
        pushBottlerStockToDiluteVol = findViewById(R.id.pushBottlerStockToDiluteVol)
        pushFlavBaseToDiluteVol = findViewById(R.id.pushFlavBaseToDiluteVol)
        pushFlavHeadlineToBottlerVol = findViewById(R.id.pushFlavHeadlineToBottlerVol)

        resWarning = findViewById(R.id.resWarning)
        footerText = findViewById(R.id.footerText)
        wordmarkText = findViewById(R.id.wordmarkText)
        updateBanner = findViewById(R.id.updateBanner)

        // Header toggles
        unitAbv = findViewById(R.id.unitAbv)
        unitProof = findViewById(R.id.unitProof)
        unitMetric = findViewById(R.id.unitMetric)
        unitImperial = findViewById(R.id.unitImperial)
        unitVol = findViewById(R.id.unitVol)
        unitMass = findViewById(R.id.unitMass)

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
            bottlerFinishedVolume,
            bottlerTargetAbv, bottlerStockAbv, bottlerAvailableStock,
            bottlerWastage, bottlerYearsBarrel, bottlerAnnualLoss,
            flavourFinishedVolume, flavourTargetAbv, flavourBaseAbv,
            flavourAddName[0], flavourAddDose[0], flavourAddAbv[0], flavourAddDensity[0],
            flavourAddName[1], flavourAddDose[1], flavourAddAbv[1], flavourAddDensity[1],
            flavourAddName[2], flavourAddDose[2], flavourAddAbv[2], flavourAddDensity[2]
        ).forEach { it.addTextChangedListener(watcher) }
    }

    // ---------------------------------------------------------------
    // Shared strengths
    // ---------------------------------------------------------------

    private fun wireSharedStrengths() {
        val baseStrengthFields = listOf(sourceAbv, bottlerStockAbv, flavourBaseAbv)
        val targetStrengthFields = listOf(targetAbv, bottlerTargetAbv, flavourTargetAbv)
        for (f in baseStrengthFields) f.addTextChangedListener(makeSharedWatcher(baseStrengthFields))
        for (f in targetStrengthFields) f.addTextChangedListener(makeSharedWatcher(targetStrengthFields))
    }

    private fun makeSharedWatcher(group: List<EditText>): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (suppressSync) return
                val text = s?.toString() ?: return
                suppressSync = true
                try {
                    for (f in group) {
                        if (f.text?.toString() != text) {
                            f.setText(text)
                            f.setSelection(f.text?.length ?: 0)
                        }
                    }
                } finally {
                    suppressSync = false
                }
            }
        }
    }

    // ---------------------------------------------------------------
    // Header toggles
    // ---------------------------------------------------------------

    private fun wireUnitToggles() {
        unitAbv.setOnClickListener { setStrengthMode(proof = false) }
        unitProof.setOnClickListener { setStrengthMode(proof = true) }
        unitMetric.setOnClickListener { setMeasurementMode(imperial = false) }
        unitImperial.setOnClickListener { setMeasurementMode(imperial = true) }
        unitVol.setOnClickListener { setMassMode(mass = false) }
        unitMass.setOnClickListener { setMassMode(mass = true) }
    }

    private fun setStrengthMode(proof: Boolean) {
        if (proof == isProofMode) return
        val factor = if (proof) 2.0 else 0.5
        suppressSync = true
        try {
            scaleField(sourceAbv, factor)
            scaleField(targetAbv, factor)
            scaleField(compAStrength, factor)
            scaleField(compBStrength, factor)
            scaleField(solveAStrength, factor)
            scaleField(solveBStrength, factor)
            scaleField(bottlerTargetAbv, factor)
            scaleField(bottlerStockAbv, factor)
            scaleField(flavourTargetAbv, factor)
            scaleField(flavourBaseAbv, factor)
            scaleField(flavourAddAbv[0], factor)
            scaleField(flavourAddAbv[1], factor)
            scaleField(flavourAddAbv[2], factor)
        } finally {
            suppressSync = false
        }
        isProofMode = proof
        updateToggleVisuals()
        updateUnitLabels()
        recalculate()
    }

    private fun setMeasurementMode(imperial: Boolean) {
        if (imperial == isImperialMode) return

        val factor: Double
        if (isMassMode) {
            factor = if (imperial) 1.0 / SpiritsMath.KG_PER_US_POUND
                     else SpiritsMath.KG_PER_US_POUND
        } else {
            factor = if (imperial) 1.0 / SpiritsMath.LITERS_PER_US_GALLON
                     else SpiritsMath.LITERS_PER_US_GALLON
        }

        scaleField(sourceVolume, factor)
        scaleField(compAVolume, factor)
        scaleField(compBVolume, factor)
        scaleField(solveBatchSize, factor)
        scaleField(bottlerAvailableStock, factor)
        scaleField(bottlerFinishedVolume, factor)
        scaleField(flavourFinishedVolume, factor)
        isImperialMode = imperial
        updateToggleVisuals()
        updateUnitLabels()
        recalculate()
    }

    private fun setMassMode(mass: Boolean) {
        if (mass == isMassMode) return

        val volumeFields = listOf(
            sourceVolume, compAVolume, compBVolume, solveBatchSize,
            bottlerAvailableStock, bottlerFinishedVolume, flavourFinishedVolume
        )
        for (field in volumeFields) {
            val raw = field.text?.toString().orEmpty()
            if (raw.isBlank()) continue
            val v = raw.toDoubleOrNull() ?: continue
            val abv = abvForVolumeField(field)

            val converted: Double = if (mass) {
                val litres = if (isImperialMode) SpiritsMath.usGallonsToLiters(v) else v
                val kg = SpiritsMath.litresToKg(litres, abv)
                if (isImperialMode) SpiritsMath.kgToUsPounds(kg) else kg
            } else {
                val kg = if (isImperialMode) SpiritsMath.usPoundsToKg(v) else v
                val litres = SpiritsMath.kgToLitres(kg, abv)
                if (isImperialMode) SpiritsMath.litersToUsGallons(litres) else litres
            }
            field.setText(formatNumber(converted))
            field.setSelection(field.text?.length ?: 0)
        }

        isMassMode = mass
        updateToggleVisuals()
        updateUnitLabels()
        applyMassVisibility()
        recalculate()
    }

    private fun applyMassVisibility() {
        val visibility = if (isMassMode) View.GONE else View.VISIBLE
        contractionRowDilution.visibility = visibility
        contractionRowCombine.visibility = visibility
        contractionRowBottler.visibility = visibility
    }

    private fun abvForVolumeField(field: EditText): Double {
        return when (field) {
            sourceVolume -> if (mode == Mode.REDUCE) {
                readStrength(sourceAbv) ?: 40.0
            } else {
                readStrength(targetAbv) ?: 40.0
            }
            compAVolume -> readStrength(compAStrength) ?: 40.0
            compBVolume -> readStrength(compBStrength) ?: 40.0
            solveBatchSize -> readStrength(targetAbv) ?: 40.0
            bottlerAvailableStock -> readStrength(bottlerStockAbv) ?: 40.0
            bottlerFinishedVolume -> readStrength(bottlerTargetAbv) ?: 40.0
            flavourFinishedVolume -> if (enhBaseMode) {
                readStrength(flavourBaseAbv) ?: 40.0
            } else {
                readStrength(flavourTargetAbv) ?: 40.0
            }
            else -> 40.0
        }
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
        paintToggle(unitVol, active = !isMassMode)
        paintToggle(unitMass, active = isMassMode)
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

        val volumeOrMassUnit = if (isMassMode) {
            if (isImperialMode) getString(R.string.unit_lb) else getString(R.string.unit_kg)
        } else {
            if (isImperialMode) getString(R.string.unit_gallons) else getString(R.string.unit_liters)
        }

        val bottleUnit = if (bottlerInCl) getString(R.string.bottler_unit_cl)
                         else getString(R.string.bottler_unit_ml)

        unitSourceStrength.text = strengthUnit
        unitTargetStrength.text = strengthUnit
        unitCompAStrength.text = strengthUnit
        unitCompBStrength.text = strengthUnit
        unitSolveAStrength.text = strengthUnit
        unitSolveBStrength.text = strengthUnit

        unitVolume.text = volumeOrMassUnit
        unitCompAVolume.text = volumeOrMassUnit
        unitCompBVolume.text = volumeOrMassUnit
        unitSolveBatchSize.text = volumeOrMassUnit

        bottlerUnitBottleSize.text = bottleUnit
        bottlerUnitTargetStrength.text = strengthUnit
        bottlerUnitStockStrength.text = strengthUnit
        bottlerUnitAvailableStock.text = volumeOrMassUnit
        bottlerUnitFinishedVolume.text = volumeOrMassUnit

        flavourUnitTargetStrength.text = strengthUnit
        flavourUnitBaseStrength.text = strengthUnit
        flavourUnitVolume.text = volumeOrMassUnit
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
        flavourCard.visibility = if (topTab == TopTab.FLAVOUR) View.VISIBLE else View.GONE
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

        pushDiluteFinalToEnhVol.visibility =
            if (mode == Mode.REDUCE) View.VISIBLE else View.GONE

        when (mode) {
            Mode.BATCH -> {
                labelVolume.text = getString(R.string.label_finished_volume)
                labelSourceStrength.text = getString(R.string.label_stock_strength)
                modeHint.text = getString(R.string.hint_mode_formulate)
                modeHint.visibility = View.VISIBLE
            }
            Mode.REDUCE -> {
                labelVolume.text = getString(R.string.label_stock_volume)
                labelSourceStrength.text = getString(R.string.label_stock_strength)
                modeHint.text = getString(R.string.hint_mode_dilute)
                modeHint.visibility = View.VISIBLE
            }
            Mode.BLEND_COMBINE, Mode.BLEND_SOLVE -> {
                modeHint.visibility = View.GONE
            }
        }

        if (mode != Mode.BLEND_SOLVE) {
            targetHint.visibility = View.GONE
        }
    }

    // ---------------------------------------------------------------
    // Bottler mode toggle
    // ---------------------------------------------------------------

    private fun wireBottlerMode() {
        bottlerModeCases.setOnClickListener { setBottlerMode(volume = false) }
        bottlerModeVolume.setOnClickListener { setBottlerMode(volume = true) }
    }

    private fun setBottlerMode(volume: Boolean) {
        if (volume == bottlerVolumeMode) return
        bottlerVolumeMode = volume
        applyBottlerMode()
        recalculate()
    }

    private fun applyBottlerMode() {
        paintToggle(bottlerModeCases, active = !bottlerVolumeMode)
        paintToggle(bottlerModeVolume, active = bottlerVolumeMode)

        bottlerCasesGroup.visibility = if (bottlerVolumeMode) View.GONE else View.VISIBLE
        bottlerVolumeGroup.visibility = if (bottlerVolumeMode) View.VISIBLE else View.GONE
        bottlerAvailableGroup.visibility = if (bottlerVolumeMode) View.GONE else View.VISIBLE
    }

    // ---------------------------------------------------------------
    // Bottler: presets, unit toggle, angels share
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
    // Enhancing: additive unit pills and mode toggle
    // ---------------------------------------------------------------

    private fun wireFlavourUnitPills() {
        for (i in 0 until 3) {
            flavourAddUnitMl[i].setOnClickListener { setFlavourUnit(i, SpiritsMath.DoseUnit.ML_PER_L) }
            flavourAddUnitG[i].setOnClickListener { setFlavourUnit(i, SpiritsMath.DoseUnit.G_PER_L) }
            flavourAddUnitDrops[i].setOnClickListener { setFlavourUnit(i, SpiritsMath.DoseUnit.DROPS_PER_L) }
        }
    }

    private fun setFlavourUnit(slot: Int, unit: SpiritsMath.DoseUnit) {
        if (flavourAddUnit[slot] == unit) return
        flavourAddUnit[slot] = unit
        paintToggle(flavourAddUnitMl[slot], active = unit == SpiritsMath.DoseUnit.ML_PER_L)
        paintToggle(flavourAddUnitG[slot], active = unit == SpiritsMath.DoseUnit.G_PER_L)
        paintToggle(flavourAddUnitDrops[slot], active = unit == SpiritsMath.DoseUnit.DROPS_PER_L)
        recalculate()
    }

    private fun wireEnhancingMode() {
        enhModeFinished.setOnClickListener { setEnhancingMode(base = false) }
        enhModeBase.setOnClickListener { setEnhancingMode(base = true) }
    }

    private fun setEnhancingMode(base: Boolean) {
        if (base == enhBaseMode) return
        enhBaseMode = base
        applyEnhancingMode()
        recalculate()
    }

    private fun applyEnhancingMode() {
        paintToggle(enhModeFinished, active = !enhBaseMode)
        paintToggle(enhModeBase, active = enhBaseMode)
        if (enhBaseMode) {
            enhFirstLabel.text = getString(R.string.label_base_volume_enh)
            enhModeHint.text = getString(R.string.hint_enh_base)
        } else {
            enhFirstLabel.text = getString(R.string.enh_mode_finished)
            enhModeHint.text = getString(R.string.hint_enh_finished)
        }
    }

    // ---------------------------------------------------------------
    // Push-forward
    // ---------------------------------------------------------------

    private fun wirePushButtons() {
        pushDiluteFinalToEnhVol.setOnClickListener { pushToEnhancing(resFinalVol) }
        pushCombineVolToEnhVol.setOnClickListener { pushToEnhancing(combPooledVol) }
        pushBottlerWastageToEnhVol.setOnClickListener { pushToEnhancing(bottlerWithWastage) }
        pushCombineAbvToEnhStrength.setOnClickListener { pushAbvToEnhancingBase() }
        pushBottlerStockToDiluteVol.setOnClickListener { pushToDilute(bottlerStockCharge) }
        pushFlavBaseToDiluteVol.setOnClickListener { pushToDilute(flavBaseSpirit) }
        pushDiluteFinalToBottlerVol.setOnClickListener { pushToBottler(resFinalVol) }
        pushCombineVolToBottlerVol.setOnClickListener { pushToBottler(combPooledVol) }
        pushFlavHeadlineToBottlerVol.setOnClickListener { pushToBottlerFromHeadline(flavHeadline) }
    }

    private fun parseLeadingNumber(text: CharSequence): Double? {
        val s = text.toString().trim()
        if (s.isEmpty()) return null
        val spaceIdx = s.indexOf(' ')
        val numStr = if (spaceIdx > 0) s.substring(0, spaceIdx) else s
        return numStr.toDoubleOrNull()
    }

    private fun pushToEnhancing(source: TextView) {
        val value = parseLeadingNumber(source.text) ?: return
        if (value <= 0.0) return
        if (!enhBaseMode) {
            enhBaseMode = true
            applyEnhancingMode()
        }
        flavourFinishedVolume.setText(formatNumber(value))
        flavourFinishedVolume.setSelection(flavourFinishedVolume.text?.length ?: 0)
        setTopTab(TopTab.FLAVOUR)
    }

    private fun pushAbvToEnhancingBase() {
        val abv = lastCombinePooledAbv ?: return
        if (abv <= 0.0) return
        val value = if (isProofMode) SpiritsMath.abvToUsProof(abv) else abv
        flavourBaseAbv.setText(formatNumber(value))
        flavourBaseAbv.setSelection(flavourBaseAbv.text?.length ?: 0)
        setTopTab(TopTab.FLAVOUR)
    }

    private fun pushToDilute(source: TextView) {
        val value = parseLeadingNumber(source.text) ?: return
        if (value <= 0.0) return
        sourceVolume.setText(formatNumber(value))
        sourceVolume.setSelection(sourceVolume.text?.length ?: 0)
        setTopTab(TopTab.CUT)
        setMode(Mode.REDUCE)
    }

    private fun pushToBottler(source: TextView) {
        val value = parseLeadingNumber(source.text) ?: return
        if (value <= 0.0) return
        if (!bottlerVolumeMode) {
            bottlerVolumeMode = true
            applyBottlerMode()
        }
        bottlerFinishedVolume.setText(formatNumber(value))
        bottlerFinishedVolume.setSelection(bottlerFinishedVolume.text?.length ?: 0)
        setTopTab(TopTab.BOTTLER)
    }

    private fun pushToBottlerFromHeadline(source: TextView) {
        val value = parseLeadingNumber(source.text) ?: return
        if (value <= 0.0) return
        if (!bottlerVolumeMode) {
            bottlerVolumeMode = true
            applyBottlerMode()
        }
        bottlerFinishedVolume.setText(formatNumber(value))
        bottlerFinishedVolume.setSelection(bottlerFinishedVolume.text?.length ?: 0)
        setTopTab(TopTab.BOTTLER)
    }

    // ---------------------------------------------------------------
    // Reading helpers
    // ---------------------------------------------------------------

    private fun readVolume(field: EditText): Double? {
        val raw = field.text?.toString().orEmpty()
        val value = raw.toDoubleOrNull() ?: return null
        return if (isMassMode) {
            val kg = if (isImperialMode) SpiritsMath.usPoundsToKg(value) else value
            val abv = abvForVolumeField(field)
            SpiritsMath.kgToLitres(kg, abv)
        } else {
            if (isImperialMode) SpiritsMath.usGallonsToLiters(value) else value
        }
    }

    private fun readStrength(field: EditText): Double? {
        val raw = field.text?.toString().orEmpty()
        val value = raw.toDoubleOrNull() ?: return null
        return if (isProofMode) SpiritsMath.usProofToAbv(value) else value
    }

    private fun formatVolumePrimary(liters: Double, abv: Double): String {
        return if (isMassMode) {
            val kg = SpiritsMath.litresToKg(liters, abv)
            if (isImperialMode) {
                String.format(Locale.US, "%.3f lb", SpiritsMath.kgToUsPounds(kg))
            } else {
                String.format(Locale.US, "%.3f kg", kg)
            }
        } else {
            if (isImperialMode) {
                String.format(Locale.US, "%.3f gal", SpiritsMath.litersToUsGallons(liters))
            } else {
                String.format(Locale.US, "%.3f L", liters)
            }
        }
    }

    private fun formatVolume(liters: Double, abv: Double): CharSequence {
        val primary = formatVolumePrimary(liters, abv)
        val secondary: String = if (isMassMode) {
            if (isImperialMode) {
                String.format(Locale.US, "%.3f gal", SpiritsMath.litersToUsGallons(liters))
            } else {
                String.format(Locale.US, "%.3f L", liters)
            }
        } else {
            val kg = SpiritsMath.litresToKg(liters, abv)
            if (isImperialMode) {
                String.format(Locale.US, "%.3f lb", SpiritsMath.kgToUsPounds(kg))
            } else {
                String.format(Locale.US, "%.3f kg", kg)
            }
        }

        val full = "$primary ($secondary)"
        val span = SpannableString(full)
        val start = primary.length
        val secondaryColor = ContextCompat.getColor(this, R.color.ink_light)

        span.setSpan(
            RelativeSizeSpan(0.65f),
            start, full.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        span.setSpan(
            ForegroundColorSpan(secondaryColor),
            start, full.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        return span
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
            TopTab.BOTTLER -> {
                if (bottlerVolumeMode) calculateBottlerVolume() else calculateBottler()
            }
            TopTab.FLAVOUR -> calculateFlavour()
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
            showBatchResults(result, stock)
        } catch (_: IllegalArgumentException) {
            resultsCard.visibility = View.GONE
        }
    }

    private fun showBatchResults(result: SpiritsMath.BatchResult, stockAbv: Double) {
        resultsCard.visibility = View.VISIBLE
        resultsGroupDilution.visibility = View.VISIBLE
        resultsGroupCombine.visibility = View.GONE
        resultsGroupSolve.visibility = View.GONE
        resultsGroupBottler.visibility = View.GONE
        resultsGroupFlavour.visibility = View.GONE

        resRow1Label.text = getString(R.string.result_stock_to_charge)
        resRow2Label.text = getString(R.string.result_water_to_add)
        resWater.text = formatVolume(result.stockToChargeLiters, stockAbv)
        resFinalVol.text = formatVolume(result.waterToAddLiters, 0.0)
        resPureAlc.text = formatVolume(result.pureAlcoholLiters, 100.0)
        resContraction.text = formatVolume(result.contractionLiters, 0.0)
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
            showReduceResults(result, target)
        } catch (_: IllegalArgumentException) {
            resultsCard.visibility = View.GONE
        }
    }

    private fun showReduceResults(result: SpiritsMath.Result, targetAbv: Double) {
        resultsCard.visibility = View.VISIBLE
        resultsGroupDilution.visibility = View.VISIBLE
        resultsGroupCombine.visibility = View.GONE
        resultsGroupSolve.visibility = View.GONE
        resultsGroupBottler.visibility = View.GONE
        resultsGroupFlavour.visibility = View.GONE

        resRow1Label.text = getString(R.string.result_water_to_add)
        resRow2Label.text = getString(R.string.result_final_volume)
        resWater.text = formatVolume(result.waterToAddLiters, 0.0)
        resFinalVol.text = formatVolume(result.finalVolumeLiters, targetAbv)
        resPureAlc.text = formatVolume(result.pureAlcoholLiters, 100.0)
        resContraction.text = formatVolume(result.contractionLiters, 0.0)
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
        resultsGroupFlavour.visibility = View.GONE

        val pooledAbv = result.pooledAbv
        lastCombinePooledAbv = pooledAbv
        combHeadline.text = formatVolumePrimary(result.pooledVolumeLiters, pooledAbv) +
            " at " + formatStrength(pooledAbv)
        combPooledVol.text = formatVolume(result.pooledVolumeLiters, pooledAbv)
        combPooledAbv.text = formatStrength(pooledAbv)
        combPureAlc.text = formatVolume(result.pureAlcoholLiters, 100.0)
        combContraction.text = formatVolume(result.contractionLiters, 0.0)
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
            resultsGroupFlavour.visibility = View.GONE
            resWarning.text = getString(R.string.msg_solve_out_of_range)
            return
        }

        if (batchSize <= 0.0) {
            resultsCard.visibility = View.GONE
            return
        }

        try {
            val result = SpiritsMath.blendSolve(abvA, abvB, target, batchSize)
            showSolveResults(result, abvA, abvB, target)
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

    private fun showSolveResults(
        result: SpiritsMath.SolveResult,
        abvA: Double,
        abvB: Double,
        targetAbv: Double
    ) {
        resultsCard.visibility = View.VISIBLE
        resultsGroupDilution.visibility = View.GONE
        resultsGroupCombine.visibility = View.GONE
        resultsGroupSolve.visibility = View.VISIBLE
        resultsGroupBottler.visibility = View.GONE
        resultsGroupFlavour.visibility = View.GONE

        solveHeadline.text = formatVolumePrimary(result.batchSizeLiters, targetAbv) + " batch"
        solveSampleA.text = formatVolume(result.sampleVolumeA, abvA)
        solveSampleB.text = formatVolume(result.sampleVolumeB, abvB)
        solvePureAlc.text = formatVolume(result.pureAlcoholLiters, 100.0)
        resWarning.text = ""
    }

    // ---------------------------------------------------------------
    // Bottler calculation — Cases mode
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
            showBottlerResults(result, target, stock)
        } catch (_: IllegalArgumentException) {
            resultsCard.visibility = View.GONE
        }
    }

    private fun showBottlerResults(
        result: SpiritsMath.BottlerResult,
        targetAbv: Double,
        stockAbv: Double
    ) {
        resultsCard.visibility = View.VISIBLE
        resultsGroupDilution.visibility = View.GONE
        resultsGroupCombine.visibility = View.GONE
        resultsGroupSolve.visibility = View.GONE
        resultsGroupBottler.visibility = View.VISIBLE
        resultsGroupFlavour.visibility = View.GONE
        bottlerCasesResultsGroup.visibility = View.VISIBLE
        bottlerVolumeResultsGroup.visibility = View.GONE

        val headline = formatVolumePrimary(result.stockToChargeLiters, stockAbv) + " of " +
            formatStrength(stockAbv) + " stock"
        bottlerHeadline.text = headline

        bottlerOrderVolume.text = formatVolume(result.orderVolumeLiters, targetAbv)
        bottlerWithWastage.text = formatVolume(result.withWastageLiters, targetAbv)
        bottlerStockCharge.text = formatVolume(result.stockToChargeLiters, stockAbv)
        bottlerWaterAdd.text = formatVolume(result.waterToAddLiters, 0.0)
        bottlerContraction.text = formatVolume(result.contractionLiters, 0.0)

        val available = readVolume(bottlerAvailableStock)
        resWarning.text = when {
            available == null || available <= 0.0 -> ""
            available >= result.stockToChargeLiters -> {
                val surplus = available - result.stockToChargeLiters
                getString(R.string.bottler_msg_surplus, formatVolume(surplus, stockAbv))
            }
            else -> {
                val shortfall = result.stockToChargeLiters - available
                getString(R.string.bottler_msg_shortfall, formatVolume(shortfall, stockAbv))
            }
        }
    }

    // ---------------------------------------------------------------
    // Bottler calculation — Volume mode (reverse)
    // ---------------------------------------------------------------

    private fun calculateBottlerVolume() {
        val bottleRaw = bottlerBottleSize.text?.toString()?.toDoubleOrNull()
        val packRaw = bottlerPackSize.text?.toString()?.toIntOrNull()
        val target = readStrength(bottlerTargetAbv)
        val stock = readStrength(bottlerStockAbv)
        val wastage = bottlerWastage.text?.toString()?.toDoubleOrNull() ?: 0.0
        val finishedVolume = readVolume(bottlerFinishedVolume)

        if (bottleRaw == null || packRaw == null || target == null ||
            stock == null || finishedVolume == null) {
            resultsCard.visibility = View.GONE
            return
        }

        val bottleMl = if (bottlerInCl) bottleRaw * 10.0 else bottleRaw
        val bottleLiters = bottleMl / 1000.0
        val caseLiters = bottleLiters * packRaw

        if (bottleLiters <= 0.0 || packRaw <= 0 || caseLiters <= 0.0 ||
            target <= 0.0 || stock <= target || finishedVolume <= 0.0) {
            resultsCard.visibility = View.GONE
            return
        }

        val wholeCases = floor(finishedVolume / caseLiters).toInt()
        val wholeCasesVolume = wholeCases * caseLiters
        val residualLiters = finishedVolume - wholeCasesVolume
        val wholeBottles = wholeCases * packRaw

        // For the base spirit / water calculation, use the usable volume:
        // the whole-cases volume. The residual is not fillable into a whole case.
        // If wholeCases is 0, still show base needed for the residual.
        val usableVolume = if (wholeCases > 0) wholeCasesVolume else finishedVolume

        val stockToCharge = usableVolume * (target / stock)
        val pureAlcohol = stockToCharge * (stock / 100.0)
        val waterPer100 = SpiritsMath.waterPer100L(stock, target)
        val waterToAdd = (stockToCharge / 100.0) * waterPer100

        // Apply wastage as an informational overlay
        val wastageMultiplier = 1.0 + (wastage / 100.0)
        val withWastage = usableVolume * wastageMultiplier

        showBottlerVolumeResults(
            wholeCases = wholeCases,
            wholeBottles = wholeBottles,
            residualLiters = residualLiters,
            residualBottleCount = if (bottleLiters > 0.0) floor(residualLiters / bottleLiters).toInt() else 0,
            stockToCharge = stockToCharge,
            waterToAdd = waterToAdd,
            pureAlcohol = pureAlcohol,
            withWastage = withWastage,
            targetAbv = target,
            stockAbv = stock,
            usableVolume = usableVolume
        )
    }

    private fun showBottlerVolumeResults(
        wholeCases: Int,
        wholeBottles: Int,
        residualLiters: Double,
        residualBottleCount: Int,
        stockToCharge: Double,
        waterToAdd: Double,
        pureAlcohol: Double,
        withWastage: Double,
        targetAbv: Double,
        stockAbv: Double,
        usableVolume: Double
    ) {
        resultsCard.visibility = View.VISIBLE
        resultsGroupDilution.visibility = View.GONE
        resultsGroupCombine.visibility = View.GONE
        resultsGroupSolve.visibility = View.GONE
        resultsGroupBottler.visibility = View.VISIBLE
        resultsGroupFlavour.visibility = View.GONE
        bottlerCasesResultsGroup.visibility = View.GONE
        bottlerVolumeResultsGroup.visibility = View.VISIBLE

        val headline = formatVolumePrimary(stockToCharge, stockAbv) + " of " +
            formatStrength(stockAbv) + " stock"
        bottlerHeadline.text = headline

        bottlerWholeCases.text = wholeCases.toString()
        bottlerWholeBottles.text = wholeBottles.toString()

        val residualDisplay = formatVolume(residualLiters, targetAbv)
        val suffix = if (residualBottleCount > 0) " · $residualBottleCount bottles" else ""
        bottlerResidual.text = "$residualDisplay$suffix"

        resWarning.text = ""
    }

    // ---------------------------------------------------------------
    // Enhancing calculation
    // ---------------------------------------------------------------

    private fun calculateFlavour() {
        val firstValue = readVolume(flavourFinishedVolume)
        val target = readStrength(flavourTargetAbv)
        val base = readStrength(flavourBaseAbv)

        if (firstValue == null || target == null || base == null ||
            firstValue <= 0.0 || target <= 0.0 || base <= 0.0) {
            resultsCard.visibility = View.GONE
            return
        }

        val additives = (0 until 3).map { i ->
            SpiritsMath.AdditiveInput(
                name = flavourAddName[i].text?.toString().orEmpty(),
                dose = flavourAddDose[i].text?.toString()?.toDoubleOrNull() ?: 0.0,
                unit = flavourAddUnit[i],
                abv = readStrength(flavourAddAbv[i]) ?: 0.0,
                density = flavourAddDensity[i].text?.toString()?.toDoubleOrNull() ?: 1.0
            )
        }

        try {
            val result = if (enhBaseMode) {
                SpiritsMath.calculateFlavourFromBase(firstValue, target, base, additives)
            } else {
                SpiritsMath.calculateFlavour(firstValue, target, base, additives)
            }
            showFlavourResults(result, target, base)
        } catch (_: IllegalArgumentException) {
            resultsCard.visibility = View.GONE
        }
    }

    private fun showFlavourResults(
        result: SpiritsMath.FlavourResult,
        targetAbv: Double,
        baseAbv: Double
    ) {
        resultsCard.visibility = View.VISIBLE
        resultsGroupDilution.visibility = View.GONE
        resultsGroupCombine.visibility = View.GONE
        resultsGroupSolve.visibility = View.GONE
        resultsGroupBottler.visibility = View.GONE
        resultsGroupFlavour.visibility = View.VISIBLE

        val effectiveTarget = if (result.finalVolumeLiters > 0.0)
            (result.pureAlcoholLiters / result.finalVolumeLiters) * 100.0 else targetAbv
        flavHeadline.text = formatVolumePrimary(result.finalVolumeLiters, effectiveTarget) +
            " at " + formatStrength(effectiveTarget)

        flavBaseSpirit.text = formatVolume(result.baseSpiritLiters, baseAbv)
        flavWaterAdd.text = formatVolume(result.waterToAddLiters, 0.0)
        flavAdd1Vol.text = formatVolume(
            result.additiveVolumesLiters.getOrElse(0) { 0.0 },
            readStrength(flavourAddAbv[0]) ?: 0.0
        )
        flavAdd2Vol.text = formatVolume(
            result.additiveVolumesLiters.getOrElse(1) { 0.0 },
            readStrength(flavourAddAbv[1]) ?: 0.0
        )
        flavAdd3Vol.text = formatVolume(
            result.additiveVolumesLiters.getOrElse(2) { 0.0 },
            readStrength(flavourAddAbv[2]) ?: 0.0
        )
        flavPureAlc.text = formatVolume(result.pureAlcoholLiters, 100.0)
        resWarning.text = ""
    }

    // ---------------------------------------------------------------
    // Coming soon (reserved, currently unused)
    // ---------------------------------------------------------------

    private fun showComingSoon() {
        resultsCard.visibility = View.VISIBLE
        resultsGroupDilution.visibility = View.GONE
        resultsGroupCombine.visibility = View.GONE
        resultsGroupSolve.visibility = View.GONE
        resultsGroupBottler.visibility = View.GONE
        resultsGroupFlavour.visibility = View.GONE
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
