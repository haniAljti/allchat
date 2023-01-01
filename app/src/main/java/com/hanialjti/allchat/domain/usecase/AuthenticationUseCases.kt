package com.hanialjti.allchat.domain.usecase

data class AuthenticationUseCases(
    val signIn: SignIn,
    val signOut: SignOut,
    val getConnectedUserUseCase: GetConnectedUserUseCase
)
