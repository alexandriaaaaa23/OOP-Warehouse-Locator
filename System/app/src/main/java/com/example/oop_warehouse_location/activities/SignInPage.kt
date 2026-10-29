package com.example.oop_warehouse_location.activities

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.oop_warehouse_location.R
import com.example.oop_warehouse_location.repository.FirebaseRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth

class SignInPage : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val repository = FirebaseRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_in_page)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        val root = findViewById<View>(R.id.main)

        // avoid keyboard (autoscroll)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, ime.bottom))

            if (insets.isVisible(WindowInsetsCompat.Type.ime())) {
                (v as ScrollView).post {
                    v.smoothScrollTo(0, v.getChildAt(0).bottom)
                }
            }
            insets
        }

        // UI references
        val emailLayout = findViewById<TextInputLayout>(R.id.textInputLayout)
        val passwordLayout = findViewById<TextInputLayout>(R.id.textInputLayout2)
        val emailInput = findViewById<TextInputEditText>(R.id.EmailInput)
        val passwordInput = findViewById<TextInputEditText>(R.id.PasswordInput)
        val btnLogIn = findViewById<MaterialButton>(R.id.btnLogIn)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        // change screen to sign up page
        val tvSignUp = findViewById<TextView>(R.id.tvSignUp)
        tvSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpPage::class.java))
        }
        // Forgot password
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)
        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPassword::class.java))
        }


        // Sign In button click
        btnLogIn.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            // Clear previous errors
            emailLayout.error = null
            passwordLayout.error = null

            // Validate inputs
            if (email.isEmpty()) {
                emailLayout.error = "Email is required"
                emailInput.requestFocus()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailLayout.error = "Enter a valid email"
                emailInput.requestFocus()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                passwordLayout.error = "Password is required"
                passwordInput.requestFocus()
                return@setOnClickListener
            }

            if (password.length < 6) {
                passwordLayout.error = "Password must be at least 6 characters"
                passwordInput.requestFocus()
                return@setOnClickListener
            }

            // Show loading state
            progressBar.visibility = View.VISIBLE
            btnLogIn.isEnabled = false

            // Firebase sign in
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Load user profile to determine routing
                        routeUserAfterAuth(progressBar, btnLogIn)
                    } else {
                        progressBar.visibility = View.GONE
                        btnLogIn.isEnabled = true
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
                progressBar.visibility = View.VISIBLE
                btnLogIn.isEnabled = false
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
            progressBar.visibility = View.GONE
            btnLogIn.isEnabled = true
            return
        }

        repository.getUser(uid)
            .addOnSuccessListener { user ->
                progressBar.visibility = View.GONE
                btnLogIn.isEnabled = true

                val whId = user.warehouseId.orEmpty()
                val accType = user.accountType.orEmpty()

                if (whId.isNotBlank()) {
                    // User has a warehouse — go to main app
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("warehouseId", whId)
                    intent.putExtra("accountType", accType)
                    startActivity(intent)
                    finish()
                } else if (accType == "Employee") {
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
                progressBar.visibility = View.GONE
                btnLogIn.isEnabled = true
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