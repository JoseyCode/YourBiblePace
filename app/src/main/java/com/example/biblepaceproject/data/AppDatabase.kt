package com.example.biblepaceproject.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ChapterRead::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chapterReadDao(): ChapterReadDao

    companion object {
        const val NAME = "yourbiblepace.db"

        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, NAME).build().also { instance = it }
        }
    }
}
