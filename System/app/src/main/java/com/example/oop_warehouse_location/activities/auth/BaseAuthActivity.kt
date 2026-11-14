package com.example.oop_warehouse_location.activities.auth

import android.util.Patterns
import android.widget.ScrollView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.oop_warehouse_location.R
import com.example.oop_warehouse_location.activities.base.BaseActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * INHERITANCE — specialised base for authentication screens.
 *
 * Extracts duplicated validation logic shared by SignIn, SignUp,
 * and ForgotPassword activities, eliminating copy-paste code.
 *
 * Also overrides [setupWindowInsets] to add auto-scroll behaviour
 * when the soft keyboard is visible (common to all auth layouts
 * that use a ScrollView root).
 */
abstract class BaseAuthActivity : BaseActivity() {

    /**
     * Auth screens typically use a ScrollView root that should auto-scroll
     * to keep the focused field visible when the IME appears.
     */
    override fun setupWindowInsets() {
        val root = findViewById<ScrollView>(R.id.main) ?: run {
            super.setupWindowInsets()
            return
        }
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
    }

    /**
     * Validates a non-empty email field with a valid format.
     *
     * @return the trimmed email string, or `null` when validation fails
     *         (the appropriate error is set on [emailLayout]).
     */
    protected fun validateEmail(emailLayout: TextInputLayout, emailInput: TextInputEditText): String? {
        val email = emailInput.text.toString().trim()
        return when {
            email.isEmpty() -> {
                emailLayout.error = "Email is required"
                emailInput.requestFocus()
                null
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                emailLayout.error = "Enter a valid email"
                emailInput.requestFocus()
                null
            }
            else -> {
                emailLayout.error = null
                email
            }
        }
    }

    /**
     * Validates a password with a minimum length.
     *
     * @return the trimmed password string, or `null` when validation fails
     *         (the appropriate error is set on [passwordLayout]).
     */
    protected fun validatePassword(
        passwordLayout: TextInputLayout,
        passwordInput: TextInputEditText,
        minLength: Int = 6
    ): String? {
        val password = passwordInput.text.toString().trim()
        return when {
            password.isEmpty() -> {
                passwordLayout.error = "Password is required"
                passwordInput.requestFocus()
                null
            }
            password.length < minLength -> {
                passwordLayout.error = "Password must be at least $minLength characters"
                passwordInput.requestFocus()
                null
            }
            else -> {
                passwordLayout.error = null
                password
            }
        }
    }
}
