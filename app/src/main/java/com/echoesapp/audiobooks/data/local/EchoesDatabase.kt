package com.echoesapp.audiobooks.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.echoesapp.audiobooks.data.local.dao.AudiobookDao
import com.echoesapp.audiobooks.data.local.dao.PlaybackProgressDao
import com.echoesapp.audiobooks.data.local.entity.AudiobookEntity
import com.echoesapp.audiobooks.data.local.entity.ChapterEntity
import com.echoesapp.audiobooks.data.local.entity.PlaybackProgressEntity

@Database(
    entities = [
        AudiobookEntity::class,
        ChapterEntity::class,
        PlaybackProgressEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class EchoesDatabase : RoomDatabase() {

    abstract fun audiobookDao(): AudiobookDao
    abstract fun playbackProgressDao(): PlaybackProgressDao

    companion object {
        const val DATABASE_NAME = "echoes_database"
    }
}
