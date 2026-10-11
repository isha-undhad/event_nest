package com.example.eventnest

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.ImageButton
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

class RegisterActivity : BaseActivity() {

    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()

    private lateinit var btnBack: ImageButton
    private lateinit var tvSignIn: TextView
    private lateinit var tilFullName: TextInputLayout
    private lateinit var etFullName: TextInputEditText
    private lateinit var tilEmail: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etPassword: TextInputEditText
    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var btnSignUp: MaterialButton
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.registerScrollView)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        setupListeners()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tvSignIn = findViewById(R.id.tvSignIn)
        tilFullName = findViewById(R.id.tilFullName)
        etFullName = findViewById(R.id.etFullName)
        tilEmail = findViewById(R.id.tilEmail)
        etEmail = findViewById(R.id.etEmail)
        tilPassword = findViewById(R.id.tilPassword)
        etPassword = findViewById(R.id.etPassword)
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnSignUp = findViewById(R.id.btnSignUp)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        tvSignIn.setOnClickListener {
            finish()
        }

        btnSignUp.setOnClickListener {
            handleRegister()
        }
    }

    private fun handleRegister() {
        val fullName = etFullName.text?.toString()?.trim().orEmpty()
        val email = etEmail.text?.toString()?.trim().orEmpty()
        val password = etPassword.text?.toString().orEmpty()
        val confirmPassword = etConfirmPassword.text?.toString().orEmpty()

        tilFullName.error = null
        tilEmail.error = null
        tilPassword.error = null
        tilConfirmPassword.error = null

        if (fullName.isEmpty()) {
            tilFullName.error = getString(R.string.error_empty_name)
            return
        }

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

        if (password.length < 6) {
            tilPassword.error = getString(R.string.error_password_too_short)
            return
        }

        if (password != confirmPassword) {
            tilConfirmPassword.error = getString(R.string.error_passwords_do_not_match)
            return
        }

        setLoading(true)

        authRepository.registerWithEmail(email, password, fullName) { result ->
            result.onSuccess { user ->
                lifecycleScope.launch {
                    userRepository.syncUserToFirestore(
                        uid = user.uid,
                        name = fullName,
                        email = email
                    )
                    setLoading(false)
                    Toast.makeText(this@RegisterActivity, getString(R.string.registration_success), Toast.LENGTH_SHORT).show()
                    navigateToMain()
                }
            }.onFailure { exception ->
                setLoading(false)
                val errorMessage = exception.localizedMessage ?: "Registration failed. Please try again."
                Toast.makeText(this@RegisterActivity, errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        btnSignUp.isEnabled = !isLoading
        btnSignUp.text = if (isLoading) "" else getString(R.string.sign_up)
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        etFullName.isEnabled = !isLoading
        etEmail.isEnabled = !isLoading
        etPassword.isEnabled = !isLoading
        etConfirmPassword.isEnabled = !isLoading
    }

    private fun navigateToMain() {
        val intent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
