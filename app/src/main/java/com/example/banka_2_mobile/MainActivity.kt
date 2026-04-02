package com.example.banka_2_mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.view.WindowManager
import com.example.banka_2_mobile.data.api.RetrofitClient
import com.example.banka_2_mobile.data.repository.AuthRepository
import com.example.banka_2_mobile.ui.navigation.NavGraph
import com.example.banka_2_mobile.ui.theme.Banka2MobileTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize RetrofitClient with AuthRepository for token management
        val authRepository = AuthRepository(applicationContext)
        RetrofitClient.init(authRepository)

        @Suppress("DEPRECATION")
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        setContent {
            Banka2MobileTheme(darkTheme = true) {
                NavGraph()
            }
        }
    }
}
