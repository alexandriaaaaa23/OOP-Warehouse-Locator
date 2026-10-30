package com.example.oop_warehouse_location.activities

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.oop_warehouse_location.R
import com.example.oop_warehouse_location.models.ActivityLog
import com.example.oop_warehouse_location.models.Parcel
import com.example.oop_warehouse_location.repository.FirebaseRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth

class AddInventoryActivity : AppCompatActivity() {

    // Selected values (radio-button behavior: only one selected at a time)
    private var selectedCarrier: String = ""
    private var selectedZone: String = ""
    private var selectedRackTier: String = ""
    private var selectedParcelType: String = ""
    private val selectedHandling: MutableList<String> = mutableListOf()

    private lateinit var repository: FirebaseRepository

    // ── View references ─────────────────────────────────────────────────
    private lateinit var etTracking: TextInputEditText
    private lateinit var tilTracking: TextInputLayout
    private lateinit var etRecipientName: TextInputEditText
    private lateinit var etRecipientPhone: TextInputEditText
    private lateinit var etDeliveryHub: TextInputEditText
    private lateinit var actvRackTier: MaterialAutoCompleteTextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnSaveItem: MaterialButton
    private lateinit var btnReceiveNext: MaterialButton
    private lateinit var btnDiscard: MaterialButton

