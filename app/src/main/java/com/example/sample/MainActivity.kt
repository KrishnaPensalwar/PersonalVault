package com.example.sample

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.example.sample.navigation.AppNavigation
import com.example.sample.security.AppLockGate
import com.example.sample.ui.theme.SampleTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SampleTheme {
                AppLockGate {
                    AppNavigation()
                }
            }
        }
    }
}
