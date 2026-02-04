package com.echoesapp.audiobooks.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.echoesapp.audiobooks.data.local.dao.AudiobookDao
import com.echoesapp.audiobooks.data.local.dao.BookmarkDao
import com.echoesapp.audiobooks.data.local.dao.ListeningSessionDao
import com.echoesapp.audiobooks.data.local.dao.PlaybackProgressDao
import com.echoesapp.audiobooks.data.local.entity.AudiobookEntity
import com.echoesapp.audiobooks.data.local.entity.BookmarkEntity
import com.echoesapp.audiobooks.data.local.entity.ChapterEntity
import com.echoesapp.audiobooks.data.local.entity.ListeningSessionEntity
import com.echoesapp.audiobooks.data.local.entity.PlaybackProgressEntity

@Database(
    entities = [
        AudiobookEntity::class,
        ChapterEntity::class,
        PlaybackProgressEntity::class,
        ListeningSessionEntity::class,
        BookmarkEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class EchoesDatabase : RoomDatabase() {

    abstract fun audiobookDao(): AudiobookDao
    abstract fun playbackProgressDao(): PlaybackProgressDao
    abstract fun listeningSessionDao(): ListeningSessionDao
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        const val DATABASE_NAME = "echoes_database"
        
        /**
         * Migration from version 1 to 2:
         * - Add new columns to playback_progress
         * - Create listening_sessions table
         * - Create bookmarks table
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Update playback_progress table
                db.execSQL("""
                    CREATE TABLE playback_progress_new (
                        bookId TEXT NOT NULL PRIMARY KEY,
                        chapterIndex INTEGER NOT NULL DEFAULT 0,
                        positionMs INTEGER NOT NULL DEFAULT 0,
                        playbackSpeed REAL NOT NULL DEFAULT 1.0,
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        totalListenedMs INTEGER NOT NULL DEFAULT 0,
                        lastPlayedAt INTEGER NOT NULL,
                        needsSync INTEGER NOT NULL DEFAULT 1,
                        FOREIGN KEY(bookId) REFERENCES audiobooks(id) ON DELETE CASCADE
                    )
                """)
                
                // Migrate existing data
                db.execSQL("""
                    INSERT INTO playback_progress_new (bookId, chapterIndex, positionMs, lastPlayedAt)
                    SELECT bookId, 0, position, lastPlayed FROM playback_progress
                """)
                
                db.execSQL("DROP TABLE playback_progress")
                db.execSQL("ALTER TABLE playback_progress_new RENAME TO playback_progress")
                db.execSQL("CREATE INDEX index_playback_progress_bookId ON playback_progress(bookId)")
                db.execSQL("CREATE INDEX index_playback_progress_lastPlayedAt ON playback_progress(lastPlayedAt)")
                
                // Create listening_sessions table
                db.execSQL("""
                    CREATE TABLE listening_sessions (
                        id TEXT NOT NULL PRIMARY KEY,
                        bookId TEXT NOT NULL,
                        startedAt INTEGER NOT NULL,
                        endedAt INTEGER,
                        durationMs INTEGER NOT NULL DEFAULT 0,
                        fromChapter INTEGER NOT NULL DEFAULT 0,
                        toChapter INTEGER NOT NULL DEFAULT 0,
                        needsSync INTEGER NOT NULL DEFAULT 1,
                        FOREIGN KEY(bookId) REFERENCES audiobooks(id) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX index_listening_sessions_bookId ON listening_sessions(bookId)")
                db.execSQL("CREATE INDEX index_listening_sessions_startedAt ON listening_sessions(startedAt)")
                
                // Create bookmarks table
                db.execSQL("""
                    CREATE TABLE bookmarks (
                        id TEXT NOT NULL PRIMARY KEY,
                        bookId TEXT NOT NULL,
                        chapterIndex INTEGER NOT NULL,
                        positionMs INTEGER NOT NULL,
                        note TEXT,
                        createdAt INTEGER NOT NULL,
                        needsSync INTEGER NOT NULL DEFAULT 1,
                        FOREIGN KEY(bookId) REFERENCES audiobooks(id) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX index_bookmarks_bookId ON bookmarks(bookId)")
                db.execSQL("CREATE INDEX index_bookmarks_createdAt ON bookmarks(createdAt)")
            }
        }
    }
}