    // Chip / card lists kept at class level so we can reset them
    private lateinit var carrierChips: List<MaterialButton>
    private lateinit var zoneChips: List<MaterialButton>
    private lateinit var typeCards: List<LinearLayout>
    private lateinit var chipGroupHandling: ChipGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_inventory)

        // Initialize repository with warehouse scope from Intent
        val warehouseId = intent.getStringExtra("warehouseId") ?: ""
        repository = FirebaseRepository(warehouseId = warehouseId)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, ime.bottom))
            insets
        }

        bindViews()
        setupBackButton()
        setupCarrierChips()
        setupZoneChips()
        setupRackTierDropdown()
        setupParcelTypeGrid()
        setupHandlingChips()
        setupActionButtons()
    }

    // ── Bind all reusable view references ───────────────────────────────
    private fun bindViews() {
        etTracking = findViewById(R.id.etTracking)
        tilTracking = findViewById(R.id.tilTracking)
        etRecipientName = findViewById(R.id.etRecipientName)
        etRecipientPhone = findViewById(R.id.etRecipientPhone)
        etDeliveryHub = findViewById(R.id.etDeliveryHub)
        actvRackTier = findViewById(R.id.actvRackTier)
        progressBar = findViewById(R.id.progressBar)
        btnSaveItem = findViewById(R.id.btnSaveItem)
        btnReceiveNext = findViewById(R.id.btnReceiveNext)
        btnDiscard = findViewById(R.id.btnDiscard)
    }

    // ── Back Button: top-left button returns to the previous screen ─────
    // NOTE: change R.id.btnBack to the actual id of your top-left button.
    private fun setupBackButton() {
        // Phone's back gesture / system back button uses the same logic
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        // Top-left button in the layout
        findViewById<View>(R.id.btnBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    // ── Carrier Chips: single-select (radio behavior) ────────────────────
    private fun setupCarrierChips() {
        val chipJT = findViewById<MaterialButton>(R.id.chipJT)
        val chipNinja = findViewById<MaterialButton>(R.id.chipNinja)
        val chipFlash = findViewById<MaterialButton>(R.id.chipFlash)

        carrierChips = listOf(chipJT, chipNinja, chipFlash)
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

        zoneChips = listOf(chipZoneA, chipZoneB, chipZoneC, chipStaging)
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

        typeCards = listOf(typeFlyer, typeSmallBox, typeMediumBox, typeBulky, typePallet, typeCustom)
        val typeLabels = listOf("Flyer", "Small Box", "Medium Box", "Bulky Large", "Freight Pallet", "Custom")

        typeCards.forEachIndexed { index, card ->
            card.setOnClickListener {
                selectCard(card, typeCards)
                selectedParcelType = typeLabels[index]
            }
        }
    }

    // ── Handling Chips: multi-select ──────────────────────────────────────
    private fun setupHandlingChips() {
        chipGroupHandling = findViewById(R.id.chipGroupHandling)
        chipGroupHandling.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedHandling.clear()
            checkedIds.forEach { id ->
                val label = when (id) {
                    R.id.chipFragile -> getString(R.string.fragile_glass)
                    R.id.chipPerishable -> getString(R.string.perishable)
                    R.id.chipHighValue -> getString(R.string.high_value)
                    R.id.chipExpress -> getString(R.string.express_24h)
                    else -> null
                }
                if (label != null) selectedHandling.add(label)
            }
        }
    }

    // ── Action Buttons: Save, Receive Next, Discard ─────────────────────
    private fun setupActionButtons() {
        btnSaveItem.setOnClickListener {
            if (validateForm()) {
                saveParcel(finishAfterSave = true)
            }
        }

        btnReceiveNext.setOnClickListener {
            if (validateForm()) {
                saveParcel(finishAfterSave = false)
            }
        }

        btnDiscard.setOnClickListener {
            clearForm()
            finish()
        }
    }

    // ── Validation ──────────────────────────────────────────────────────
    private fun validateForm(): Boolean {
        val trackingId = etTracking.text?.toString()?.trim().orEmpty()

        if (selectedCarrier.isBlank()) {
            Toast.makeText(this, "Please select a carrier", Toast.LENGTH_SHORT).show()
            return false
        }
        if (trackingId.isBlank()) {
            tilTracking.error = "Tracking ID is required"
            etTracking.requestFocus()
            return false
        } else {
            tilTracking.error = null
        }
        return true
    }

    // ── Build parcel, save to Firestore, log activity ───────────────────
    private fun saveParcel(finishAfterSave: Boolean) {
        setLoading(true)

        val parcel = Parcel(
            parcelID = etTracking.text?.toString()?.trim().orEmpty(),
            carrier = selectedCarrier,
            receiptName = etRecipientName.text?.toString()?.trim().orEmpty(),
            recipientPhone = etRecipientPhone.text?.toString()?.trim().orEmpty(),
            deliveryHub = etDeliveryHub.text?.toString()?.trim().orEmpty(),
            zone = selectedZone,
            rack = selectedRackTier,
            parcelType = selectedParcelType,
            handling = selectedHandling.toMutableList(),
            status = Parcel.STATUS_IN_STOCK,
            createdAt = System.currentTimeMillis()
        )

        repository.saveParcel(parcel)
            .addOnSuccessListener {
                // Log the activity
                val userId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
                val log = ActivityLog(
                    parcelId = parcel.parcelID,
                    userId = userId,
                    action = "Parcel received"
                )
                repository.logActivity(log)

                setLoading(false)
                Toast.makeText(this, "Parcel saved successfully", Toast.LENGTH_SHORT).show()

                if (finishAfterSave) {
                    finish()
                } else {
                    // "Receive Next" — clear the form and stay on screen
                    clearForm()
                }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                Toast.makeText(
                    this,
                    "Failed to save: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // ── Clear / reset the entire form ───────────────────────────────────
    private fun clearForm() {
        // Text fields
        etTracking.text?.clear()
        tilTracking.error = null
        etRecipientName.text?.clear()
        etRecipientPhone.text?.clear()
        etDeliveryHub.text?.clear()
        actvRackTier.text?.clear()

        // Selection state
        selectedCarrier = ""
        selectedZone = ""
        selectedRackTier = ""
        selectedParcelType = ""
        selectedHandling.clear()

        // Reset carrier chips to inactive style
        carrierChips.forEach { chip ->
            chip.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.white))
            chip.setTextColor(ContextCompat.getColor(this, R.color.black))
            chip.strokeColor = ContextCompat.getColorStateList(this, R.color.borderGray)
        }

        // Reset zone chips to inactive style
        zoneChips.forEach { chip ->
            chip.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.white))
            chip.setTextColor(ContextCompat.getColor(this, R.color.black))
            chip.strokeColor = ContextCompat.getColorStateList(this, R.color.borderGray)
        }

        // Reset parcel type cards
        typeCards.forEach { card ->
            card.setBackgroundResource(R.drawable.bg_stat_card)
        }

        // Uncheck all handling chips
        chipGroupHandling.clearCheck()
    }

    // ── Toggle progress bar + button enabled state ──────────────────────
    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnSaveItem.isEnabled = !loading
        btnReceiveNext.isEnabled = !loading
        btnDiscard.isEnabled = !loading
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

    // ── Helper: highlight selected parcel-type card, deselect others
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