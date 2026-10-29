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

    suspend fun saveSession(summary: WorkoutSummary): Long {
        return dao.insertSession(summary.toEntity())
    }

    suspend fun getSessionById(id: Long): WorkoutSessionEntity? {
        return dao.getSessionById(id)
    }
}
