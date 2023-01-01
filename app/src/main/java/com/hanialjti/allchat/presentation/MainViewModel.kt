package com.hanialjti.allchat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanialjti.allchat.data.local.datastore.LoggedInUser
import com.hanialjti.allchat.data.local.datastore.UserCredentials
import com.hanialjti.allchat.data.local.datastore.UserPreferencesManager
import com.hanialjti.allchat.data.repository.IChatRepository
import com.hanialjti.allchat.domain.usecase.LoggedInUserUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val userPreferencesManager: UserPreferencesManager,
    private val chatRepository: IChatRepository,
    private val getLoggedInUserUseCase: LoggedInUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> get() = _uiState

    init {
//        viewModelScope.launch { chatRepository.listenForMessageUpdates() }
//        viewModelScope.launch { chatRepository.observeChatStates() }

        viewModelScope.launch {
            getLoggedInUserUseCase()
                .collectLatest { user ->
                    _uiState.update {
                        it.copy(loggedInUser = user)
                    }
                }
        }
    }

    fun updateUserCredentials(userCredentials: UserCredentials) {
        viewModelScope.launch {
            userPreferencesManager.updateUserCredentials(userCredentials)
//            _uiState.update {
//                MainUiState(isLoggedIn = true, userCredentials = userCredentials)
//            }
        }
    }

}

data class MainUiState(
    val isLoggedIn: Boolean = false,
    val loggedInUser: LoggedInUser? = null
)