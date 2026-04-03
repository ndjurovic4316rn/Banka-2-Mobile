package com.example.banka_2_mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.banka_2_mobile.data.api.RetrofitClient
import com.example.banka_2_mobile.data.repository.AuthRepository
import com.example.banka_2_mobile.ui.navigation.NavGraph
import com.example.banka_2_mobile.ui.theme.Banka2MobileTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(0xFF070B24.toInt()),
            navigationBarStyle = SystemBarStyle.dark(0xFF070B24.toInt())
        )
        super.onCreate(savedInstanceState)

        val authRepository = AuthRepository(applicationContext)
        RetrofitClient.init(authRepository)

        setContent {
            Banka2MobileTheme {
                NavGraph()
            }
        }
    }
}
