package com.talania.personalhub.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.talania.personalhub.core.designsystem.HubTheme
import com.talania.personalhub.feature.settings.data.ThemeMode
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val bootstrap: BootstrapViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by bootstrap.state.collectAsStateWithLifecycle()
            val theme = (state as? BootstrapState.Ready)?.preferences?.theme ?: ThemeMode.SYSTEM
            val dark = when (theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            HubTheme(dark) { BaseScreen(state, bootstrap::retry) }
        }
    }
}
