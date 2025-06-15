package com.bignerdranch.android.myapplication

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Entity(tableName = "workout_rounds")
data class WorkoutRoundEntity (
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordId: Long,
    val roundNumber: Int,
    val absoluteTime: Long,
    val intervalTime: Long
) {
    //val MIGRATION_1_2 = object : Migration(1, 2) {
      //  override fun migrate(database: SupportSQLiteDatabase) {
        //    database.execSQL("ALTER TABLE workout_round_table ADD COLUMN note TEXT NOT NULL DEFAULT''")
        //}
   // }
}