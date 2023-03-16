package com.hanialjti.allchat.data.repository

import com.hanialjti.allchat.data.local.room.dao.InfoDao
import com.hanialjti.allchat.data.local.room.entity.InfoEntity
import com.hanialjti.allchat.data.model.Avatar
import com.hanialjti.allchat.data.remote.InfoRemoteDataSource
import com.hanialjti.allchat.data.remote.model.CallResult
import com.hanialjti.allchat.data.remote.model.NicknameUpdate
import kotlinx.coroutines.flow.onEach

class InfoRepository(
    private val infoDao: InfoDao,
    private val infoRemoteDataSource: InfoRemoteDataSource,
    private val fileRepository: FileRepository
) {

    suspend fun fetchAndSaveInfo(id: String, isGroupChat: Boolean): InfoEntity? {
        val infoResult = infoRemoteDataSource.getUpdatedEntityInfo(id, isGroupChat)
        if (infoResult is CallResult.Error) {
            return null
        }
        val avatar = (infoResult as CallResult.Success).data?.avatar
        val nickname = infoResult.data?.name
        val avatarPath = when (avatar) {
            is Avatar.Raw -> {
                val imageFile = fileRepository.createNewAvatarFile("$id.png")
                fileRepository.downloadAndSaveToInternalStorage(avatar.bytes, imageFile)
            }
            is Avatar.Url -> {
                val imageFile = fileRepository.createNewAvatarFile("$id.png")
                fileRepository.downloadAndSaveToInternalStorage(avatar.imageUrl, imageFile)
            }
            else -> {
                null
            }
        }
        return nickname?.let {
            val infoEntity = InfoEntity(
                id = id,
                cachePath = avatarPath?.toString(),
                avatarHash = if (avatar is Avatar.Raw) infoRemoteDataSource.hashAvatarBytes(avatar.bytes) else null,
                nickname = it
            )
            infoDao.insert(infoEntity)
            infoEntity
        }
    }

    suspend fun nicknameStream() = infoRemoteDataSource.infoUpdateStream()
        .onEach { info ->
            when (info) {
                is NicknameUpdate -> {
                    if (infoDao.getOne(info.entityId) == null) {
                        infoDao.insert(
                            InfoEntity(
                                id = info.entityId,
                                nickname = info.nickname,
                                cachePath = null,
                                avatarHash = null
                            )
                        )
                    } else {
                        infoDao.updateNickname(info.nickname, info.entityId)
                    }
                }
                else -> {}
            }
        }

    suspend fun fetchUserNickname(userId: String) =
        infoRemoteDataSource.fetchNickname(userId, false)

    suspend fun getInfoFor(id: String): InfoEntity? = infoDao.getOne(id)
}