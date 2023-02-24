package com.hanialjti.allchat.data.remote.model

sealed class InfoUpdate(val entityId: String)

class NicknameUpdate(userId: String, val nickname: String): InfoUpdate(userId)

class AvatarUrlUpdate(userId: String, val avatarUrl: String): InfoUpdate(userId)

class AvatarMetaDataUpdate(userId: String, val hash: String): InfoUpdate(userId)