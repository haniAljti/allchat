package com.hanialjti.allchat.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanialjti.allchat.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditUserInfoViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditUserInfoUiState())
    val uiState: StateFlow<EditUserInfoUiState> get() = _uiState

    init {
        viewModelScope.launch { userRepository.listenForUsername() }
    }

    fun setUserName(name: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(name = name)
            }
        }
    }

    fun updateUserInfo() {
        viewModelScope.launch {
            val username = _uiState.value.name
            userRepository.updateUserInfo(username)
        }
    }
}

data class EditUserInfoUiState(
    val name: String = "",
    val Image: String? = null,
)