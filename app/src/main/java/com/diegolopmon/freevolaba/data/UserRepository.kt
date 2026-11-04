package com.diegolopmon.freevolaba.data

import android.content.Context
import com.diegolopmon.freevolaba.db.UserEntity
import com.diegolopmon.freevolaba.db.WorkoutDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class UserRepository(context: Context) {
    private val database = WorkoutDatabase.getDatabase(context)
    private val userDao = database.userDao()
    private val userPreferences = UserPreferences(context)

    val allUsers: Flow<List<UserEntity>> = userDao.getAllUsers()

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeUser: Flow<UserEntity?> = userPreferences.selectedUserId.flatMapLatest { id ->
        if (id != null) {
            userDao.getUserById(id)
        } else {
            flowOf(null)
        }
    }

    fun selectUser(userId: Long?) {
        userPreferences.setSelectedUserId(userId)
    }

    suspend fun createUser(
        name: String,
        avatarId: String,
        avatarPath: String? = null,
        originalAvatarPath: String? = null,
        birthDate: Long? = null,
        heightCm: Int? = null,
        weightKg: Double? = null
    ): Long {
        val newUserId = userDao.insertUser(
            UserEntity(
                name = name,
                avatarId = avatarId,
                avatarPath = avatarPath,
                originalAvatarPath = originalAvatarPath,
                birthDate = birthDate,
                heightCm = heightCm,
                weightKg = weightKg
            )
        )
        userPreferences.setSelectedUserId(newUserId)
        return newUserId
    }

    suspend fun updateUser(user: UserEntity) {
        userDao.updateUser(user)
    }

    suspend fun deleteUser(userId: Long) {
        if (userPreferences.selectedUserId.value == userId) {
            userPreferences.setSelectedUserId(null)
        }
        userDao.deleteUserById(userId)
    }
}
