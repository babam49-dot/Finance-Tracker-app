package com.example.ethfinance

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.LocaleListCompat
import com.example.ethfinance.core.designsystem.EthFinanceTheme
import com.example.ethfinance.data.local.prefs.SettingsDataStore
import com.example.ethfinance.presentation.navigation.AppNavigation
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val language by settingsDataStore.languageFlow.collectAsState(initial = "en")
            val localizedContext = createLocalizedContext(language)

            CompositionLocalProvider(LocalContext provides localizedContext) {
                EthFinanceTheme {
                    AppNavigation()
                }
            }
        }
    }

    private fun createLocalizedContext(languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        return createConfigurationContext(config)
    }
}
