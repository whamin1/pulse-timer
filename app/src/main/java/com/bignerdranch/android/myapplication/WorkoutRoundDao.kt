package com.bignerdranch.android.myapplication

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WorkoutRoundDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(round: WorkoutRoundEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rounds: List<WorkoutRoundEntity>)

    @Delete
    suspend fun delete(round: WorkoutRoundEntity)

    @Query("SELECT * FROM workout_rounds WHERE recordId = :recordId ORDER BY roundNumber ASC")
    suspend fun getRoundsByRecordId(recordId: Long): List<WorkoutRoundEntity>
}