package com.neatly.app

import android.app.AlertDialog
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
    private enum class Mode { BATCH, REDUCE, BLEND_COMBINE, BLEND_SOLVE, FROM_ORDER }
    private enum class Country { GLOBAL, EU_UK, US, RSA }
    private enum class Product { BRANDY, WHISKY, RUM, VODKA }

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
    private lateinit var groupFromOrder: View
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
    private lateinit var subFromOrder: TextView

    // From Order inputs
    private lateinit var orderModeRatio: TextView
    private lateinit var orderModeCap: TextView
    private lateinit var orderModeHint: TextView
    private lateinit var labelOrderVolume: TextView
    private lateinit var orderVolume: EditText
    private lateinit var unitOrderVolume: TextView
    private lateinit var orderTargetAbv: EditText
    private lateinit var unitOrderTargetStrength: TextView
    private lateinit var orderStrongerAbv: EditText
    private lateinit var unitOrderStronger: TextView
    private lateinit var orderLighterAbv: EditText
    private lateinit var unitOrderLighter: TextView
    private lateinit var orderRatioGroup: View
    private lateinit var orderRatio: EditText
    private lateinit var orderCapGroup: View
    private lateinit var orderCapStronger: TextView
    private lateinit var orderCapLighter: TextView
    private lateinit var labelCapVolume: TextView
    private lateinit var orderCapVolume: EditText
    private lateinit var unitOrderCapVolume: TextView

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
    private lateinit var labelBottlerFinishedVolume: TextView
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
    private lateinit var resultsGroupFromOrder: View
    private lateinit var resultsGroupSolve: View
    private lateinit var resultsGroupBottler: View
    private lateinit var resultsGroupFlavour: View
    private lateinit var contractionRowDilution: View
    private lateinit var contractionRowCombine: View
    private lateinit var contractionRowBottler: View
    private lateinit var bottlerCasesResultsGroup: View
    private lateinit var bottlerVolumeResultsGroup: View
    private lateinit var bottlerAngelsShareRow: View
    private lateinit var bottlerAngelsShare: TextView

    private lateinit var resRow1Label: TextView
    private lateinit var resRow2Label: TextView
    private lateinit var resWater: TextView
    private lateinit var resFinalVol: TextView
    private lateinit var resPureAlc: TextView
    private lateinit var resContraction: TextView

    private lateinit var combPooledVol: TextView
    private lateinit var combPooledAbv: TextView
    private lateinit var combPureAlc: TextView
    private lateinit var combContraction: TextView

    private lateinit var orderCompoundVolume: TextView
    private lateinit var orderStrongerUsed: TextView
    private lateinit var orderLighterUsed: TextView
    private lateinit var orderLowestProof: TextView
    private lateinit var orderHighestProof: TextView
    private lateinit var orderMinimumRequired: EditText
    private lateinit var orderComplianceInfo: TextView
    private lateinit var orderWaterAdd: TextView
    private lateinit var orderPureAlc: TextView
    private lateinit var orderFinalTotal: TextView

    private lateinit var labelSolveBatchSize: TextView
    private lateinit var solveSampleA: TextView
    private lateinit var solveSampleB: TextView
    private lateinit var solvePureAlc: TextView
    private lateinit var solveFinalTotal: TextView

    private lateinit var bottlerOrderVolume: TextView
    private lateinit var bottlerWithWastage: TextView
    private lateinit var bottlerStockCharge: TextView
    private lateinit var bottlerWaterAdd: TextView
    private lateinit var bottlerContraction: TextView
    private lateinit var bottlerWholeCases: TextView
    private lateinit var bottlerWholeBottles: TextView
    private lateinit var bottlerResidual: TextView
    private lateinit var bottlerTotalBottled: TextView

    private lateinit var flavBaseSpirit: TextView
    private lateinit var flavWaterAdd: TextView
    private lateinit var flavAdd1VolLabel: TextView
    private lateinit var flavAdd1Vol: TextView
    private lateinit var flavAdd2VolLabel: TextView
    private lateinit var flavAdd2Vol: TextView
    private lateinit var flavAdd3VolLabel: TextView
    private lateinit var flavAdd3Vol: TextView
    private lateinit var flavPureAlc: TextView
    private lateinit var flavFinalTotal: TextView

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
    private lateinit var pushOrderToBottlerVol: TextView

    private lateinit var resWarning: TextView
    private lateinit var footerText: TextView
    private lateinit var wordmarkText: TextView
    private lateinit var updateBanner: MaterialCardView

    // Export UI
    private lateinit var exportContainer: View
    private lateinit var btnExportPdf: View
    private lateinit var btnExportTsv: View

    // Export Data Caches
    private var cutExportData: Map<String, String>? = null
    private var bottlerExportData: Map<String, String>? = null
    private var enhancingExportData: Map<String, String>? = null

    // Header pills
    private lateinit var unitAbv: TextView
    private lateinit var unitProof: TextView
    private lateinit var unitMetric: TextView
    private lateinit var unitImperial: TextView
    private lateinit var unitVol: TextView
    private lateinit var unitMass: TextView
    private lateinit var productBrandy: TextView
    private lateinit var productWhisky: TextView
    private lateinit var productRum: TextView
    private lateinit var productVodka: TextView

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
    private var orderRatioMode = true
    private var orderCapOnStronger = true
    private var suppressSync = false
    private var selectedCountry: Country = Country.GLOBAL
    private var selectedProduct: Product = Product.BRANDY
    private var suppressMinimumChange = false

    private var lastCombinePooledAbv: Double? = null
    private var lastOrderVolume: Double? = null

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
        wireProductPills()
        wireTopTabs()
        wireSubTabs()
        wireOrderMode()
        wireBottlerMode()
        wireBottlerPresets()
        wireBottlerUnitToggle()
        wireBottlerAngelsCheckbox()
        wireFlavourUnitPills()
        wireEnhancingMode()
        wirePushButtons()
        wireComplianceInfo()
        wireUpdateBanner()
        wireExportButtons()

        updateToggleVisuals()
        updateUnitLabels()
        applyMassVisibility()
        applyEnhancingMode()
        applyOrderMode()
        applyBottlerMode()
        applyMode()
        applyTopTab()
        applyComplianceDefault()
        updateDynamicLabels()
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

        cutCard = findViewById(R.id.cutCard)
        groupSingle = findViewById(R.id.groupSingle)
        groupCombine = findViewById(R.id.groupCombine)
        groupSolve = findViewById(R.id.groupSolve)
        groupFromOrder = findViewById(R.id.groupFromOrder)
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
        subFromOrder = findViewById(R.id.subFromOrder)

        orderModeRatio = findViewById(R.id.orderModeRatio)
        orderModeCap = findViewById(R.id.orderModeCap)
        orderModeHint = findViewById(R.id.orderModeHint)
        labelOrderVolume = findViewById(R.id.labelOrderVolume)
        orderVolume = findViewById(R.id.orderVolume)
        unitOrderVolume = findViewById(R.id.unitOrderVolume)
        orderTargetAbv = findViewById(R.id.orderTargetAbv)
        unitOrderTargetStrength = findViewById(R.id.unitOrderTargetStrength)
        orderStrongerAbv = findViewById(R.id.orderStrongerAbv)
        unitOrderStronger = findViewById(R.id.unitOrderStronger)
        orderLighterAbv = findViewById(R.id.orderLighterAbv)
        unitOrderLighter = findViewById(R.id.unitOrderLighter)
        orderRatioGroup = findViewById(R.id.orderRatioGroup)
        orderRatio = findViewById(R.id.orderRatio)
        orderCapGroup = findViewById(R.id.orderCapGroup)
        orderCapStronger = findViewById(R.id.orderCapStronger)
        orderCapLighter = findViewById(R.id.orderCapLighter)
        labelCapVolume = findViewById(R.id.labelCapVolume)
        orderCapVolume = findViewById(R.id.orderCapVolume)
        unitOrderCapVolume = findViewById(R.id.unitOrderCapVolume)

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
        labelBottlerFinishedVolume = findViewById(R.id.labelBottlerFinishedVolume)
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

        resultsCard = findViewById(R.id.resultsCard)
        resultsGroupDilution = findViewById(R.id.resultsGroupDilution)
        resultsGroupCombine = findViewById(R.id.resultsGroupCombine)
        resultsGroupFromOrder = findViewById(R.id.resultsGroupFromOrder)
        resultsGroupSolve = findViewById(R.id.resultsGroupSolve)
        resultsGroupBottler = findViewById(R.id.resultsGroupBottler)
        resultsGroupFlavour = findViewById(R.id.resultsGroupFlavour)
        contractionRowDilution = findViewById(R.id.contractionRowDilution)
        contractionRowCombine = findViewById(R.id.contractionRowCombine)
        contractionRowBottler = findViewById(R.id.contractionRowBottler)
        bottlerCasesResultsGroup = findViewById(R.id.bottlerCasesResultsGroup)
        bottlerVolumeResultsGroup = findViewById(R.id.bottlerVolumeResultsGroup)
        bottlerAngelsShareRow = findViewById(R.id.bottlerAngelsShareRow)
        bottlerAngelsShare = findViewById(R.id.bottlerAngelsShare)

        resRow1Label = findViewById(R.id.resRow1Label)
        resRow2Label = findViewById(R.id.resRow2Label)
        resWater = findViewById(R.id.resWater)
        resFinalVol = findViewById(R.id.resFinalVol)
        resPureAlc = findViewById(R.id.resPureAlc)
        resContraction = findViewById(R.id.resContraction)

        combPooledVol = findViewById(R.id.combPooledVol)
        combPooledAbv = findViewById(R.id.combPooledAbv)
        combPureAlc = findViewById(R.id.combPureAlc)
        combContraction = findViewById(R.id.combContraction)

        orderCompoundVolume = findViewById(R.id.orderCompoundVolume)
        orderStrongerUsed = findViewById(R.id.orderStrongerUsed)
        orderLighterUsed = findViewById(R.id.orderLighterUsed)
        orderLowestProof = findViewById(R.id.orderLowestProof)
        orderHighestProof = findViewById(R.id.orderHighestProof)
        orderMinimumRequired = findViewById(R.id.orderMinimumRequired)
        orderComplianceInfo = findViewById(R.id.orderComplianceInfo)
        orderWaterAdd = findViewById(R.id.orderWaterAdd)
        orderPureAlc = findViewById(R.id.orderPureAlc)
        orderFinalTotal = findViewById(R.id.orderFinalTotal)

        labelSolveBatchSize = findViewById(R.id.labelSolveBatchSize)
        solveSampleA = findViewById(R.id.solveSampleA)
        solveSampleB = findViewById(R.id.solveSampleB)
        solvePureAlc = findViewById(R.id.solvePureAlc)
        solveFinalTotal = findViewById(R.id.solveFinalTotal)

        bottlerOrderVolume = findViewById(R.id.bottlerOrderVolume)
        bottlerWithWastage = findViewById(R.id.bottlerWithWastage)
        bottlerStockCharge = findViewById(R.id.bottlerStockCharge)
        bottlerWaterAdd = findViewById(R.id.bottlerWaterAdd)
        bottlerContraction = findViewById(R.id.bottlerContraction)
        bottlerWholeCases = findViewById(R.id.bottlerWholeCases)
        bottlerWholeBottles = findViewById(R.id.bottlerWholeBottles)
        bottlerResidual = findViewById(R.id.bottlerResidual)
        bottlerTotalBottled = findViewById(R.id.bottlerTotalBottled)

        flavBaseSpirit = findViewById(R.id.flavBaseSpirit)
        flavWaterAdd = findViewById(R.id.flavWaterAdd)
        flavAdd1VolLabel = findViewById(R.id.flavAdd1VolLabel)
        flavAdd1Vol = findViewById(R.id.flavAdd1Vol)
        flavAdd2VolLabel = findViewById(R.id.flavAdd2VolLabel)
        flavAdd2Vol = findViewById(R.id.flavAdd2Vol)
        flavAdd3VolLabel = findViewById(R.id.flavAdd3VolLabel)
        flavAdd3Vol = findViewById(R.id.flavAdd3Vol)
        flavPureAlc = findViewById(R.id.flavPureAlc)
        flavFinalTotal = findViewById(R.id.flavFinalTotal)

        pushDiluteFinalToEnhVol = findViewById(R.id.pushDiluteFinalToEnhVol)
        pushDiluteFinalToBottlerVol = findViewById(R.id.pushDiluteFinalToBottlerVol)
        pushCombineVolToEnhVol = findViewById(R.id.pushCombineVolToEnhVol)
        pushCombineVolToBottlerVol = findViewById(R.id.pushCombineVolToBottlerVol)
        pushCombineAbvToEnhStrength = findViewById(R.id.pushCombineAbvToEnhStrength)
        pushBottlerWastageToEnhVol = findViewById(R.id.pushBottlerWastageToEnhVol)
        pushBottlerStockToDiluteVol = findViewById(R.id.pushBottlerStockToDiluteVol)
        pushFlavBaseToDiluteVol = findViewById(R.id.pushFlavBaseToDiluteVol)
        pushFlavHeadlineToBottlerVol = findViewById(R.id.pushFlavHeadlineToBottlerVol)
        pushOrderToBottlerVol = findViewById(R.id.pushOrderToBottlerVol)

        resWarning = findViewById(R.id.resWarning)
        footerText = findViewById(R.id.footerText)
        wordmarkText = findViewById(R.id.wordmarkText)
        updateBanner = findViewById(R.id.updateBanner)

        exportContainer = findViewById(R.id.exportContainer)
        btnExportPdf = findViewById(R.id.btnExportPdf)
        btnExportTsv = findViewById(R.id.btnExportTsv)

        unitAbv = findViewById(R.id.unitAbv)
        unitProof = findViewById(R.id.unitProof)
        unitMetric = findViewById(R.id.unitMetric)
        unitImperial = findViewById(R.id.unitImperial)
        unitVol = findViewById(R.id.unitVol)
        unitMass = findViewById(R.id.unitMass)
        productBrandy = findViewById(R.id.productBrandy)
        productWhisky = findViewById(R.id.productWhisky)
        productRum = findViewById(R.id.productRum)
        productVodka = findViewById(R.id.productVodka)

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
            orderVolume, orderTargetAbv, orderStrongerAbv, orderLighterAbv,
            orderRatio, orderCapVolume,
            bottlerBottleSize, bottlerPackSize, bottlerCases,
            bottlerFinishedVolume,
            bottlerTargetAbv, bottlerStockAbv, bottlerAvailableStock,
            bottlerWastage, bottlerYearsBarrel, bottlerAnnualLoss,
            flavourFinishedVolume, flavourTargetAbv, flavourBaseAbv,
            flavourAddName[0], flavourAddDose[0], flavourAddAbv[0], flavourAddDensity[0],
            flavourAddName[1], flavourAddDose[1], flavourAddAbv[1], flavourAddDensity[1],
            flavourAddName[2], flavourAddDose[2], flavourAddAbv[2], flavourAddDensity[2]
        ).forEach { it.addTextChangedListener(watcher) }

        orderMinimumRequired.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (suppressMinimumChange) return
                updateComplianceNote()
            }
        })
    }

    private fun wireSharedStrengths() {
        val baseStrengthFields = listOf(sourceAbv, bottlerStockAbv, flavourBaseAbv)
        val targetStrengthFields = listOf(targetAbv, bottlerTargetAbv, flavourTargetAbv, orderTargetAbv)
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

    private fun wireUnitToggles() {
        unitAbv.setOnClickListener { setStrengthMode(proof = false) }
        unitProof.setOnClickListener { setStrengthMode(proof = true) }
        unitMetric.setOnClickListener { setMeasurementMode(imperial = false) }
        unitImperial.setOnClickListener { setMeasurementMode(imperial = true) }
        unitVol.setOnClickListener { setMassMode(mass = false) }
        unitMass.setOnClickListener { setMassMode(mass = true) }
    }

    private fun wireProductPills() {
        productBrandy.setOnClickListener { setProduct(Product.BRANDY) }
        productWhisky.setOnClickListener { setProduct(Product.WHISKY) }
        productRum.setOnClickListener { setProduct(Product.RUM) }
        productVodka.setOnClickListener { setProduct(Product.VODKA) }
    }

    private fun setProduct(product: Product) {
        if (product == selectedProduct) return
        selectedProduct = product
        paintToggle(productBrandy, active = product == Product.BRANDY)
        paintToggle(productWhisky, active = product == Product.WHISKY)
        paintToggle(productRum, active = product == Product.RUM)
        paintToggle(productVodka, active = product == Product.VODKA)
        applyComplianceDefault()
        recalculate()
    }

    private fun setCountry(country: Country) {
        if (country == selectedCountry) return
        selectedCountry = country
        applyComplianceDefault()
        updateComplianceNote()
    }

    private fun defaultMinimumFor(country: Country, product: Product): Double? {
        return when (country) {
            Country.RSA -> when (product) {
                Product.BRANDY -> 30.0
                Product.WHISKY -> 25.0
                else -> null
            }
            Country.US -> when (product) {
                Product.WHISKY -> 20.0
                else -> null
            }
            Country.EU_UK, Country.GLOBAL -> null
        }
    }

    private fun applyComplianceDefault() {
        val default = defaultMinimumFor(selectedCountry, selectedProduct)
        suppressMinimumChange = true
        try {
            if (default != null) {
                orderMinimumRequired.setText(formatNumber(default))
            } else {
                orderMinimumRequired.setText("")
            }
            orderMinimumRequired.setSelection(orderMinimumRequired.text?.length ?: 0)
        } finally {
            suppressMinimumChange = false
        }
        updateComplianceNote()
    }

    private fun updateComplianceNote() {
        val lowText = orderLowestProof.text?.toString().orEmpty()
        val lowValue = parseLeadingNumber(lowText)
        val minRaw = orderMinimumRequired.text?.toString().orEmpty()
        val minimum = minRaw.toDoubleOrNull()

        if (minimum == null || minimum <= 0.0 || lowValue == null) {
            resWarning.text = ""
            return
        }

        if (lowValue < minimum) {
            resWarning.text = getString(
                R.string.msg_below_minimum,
                String.format(Locale.US, "%.1f %%", minimum)
            )
        } else {
            resWarning.text = ""
        }
    }

    private fun wireComplianceInfo() {
        orderComplianceInfo.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.dialog_compliance_title)
                .setMessage(R.string.dialog_compliance_body)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
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
            scaleField(orderTargetAbv, factor)
            scaleField(orderStrongerAbv, factor)
            scaleField(orderLighterAbv, factor)
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
        scaleField(orderVolume, factor)
        scaleField(orderCapVolume, factor)
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
            orderVolume, orderCapVolume,
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
        updateDynamicLabels()
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
            orderVolume -> readStrength(orderTargetAbv) ?: 40.0
            orderCapVolume -> if (orderCapOnStronger) {
                readStrength(orderStrongerAbv) ?: 40.0
            } else {
                readStrength(orderLighterAbv) ?: 40.0
            }
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

        unitOrderVolume.text = volumeOrMassUnit
        unitOrderTargetStrength.text = strengthUnit
        unitOrderStronger.text = strengthUnit
        unitOrderLighter.text = strengthUnit
        unitOrderCapVolume.text = volumeOrMassUnit

        bottlerUnitBottleSize.text = bottleUnit
        bottlerUnitTargetStrength.text = strengthUnit
        bottlerUnitStockStrength.text = strengthUnit
        bottlerUnitAvailableStock.text = volumeOrMassUnit
        bottlerUnitFinishedVolume.text = volumeOrMassUnit

        flavourUnitTargetStrength.text = strengthUnit
        flavourUnitBaseStrength.text = strengthUnit
        flavourUnitVolume.text = volumeOrMassUnit
    }

    private fun updateDynamicLabels() {
        // Cut
        when (mode) {
            Mode.BATCH -> labelVolume.text = getString(if (isMassMode) R.string.label_finished_volume_mass else R.string.label_finished_volume)
            Mode.REDUCE -> labelVolume.text = getString(if (isMassMode) R.string.label_stock_volume_mass else R.string.label_stock_volume)
            else -> {}
        }
        labelSolveBatchSize.text = getString(if (isMassMode) R.string.label_batch_size_mass else R.string.label_batch_size)
        labelOrderVolume.text = getString(if (isMassMode) R.string.label_order_mass else R.string.label_order_volume)
        labelCapVolume.text = getString(if (isMassMode) R.string.label_cap_mass else R.string.label_cap_volume)

        // Enhancing
        enhModeFinished.text = getString(if (isMassMode) R.string.enh_mode_finished_mass else R.string.enh_mode_finished)
        enhModeBase.text = getString(if (isMassMode) R.string.enh_mode_base_mass else R.string.enh_mode_base)
        if (enhBaseMode) {
            enhFirstLabel.text = getString(if (isMassMode) R.string.label_base_volume_enh_mass else R.string.label_base_volume_enh)
        } else {
            enhFirstLabel.text = getString(if (isMassMode) R.string.label_finished_volume_mass else R.string.label_finished_volume)
        }

        // Bottler
        labelBottlerFinishedVolume.text = getString(if (isMassMode) R.string.bottler_label_finished_mass else R.string.bottler_label_finished_volume)
        bottlerModeVolume.text = getString(if (isMassMode) R.string.bottler_mode_mass else R.string.bottler_mode_volume)
    }

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

    private fun wireSubTabs() {
        subBatch.setOnClickListener { setMode(Mode.BATCH) }
        subReduce.setOnClickListener { setMode(Mode.REDUCE) }
        subBlend.setOnClickListener { setMode(Mode.BLEND_COMBINE) }
        subCombine.setOnClickListener { setMode(Mode.BLEND_COMBINE) }
        subSolve.setOnClickListener { setMode(Mode.BLEND_SOLVE) }
        subFromOrder.setOnClickListener { setMode(Mode.FROM_ORDER) }
    }

    private fun setMode(newMode: Mode) {
        mode = newMode
        applyMode()
        recalculate()
    }

    private fun applyMode() {
        paintToggle(subBatch, active = mode == Mode.BATCH)
        paintToggle(subReduce, active = mode == Mode.REDUCE)
        paintToggle(
            subBlend,
            active = mode == Mode.BLEND_COMBINE || mode == Mode.BLEND_SOLVE || mode == Mode.FROM_ORDER
        )

        val blendActive = mode == Mode.BLEND_COMBINE || mode == Mode.BLEND_SOLVE || mode == Mode.FROM_ORDER
        blendSubTabs.visibility = if (blendActive) View.VISIBLE else View.GONE
        paintToggle(subCombine, active = mode == Mode.BLEND_COMBINE)
        paintToggle(subSolve, active = mode == Mode.BLEND_SOLVE)
        paintToggle(subFromOrder, active = mode == Mode.FROM_ORDER)

        groupSingle.visibility = if (mode == Mode.BATCH || mode == Mode.REDUCE) View.VISIBLE else View.GONE
        groupCombine.visibility = if (mode == Mode.BLEND_COMBINE) View.VISIBLE else View.GONE
        groupSolve.visibility = if (mode == Mode.BLEND_SOLVE) View.VISIBLE else View.GONE
        groupFromOrder.visibility = if (mode == Mode.FROM_ORDER) View.VISIBLE else View.GONE

        groupTarget.visibility =
            if (mode == Mode.BLEND_COMBINE || mode == Mode.FROM_ORDER) View.GONE else View.VISIBLE

        pushDiluteFinalToEnhVol.visibility =
            if (mode == Mode.REDUCE) View.VISIBLE else View.GONE

        pushDiluteFinalToBottlerVol.visibility =
            if (mode == Mode.REDUCE) View.VISIBLE else View.GONE

        when (mode) {
            Mode.BATCH -> {
                labelSourceStrength.text = getString(R.string.label_stock_strength)
                modeHint.text = getString(R.string.hint_mode_formulate)
                modeHint.visibility = View.VISIBLE
            }
            Mode.REDUCE -> {
                labelSourceStrength.text = getString(R.string.label_stock_strength)
                modeHint.text = getString(R.string.hint_mode_dilute)
                modeHint.visibility = View.VISIBLE
            }
            Mode.BLEND_COMBINE, Mode.BLEND_SOLVE, Mode.FROM_ORDER -> {
                modeHint.visibility = View.GONE
            }
        }

        if (mode != Mode.BLEND_SOLVE) {
            targetHint.visibility = View.GONE
        }
        updateDynamicLabels()
    }

    private fun wireOrderMode() {
        orderModeRatio.setOnClickListener { setOrderMode(ratio = true) }
        orderModeCap.setOnClickListener { setOrderMode(ratio = false) }
        orderCapStronger.setOnClickListener { setOrderCapOn(stronger = true) }
        orderCapLighter.setOnClickListener { setOrderCapOn(stronger = false) }
    }

    private fun setOrderMode(ratio: Boolean) {
        if (ratio == orderRatioMode) return
        orderRatioMode = ratio
        applyOrderMode()
        recalculate()
    }

    private fun setOrderCapOn(stronger: Boolean) {
        if (stronger == orderCapOnStronger) return
        orderCapOnStronger = stronger
        applyOrderMode()
        recalculate()
    }

    private fun applyOrderMode() {
        paintToggle(orderModeRatio, active = orderRatioMode)
        paintToggle(orderModeCap, active = !orderRatioMode)

        orderRatioGroup.visibility = if (orderRatioMode) View.VISIBLE else View.GONE
        orderCapGroup.visibility = if (orderRatioMode) View.GONE else View.VISIBLE

        paintToggle(orderCapStronger, active = orderCapOnStronger)
        paintToggle(orderCapLighter, active = !orderCapOnStronger)

        if (orderRatioMode) {
            orderModeHint.text = getString(R.string.hint_order_ratio)
        } else {
            orderModeHint.text = getString(R.string.hint_order_cap)
        }
    }

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
        updateDynamicLabels()
    }

    private fun wireBottlerPresets() {
        bottlerPresetGlobal.setOnClickListener {
            applyBottlerPreset(750.0, "12", bottlerPresetGlobal)
            setCountry(Country.GLOBAL)
        }
        bottlerPresetEu.setOnClickListener {
            applyBottlerPreset(700.0, "6", bottlerPresetEu)
            setCountry(Country.EU_UK)
        }
        bottlerPresetUs.setOnClickListener {
            applyBottlerPreset(750.0, "12", bottlerPresetUs)
            setCountry(Country.US)
        }
        bottlerPresetRsa.setOnClickListener {
            applyBottlerPreset(750.0, "12", bottlerPresetRsa)
            setCountry(Country.RSA)
        }
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
            enhModeHint.text = getString(R.string.hint_enh_base)
        } else {
            enhModeHint.text = getString(R.string.hint_enh_finished)
        }
        updateDynamicLabels()
    }

    private fun wirePushButtons() {
        pushDiluteFinalToEnhVol.setOnClickListener { pushToEnhancing(resFinalVol) }
        pushCombineVolToEnhVol.setOnClickListener { pushToEnhancing(combPooledVol) }
        pushBottlerWastageToEnhVol.setOnClickListener { pushToEnhancing(bottlerWithWastage) }
        pushCombineAbvToEnhStrength.setOnClickListener { pushAbvToEnhancingBase() }
        pushBottlerStockToDiluteVol.setOnClickListener { pushToDilute(bottlerStockCharge) }
        pushFlavBaseToDiluteVol.setOnClickListener { pushToDilute(flavBaseSpirit) }
        pushDiluteFinalToBottlerVol.setOnClickListener { pushToBottler(resFinalVol) }
        pushCombineVolToBottlerVol.setOnClickListener { pushToBottler(combPooledVol) }
        pushFlavHeadlineToBottlerVol.setOnClickListener { pushToBottler(flavFinalTotal) }
        pushOrderToBottlerVol.setOnClickListener { pushOrderVolumeToBottler() }
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

    private fun pushOrderVolumeToBottler() {
        val value = lastOrderVolume ?: return
        if (value <= 0.0) return
        if (!bottlerVolumeMode) {
            bottlerVolumeMode = true
            applyBottlerMode()
        }
        bottlerFinishedVolume.setText(formatNumber(value))
        bottlerFinishedVolume.setSelection(bottlerFinishedVolume.text?.length ?: 0)
        setTopTab(TopTab.BOTTLER)
    }

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

    private fun formatAdditiveVolume(liters: Double, kg: Double): CharSequence {
        val primary: String
        val secondary: String

        if (isMassMode) {
            primary = if (isImperialMode) String.format(Locale.US, "%.3f lb", SpiritsMath.kgToUsPounds(kg))
                      else String.format(Locale.US, "%.3f kg", kg)
            secondary = if (isImperialMode) String.format(Locale.US, "%.3f gal", SpiritsMath.litersToUsGallons(liters))
                        else String.format(Locale.US, "%.3f L", liters)
        } else {
            primary = if (isImperialMode) String.format(Locale.US, "%.3f gal", SpiritsMath.litersToUsGallons(liters))
                      else String.format(Locale.US, "%.3f L", liters)
            secondary = if (isImperialMode) String.format(Locale.US, "%.3f lb", SpiritsMath.kgToUsPounds(kg))
                        else String.format(Locale.US, "%.3f kg", kg)
        }

        val full = "$primary ($secondary)"
        val span = SpannableString(full)
        span.setSpan(
            RelativeSizeSpan(0.65f),
            primary.length, full.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        span.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(this, R.color.ink_light)),
            primary.length, full.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return span
    }

    private fun formatContraction(liters: Double): String {
        return if (isMassMode) {
            if (isImperialMode) {
                String.format(Locale.US, "%.3f gal", SpiritsMath.litersToUsGallons(liters))
            } else {
                String.format(Locale.US, "%.3f L", liters)
            }
        } else {
            if (isImperialMode) {
                String.format(Locale.US, "%.3f gal", SpiritsMath.litersToUsGallons(liters))
            } else {
                String.format(Locale.US, "%.3f L", liters)
            }
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
        when (topTab) {
            TopTab.CUT -> when (mode) {
                Mode.BATCH -> calculateBatch()
                Mode.REDUCE -> calculateReduce()
                Mode.BLEND_COMBINE -> calculateCombine()
                Mode.BLEND_SOLVE -> calculateSolve()
                Mode.FROM_ORDER -> calculateFromOrder()
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
            updateExportVisibility()
            return
        }

        try {
            val result = SpiritsMath.calculateBatch(finishedVolume, stock, target)
            showBatchResults(result, stock)
        } catch (_: IllegalArgumentException) {
            resultsCard.visibility = View.GONE
            updateExportVisibility()
        }
    }

    private fun showBatchResults(result: SpiritsMath.BatchResult, stockAbv: Double) {
        resultsCard.visibility = View.VISIBLE
        resultsGroupDilution.visibility = View.VISIBLE
        resultsGroupCombine.visibility = View.GONE
        resultsGroupFromOrder.visibility = View.GONE
        resultsGroupSolve.visibility = View.GONE
        resultsGroupBottler.visibility = View.GONE
        resultsGroupFlavour.visibility = View.GONE

        resRow1Label.text = getString(R.string.result_stock_to_charge)
        resRow2Label.text = getString(R.string.result_water_to_add)
        resWater.text = formatVolume(result.stockToChargeLiters, stockAbv)
        resFinalVol.text = formatVolume(result.waterToAddLiters, 0.0)
        resPureAlc.text = formatVolume(result.pureAlcoholLiters, 100.0)
        resContraction.text = formatContraction(result.contractionLiters)
        resWarning.text = ""

        cutExportData = mapOf(
            "Cut_Mode" to "Formulate",
            "Cut_Target_Vol" to formatVolume(result.finalVolumeLiters, readStrength(targetAbv) ?: stockAbv).toString(),
            "Cut_Base_Strength" to formatStrength(stockAbv),
            "Cut_Target_Strength" to formatStrength(readStrength(targetAbv) ?: 0.0),
            "Cut_Base_Spirit" to formatVolume(result.stockToChargeLiters, stockAbv).toString(),
            "Cut_Water" to formatVolume(result.waterToAddLiters, 0.0).toString(),
            "Cut_Pure_Alcohol" to formatVolume(result.pureAlcoholLiters, 100.0).toString(),
            "Cut_Contraction" to formatContraction(result.contractionLiters)
        )
        updateExportVisibility()
    }

    private fun calculateReduce() {
        val volume = readVolume(sourceVolume)
        val source = readStrength(sourceAbv)
        val target = readStrength(targetAbv)

        if (volume == null || source == null || target == null ||
            target >= source || target <= 0.0 || source <= 0.0 || volume <= 0.0) {
            resultsCard.visibility = View.GONE
            updateExportVisibility()
            return
        }

        try {
            val result = SpiritsMath.calculate(volume, source, target)
            showReduceResults(result, target)
        } catch (_: IllegalArgumentException) {
            resultsCard.visibility = View.GONE
            updateExportVisibility()
        }
    }

    private fun showReduceResults(result: SpiritsMath.Result, targetAbv: Double) {
        resultsCard.visibility = View.VISIBLE
        resultsGroupDilution.visibility = View.VISIBLE
        resultsGroupCombine.visibility = View.GONE
        resultsGroupFromOrder.visibility = View.GONE
        resultsGroupSolve.visibility = View.GONE
        resultsGroupBottler.visibility = View.GONE
        resultsGroupFlavour.visibility = View.GONE

        resRow1Label.text = getString(R.string.result_water_to_add)
        resRow2Label.text = getString(R.string.result_final_volume)
        resWater.text = formatVolume(result.waterToAddLiters, 0.0)
        resFinalVol.text = formatVolume(result.finalVolumeLiters, targetAbv)
        resPureAlc.text = formatVolume(result.pureAlcoholLiters, 100.0)
        resContraction.text = formatContraction(result.contractionLiters)
        resWarning.text = ""

        val source = readStrength(sourceAbv) ?: 0.0
        val volume = readVolume(sourceVolume) ?: 0.0
        cutExportData = mapOf(
            "Cut_Mode" to "Dilute",
            "Cut_Base_Vol" to formatVolume(volume, source).toString(),
            "Cut_Base_Strength" to formatStrength(source),
            "Cut_Target_Strength" to formatStrength(targetAbv),
            "Cut_Water" to formatVolume(result.waterToAddLiters, 0.0).toString(),
            "Cut_Final_Volume" to formatVolume(result.finalVolumeLiters, targetAbv).toString(),
            "Cut_Pure_Alcohol" to formatVolume(result.pureAlcoholLiters, 100.0).toString(),
            "Cut_Contraction" to formatContraction(result.contractionLiters)
        )
        updateExportVisibility()
    }

    private fun calculateCombine() {
        val volA = readVolume(compAVolume)
        val abvA = readStrength(compAStrength)
        val volB = readVolume(compBVolume)
        val abvB = readStrength(compBStrength)

        if (volA == null || abvA == null || volB == null || abvB == null ||
            volA <= 0.0 || volB <= 0.0 || abvA <= 0.0 || abvB <= 0.0) {
            resultsCard.visibility = View.GONE
            updateExportVisibility()
            return
        }

        try {
            val result = SpiritsMath.blendCombine(volA, abvA, volB, abvB)
            showCombineResults(result)
        } catch (_: IllegalArgumentException) {
            resultsCard.visibility = View.GONE
            updateExportVisibility()
        }
    }

    private fun showCombineResults(result: SpiritsMath.BlendResult) {
        resultsCard.visibility = View.VISIBLE
        resultsGroupDilution.visibility = View.GONE
        resultsGroupCombine.visibility = View.VISIBLE
        resultsGroupFromOrder.visibility = View.GONE
        resultsGroupSolve.visibility = View.GONE
        resultsGroupBottler.visibility = View.GONE
        resultsGroupFlavour.visibility = View.GONE

        val pooledAbv = result.pooledAbv
        lastCombinePooledAbv = pooledAbv
        combPooledVol.text = formatVolume(result.pooledVolumeLiters, pooledAbv)
        combPooledAbv.text = formatStrength(pooledAbv)
        combPureAlc.text = formatVolume(result.pureAlcoholLiters, 100.0)
        combContraction.text = formatContraction(result.contractionLiters)
        resWarning.text = ""

        cutExportData = mapOf(
            "Cut_Mode" to "Compounding",
            "Cut_A_Vol" to formatVolume(readVolume(compAVolume) ?: 0.0, readStrength(compAStrength) ?: 0.0).toString(),
            "Cut_A_Strength" to formatStrength(readStrength(compAStrength) ?: 0.0),
            "Cut_B_Vol" to formatVolume(readVolume(compBVolume) ?: 0.0, readStrength(compBStrength) ?: 0.0).toString(),
            "Cut_B_Strength" to formatStrength(readStrength(compBStrength) ?: 0.0),
            "Cut_Final_Volume" to formatVolume(result.pooledVolumeLiters, result.pooledAbv).toString(),
            "Cut_Final_Strength" to formatStrength(result.pooledAbv),
            "Cut_Pure_Alcohol" to formatVolume(result.pureAlcoholLiters, 100.0).toString(),
            "Cut_Contraction" to formatContraction(result.contractionLiters)
        )
        updateExportVisibility()
    }

    private fun calculateFromOrder() {
        val vol = readVolume(orderVolume)
        val target = readStrength(orderTargetAbv)
        val a = readStrength(orderStrongerAbv)
        val b = readStrength(orderLighterAbv)

        if (vol == null || target == null || a == null || b == null ||
            vol <= 0.0 || target <= 0.0 || a <= 0.0 || b <= 0.0) {
            resultsCard.visibility = View.GONE
            updateExportVisibility()
            return
        }

        val sAbv = maxOf(a, b)
        val lAbv = minOf(a, b)
        val swapped = a < b

        if (sAbv <= target || lAbv <= target || sAbv == lAbv) {
            resultsCard.visibility = View.GONE
            updateExportVisibility()
            return
        }

        try {
            val result = if (orderRatioMode) {
                val ratioPct = orderRatio.text?.toString()?.toDoubleOrNull() ?: 30.0
                if (ratioPct <= 0.0 || ratioPct >= 100.0) {
                    resultsCard.visibility = View.GONE
                    updateExportVisibility()
                    return
                }
                SpiritsMath.compoundFromOrderByRatio(
                    vol, target, sAbv, lAbv, ratioPct / 100.0
                )
            } else {
                val capVol = readVolume(orderCapVolume)
                if (capVol == null || capVol <= 0.0) {
                    resultsCard.visibility = View.GONE
                    updateExportVisibility()
                    return
                }
                SpiritsMath.compoundFromOrderByCap(
                    vol, target, sAbv, lAbv, capVol, orderCapOnStronger
                )
            }
            showFromOrderResults(result, target, sAbv, lAbv, swapped)
        } catch (_: IllegalArgumentException) {
            resultsCard.visibility = View.GONE
            updateExportVisibility()
        }
    }

    private fun showFromOrderResults(
        result: SpiritsMath.OrderResult,
        targetAbv: Double,
        strongerAbv: Double,
        lighterAbv: Double,
        swapped: Boolean
    ) {
        resultsCard.visibility = View.VISIBLE
        resultsGroupDilution.visibility = View.GONE
        resultsGroupCombine.visibility = View.GONE
        resultsGroupFromOrder.visibility = View.VISIBLE
        resultsGroupSolve.visibility = View.GONE
        resultsGroupBottler.visibility = View.GONE
        resultsGroupFlavour.visibility = View.GONE

        lastOrderVolume = result.orderVolumeLiters

        orderCompoundVolume.text = formatVolume(result.compoundVolumeLiters, result.compoundAbv)
        orderStrongerUsed.text = formatVolume(result.strongerVolumeLiters, strongerAbv)
        orderLighterUsed.text = formatVolume(result.lighterVolumeLiters, lighterAbv)
        orderLowestProof.text = String.format(Locale.US, "%.1f %%", result.lowestProofRatio * 100.0)
        orderHighestProof.text = String.format(Locale.US, "%.1f %%", result.highestProofRatio * 100.0)
        orderWaterAdd.text = formatVolume(result.waterToAddLiters, 0.0)
        orderPureAlc.text = formatVolume(result.pureAlcoholLiters, 100.0)
        orderFinalTotal.text = formatVolume(result.orderVolumeLiters, targetAbv)
        resWarning.text = if (swapped) getString(R.string.msg_order_strengths_swapped) else ""

        updateComplianceNote()

        cutExportData = mapOf(
            "Cut_Mode" to if (orderRatioMode) "From Order (Ratio)" else "From Order (Cap)",
            "Cut_Order_Vol" to formatVolume(result.orderVolumeLiters, targetAbv).toString(),
            "Cut_Target_Strength" to formatStrength(targetAbv),
            "Cut_Stronger_Strength" to formatStrength(strongerAbv),
            "Cut_Lighter_Strength" to formatStrength(lighterAbv),
            "Cut_Compound_Vol" to formatVolume(result.compoundVolumeLiters, result.compoundAbv).toString(),
            "Cut_Stronger_Used" to formatVolume(result.strongerVolumeLiters, strongerAbv).toString(),
            "Cut_Lighter_Used" to formatVolume(result.lighterVolumeLiters, lighterAbv).toString(),
            "Cut_Water" to formatVolume(result.waterToAddLiters, 0.0).toString(),
            "Cut_Pure_Alcohol" to formatVolume(result.pureAlcoholLiters, 100.0).toString()
        )
        updateExportVisibility()
    }

    private fun calculateSolve() {
        val abvA = readStrength(solveAStrength)
        val abvB = readStrength(solveBStrength)
        val target = readStrength(targetAbv)
        val batchSize = readVolume(solveBatchSize)

        updateSolveHint(abvA, abvB)

        if (abvA == null || abvB == null || target == null || batchSize == null) {
            resultsCard.visibility = View.GONE
            updateExportVisibility()
            return
        }

        val low = minOf(abvA, abvB)
        val high = maxOf(abvA, abvB)
        if (target <= low || target >= high) {
            resultsCard.visibility = View.VISIBLE
            resultsGroupDilution.visibility = View.GONE
            resultsGroupCombine.visibility = View.GONE
            resultsGroupFromOrder.visibility = View.GONE
            resultsGroupSolve.visibility = View.GONE
            resultsGroupBottler.visibility = View.GONE
            resultsGroupFlavour.visibility = View.GONE
            resWarning.text = getString(R.string.msg_solve_out_of_range)
            updateExportVisibility()
            return
        }

        if (batchSize <= 0.0) {
            resultsCard.visibility = View.GONE
            updateExportVisibility()
            return
        }

        try {
            val result = SpiritsMath.blendSolve(abvA, abvB, target, batchSize)
            showSolveResults(result, abvA, abvB, target)
        } catch (_: IllegalArgumentException) {
            resultsCard.visibility = View.GONE
            updateExportVisibility()
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
        resultsGroupFromOrder.visibility = View.GONE
        resultsGroupSolve.visibility = View.VISIBLE
        resultsGroupBottler.visibility = View.GONE
        resultsGroupFlavour.visibility = View.GONE

        solveSampleA.text = formatVolume(result.sampleVolumeA, abvA)
        solveSampleB.text = formatVolume(result.sampleVolumeB, abvB)
        solvePureAlc.text = formatVolume(result.pureAlcoholLiters, 100.0)
        solveFinalTotal.text = formatVolume(result.batchSizeLiters, targetAbv)
        resWarning.text = ""

        cutExportData = mapOf(
            "Cut_Mode" to "Solve",
            "Cut_A_Strength" to formatStrength(abvA),
            "Cut_B_Strength" to formatStrength(abvB),
            "Cut_Target_Strength" to formatStrength(targetAbv),
            "Cut_Total_Vol" to formatVolume(result.batchSizeLiters, targetAbv).toString(),
            "Cut_Comp_A" to formatVolume(result.sampleVolumeA, abvA).toString(),
            "Cut_Comp_B" to formatVolume(result.sampleVolumeB, abvB).toString(),
            "Cut_Pure_Alcohol" to formatVolume(result.pureAlcoholLiters, 100.0).toString()
        )
        updateExportVisibility()
    }

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
            updateExportVisibility()
            return
        }

        val bottleMl = if (bottlerInCl) bottleRaw * 10.0 else bottleRaw

        if (bottleMl <= 0.0 || packRaw <= 0 || casesRaw <= 0 ||
            target <= 0.0 || stock <= target) {
            resultsCard.visibility = View.GONE
            updateExportVisibility()
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
            updateExportVisibility()
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
        resultsGroupFromOrder.visibility = View.GONE
        resultsGroupSolve.visibility = View.GONE
        resultsGroupBottler.visibility = View.VISIBLE
        resultsGroupFlavour.visibility = View.GONE
        bottlerCasesResultsGroup.visibility = View.VISIBLE
        bottlerVolumeResultsGroup.visibility = View.GONE

        bottlerOrderVolume.text = formatVolume(result.orderVolumeLiters, targetAbv)
        bottlerWithWastage.text = formatVolume(result.withWastageLiters, targetAbv)
        bottlerStockCharge.text = formatVolume(result.stockToChargeLiters, stockAbv)
        bottlerWaterAdd.text = formatVolume(result.waterToAddLiters, 0.0)
        bottlerContraction.text = formatContraction(result.contractionLiters)

        if (bottlerAngelsCheckbox.isChecked) {
            bottlerAngelsShareRow.visibility = View.VISIBLE
            bottlerAngelsShare.text = formatVolume(result.angelsShareStartVolume, stockAbv)
        } else {
            bottlerAngelsShareRow.visibility = View.GONE
        }

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

        bottlerExportData = mapOf(
            "Bottler_Mode" to "Cases",
            "Bottler_Order_Vol" to formatVolume(result.orderVolumeLiters, targetAbv).toString(),
            "Bottler_With_Wastage" to formatVolume(result.withWastageLiters, targetAbv).toString(),
            "Bottler_Base_Spirit" to formatVolume(result.stockToChargeLiters, stockAbv).toString(),
            "Bottler_Water" to formatVolume(result.waterToAddLiters, 0.0).toString(),
            "Bottler_Contraction" to formatContraction(result.contractionLiters),
            "Bottler_Start_Vol" to if (bottlerAngelsCheckbox.isChecked) formatVolume(result.angelsShareStartVolume, stockAbv).toString() else "0.000"
        )
        updateExportVisibility()
    }

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
            updateExportVisibility()
            return
        }

        val bottleMl = if (bottlerInCl) bottleRaw * 10.0 else bottleRaw
        val bottleLiters = bottleMl / 1000.0
        val caseLiters = bottleLiters * packRaw

        if (bottleLiters <= 0.0 || packRaw <= 0 || caseLiters <= 0.0 ||
            target <= 0.0 || stock <= target || finishedVolume <= 0.0) {
            resultsCard.visibility = View.GONE
            updateExportVisibility()
            return
        }

        val wholeCases = floor(finishedVolume / caseLiters).toInt()
        val wholeCasesVolume = wholeCases * caseLiters
        val residualLiters = finishedVolume - wholeCasesVolume
        val wholeBottles = wholeCases * packRaw

        val usableVolume = if (wholeCases > 0) wholeCasesVolume else finishedVolume

        val stockToCharge = usableVolume * (target / stock)
        val pureAlcohol = stockToCharge * (stock / 100.0)
        val waterPer100 = SpiritsMath.waterPer100L(stock, target)
        val waterToAdd = (stockToCharge / 100.0) * waterPer100

        val wastageMultiplier = 1.0 + (wastage / 100.0)
        val withWastage = usableVolume * wastageMultiplier

        showBottlerVolumeResults(
            wholeCases = wholeCases,
            wholeBottles = wholeBottles,
            wholeCasesVolume = wholeCasesVolume,
            residualLiters = residualLiters,
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
        wholeCasesVolume: Double,
        residualLiters: Double,
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
        resultsGroupFromOrder.visibility = View.GONE
        resultsGroupSolve.visibility = View.GONE
        resultsGroupBottler.visibility = View.VISIBLE
        resultsGroupFlavour.visibility = View.GONE
        bottlerCasesResultsGroup.visibility = View.GONE
        bottlerVolumeResultsGroup.visibility = View.VISIBLE

        bottlerWholeCases.text = wholeCases.toString()
        bottlerWholeBottles.text = wholeBottles.toString()
        bottlerResidual.text = formatVolume(residualLiters, targetAbv)
        bottlerTotalBottled.text = formatVolume(wholeCasesVolume, targetAbv)

        resWarning.text = ""

        bottlerExportData = mapOf(
            "Bottler_Mode" to "Volume",
            "Bottler_Whole_Cases" to wholeCases.toString(),
            "Bottler_Bottles" to wholeBottles.toString(),
            "Bottler_Residual" to formatVolume(residualLiters, targetAbv).toString(),
            "Bottler_Total_Bottled" to formatVolume(wholeCasesVolume, targetAbv).toString()
        )
        updateExportVisibility()
    }

    private fun calculateFlavour() {
        val firstValue = readVolume(flavourFinishedVolume)
        val target = readStrength(flavourTargetAbv)
        val base = readStrength(flavourBaseAbv)

        if (firstValue == null || target == null || base == null ||
            firstValue <= 0.0 || target <= 0.0 || base <= 0.0) {
            resultsCard.visibility = View.GONE
            updateExportVisibility()
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
            updateExportVisibility()
        }
    }

    private fun getAdditiveName(index: Int): String {
        val name = flavourAddName[index].text?.toString()?.trim() ?: ""
        return if (name.isBlank()) "Additive ${index + 1}" else name.replace(" ", "_")
    }

    private fun showFlavourResults(
        result: SpiritsMath.FlavourResult,
        targetAbv: Double,
        baseAbv: Double
    ) {
        resultsCard.visibility = View.VISIBLE
        resultsGroupDilution.visibility = View.GONE
        resultsGroupCombine.visibility = View.GONE
        resultsGroupFromOrder.visibility = View.GONE
        resultsGroupSolve.visibility = View.GONE
        resultsGroupBottler.visibility = View.GONE
        resultsGroupFlavour.visibility = View.VISIBLE

        val effectiveTarget = if (result.finalVolumeLiters > 0.0)
            (result.pureAlcoholLiters / result.finalVolumeLiters) * 100.0 else targetAbv

        flavBaseSpirit.text = formatVolume(result.baseSpiritLiters, baseAbv)
        flavWaterAdd.text = formatVolume(result.waterToAddLiters, 0.0)
        
        val add1Vol = result.additiveVolumesLiters.getOrElse(0) { 0.0 }
        val add1Mass = result.additiveMassesKg.getOrElse(0) { 0.0 }
        flavAdd1Vol.text = formatAdditiveVolume(add1Vol, add1Mass)
        
        val add2Vol = result.additiveVolumesLiters.getOrElse(1) { 0.0 }
        val add2Mass = result.additiveMassesKg.getOrElse(1) { 0.0 }
        flavAdd2Vol.text = formatAdditiveVolume(add2Vol, add2Mass)
        
        val add3Vol = result.additiveVolumesLiters.getOrElse(2) { 0.0 }
        val add3Mass = result.additiveMassesKg.getOrElse(2) { 0.0 }
        flavAdd3Vol.text = formatAdditiveVolume(add3Vol, add3Mass)

        flavPureAlc.text = formatVolume(result.pureAlcoholLiters, 100.0)
        flavFinalTotal.text = formatVolume(result.finalVolumeLiters, effectiveTarget)
        resWarning.text = ""

        enhancingExportData = mapOf(
            "Enh_Mode" to if (enhBaseMode) "Base Volume" else "Finished Volume",
            "Enh_Base_Spirit" to formatVolume(result.baseSpiritLiters, baseAbv).toString(),
            "Enh_Water" to formatVolume(result.waterToAddLiters, 0.0).toString(),
            "Enh_${getAdditiveName(0)}" to formatVolume(result.additiveVolumesLiters.getOrElse(0) { 0.0 }, 0.0).toString(),
            "Enh_${getAdditiveName(1)}" to formatVolume(result.additiveVolumesLiters.getOrElse(1) { 0.0 }, 0.0).toString(),
            "Enh_${getAdditiveName(2)}" to formatVolume(result.additiveVolumesLiters.getOrElse(2) { 0.0 }, 0.0).toString(),
            "Enh_Pure_Alcohol" to formatVolume(result.pureAlcoholLiters, 100.0).toString(),
            "Enh_Total_Volume" to formatVolume(result.finalVolumeLiters, effectiveTarget).toString()
        )
        updateExportVisibility()
    }

    private val defaultCutData = mapOf(
        "Cut_Mode" to "None",
        "Cut_Target_Vol" to "0.000 L",
        "Cut_Base_Vol" to "0.000 L",
        "Cut_Base_Strength" to "0.00 % ABV",
        "Cut_Target_Strength" to "0.00 % ABV",
        "Cut_Base_Spirit" to "0.000 L",
        "Cut_Water" to "0.000 L",
        "Cut_Pure_Alcohol" to "0.000 L",
        "Cut_Contraction" to "0.000 L"
    )

    private val defaultBottlerData = mapOf(
        "Bottler_Mode" to "None",
        "Bottler_Order_Vol" to "0.000 L",
        "Bottler_Base_Spirit" to "0.000 L",
        "Bottler_Water" to "0.000 L",
        "Bottler_Contraction" to "0.000 L"
    )

    private val defaultEnhancingData = mapOf(
        "Enh_Mode" to "None",
        "Enh_Base_Spirit" to "0.000 L",
        "Enh_Water" to "0.000 L",
        "Enh_Additive_1" to "0.000 L",
        "Enh_Additive_2" to "0.000 L",
        "Enh_Additive_3" to "0.000 L",
        "Enh_Pure_Alcohol" to "0.000 L",
        "Enh_Total_Volume" to "0.000 L"
    )

    private fun wireExportButtons() {
        btnExportPdf.setOnClickListener {
            val data = mutableMapOf<String, Map<String, String>>()
            data["Cut"] = cutExportData ?: defaultCutData
            data["Bottler"] = bottlerExportData ?: defaultBottlerData
            data["Enhancing"] = enhancingExportData ?: defaultEnhancingData
            ExportManager.exportPdf(this, data)
        }
        btnExportTsv.setOnClickListener {
            val data = mutableMapOf<String, Map<String, String>>()
            data["Cut"] = cutExportData ?: defaultCutData
            data["Bottler"] = bottlerExportData ?: defaultBottlerData
            data["Enhancing"] = enhancingExportData ?: defaultEnhancingData
            ExportManager.exportTsv(this, data)
        }
    }

    private fun updateExportVisibility() {
        val hasData = cutExportData != null || bottlerExportData != null || enhancingExportData != null
        exportContainer.visibility = if (hasData) View.VISIBLE else View.GONE
    }

    private fun showComingSoon() {
        resultsCard.visibility = View.VISIBLE
        resultsGroupDilution.visibility = View.GONE
        resultsGroupCombine.visibility = View.GONE
        resultsGroupFromOrder.visibility = View.GONE
        resultsGroupSolve.visibility = View.GONE
        resultsGroupBottler.visibility = View.GONE
        resultsGroupFlavour.visibility = View.GONE
        resWarning.text = getString(R.string.msg_coming_soon)
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
