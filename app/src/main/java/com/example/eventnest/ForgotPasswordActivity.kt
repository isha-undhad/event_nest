package com.example.eventnest

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
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class ForgotPasswordActivity : BaseActivity() {

    private val authRepository = AuthRepository()

    private lateinit var btnBack: ImageButton
    private lateinit var tvBackToLogin: TextView
    private lateinit var tilEmail: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var btnResetPassword: MaterialButton
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.forgotPasswordScrollView)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        setupListeners()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tvBackToLogin = findViewById(R.id.tvBackToLogin)
        tilEmail = findViewById(R.id.tilEmail)
        etEmail = findViewById(R.id.etEmail)
        btnResetPassword = findViewById(R.id.btnResetPassword)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        tvBackToLogin.setOnClickListener {
            finish()
        }

        btnResetPassword.setOnClickListener {
            handlePasswordReset()
        }
    }

    private fun handlePasswordReset() {
        val email = etEmail.text?.toString()?.trim().orEmpty()

        tilEmail.error = null

        if (email.isEmpty()) {
            tilEmail.error = getString(R.string.error_empty_email)
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = getString(R.string.error_invalid_email)
            return
        }

        setLoading(true)

        authRepository.sendPasswordResetEmail(email) { result ->
            setLoading(false)
            result.onSuccess {
                Toast.makeText(this, getString(R.string.reset_email_sent_success), Toast.LENGTH_LONG).show()
                finish()
            }.onFailure { exception ->
                val errorMessage = exception.localizedMessage ?: "Failed to send reset email. Please try again."
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        btnResetPassword.isEnabled = !isLoading
        btnResetPassword.text = if (isLoading) "" else getString(R.string.send_reset_link)
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        etEmail.isEnabled = !isLoading
    }
}
