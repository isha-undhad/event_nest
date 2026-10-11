package com.example.eventnest.utils

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

object NavigationUtils {

    /**
     * Applies edge-to-edge window insets to the root layout and BottomNavigationView.
     * The root layout receives top, left, and right insets (status bar and horizontal cutouts),
     * while the BottomNavigationView receives the bottom system navigation bar inset as padding.
     *
     * This guarantees that navigation items (icons + labels) sit comfortably above both
     * 3-button navigation bars and gesture navigation bars without clipping on any device.
     */
    fun setupBottomNavInsets(rootView: View, bottomNav: BottomNavigationView) {
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            bottomNav.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
    }
}
