package com.yach.kinetix

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.yach.kinetix.core.ui.theme.KinetixTheme
import com.yach.kinetix.frontofhouse.ui.PosScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KinetixTheme {
                // Superficie principal usando el color de fondo del tema
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Para previsualizar el Backoffice, llamamos a BackofficeScreen
                    // con su respectivo ViewModel (inyectado automáticamente por Hilt)
                    val treasuryViewModel: com.yach.kinetix.treasury.ui.TreasuryViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                    com.yach.kinetix.treasury.ui.TreasuryScreen(viewModel = treasuryViewModel)
                }
            }
        }
    }
}
