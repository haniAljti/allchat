package com.hanialjti.allchat.data.remote.model

sealed class UserUpdate(val userId: String)

//class NicknameUpdate(userId: String, val nickname: String): UserUpdate(userId)
//
//class AvatarUpdate(userId: String, val avatarUrl: String): UserUpdate(userId)
//
//class AvatarMetaDataUpdate(userId: String, val hash: String): UserUpdate(userId)

class PresenceUpdate(userId: String, val presence: RemotePresence): UserUpdate(userId)

class NewUserUpdate(userId: String, val nickname: String, val avatar: String): UserUpdate(userId)