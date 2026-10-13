package com.neatly.app

import android.os.Bundle
import android.text.Editable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.RelativeSizeSpan
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var sourceVolume: EditText
    private lateinit var sourceAbv: EditText
    private lateinit var targetAbv: EditText
    private lateinit var resultsCard: MaterialCardView
    private lateinit var resWater: TextView
    private lateinit var resFinalVol: TextView
    private lateinit var resPureAlc: TextView
    private lateinit var resContraction: TextView
    private lateinit var resWarning: TextView
    private lateinit var footerText: TextView
    private lateinit var wordmarkText: TextView

    private lateinit var unitAbv: TextView
    private lateinit var unitProof: TextView
    private lateinit var tabCut: TextView
    private lateinit var tab100l: TextView
    private lateinit var tabFlavour: TextView
    private lateinit var subVolume: TextView
    private lateinit var subOnHand: TextView
    private lateinit var subBlend: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindViews()
        buildWordmark()
        populateFooter()
        wireInputs()
        wireUnitToggle()
        wireMainTabs()
        wireSubTabs()
        checkForUpdate()
    }

    private fun bindViews() {
        sourceVolume = findViewById(R.id.sourceVolume)
        sourceAbv = findViewById(R.id.sourceAbv)
        targetAbv = findViewById(R.id.targetAbv)
        resultsCard = findViewById(R.id.resultsCard)
        resWater = findViewById(R.id.resWater)
        resFinalVol = findViewById(R.id.resFinalVol)
        resPureAlc = findViewById(R.id.resPureAlc)
        resContraction = findViewById(R.id.resContraction)
        resWarning = findViewById(R.id.resWarning)
        footerText = findViewById(R.id.footerText)
        wordmarkText = findViewById(R.id.wordmarkText)

        unitAbv = findViewById(R.id.unitAbv)
        unitProof = findViewById(R.id.unitProof)
        tabCut = findViewById(R.id.tabCut)
        tab100l = findViewById(R.id.tab100l)
        tabFlavour = findViewById(R.id.tabFlavour)
        subVolume = findViewById(R.id.subVolume)
        subOnHand = findViewById(R.id.subOnHand)
        subBlend = findViewById(R.id.subBlend)
    }

    /**
     * Builds the wordmark "Neatly" as a single string with two font spans:
     * "N" in Cinzel Decorative, "eatly" in Bodoni Moda.
     * A small RelativeSizeSpan on the N compensates for Cinzel's larger
     * cap-height, so the two halves sit visually level.
     */
    private fun buildWordmark() {
        val cinzelTypeface = ResourcesCompat.getFont(this, R.font.cinzel_decorative_regular)
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

        // Slight size reduction on the Cinzel N to match Bodoni's cap-height.
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
    }

    private fun recalculate() {
        val volText = sourceVolume.text?.toString().orEmpty()
        val srcText = sourceAbv.text?.toString().orEmpty()
        val tgtText = targetAbv.text?.toString().orEmpty()

        if (volText.isBlank() || srcText.isBlank() || tgtText.isBlank()) {
            resultsCard.visibility = View.GONE
            return
        }

        val volume = volText.toDoubleOrNull()
        val source = srcText.toDoubleOrNull()
        val target = tgtText.toDoubleOrNull()

        if (volume == null || source == null || target == null) {
            resultsCard.visibility = View.GONE
            return
        }

        if (target >= source || target <= 0.0 || source <= 0.0 || volume <= 0.0) {
            resultsCard.visibility = View.GONE
            return
        }

        try {
            val result = SpiritsMath.calculate(volume, source, target)
            renderResult(result)
        } catch (_: IllegalArgumentException) {
            resultsCard.visibility = View.GONE
        }
    }

    private fun renderResult(result: SpiritsMath.Result) {
        resultsCard.visibility = View.VISIBLE
        resWater.text = formatLiters(result.waterToAddLiters)
        resFinalVol.text = formatLiters(result.finalVolumeLiters)
        resPureAlc.text = formatLiters(result.pureAlcoholLiters)
        resContraction.text = formatLiters(result.contractionLiters)
        resWarning.text = ""
    }

    private fun formatLiters(value: Double): String =
        String.format(Locale.US, "%.3f L", value)

    private fun wireUnitToggle() {
        unitAbv.setOnClickListener { selectUnit(proof = false) }
        unitProof.setOnClickListener { selectUnit(proof = true) }
    }

    private fun selectUnit(proof: Boolean) {
        if (proof) {
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
        }
    }

    private fun wireSubTabs() {
        subVolume.setOnClickListener { selectSubTab(subVolume) }
        subOnHand.setOnClickListener { selectSubTab(subOnHand) }
        subBlend.setOnClickListener { selectSubTab(subBlend) }
    }

    private fun selectSubTab(selected: TextView) {
        listOf(subVolume, subOnHand, subBlend).forEach { tv ->
            if (tv === selected) {
                tv.setBackgroundResource(R.drawable.pill_active)
                tv.setTextColor(ContextCompat.getColor(this, R.color.on_accent))
            } else {
                tv.setBackgroundResource(0)
                tv.setTextColor(ContextCompat.getColor(this, R.color.ink_muted))
            }
        }
        if (selected !== subVolume) {
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

    private fun checkForUpdate() {
        lifecycleScope.launch {
            Updater.checkForUpdate(this@MainActivity) {
                // Reserved for the "Update ready" footer indicator in a future build.
            }
        }
    }
}
