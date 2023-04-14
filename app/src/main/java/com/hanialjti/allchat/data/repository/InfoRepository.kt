package com.hanialjti.allchat.data.repository

import com.hanialjti.allchat.data.local.room.dao.InfoDao
import com.hanialjti.allchat.data.local.room.entity.InfoEntity
import com.hanialjti.allchat.data.model.Avatar
import com.hanialjti.allchat.data.remote.FileUploader
import com.hanialjti.allchat.data.remote.InfoRemoteDataSource
import com.hanialjti.allchat.data.remote.model.CallResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InfoRepository(
    private val infoDao: InfoDao,
    private val infoRemoteDataSource: InfoRemoteDataSource,
    private val fileRepository: FileRepository,
    private val fileUploader: FileUploader,
    private val dispatcher: CoroutineDispatcher,
    private val externalScope: CoroutineScope
) {
    suspend fun fetchAndSaveInfo(id: String, isGroupChat: Boolean): InfoEntity =
        withContext(dispatcher) {
            var infoEntity = infoDao.getOne(id) ?: InfoEntity(id)
            val avatarResult = infoRemoteDataSource
                .fetchAvatarData(id, null, isGroupChat) as? CallResult.Success
            val avatar = avatarResult?.data
            val avatarPath = when (avatar) {
                is Avatar.Raw -> {
                    val imageFile = fileRepository.createNewAvatarFile("$id.png")
                    fileRepository.downloadAndSaveToInternalStorage(avatar.bytes, imageFile)
                }
                is Avatar.Url -> {
                    val imageFile = fileRepository.createNewAvatarFile("$id.png")
                    fileRepository.downloadAndSaveToInternalStorage(avatar.imageUrl, imageFile)
                }
                else -> null
            }
            val nicknameResult = infoRemoteDataSource
                .fetchNickname(id, isGroupChat) as? CallResult.Success

            infoEntity = infoEntity.copy(
                nickname = nicknameResult?.data,
                cachePath = avatarPath?.toString(),
                avatarHash = if (avatar is Avatar.Raw) infoRemoteDataSource.hashAvatarBytes(
                    avatar.bytes
                ) else null,
            )
            infoDao.insert(infoEntity)
            return@withContext infoEntity
        }

    suspend fun getParticipantsInfo(chatId: String) = infoDao.getParticipantsInfo(chatId)

    suspend fun updateChatAvatar(chatId: String, bytes: ByteArray) =
        externalScope.launch(dispatcher) {
            val avatarFile = fileRepository.createNewAvatarFile(chatId)
            val avatarUri = fileRepository.downloadAndSaveToInternalStorage(bytes, avatarFile)
            val avatarUrlResult = fileUploader.upload(avatarFile)
            if (avatarUrlResult is CallResult.Success) {
                infoRemoteDataSource.updateAvatar(avatarUrlResult.data, chatId)
                infoDao.updateAvatar(avatarUri?.toString(), null, chatId)
            }
        }

    suspend fun nicknameStream() = infoRemoteDataSource.infoUpdateStream()
        .onEach { info ->
            externalScope.launch(dispatcher) {
                when (info) {
//                    is NicknameUpdate -> {
//                        Logger.d { "Subject of ${info.entityId} updated. New subject ${info.nickname}." }
//                        if (infoDao.getOne(info.entityId) == null) {
//                            infoDao.insert(
//                                InfoEntity(
//                                    id = info.entityId,
//                                    nickname = info.nickname,
//                                    cachePath = null,
//                                    avatarHash = null
//                                )
//                            )
//                        } else {
//                            infoDao.updateNickname(info.nickname, info.entityId)
//                        }
//                    }
                    else -> {}
                }
            }
        }

    suspend fun fetchUserNickname(userId: String) =
        infoRemoteDataSource.fetchNickname(userId, false)

    suspend fun getInfoFor(id: String): InfoEntity? = infoDao.getOne(id)
}