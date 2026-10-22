package com.example.oop_warehouse_location.activities

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.oop_warehouse_location.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.MaterialAutoCompleteTextView

class AddInventoryActivity : AppCompatActivity() {

    // Selected values (radio-button behavior: only one selected at a time)
    private var selectedCarrier: String = ""
    private var selectedZone: String = ""
    private var selectedRackTier: String = ""
    private var selectedParcelType: String = ""
    private var selectedHandling: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_inventory)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, ime.bottom))
            insets
        }

        setupCarrierChips()
        setupZoneChips()
        setupRackTierDropdown()
        setupParcelTypeGrid()
        setupHandlingChips()
    }

    // ── Carrier Chips: single-select (radio behavior) ────────────────────
    private fun setupCarrierChips() {
        val chipJT = findViewById<MaterialButton>(R.id.chipJT)
        val chipNinja = findViewById<MaterialButton>(R.id.chipNinja)
        val chipFlash = findViewById<MaterialButton>(R.id.chipFlash)

        val carrierChips = listOf(chipJT, chipNinja, chipFlash)
        val carrierLabels = listOf("J&T Express", "Ninja Van", "Flash Express")

        carrierChips.forEachIndexed { index, chip ->
            chip.setOnClickListener {
                selectChip(chip, carrierChips)
                selectedCarrier = carrierLabels[index]
            }
        }
    }

    // ── Zone Chips: single-select (radio behavior) ──────────────────────
    private fun setupZoneChips() {
        val chipZoneA = findViewById<MaterialButton>(R.id.chipZoneA)
        val chipZoneB = findViewById<MaterialButton>(R.id.chipZoneB)
        val chipZoneC = findViewById<MaterialButton>(R.id.chipZoneC)
        val chipStaging = findViewById<MaterialButton>(R.id.chipStaging)

        val zoneChips = listOf(chipZoneA, chipZoneB, chipZoneC, chipStaging)
        val zoneLabels = listOf("Zone A", "Zone B", "Zone C", "Staging")

        zoneChips.forEachIndexed { index, chip ->
            chip.setOnClickListener {
                selectChip(chip, zoneChips)
                selectedZone = zoneLabels[index]
            }
        }
    }

    // ── Rack Tier: dropdown (only one item can be chosen) ───────────────
    private fun setupRackTierDropdown() {
        val rackTiers = arrayOf("Rack 01 - Top", "Rack 02 - Top", "Rack 03 - Mid", "Rack 04 - Mid", "Rack 05 - Bottom", "Rack 06 - Floor")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, rackTiers)
        val actvRackTier = findViewById<MaterialAutoCompleteTextView>(R.id.actvRackTier)
        actvRackTier.setAdapter(adapter)
        actvRackTier.setOnItemClickListener { _, _, position, _ ->
            selectedRackTier = rackTiers[position]
        }
    }

    // ── Parcel Type Grid: single-select (radio behavior) ────────────────
    private fun setupParcelTypeGrid() {
        val typeFlyer = findViewById<LinearLayout>(R.id.typeFlyer)
        val typeSmallBox = findViewById<LinearLayout>(R.id.typeSmallBox)
        val typeMediumBox = findViewById<LinearLayout>(R.id.typeMediumBox)
        val typeBulky = findViewById<LinearLayout>(R.id.typeBulky)
        val typePallet = findViewById<LinearLayout>(R.id.typePallet)
        val typeCustom = findViewById<LinearLayout>(R.id.typeCustom)

        val typeCards = listOf(typeFlyer, typeSmallBox, typeMediumBox, typeBulky, typePallet, typeCustom)
        val typeLabels = listOf("Flyer", "Small Box", "Medium Box", "Bulky Large", "Freight Pallet", "Custom")

        typeCards.forEachIndexed { index, card ->
            card.setOnClickListener {
                selectCard(card, typeCards)
                selectedParcelType = typeLabels[index]
            }
        }
    }

    // ── Handling Chips: single-select via ChipGroup (singleSelection=true) ──
    private fun setupHandlingChips() {
        val chipGroupHandling = findViewById<ChipGroup>(R.id.chipGroupHandling)
        chipGroupHandling.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedHandling = if (checkedIds.isNotEmpty()) {
                when (checkedIds.first()) {
                    R.id.chipFragile -> getString(R.string.fragile_glass)
                    R.id.chipPerishable -> getString(R.string.perishable)
                    R.id.chipHighValue -> getString(R.string.high_value)
                    R.id.chipExpress -> getString(R.string.express_24h)
                    else -> ""
                }
            } else ""
        }
    }

    // ── Helper: highlight selected MaterialButton chip, deselect others ─
    private fun selectChip(selected: MaterialButton, allChips: List<MaterialButton>) {
        allChips.forEach { chip ->
            if (chip == selected) {
                // Active style
                chip.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.pink))
                chip.setTextColor(ContextCompat.getColor(this, R.color.white))
                chip.strokeColor = ContextCompat.getColorStateList(this, R.color.pink)
            } else {
                // Inactive style
                chip.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.white))
                chip.setTextColor(ContextCompat.getColor(this, R.color.black))
                chip.strokeColor = ContextCompat.getColorStateList(this, R.color.borderGray)
            }
        }
    }

    // ── Helper: highlight selected parcel-type card, deselect others ────
    private fun selectCard(selected: View, allCards: List<View>) {
        allCards.forEach { card ->
            if (card == selected) {
                card.setBackgroundResource(R.drawable.bg_badge_pink)
            } else {
                card.setBackgroundResource(R.drawable.bg_stat_card)
            }
        }
    }
}