package com.example.eventnest

import android.app.Activity
import android.app.Application
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowCompat

class EventNestApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Globally enforce light mode to prevent system night mode from toggling white status bar icons
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // Global activity lifecycle callback ensuring every activity, fragment, and screen
        // renders dark/black status bar and navigation bar icons.
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                applySystemBarConfiguration(activity)
            }

            override fun onActivityStarted(activity: Activity) {}

            override fun onActivityResumed(activity: Activity) {
                applyLightSystemBarAppearance(activity)
            }

            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {}

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    private fun applySystemBarConfiguration(activity: Activity) {
        if (activity is ComponentActivity) {
            activity.enableEdgeToEdge(
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
        applyLightSystemBarAppearance(activity)
    }

    private fun applyLightSystemBarAppearance(activity: Activity) {
        WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }
}
