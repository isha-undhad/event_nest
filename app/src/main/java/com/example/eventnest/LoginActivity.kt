package com.example.eventnest

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class LoginActivity : BaseActivity() {

    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()

    private lateinit var tilEmail: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etPassword: TextInputEditText
    private lateinit var tvForgotPassword: TextView
    private lateinit var btnSignIn: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var btnGoogleSignIn: View
    private lateinit var tvSignUp: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.loginScrollView)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        setupListeners()
    }

    private fun initViews() {
        tilEmail = findViewById(R.id.tilEmail)
        etEmail = findViewById(R.id.etEmail)
        tilPassword = findViewById(R.id.tilPassword)
        etPassword = findViewById(R.id.etPassword)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        btnSignIn = findViewById(R.id.btnSignIn)
        progressBar = findViewById(R.id.progressBar)
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)
        tvSignUp = findViewById(R.id.tvSignUp)
    }

    private fun setupListeners() {
        btnSignIn.setOnClickListener {
            handleEmailLogin()
        }

        btnGoogleSignIn.setOnClickListener {
            handleGoogleSignIn()
        }

        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        tvSignUp.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun handleEmailLogin() {
        val email = etEmail.text?.toString()?.trim().orEmpty()
        val password = etPassword.text?.toString().orEmpty()

        tilEmail.error = null
        tilPassword.error = null

        if (email.isEmpty()) {
            tilEmail.error = getString(R.string.error_empty_email)
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = getString(R.string.error_invalid_email)
            return
        }

        if (password.isEmpty()) {
            tilPassword.error = getString(R.string.error_empty_password)
            return
        }

        setLoading(true)

        authRepository.loginWithEmail(email, password) { result ->
            setLoading(false)
            result.onSuccess {
                Toast.makeText(this, getString(R.string.login_success), Toast.LENGTH_SHORT).show()
                navigateToMain()
            }.onFailure { exception ->
                val errorMessage = exception.localizedMessage ?: "Authentication failed. Please check your credentials."
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun handleGoogleSignIn() {
        val webClientId = getString(R.string.default_web_client_id)
        if (webClientId.isBlank() || webClientId == "PASTE_YOUR_WEB_CLIENT_ID_HERE") {
            Toast.makeText(
                this,
                getString(R.string.error_google_client_id_missing),
                Toast.LENGTH_LONG
            ).show()
            return
        }

        setLoading(true)

        lifecycleScope.launch {
            val result = authRepository.signInWithGoogle(this@LoginActivity, webClientId)
            result.onSuccess { user ->
                val name = user.displayName?.takeIf { it.isNotBlank() } ?: user.email?.substringBefore("@") ?: "User"
                val email = user.email ?: ""
                val syncResult = userRepository.syncUserToFirestore(
                    uid = user.uid,
                    name = name,
                    email = email
                )
                syncResult.onSuccess {
                    Log.d(TAG, "Successfully synced user ${user.uid} to Firestore.")
                }.onFailure { syncException ->
                    Log.e(TAG, "Failed to sync user ${user.uid} to Firestore.", syncException)
                }
                setLoading(false)
                Toast.makeText(this@LoginActivity, "Welcome, $name!", Toast.LENGTH_SHORT).show()
                navigateToMain()
            }.onFailure { exception ->
                setLoading(false)
                val errorMessage = exception.localizedMessage ?: "Google Sign-In failed."
                Toast.makeText(this@LoginActivity, errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        btnSignIn.isEnabled = !isLoading
        btnSignIn.text = if (isLoading) "" else getString(R.string.sign_in)
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnGoogleSignIn.isEnabled = !isLoading
        etEmail.isEnabled = !isLoading
        etPassword.isEnabled = !isLoading
    }

    private fun navigateToMain() {
        val intent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    companion object {
        private const val TAG = "LoginActivity"
    }
}
