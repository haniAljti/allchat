package com.hanialjti.allchat.domain.usecase

import com.hanialjti.allchat.data.local.room.entity.asUser
import com.hanialjti.allchat.data.model.User
import com.hanialjti.allchat.data.repository.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapLatest

class GetUsersUseCase(
    private val userRepository: UserRepository
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend operator fun invoke(): Flow<List<User>> {
        val owner = userRepository.loggedInUser.firstOrNull()
        return owner?.let { loggedInUser ->

            val userId = loggedInUser.id

            userRepository.getAllUsersByOwnerId(userId)
                .mapLatest { userList ->
                    userList.map { it.asUser() }
//                    it.map { contact ->
//                            val user = contact.to
//                            if (user != null) {
//                                User(
//                                    id = user?.id,
//                                    name = user?.name,
//                                    image = if (user?.image != null) ContactImage.DynamicImage(
//                                        user.image
//                                    ) else ContactImage.ImageRes(R.drawable.ic_user),
//                                    isOnline = user?.isOnline ?: false,
//                                    lastOnline = user?.lastOnline
//                                )
//                            } else null
//                        }
//                        .filter { it != null }
                }

        } ?: emptyFlow()
    }
}