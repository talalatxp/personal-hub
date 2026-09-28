package com.talania.personalhub.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talania.personalhub.core.database.RuntimeRepository
import com.talania.personalhub.core.model.DomainResult
import com.talania.personalhub.feature.settings.data.HubPreferences
import com.talania.personalhub.feature.settings.data.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface BootstrapState {
    data object Loading : BootstrapState
    data class Ready(val preferences: HubPreferences) : BootstrapState
    data object StorageError : BootstrapState
}

@HiltViewModel
class BootstrapViewModel @Inject constructor(
    private val runtime: RuntimeRepository,
    private val preferences: PreferencesRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow<BootstrapState>(BootstrapState.Loading)
    val state = mutableState.asStateFlow()
    private var loading: Job? = null

    init {
        retry()
    }

    fun retry() {
        if (loading?.isActive == true) return
        loading = viewModelScope.launch {
            mutableState.value = BootstrapState.Loading
            try {
                when (runtime.initialize()) {
                    is DomainResult.Failure -> mutableState.value = BootstrapState.StorageError

                    is DomainResult.Success -> preferences.observe().collect {
                        mutableState.value =
                            BootstrapState.Ready(it)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.value = BootstrapState.StorageError
            }
        }
    }
}
