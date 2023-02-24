package com.hanialjti.allchat.data.local.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "entity_info")
data class InfoEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "avatar_path")
    val cachePath: String?,
    @ColumnInfo(name = "avatar_hash")
    val avatarHash: String?,
    val nickname: String
)