package com.bignerdranch.android.myapplication

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey

@Entity(tableName = "workout_records")
data class WorkoutRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mode: String,
    val totalTime: Long,
    val roundCount: Int,
    val avgRoundTime: Long,
    val timestamp: Long,
    val memo: String? = null
)
