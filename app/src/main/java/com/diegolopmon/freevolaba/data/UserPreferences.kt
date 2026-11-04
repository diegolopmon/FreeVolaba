package com.diegolopmon.freevolaba.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _selectedUserId = MutableStateFlow(readSelectedUserId())
    val selectedUserId: StateFlow<Long?> = _selectedUserId.asStateFlow()

    private fun readSelectedUserId(): Long? {
        val id = prefs.getLong(KEY_SELECTED_USER_ID, -1L)
        return if (id != -1L) id else null
    }

    fun setSelectedUserId(id: Long?) {
        if (id == null) {
            prefs.edit().remove(KEY_SELECTED_USER_ID).apply()
        } else {
            prefs.edit().putLong(KEY_SELECTED_USER_ID, id).apply()
        }
        _selectedUserId.value = id
    }

    companion object {
        private const val PREFS_NAME = "user_preferences"
        private const val KEY_SELECTED_USER_ID = "selected_user_id"
    }
}
