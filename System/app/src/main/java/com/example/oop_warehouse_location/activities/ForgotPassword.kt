package com.example.oop_warehouse_location.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.oop_warehouse_location.R
import com.example.oop_warehouse_location.activities.auth.BaseAuthActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * INHERITANCE — extends [BaseAuthActivity] which provides:
 *   • FirebaseAuth initialisation (from BaseActivity)
 *   • Edge-to-edge setup (from BaseActivity)
 *   • [validateEmail] helper (from BaseAuthActivity)
 *   • [setLoading] toggle (from BaseActivity)
 *
 * Overrides [setupWindowInsets] because this layout uses a LinearLayout
 * root (not ScrollView) with custom padding that includes dp offsets.
 */
class ForgotPassword : BaseAuthActivity() {

    override fun getLayoutResId(): Int = R.layout.activity_forgot_password

    /**
     * Custom insets for Forgot Password — the layout uses a LinearLayout root
     * with extra horizontal/vertical dp padding, unlike the ScrollView-based
     * auth screens.
     */
    override fun setupWindowInsets() {
        val root = findViewById<View>(R.id.main) ?: return
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val horizontalPadding = (32 * resources.displayMetrics.density).toInt()
            val verticalPadding = (25 * resources.displayMetrics.density).toInt()
            v.setPadding(
                systemBars.left + horizontalPadding,
                systemBars.top + verticalPadding,
                systemBars.right + horizontalPadding,
                systemBars.bottom + verticalPadding
            )
            insets
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // UI references
        val emailLayout = findViewById<TextInputLayout>(R.id.textInputLayout)
        val emailInput = findViewById<TextInputEditText>(R.id.EmailInput)
        val btnSubmit = findViewById<MaterialButton>(R.id.btnSubmit)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        // Back to sign in
        findViewById<TextView>(R.id.tvSignIn).setOnClickListener {
            startActivity(Intent(this, SignInPage::class.java))
            finish()
        }

        // Submit button – send password reset email
        btnSubmit.setOnClickListener {
            // Clear previous errors
            emailLayout.error = null
            emailLayout.isErrorEnabled = false

            // Use inherited validation helper (POLYMORPHISM via BaseAuthActivity)
            val email = validateEmail(emailLayout, emailInput) ?: return@setOnClickListener

            // Show loading state — uses inherited helper
            setLoading(progressBar, btnSubmit, true)

            // Send password reset email via Firebase
            try {
                auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener {
                        setLoading(progressBar, btnSubmit, false)
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
                        setLoading(progressBar, btnSubmit, false)
                        Toast.makeText(
                            this,
                            e.message ?: "Failed to send reset email",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            } catch (e: Exception) {
                setLoading(progressBar, btnSubmit, false)
                Toast.makeText(this, "An error occurred: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
