package com.example.oop_warehouse_location.activities

import android.content.Intent
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.example.oop_warehouse_location.R
import com.example.oop_warehouse_location.activities.base.BaseActivity
import com.example.oop_warehouse_location.repository.FirebaseRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * INHERITANCE — extends [BaseActivity] which provides:
 *   • FirebaseAuth initialisation
 *   • Edge-to-edge + window-insets setup (ScrollView root with R.id.main)
 *   • [setLoading] toggle
 */
class JoinWarehouseActivity : BaseActivity() {

    private val repository = FirebaseRepository()

    override fun getLayoutResId(): Int = R.layout.activity_join_warehouse

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tilInviteCode = findViewById<TextInputLayout>(R.id.tilInviteCode)
        val etInviteCode = findViewById<TextInputEditText>(R.id.etInviteCode)
        val btnJoin = findViewById<MaterialButton>(R.id.btnJoin)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        // Sign out link
        findViewById<TextView>(R.id.tvSignOut).setOnClickListener {
            auth.signOut()
            val intent = Intent(this, SignInPage::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        btnJoin.setOnClickListener {
            val inviteCode = etInviteCode.text.toString().trim().uppercase()
            tilInviteCode.error = null

            if (inviteCode.isEmpty()) {
                tilInviteCode.error = "Please enter an invite code"
                etInviteCode.requestFocus()
                return@setOnClickListener
            }

            if (inviteCode.length != 6) {
                tilInviteCode.error = "Invite code must be 6 characters"
                etInviteCode.requestFocus()
                return@setOnClickListener
            }

            val currentUid = auth.currentUser?.uid
            if (currentUid == null) {
                Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Show loading state — uses inherited helper
            setLoading(progressBar, btnJoin, true)

            // Atomically join the warehouse (lookup + member addition + user profile update)
            repository.joinWarehouseByInviteCode(inviteCode, currentUid)
                .addOnSuccessListener { warehouse ->
                    setLoading(progressBar, btnJoin, false)
                    Toast.makeText(
                        this,
                        "Joined ${warehouse.name}!",
                        Toast.LENGTH_SHORT
                    ).show()

                    val intent = Intent(this, MainActivity::class.java).apply {
                        putExtra("warehouseId", warehouse.warehouseId)
                        putExtra("accountType", "Employee")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                }
                .addOnFailureListener { e ->
                    setLoading(progressBar, btnJoin, false)
                    val message = e.localizedMessage.orEmpty()
                    if (message.contains("Invalid invite code", ignoreCase = true)) {
                        tilInviteCode.error = "Invalid invite code. Please check and try again."
                    } else {
                        Toast.makeText(this, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }
}
