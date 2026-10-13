package com.neatly.app

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var sourceVolume: TextInputEditText
    private lateinit var sourceAbv: TextInputEditText
    private lateinit var targetAbv: TextInputEditText
    private lateinit var calculateBtn: MaterialButton
    private lateinit var resultsCard: MaterialCardView
    private lateinit var resWater: TextView
    private lateinit var resFinalVol: TextView
    private lateinit var resPureAlc: TextView
    private lateinit var resContraction: TextView
    private lateinit var resWarning: TextView

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
        wireSubtabs()
        wireCalculate()
        checkForUpdate()
    }

    private fun bindViews() {
        sourceVolume = findViewById(R.id.sourceVolume)
        sourceAbv = findViewById(R.id.sourceAbv)
        targetAbv = findViewById(R.id.targetAbv)
        calculateBtn = findViewById(R.id.calculateBtn)
        resultsCard = findViewById(R.id.resultsCard)
        resWater = findViewById(R.id.resWater)
        resFinalVol = findViewById(R.id.resFinalVol)
        resPureAlc = findViewById(R.id.resPureAlc)
        resContraction = findViewById(R.id.resContraction)
        resWarning = findViewById(R.id.resWarning)
    }

    private fun wireSubtabs() {
        val subTabs = findViewById<TabLayout>(R.id.subTabs)
        subTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                if (tab.position != 0) {
                    showNotice(getString(R.string.msg_coming_soon))
                } else {
                    resultsCard.visibility = View.GONE
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun wireCalculate() {
        calculateBtn.setOnClickListener {
            val volume = sourceVolume.text?.toString()?.toDoubleOrNull()
            val source = sourceAbv.text?.toString()?.toDoubleOrNull()
            val target = targetAbv.text?.toString()?.toDoubleOrNull()

            if (volume == null || source == null || target == null) {
                showNotice(getString(R.string.msg_fill_all_fields))
                return@setOnClickListener
            }
            if (target >= source) {
                showNotice(getString(R.string.msg_target_too_high))
                return@setOnClickListener
            }

            try {
                val result = SpiritsMath.calculate(volume, source, target)
                renderResult(result)
            } catch (e: IllegalArgumentException) {
                showNotice(e.message ?: getString(R.string.msg_calc_error))
            }
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

    private fun showNotice(message: String) {
        resultsCard.visibility = View.VISIBLE
        resWater.text = ""
        resFinalVol.text = ""
        resPureAlc.text = ""
        resContraction.text = ""
        resWarning.text = message
    }

    private fun formatLiters(value: Double): String {
        return String.format(Locale.US, "%.3f L", value)
    }

    private fun checkForUpdate() {
        lifecycleScope.launch {
            Updater.checkForUpdate(this@MainActivity) {
                // Placeholder hook for the "Update ready" indicator.
                // The next build will surface this in the footer.
                // For now, taps on the Calculate button remain
                // the only interactive surface.
            }
        }
    }

    /**
     * Called by the footer "Update ready" indicator in a future build.
     * Kept here so the permission gate is co-located with the Activity
     * that owns it.
     */
    private fun attemptInstall() {
        if (Updater.canInstall(this)) {
            Updater.install(this)
        } else {
            Updater.requestInstallPermission(this)
            installPermissionLauncher.launch(
                android.content.Intent(
                    android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    android.net.Uri.parse("package:$packageName")
                )
            )
        }
    }
}
