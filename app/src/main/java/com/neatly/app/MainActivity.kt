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

    private enum class SubMode { VOLUME, ON_HAND, BLEND }

    private lateinit var sourceVolume: EditText
    private lateinit var sourceAbv: EditText
    private lateinit var targetAbv: EditText
    private lateinit var diluteWithAbv: EditText
    private lateinit var diluteWithGroup: View
    private lateinit var labelVolume: TextView
    private lateinit var labelSourceStrength: TextView

    private lateinit var resultsCard: MaterialCardView
    private lateinit var resWater: TextView
    private lateinit var resFinalVol: TextView
    private lateinit var resPureAlc: TextView
    private lateinit var resContraction: TextView
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
    private lateinit var unitDiluteWith: TextView

    private lateinit var tabCut: TextView
    private lateinit var tab100l: TextView
    private lateinit var tabFlavour: TextView
    private lateinit var subVolume: TextView
    private lateinit var subOnHand: TextView
    private lateinit var subBlend: TextView

    private var isProofMode = false
    private var isImperialMode = false
    private var subMode = SubMode.VOLUME

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
        updateSubModeLabels()
    }

    override fun onResume() {
        super.onResume()
        // Re-check for updates every time the app comes to the foreground.
        // The Updater itself throttles to one API call every 15 minutes.
        // We skip the check entirely if the banner is already visible, so
        // we don't re-download the same APK on every resume.
        if (updateBanner.visibility != View.VISIBLE) {
            checkForUpdate()
        }
    }

    private fun bindViews() {
        sourceVolume = findViewById(R.id.sourceVolume)
        sourceAbv = findViewById(R.id.sourceAbv)
        targetAbv = findViewById(R.id.targetAbv)
        diluteWithAbv = findViewById(R.id.diluteWithAbv)
        diluteWithGroup = findViewById(R.id.diluteWithGroup)
        labelVolume = findViewById(R.id.labelVolume)
        labelSourceStrength = findViewById(R.id.labelSourceStrength)

        resultsCard = findViewById(R.id.resultsCard)
        resWater = findViewById(R.id.resWater)
        resFinalVol = findViewById(R.id.resFinalVol)
        resPureAlc = findViewById(R.id.resPureAlc)
        resContraction = findViewById(R.id.resContraction)
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
        unitDiluteWith = findViewById(R.id.unitDiluteWith)

        tabCut = findViewById(R.id.tabCut)
        tab100l = findViewById(R.id.tab100l)
        tabFlavour = findViewById(R.id.tabFlavour)
        subVolume = findViewById(R.id.subVolume)
        subOnHand = findViewById(R.id.subOnHand)
        subBlend = findViewById(R.id.subBlend)
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
        sourceVolume.addTextChangedListener(watcher)
        sourceAbv.addTextChangedListener(watcher)
        targetAbv.addTextChangedListener(watcher)
        diluteWithAbv.addTextChangedListener(watcher)
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
        scaleField(diluteWithAbv, factor)
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
        if (isProofMode) {
            unitProof.setBackgroundResource(R.drawable.pill_active)
            unitProof.setTextColor(ContextCompat.getColor(this, R.color.on_accent))
            unitAbv.setBackgroundResource(0)
            unitAbv.setTextColor(ContextCompat.getColor(this, R.color.ink_muted))
        } else {
            unitAbv.setBackgroundResource(R.drawable.pill_active)
            unitAbv.setTextColor(ContextCompat.getColor(this, R.color.on_accent))
            unitProof.setBackgroundResource(0)
            unitProof.setTextColor(ContextCompat.getColor(this, R.color.ink_muted))
        }

        if (isImperialMode) {
            unitImperial.setBackgroundResource(R.drawable.pill_active)
            unitImperial.setTextColor(ContextCompat.getColor(this, R.color.on_accent))
            unitMetric.setBackgroundResource(0)
            unitMetric.setTextColor(ContextCompat.getColor(this, R.color.ink_muted))
        } else {
            unitMetric.setBackgroundResource(R.drawable.pill_active)
            unitMetric.setTextColor(ContextCompat.getColor(this, R.color.on_accent))
            unitImperial.setBackgroundResource(0)
            unitImperial.setTextColor(ContextCompat.getColor(this, R.color.ink_muted))
        }
    }

    private fun updateUnitLabels() {
        val strengthUnit = if (isProofMode) getString(R.string.unit_proof)
                           else getString(R.string.unit_abv)
        val volumeUnit = if (isImperialMode) getString(R.string.unit_gallons)
                         else getString(R.string.unit_liters)
        unitSourceStrength.text = strengthUnit
        unitTargetStrength.text = strengthUnit
        unitDiluteWith.text = strengthUnit
        unitVolume.text = volumeUnit
    }

    private fun wireSubTabs() {
        subVolume.setOnClickListener { selectSubTab(SubMode.VOLUME, subVolume) }
        subOnHand.setOnClickListener { selectSubTab(SubMode.ON_HAND, subOnHand) }
        subBlend.setOnClickListener { selectSubTab(SubMode.BLEND, subBlend) }
    }

    private fun selectSubTab(mode: SubMode, selected: TextView) {
        listOf(subVolume, subOnHand, subBlend).forEach { tv ->
            if (tv === selected) {
                tv.setBackgroundResource(R.drawable.pill_active)
                tv.setTextColor(ContextCompat.getColor(this, R.color.on_accent))
            } else {
                tv.setBackgroundResource(0)
                tv.setTextColor(ContextCompat.getColor(this, R.color.ink_muted))
            }
        }
        subMode = mode
        updateSubModeLabels()
        when (mode) {
            SubMode.VOLUME, SubMode.ON_HAND -> recalculate()
            SubMode.BLEND -> showComingSoon()
        }
    }

    private fun updateSubModeLabels() {
        when (subMode) {
            SubMode.VOLUME -> {
                labelVolume.text = getString(R.string.label_first_volume)
                labelSourceStrength.text = getString(R.string.label_first_strength)
                diluteWithGroup.visibility = View.GONE
            }
            SubMode.ON_HAND -> {
                labelVolume.text = getString(R.string.label_volume_on_hand)
                labelSourceStrength.text = getString(R.string.label_strength_on_hand)
                diluteWithGroup.visibility = View.VISIBLE
            }
            SubMode.BLEND -> {
                // Blend not yet implemented; labels don't matter.
            }
        }
    }

    private fun readVolumeLiters(): Double? {
        val raw = sourceVolume.text?.toString().orEmpty()
        val value = raw.toDoubleOrNull() ?: return null
        return if (isImperialMode) SpiritsMath.usGallonsToLiters(value) else value
    }

    private fun readStrengthAbv(field: EditText): Double? {
        val raw = field.text?.toString().orEmpty()
        val value = raw.toDoubleOrNull() ?: return null
        return if (isProofMode) SpiritsMath.usProofToAbv(value) else value
    }

    private fun readDiluteWithAbv(): Double {
        val raw = diluteWithAbv.text?.toString().orEmpty()
        if (raw.isBlank()) return 0.0
        val value = raw.toDoubleOrNull() ?: return 0.0
        return if (isProofMode) SpiritsMath.usProofToAbv(value) else value
    }

    private fun formatVolume(liters: Double): String {
        return if (isImperialMode) {
            String.format(Locale.US, "%.3f gal", SpiritsMath.litersToUsGallons(liters))
        } else {
            String.format(Locale.US, "%.3f L", liters)
        }
    }

    private fun recalculate() {
        if (subMode == SubMode.BLEND) return

        val volume = readVolumeLiters()
        val source = readStrengthAbv(sourceAbv)
        val target = readStrengthAbv(targetAbv)

        if (volume == null || source == null || target == null) {
            resultsCard.visibility = View.GONE
            return
        }

        if (target >= source || target <= 0.0 || source <= 0.0 || volume <= 0.0) {
            resultsCard.visibility = View.GONE
            return
        }

        try {
            val result = if (subMode == SubMode.ON_HAND) {
                val diluteWith = readDiluteWithAbv()
                if (diluteWith >= target) {
                    resultsCard.visibility = View.GONE
                    return
                }
                SpiritsMath.calculateOnHand(volume, source, target, diluteWith)
            } else {
                SpiritsMath.calculate(volume, source, target)
            }
            renderResult(result)
        } catch (_: IllegalArgumentException) {
            resultsCard.visibility = View.GONE
        }
    }

    private fun renderResult(result: SpiritsMath.Result) {
        resultsCard.visibility = View.VISIBLE
        resWater.text = formatVolume(result.waterToAddLiters)
        resFinalVol.text = formatVolume(result.finalVolumeLiters)
        resPureAlc.text = formatVolume(result.pureAlcoholLiters)
        resContraction.text = formatVolume(result.contractionLiters)
        resWarning.text = ""
    }

    private fun wireMainTabs() {
        tabCut.setOnClickListener { selectMainTab(tabCut) }
        tab100l.setOnClickListener { selectMainTab(tab100l) }
        tabFlavour.setOnClickListener { selectMainTab(tabFlavour) }
    }

    private fun selectMainTab(selected: TextView) {
        listOf(tabCut, tab100l, tabFlavour).forEach { tv ->
            if (tv === selected) {
                tv.setBackgroundResource(R.drawable.pill_active)
                tv.setTextColor(ContextCompat.getColor(this, R.color.on_accent))
            } else {
                tv.setBackgroundResource(0)
                tv.setTextColor(ContextCompat.getColor(this, R.color.ink_muted))
            }
        }
        if (selected !== tabCut) {
            showComingSoon()
        } else {
            recalculate()
        }
    }

    private fun showComingSoon() {
        resultsCard.visibility = View.VISIBLE
        resWater.text = ""
        resFinalVol.text = ""
        resPureAlc.text = ""
        resContraction.text = ""
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
            Updater.checkForUpdate(this@MainActivity) {
                updateBanner.visibility = View.VISIBLE
            }
        }
    }
}
