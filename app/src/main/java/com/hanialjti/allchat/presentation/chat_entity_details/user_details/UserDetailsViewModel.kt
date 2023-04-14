package com.hanialjti.allchat.presentation.chat_entity_details.user_details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanialjti.allchat.data.repository.UserRepository
import com.hanialjti.allchat.presentation.chat.defaultName
import com.hanialjti.allchat.presentation.conversation.ContactImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UserDetailsViewModel(
    private val userRepository: UserRepository,
    private val userId: String
) : ViewModel() {

    private val _userDetailsUiState = MutableStateFlow(UserDetailsUiState())
    val userDetailsUiState = _userDetailsUiState.asStateFlow()

    init {
        viewModelScope.launch {
            val userDetails = userRepository.getUserDetails(userId)

            userDetails?.let {
                _userDetailsUiState.update { uiState ->
                    uiState.copy(
                        name = userDetails.name ?: defaultName,
                        avatar = userDetails.avatar,
                        status = userDetails.status,
                        isBlocked = userDetails.isBlocked,
                        isOnline = userDetails.isOnline
                    )
                }
            }

        }
    }

}

data class UserDetailsUiState(
    val name: String = "",
    val avatar: ContactImage = ContactImage.DefaultProfileImage(false),
    val status: String? = null,
    val isBlocked: Boolean = false,
    val isOnline: Boolean = false
)