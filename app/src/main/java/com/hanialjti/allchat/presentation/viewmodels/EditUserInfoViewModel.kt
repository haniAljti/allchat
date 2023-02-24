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

//    init {
//        viewModelScope.launch { userRepository.listenForUsername() }
//    }

    fun setUserName(name: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(name = name)
            }
        }
    }

    fun setAvatar(avatar: ByteArray?) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(avatar = avatar)
            }
        }
    }

    fun updateUserInfo() {
        viewModelScope.launch {
            val username = _uiState.value.name
            val avatar = _uiState.value.avatar
            if (username.isNotBlank()) userRepository.updateUserNickname(username)
            userRepository.updateAvatar(avatar)
        }
    }
}

data class EditUserInfoUiState(
    val name: String = "",
    val avatar: ByteArray? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EditUserInfoUiState

        if (name != other.name) return false
        if (avatar != null) {
            if (other.avatar == null) return false
            if (!avatar.contentEquals(other.avatar)) return false
        } else if (other.avatar != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + (avatar?.contentHashCode() ?: 0)
        return result
    }
}