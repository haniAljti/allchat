package com.hanialjti.allchat.data.remote

import com.hanialjti.allchat.data.model.Avatar
import com.hanialjti.allchat.data.remote.model.CallResult
import com.hanialjti.allchat.data.remote.model.InfoUpdate
import com.hanialjti.allchat.data.remote.model.RemoteEntityInfo
import kotlinx.coroutines.flow.Flow

interface InfoRemoteDataSource {
    suspend fun getUpdatedEntityInfo(id: String): CallResult<RemoteEntityInfo>
    suspend fun fetchAvatarData(id: String, hash: String?): CallResult<Avatar?>
    suspend fun fetchNickname(id: String): CallResult<String?>
    suspend fun updateNickname(nickname: String, id: String? = null): CallResult<Boolean>
    suspend fun updateAvatar(data: ByteArray?, id: String? = null): CallResult<Boolean>
    suspend fun updatedInfoFlow(): Flow<InfoUpdate>
    suspend fun hashAvatarBytes(avatarBytes: ByteArray): String?
}