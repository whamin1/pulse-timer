package com.bignerdranch.android.myapplication

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutRecordDao {

    @Insert
    suspend fun insert(record: WorkoutRecordEntity): Long

    @Query("SELECT * FROM workout_records ORDER BY timestamp DESC")
    suspend fun getAll(): List<WorkoutRecordEntity>

    @Query("SELECT * FROM workout_records ORDER BY timestamp DESC")
    fun getAllRecordFlow(): Flow<List<WorkoutRecordEntity>>

    @Delete
    suspend fun delete(record: WorkoutRecordEntity)

    @Query("UPDATE workout_records SET memo = :memo WHERE id = :recordId")
    suspend fun updateMemo(recordId: Long, memo: String)

    @Query("SELECT * FROM workout_records WHERE id = :id")
    suspend fun getRecordById(id: Long): WorkoutRecordEntity?
}