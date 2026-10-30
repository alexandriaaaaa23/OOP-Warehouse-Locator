package com.example.oop_warehouse_location.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.oop_warehouse_location.R
import com.example.oop_warehouse_location.repository.FirebaseRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth

class JoinWarehouseActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val repository = FirebaseRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_join_warehouse)

        auth = FirebaseAuth.getInstance()

        val root = findViewById<ScrollView>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, ime.bottom))
            insets
        }

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

            progressBar.visibility = View.VISIBLE
            btnJoin.isEnabled = false

            // Atomically join the warehouse (lookup + member addition + user profile update)
            repository.joinWarehouseByInviteCode(inviteCode, currentUid)
                .addOnSuccessListener { warehouse ->
                    progressBar.visibility = View.GONE
                    btnJoin.isEnabled = true
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
                    progressBar.visibility = View.GONE
                    btnJoin.isEnabled = true
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
