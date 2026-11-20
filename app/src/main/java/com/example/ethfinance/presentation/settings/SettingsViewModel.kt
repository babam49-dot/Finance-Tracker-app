package com.example.ethfinance.presentation.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ethfinance.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val mode: String = "DEMO",
    val theme: String = "SYSTEM",
    val fontScale: Float = 1.0f,
    val boldNumbers: Boolean = false,
    val language: String = "en",
    val useEthiopianCalendar: Boolean = false,
    val currencyDisplay: String = "ETB",
    val keepOriginalSms: Boolean = false,
    val showModeDialog: Boolean = false
)

sealed interface SettingsUiEvent {
    data class SetTheme(val theme: String) : SettingsUiEvent
    data class SetFontScale(val scale: Float) : SettingsUiEvent
    data class SetBoldNumbers(val bold: Boolean) : SettingsUiEvent
    data class SetLanguage(val languageCode: String) : SettingsUiEvent
    data class SetEthiopianCalendar(val enabled: Boolean) : SettingsUiEvent
    data class SetCurrencyDisplay(val currency: String) : SettingsUiEvent
    data object RequestToggleMode : SettingsUiEvent
    data object ConfirmSwitchMode : SettingsUiEvent
    data object DismissModeDialog : SettingsUiEvent
    data object DeleteAllData : SettingsUiEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val baseFlow = combine(
                settingsRepository.modeFlow,
                settingsRepository.themeFlow,
                settingsRepository.fontScaleFlow,
                settingsRepository.boldNumbersFlow,
                settingsRepository.languageFlow
            ) { mode, theme, fontScale, bold, lang ->
                listOf(mode, theme, fontScale, bold, lang)
            }

            val extraFlow = combine(
                settingsRepository.useEthiopianCalendarFlow,
                settingsRepository.currencyDisplayFlow
            ) { ethCal, curr ->
                Pair(ethCal, curr)
            }

            combine(baseFlow, extraFlow) { base, extra ->
                SettingsUiState(
                    mode = base[0] as String,
                    theme = base[1] as String,
                    fontScale = base[2] as Float,
                    boldNumbers = base[3] as Boolean,
                    language = base[4] as String,
                    useEthiopianCalendar = extra.first,
                    currencyDisplay = extra.second
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun onEvent(event: SettingsUiEvent) {
        when (event) {
            is SettingsUiEvent.SetTheme -> {
                viewModelScope.launch { settingsRepository.setTheme(event.theme) }
            }
            is SettingsUiEvent.SetFontScale -> {
                viewModelScope.launch { settingsRepository.setFontScale(event.scale) }
            }
            is SettingsUiEvent.SetBoldNumbers -> {
                viewModelScope.launch { settingsRepository.setBoldNumbers(event.bold) }
            }
            is SettingsUiEvent.SetLanguage -> {
                viewModelScope.launch {
                    settingsRepository.setLanguage(event.languageCode)
                    val appLocale = LocaleListCompat.forLanguageTags(event.languageCode)
                    AppCompatDelegate.setApplicationLocales(appLocale)
                }
            }
            is SettingsUiEvent.SetEthiopianCalendar -> {
                viewModelScope.launch { settingsRepository.setEthiopianCalendar(event.enabled) }
            }
            is SettingsUiEvent.SetCurrencyDisplay -> {
                viewModelScope.launch { settingsRepository.setCurrencyDisplay(event.currency) }
            }
            is SettingsUiEvent.RequestToggleMode -> {
                _uiState.update { it.copy(showModeDialog = true) }
            }
            is SettingsUiEvent.ConfirmSwitchMode -> {
                viewModelScope.launch {
                    if (_uiState.value.mode == "DEMO") {
                        settingsRepository.switchToLiveMode()
                    } else {
                        settingsRepository.switchToDemoMode()
                    }
                    _uiState.update { it.copy(showModeDialog = false) }
                }
            }
            is SettingsUiEvent.DismissModeDialog -> {
                _uiState.update { it.copy(showModeDialog = false) }
            }
            is SettingsUiEvent.DeleteAllData -> {
                viewModelScope.launch {
                    settingsRepository.deleteAllUserData()
                }
            }
        }
    }
}
