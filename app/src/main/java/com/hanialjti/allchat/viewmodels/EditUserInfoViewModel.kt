package com.hanialjti.allchat.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditUserInfoViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(EditUserInfoUiState())
    val uiState: StateFlow<EditUserInfoUiState> get() = _uiState

    fun setUserName(name: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(name = name)
            }
        }
    }
}

data class EditUserInfoUiState(
    val name: String = "",
    val Image: String? = null,
)