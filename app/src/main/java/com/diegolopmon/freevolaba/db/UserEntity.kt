package com.diegolopmon.freevolaba.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val avatarId: String,
    val avatarPath: String? = null,
    val originalAvatarPath: String? = null,
    val birthDate: Long? = null,
    val heightCm: Int? = null,
    val weightKg: Double? = null,
    val createdAt: Long = System.currentTimeMillis()
)
