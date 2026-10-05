package com.example.oop_warehouse_location

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
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth

class SignInPage : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            startActivity(Intent(this, forgotPassword::class.java))
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

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
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
                    progressBar.visibility = View.GONE
                    btnLogIn.isEnabled = true

                    if (task.isSuccessful) {
                        Toast.makeText(this, "Sign in successful!", Toast.LENGTH_SHORT).show()
                        // TODO: Navigate to your main/home activity
                        // startActivity(Intent(this, MainActivity::class.java))
                        // finish()
                    } else {
                        val errorMessage = task.exception?.message ?: "Authentication failed"
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    override fun onStart() {
        super.onStart()
        // If user is already signed in, navigate to main screen
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Toast.makeText(this, "Welcome back, ${currentUser.email}!", Toast.LENGTH_SHORT).show()
            // TODO: Navigate to your main/home activity
            // startActivity(Intent(this, MainActivity::class.java))
            // finish()
        }
    }
}