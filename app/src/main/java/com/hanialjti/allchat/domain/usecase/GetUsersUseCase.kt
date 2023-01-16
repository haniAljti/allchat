package com.hanialjti.allchat.domain.usecase

import com.hanialjti.allchat.data.model.User
import com.hanialjti.allchat.data.repository.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.firstOrNull

class GetUsersUseCase(
    private val userRepository: UserRepository
) {

    operator fun invoke(): Flow<List<User>> = userRepository.getAllUsersByOwnerId()

}