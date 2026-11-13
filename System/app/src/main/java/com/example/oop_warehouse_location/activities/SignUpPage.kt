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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.oop_warehouse_location.R
import com.example.oop_warehouse_location.models.user.User
import com.example.oop_warehouse_location.models.user.UserFactory
import com.example.oop_warehouse_location.models.Warehouse
import com.example.oop_warehouse_location.repository.FirebaseRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import java.util.UUID

class SignUpPage : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val repository = FirebaseRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up_page)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        val root = findViewById<ScrollView>(R.id.main)

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

        // ID
        val accountTypeLayout = findViewById<TextInputLayout>(R.id.textInputLayout6)
        val accountTypeInput = findViewById<MaterialAutoCompleteTextView>(R.id.statusInput)
        val firstNameLayout = findViewById<TextInputLayout>(R.id.textInputLayout)
        val firstNameInput = findViewById<TextInputEditText>(R.id.FirstNameInput)
        val lastNameLayout = findViewById<TextInputLayout>(R.id.textInputLayout2)
        val lastNameInput = findViewById<TextInputEditText>(R.id.LastnameInput)
        val emailLayout = findViewById<TextInputLayout>(R.id.textInputLayout3)
        val emailInput = findViewById<TextInputEditText>(R.id.EmailAddressInput)
        val passwordLayout = findViewById<TextInputLayout>(R.id.textInputLayout4)
        val passwordInput = findViewById<TextInputEditText>(R.id.PasswordInput)
        val confirmPasswordLayout = findViewById<TextInputLayout>(R.id.textInputLayout5)
        val confirmPasswordInput = findViewById<TextInputEditText>(R.id.confirmPasswordInput)
        val btnSignUp = findViewById<MaterialButton>(R.id.btnLogIn)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        // change screen to Sign In
        val tvSignIn = findViewById<TextView>(R.id.tvSignIn)
        tvSignIn.setOnClickListener {
            startActivity(Intent(this, SignInPage::class.java))
            finish()
        }

        // Sign Up button click
        btnSignUp.setOnClickListener {
            val accountType = accountTypeInput.text.toString().trim()
            val firstName = firstNameInput.text.toString().trim()
            val lastName = lastNameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            // Clear previous errors
            accountTypeLayout.error = null
            firstNameLayout.error = null
            lastNameLayout.error = null
            emailLayout.error = null
            passwordLayout.error = null
            confirmPasswordLayout.error = null

            // Validate inputs
            if (accountType.isEmpty()) {
                accountTypeLayout.error = "Select an account type"
                accountTypeInput.requestFocus()
                return@setOnClickListener
            }

            if (firstName.isEmpty()) {
                firstNameLayout.error = "First name is required"
                firstNameInput.requestFocus()
                return@setOnClickListener
            }

            if (lastName.isEmpty()) {
                lastNameLayout.error = "Last name is required"
                lastNameInput.requestFocus()
                return@setOnClickListener
            }

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

            if (confirmPassword.isEmpty()) {
                confirmPasswordLayout.error = "Confirm your password"
                confirmPasswordInput.requestFocus()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                confirmPasswordLayout.error = "Passwords do not match"
                confirmPasswordInput.requestFocus()
                return@setOnClickListener
            }

            // Show loading state
            progressBar.visibility = View.VISIBLE
            btnSignUp.isEnabled = false

            // Firebase create user
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val firebaseUser = auth.currentUser
                        val uid = firebaseUser?.uid ?: ""

                        // Set display name
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName("$firstName $lastName")
                            .build()

                        firebaseUser?.updateProfile(profileUpdates)

                        // Persist Firestore profile even if displayName update fails
                        setupUserProfile(
                            uid = uid,
                            firstName = firstName,
                            lastName = lastName,
                            email = email,
                            accountType = accountType,
                            progressBar = progressBar,
                            btnSignUp = btnSignUp
                        )
                    } else {
                        progressBar.visibility = View.GONE
                        btnSignUp.isEnabled = true
                        val errorMessage = task.exception?.message ?: "Registration failed"
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
        }
    }


    private fun setupUserProfile(
        uid: String,
        firstName: String,
        lastName: String,
        email: String,
        accountType: String,
        progressBar: ProgressBar,
        btnSignUp: MaterialButton
    ) {
        val user = UserFactory.create(
            accountType = accountType,
            uid = uid,
            firstName = firstName,
            lastName = lastName,
            email = email,
            warehouseId = "",  // will be populated for Manager or upon joining for Employee
            createdAt = System.currentTimeMillis()
        )

        if (user.canManageWarehouse()) {
            repository.generateUniqueInviteCode()
                .addOnSuccessListener { inviteCode ->
                    createManagerWarehouse(
                        user = user,
                        inviteCode = inviteCode,
                        progressBar = progressBar,
                        btnSignUp = btnSignUp
                    )
                }
                .addOnFailureListener { e ->
                    progressBar.visibility = View.GONE
                    btnSignUp.isEnabled = true
                    Toast.makeText(this, "Failed to generate invite code: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
        } else {
            // Employee — save user without warehouseId, then redirect to JoinWarehouseActivity
            repository.saveUser(user)
                .addOnSuccessListener {
                    progressBar.visibility = View.GONE
                    btnSignUp.isEnabled = true
                    Toast.makeText(this, "Account created! Please join a warehouse.", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, JoinWarehouseActivity::class.java))
                    finish()
                }
                .addOnFailureListener { e ->
                    progressBar.visibility = View.GONE
                    btnSignUp.isEnabled = true
                    Toast.makeText(this, "Failed to save profile: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun createManagerWarehouse(
        user: User,
        inviteCode: String,
        progressBar: ProgressBar,
        btnSignUp: MaterialButton
    ) {
        val warehouseId = UUID.randomUUID().toString()
        user.warehouseId = warehouseId

        val warehouse = Warehouse(
            warehouseId = warehouseId,
            name = "${user.firstName}'s Warehouse",
            totalCapacity = 500,
            ownerId = user.uid,
            inviteCode = inviteCode,
            members = mutableListOf(),
            createdAt = System.currentTimeMillis()
        )

        repository.createWarehouse(warehouse)
            .addOnSuccessListener {
                repository.saveUser(user)
                    .addOnSuccessListener {
                        progressBar.visibility = View.GONE
                        btnSignUp.isEnabled = true
                        Toast.makeText(
                            this,
                            "Account created! Your invite code: $inviteCode",
                            Toast.LENGTH_LONG
                        ).show()
                        val intent = Intent(this, MainActivity::class.java)
                        intent.putExtra("warehouseId", warehouseId)
                        intent.putExtra("accountType", user.getRole())
                        startActivity(intent)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        progressBar.visibility = View.GONE
                        btnSignUp.isEnabled = true
                        Toast.makeText(this, "Failed to save user: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                btnSignUp.isEnabled = true
                Toast.makeText(this, "Failed to create warehouse: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }
}