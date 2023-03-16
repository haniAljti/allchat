package com.hanialjti.allchat.presentation.info

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class InfoViewModel: ViewModel() {

    private val _infoUiState = MutableStateFlow(InfoUiState())
    val infoUiState = _infoUiState.asStateFlow()


}

data class InfoUiState(
    val name: String? = null,
)