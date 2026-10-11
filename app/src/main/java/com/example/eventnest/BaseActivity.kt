package com.example.eventnest

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat

/**
 * BaseActivity provides centralized edge-to-edge system bar configuration
 * across the entire application. It ensures status bar and navigation bar icons
 * are always clearly visible (dark/black on light background) and transparent
 * so that screen backgrounds extend naturally behind them.
 */
open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setupEdgeToEdge()
        super.onCreate(savedInstanceState)
        applySystemBarAppearance()
    }

    override fun onResume() {
        super.onResume()
        applySystemBarAppearance()
    }

    /**
     * Configures edge-to-edge with explicit light status bar and navigation bar styles.
     * SystemBarStyle.light guarantees dark system icons/text on transparent bars.
     */
    protected open fun setupEdgeToEdge() {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            )
        )
    }

    /**
     * Enforces dark/black text and icons for both status bar and navigation bar
     * using the modern WindowInsetsController API.
     */
    fun applySystemBarAppearance() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }
}
