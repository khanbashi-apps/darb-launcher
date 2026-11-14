package com.khanbashi.darb

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.khanbashi.darb.location.SpeedTracker
import com.khanbashi.darb.quran.QuranPlayer
import com.khanbashi.darb.ui.DarbApp

class MainActivity : ComponentActivity() {

    private val locationPermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result.values.any { it }) SpeedTracker.start(this)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        // As a home launcher, the system Back button must never close the home screen.
        onBackPressedDispatcher.addCallback(this) { }

        QuranPlayer.init(applicationContext)

        if (SpeedTracker.hasPermission(this)) {
            SpeedTracker.start(this)
        } else {
            locationPermission.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }

        setContent { DarbApp() }
    }
}
