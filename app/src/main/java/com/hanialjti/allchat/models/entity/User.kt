package com.hanialjti.allchat.models.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity
data class User(
    @PrimaryKey
    @ColumnInfo(name = "userId")
    val id: String = UUID.randomUUID().toString(),
    val name: String? = null,
    val image: String? = null,
    val isOnline: Boolean = false,
    val lastOnline: Long? = null
)
