package com.example.oop_warehouse_location.activities

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.oop_warehouse_location.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth

class ForgotPassword : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_forgot_password)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // UI references
        val emailLayout = findViewById<TextInputLayout>(R.id.textInputLayout)
        val emailInput = findViewById<TextInputEditText>(R.id.EmailInput)
        val btnSubmit = findViewById<MaterialButton>(R.id.btnSubmit)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        // Back to sign in
        val tvSignIn = findViewById<TextView>(R.id.tvSignIn)
        tvSignIn.setOnClickListener {
            startActivity(Intent(this, SignInPage::class.java))
            finish()
        }

        // Submit button – send password reset email
        btnSubmit.setOnClickListener {
            val email = emailInput.text?.toString()?.trim() ?: ""

            // Clear previous errors
            emailLayout.error = null
            emailLayout.isErrorEnabled = false

            // Validate email
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

            // Show loading state
            progressBar.visibility = View.VISIBLE
            btnSubmit.isEnabled = false

            // Send password reset email via Firebase
            try {
                auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener {
                        progressBar.visibility = View.GONE
                        btnSubmit.isEnabled = true
                        Toast.makeText(
                            this,
                            "Password reset email sent. Check your inbox.",
                            Toast.LENGTH_LONG
                        ).show()
                        // Navigate back to sign in
                        startActivity(Intent(this, SignInPage::class.java))
                        finish()
                    }
                    .addOnFailureListener { e ->
                        progressBar.visibility = View.GONE
                        btnSubmit.isEnabled = true
                        Toast.makeText(
                            this,
                            e.message ?: "Failed to send reset email",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                btnSubmit.isEnabled = true
                Toast.makeText(this, "An error occurred: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}