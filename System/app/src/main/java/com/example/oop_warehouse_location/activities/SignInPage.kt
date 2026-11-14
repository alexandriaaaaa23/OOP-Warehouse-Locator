package com.example.oop_warehouse_location.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import com.example.oop_warehouse_location.R
import com.example.oop_warehouse_location.activities.auth.BaseAuthActivity
import com.example.oop_warehouse_location.models.user.Employee
import com.example.oop_warehouse_location.repository.FirebaseRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * INHERITANCE — extends [BaseAuthActivity] which provides:
 *   • FirebaseAuth initialisation (from BaseActivity)
 *   • Edge-to-edge + window-insets with auto-scroll (from BaseAuthActivity)
 *   • [validateEmail] / [validatePassword] helpers (from BaseAuthActivity)
 *   • [setLoading] toggle (from BaseActivity)
 */
class SignInPage : BaseAuthActivity() {

    private val repository = FirebaseRepository()

    override fun getLayoutResId(): Int = R.layout.activity_sign_in_page

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // UI references
        val emailLayout = findViewById<TextInputLayout>(R.id.textInputLayout)
        val passwordLayout = findViewById<TextInputLayout>(R.id.textInputLayout2)
        val emailInput = findViewById<TextInputEditText>(R.id.EmailInput)
        val passwordInput = findViewById<TextInputEditText>(R.id.PasswordInput)
        val btnLogIn = findViewById<MaterialButton>(R.id.btnLogIn)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        // Navigate to Sign Up
        findViewById<TextView>(R.id.tvSignUp).setOnClickListener {
            startActivity(Intent(this, SignUpPage::class.java))
        }

        // Navigate to Forgot Password
        findViewById<TextView>(R.id.tvForgotPassword).setOnClickListener {
            startActivity(Intent(this, ForgotPassword::class.java))
        }

        // Sign In button click
        btnLogIn.setOnClickListener {
            // Clear previous errors
            emailLayout.error = null
            passwordLayout.error = null

            // Use inherited validation helpers (POLYMORPHISM via BaseAuthActivity)
            val email = validateEmail(emailLayout, emailInput) ?: return@setOnClickListener
            val password = validatePassword(passwordLayout, passwordInput) ?: return@setOnClickListener

            // Show loading state — uses inherited helper
            setLoading(progressBar, btnLogIn, true)

            // Firebase sign in
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        routeUserAfterAuth(progressBar, btnLogIn)
                    } else {
                        setLoading(progressBar, btnLogIn, false)
                        val errorMessage = task.exception?.message ?: "Authentication failed"
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    override fun onStart() {
        super.onStart()
        // If user is already signed in, navigate based on their profile
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val progressBar = findViewById<ProgressBar>(R.id.progressBar)
            val btnLogIn = findViewById<MaterialButton>(R.id.btnLogIn)
            if (progressBar != null && btnLogIn != null) {
                setLoading(progressBar, btnLogIn, true)
                routeUserAfterAuth(progressBar, btnLogIn)
            }
        }
    }

    /**
     * Loads the user's Firestore profile and navigates to:
     * - MainActivity (with warehouseId) if the user has a warehouse
     * - JoinWarehouseActivity if the user is an Employee without a warehouse
     */
    private fun routeUserAfterAuth(progressBar: ProgressBar, btnLogIn: MaterialButton) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            setLoading(progressBar, btnLogIn, false)
            return
        }

        repository.getUser(uid)
            .addOnSuccessListener { user ->
                setLoading(progressBar, btnLogIn, false)

                val whId = user.warehouseId.orEmpty()
                val accType = user.getRole()

                if (whId.isNotBlank()) {
                    if (user is Employee) {
                        // Check if employee is still a member of the warehouse
                        repository.getWarehouse(whId).addOnSuccessListener { wh ->
                            setLoading(progressBar, btnLogIn, false)
                            if (wh.members.contains(uid)) {
                                val intent = Intent(this, MainActivity::class.java).apply {
                                    putExtra("warehouseId", whId)
                                    putExtra("accountType", accType)
                                }
                                startActivity(intent)
                                finish()
                            } else {
                                repository.updateUserWarehouseId(uid, "")
                                Toast.makeText(this, R.string.removed_by_manager_notice, Toast.LENGTH_LONG).show()
                                startActivity(Intent(this, JoinWarehouseActivity::class.java))
                                finish()
                            }
                        }.addOnFailureListener {
                            val intent = Intent(this, MainActivity::class.java).apply {
                                putExtra("warehouseId", whId)
                                putExtra("accountType", accType)
                            }
                            startActivity(intent)
                            finish()
                        }
                        return@addOnSuccessListener
                    }

                    // Manager has a warehouse — go to main app
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("warehouseId", whId)
                    intent.putExtra("accountType", accType)
                    startActivity(intent)
                    finish()
                } else if (user is Employee) {
                    // Employee hasn't joined a warehouse yet
                    startActivity(Intent(this, JoinWarehouseActivity::class.java))
                    finish()
                } else {
                    // Manager or unknown without a warehouse
                    Toast.makeText(this, "Welcome!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("warehouseId", whId)
                    intent.putExtra("accountType", accType)
                    startActivity(intent)
                    finish()
                }
            }
            .addOnFailureListener {
                setLoading(progressBar, btnLogIn, false)
                // Profile not found in Firestore — fallback to MainActivity with empty extras
                Toast.makeText(this, "Sign in successful!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("warehouseId", "")
                intent.putExtra("accountType", "")
                startActivity(intent)
                finish()
            }
    }
}
