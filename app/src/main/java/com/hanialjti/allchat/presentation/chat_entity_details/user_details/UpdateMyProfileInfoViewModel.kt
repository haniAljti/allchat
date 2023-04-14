package com.hanialjti.allchat.presentation.chat_entity_details.user_details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanialjti.allchat.data.repository.UserRepository
import com.hanialjti.allchat.presentation.conversation.ContactImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UpdateMyProfileInfoViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditUserInfoUiState())
    val uiState: StateFlow<EditUserInfoUiState> get() = _uiState

    init {
        viewModelScope.launch {

            val myDetails = userRepository.getUserDetails(null) ?: return@launch

            _uiState.update {
                it.copy(
                    name = myDetails.name ?: "",
                    status = myDetails.status ?: "",
                    avatar = myDetails.avatar,
                )
            }
        }
    }

    fun setUsername(name: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(name = name)
            }
        }
    }

    fun setStatus(status: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(status = status)
            }
        }
    }

    fun setAvatar(avatar: ContactImage) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(avatar = avatar)
            }
        }
    }

    fun updateUserInfo() {
        viewModelScope.launch {
            val username = _uiState.value.name
            val status = _uiState.value.status
            val avatar = _uiState.value.avatar

            userRepository.updateMyInfo(
                username,
                avatar,
                status
            )
        }
    }
}

data class EditUserInfoUiState(
    val name: String = "",
    val status: String = "",
    val avatar: ContactImage = ContactImage.DefaultUserImage,
)