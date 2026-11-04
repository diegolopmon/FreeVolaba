package com.diegolopmon.freevolaba.data

import android.content.Context
import com.diegolopmon.freevolaba.db.WorkoutDatabase
import com.diegolopmon.freevolaba.db.WorkoutSessionEntity
import com.diegolopmon.freevolaba.training.WorkoutSummary
import kotlinx.coroutines.flow.Flow

class WorkoutRepository(context: Context) {
    private val database = WorkoutDatabase.getDatabase(context)
    private val dao = database.workoutSessionDao()

    val allSessions: Flow<List<WorkoutSessionEntity>> = dao.getAllSessions()

    fun getSessionsForUser(userId: Long): Flow<List<WorkoutSessionEntity>> {
        return dao.getSessionsForUser(userId)
    }

    suspend fun saveSession(summary: WorkoutSummary, userId: Long): Long {
        return dao.insertSession(summary.toEntity(userId))
    }

    suspend fun deleteSessionById(id: Long) {
        dao.deleteSessionById(id)
    }
}
