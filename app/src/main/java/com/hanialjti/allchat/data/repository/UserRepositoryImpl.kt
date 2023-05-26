package com.hanialjti.allchat.data.repository

import com.hanialjti.allchat.common.utils.Logger
import com.hanialjti.allchat.common.utils.StringUtils
import com.hanialjti.allchat.data.local.room.AllChatLocalRoomDatabase
import com.hanialjti.allchat.data.local.room.dao.BlockedUserDao
import com.hanialjti.allchat.data.local.room.dao.InfoDao
import com.hanialjti.allchat.data.local.room.dao.UserDao
import com.hanialjti.allchat.data.local.room.entity.BlockedUserEntity
import com.hanialjti.allchat.data.local.room.entity.InfoEntity
import com.hanialjti.allchat.data.local.room.entity.UserEntity
import com.hanialjti.allchat.data.local.room.entity.asUser
import com.hanialjti.allchat.data.local.room.model.asUserDetails
import com.hanialjti.allchat.data.model.Avatar
import com.hanialjti.allchat.data.model.UserDetails
import com.hanialjti.allchat.data.remote.UserRemoteDataSource
import com.hanialjti.allchat.data.remote.model.*
import com.hanialjti.allchat.presentation.conversation.ContactImage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.util.*

class UserRepositoryImpl(
    localDataSource: AllChatLocalRoomDatabase,
    private val blockedUserDao: BlockedUserDao,
    private val remoteDataSource: UserRemoteDataSource,
    private val authenticationRepository: AuthRepository,
    private val preferencesRepository: PreferencesRepository,
    private val fileRepository: FileRepository,
    private val userDao: UserDao = localDataSource.userDao(),
    private val infoDao: InfoDao = localDataSource.infoDao()
) : UserRepository {


    private suspend fun ownerId() = authenticationRepository.loggedInUserStream.first()

    override suspend fun getAndSaveUser(userId: String?) {

        val userId = userId ?: ownerId() ?: return

        var user: UserEntity? = userDao.findById(userId)

        if (user != null) {
            return
        }

        user = UserEntity(id = userId)
        userDao.insertUser(user)
        fetchAndUpdateUserInfo(userId)
    }

    override suspend fun getUserDetails(userId: String?): UserDetails? {
        val userDetails = (userId ?: ownerId())?.let { userDao.getUserDetails(it) }
        return userDetails?.asUserDetails()
    }

    private suspend fun fetchAndUpdateUserInfo(userId: String) {
        try {

            val userInfoResult = remoteDataSource.getUpdatedUserInfo(userId)

            if (userInfoResult is CallResult.Success) {

                val avatar = userInfoResult.data?.avatar
                val nickname = userInfoResult.data?.name
                val avatarUri = downloadAvatar(userId, avatar)

                val info = infoDao.getOne(userId)?.copy(
                    cachePath = avatarUri?.toString(),
                    avatarHash = extractAvatarHash(avatar),
                    nickname = nickname
                ) ?: InfoEntity(
                    id = userId,
                    cachePath = avatarUri?.toString(),
                    avatarHash = extractAvatarHash(avatar),
                    nickname = nickname
                )

                infoDao.insert(info)
            }
        } catch (e: Exception) {
            Logger.e(e)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun userUpdatesStream() =
        authenticationRepository
            .loggedInUserStream
            .flatMapLatest { owner ->
                Logger.d { "owner is $owner" }
                if (owner == null) flowOf()
                else {
                    remoteDataSource.usersUpdateStream()
                        .onStart {
                            Logger.d { "Listening for user updates" }
                        }
                        .onEach { userUpdate ->

                            val user =
                                userDao.findById(userUpdate.userId) ?: UserEntity(userUpdate.userId)

                            if (userUpdate !is PresenceUpdate) {
                                userDao.upsertUser(user)
                            }

                            when (userUpdate) {
                                is PresenceUpdate -> {
                                    userDao.updatePresence(
                                        userId = userUpdate.userId,
                                        isOnline = userUpdate.isOnline,
                                        lastOnline = userUpdate.lastOnline,
                                        status = userUpdate.status
                                    )
                                }
                                is NicknameUpdated -> {
                                    val info = infoDao.getOne(userUpdate.userId)?.copy(
                                        nickname = userUpdate.nickname
                                    ) ?: InfoEntity(
                                        id = userUpdate.userId,
                                        nickname = userUpdate.nickname
                                    )
                                    infoDao.insert(info)
                                }
                                is AvatarUpdated -> {
                                    val avatar = userUpdate.avatar
                                    val userId = userUpdate.userId
                                    val avatarUri = downloadAvatar(userId, avatar)
                                    val hash = extractAvatarHash(avatar)
                                    val info = infoDao.getOne(userId)?.copy(
                                        cachePath = avatarUri?.toString(),
                                        avatarHash = hash,
                                    ) ?: InfoEntity(
                                        id = userId,
                                        cachePath = avatarUri?.toString(),
                                        avatarHash = hash,
                                    )
                                    infoDao.insert(info)
                                }
                                is AvatarMetadataUpdated -> {
                                    Logger.d { "Received avatar metadata update" }
                                    val info = infoDao.getOne(userUpdate.userId)

                                    if (info == null || info.avatarHash != userUpdate.hash) {

                                        val avatarResult = remoteDataSource.fetchAvatar(
                                            userUpdate.userId,
                                            userUpdate.hash
                                        )

                                        if (avatarResult is CallResult.Success) {
                                            val avatar = avatarResult.data
                                            val userId = userUpdate.userId
                                            val avatarPath = downloadAvatar(userId, avatar)
                                            infoDao.insert(
                                                info?.copy(
                                                    cachePath = avatarPath?.toString(),
                                                    avatarHash = extractAvatarHash(avatar),
                                                ) ?: InfoEntity(
                                                    id = userUpdate.userId,
                                                    cachePath = avatarPath?.toString(),
                                                    avatarHash = extractAvatarHash(avatar),
                                                )
                                            )
                                        }
                                    }

                                }
                            }
                        }
                }
            }


    private fun extractAvatarHash(avatar: Avatar?) =
        if (avatar is Avatar.Raw) StringUtils.sha1(avatar.bytes) else null

    private suspend fun downloadAvatar(
        userId: String,
        avatar: Avatar?
    ) = when (avatar) {
        is Avatar.Raw -> {
            val imageFile =
                fileRepository.createNewAvatarFile("${userId}.png")
            fileRepository.downloadAndSaveToInternalStorage(
                avatar.bytes,
                imageFile
            )
        }
        is Avatar.Url -> {
            val imageFile =
                fileRepository.createNewAvatarFile("${userId}.png")
            fileRepository.downloadAndSaveToInternalStorage(
                avatar.imageUrl,
                imageFile
            )
        }
        else -> null
    }


    override suspend fun blockUser(userId: String) {
        val owner = authenticationRepository.loggedInUserStream.first() ?: return
        val blockResult = remoteDataSource.blockUser(userId)
        if (blockResult is CallResult.Success) {
            blockedUserDao.insertBlockedUser(BlockedUserEntity(userId, owner))
        }
    }

    override suspend fun unblockUser(userId: String) {
        val owner = authenticationRepository.loggedInUserStream.first() ?: return
        val blockResult = remoteDataSource.unblockUser(userId)
        if (blockResult is CallResult.Success) {
            blockedUserDao.removeBlockedUser(BlockedUserEntity(userId, owner))
        }
    }

    override fun isBlocked(userId: String): Flow<Boolean> = authenticationRepository
        .loggedInUserStream
        .transform { owner ->
            if (owner != null)
                blockedUserDao.fetchBlockedUserFlow(userId, owner)
                    .collect { emit(it != null) }
            else emptyFlow<Boolean>()
        }


    override suspend fun updateMyInfo(
        name: String,
        avatar: ContactImage?,
        status: String?
    ): CallResult<Boolean> {
        val myProfileUpdateResult = remoteDataSource.updateMyInfo(name, avatar, status)

        if (myProfileUpdateResult is CallResult.Success) run {

            val ownerId = ownerId() ?: return@run
            var myInfo = infoDao.getOne(ownerId) ?: InfoEntity(ownerId)

            if (avatar is ContactImage.DynamicRawImage) {
                val avatarFile = fileRepository.createNewAvatarFile(ownerId)
                val avatarUri =
                    fileRepository.downloadAndSaveToInternalStorage(avatar.bytes, avatarFile)
                myInfo = myInfo.copy(
                    cachePath = avatarUri?.toString(),
                    avatarHash = extractAvatarHash(Avatar.Raw(avatar.bytes))
                )
            }

            infoDao.insert(myInfo.copy(nickname = name))

            preferencesRepository.updatePresenceStatus(status)
            userDao.updatePresence(ownerId, true, null, status)
        }
        return myProfileUpdateResult
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getAllUsers() = authenticationRepository
        .loggedInUserStream
        .flatMapLatest {
            it?.let {
                userDao
                    .getAllByOwnerId(it)
                    .map { allUsers ->
                        allUsers.map { userEntry -> userEntry.asUserDetails() }
                    }
            } ?: emptyFlow()
        }


    override suspend fun getUsers(userIds: List<String>) = userDao.getUsers(userIds)
}