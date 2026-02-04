package com.echoesapp.audiobooks.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.echoesapp.audiobooks.data.local.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {

    // ==================== Bookmark Queries ====================
    
    @Query("SELECT * FROM bookmarks WHERE id = :bookmarkId")
    suspend fun getBookmark(bookmarkId: String): BookmarkEntity?
    
    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY chapterIndex, positionMs")
    fun getBookmarksForBook(bookId: String): Flow<List<BookmarkEntity>>
    
    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY chapterIndex, positionMs")
    suspend fun getBookmarksForBookSnapshot(bookId: String): List<BookmarkEntity>
    
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentBookmarks(limit: Int): Flow<List<BookmarkEntity>>
    
    @Query("SELECT COUNT(*) FROM bookmarks WHERE bookId = :bookId")
    suspend fun getBookmarkCountForBook(bookId: String): Int

    // ==================== Bookmark Management ====================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)
    
    @Query("UPDATE bookmarks SET note = :note, needsSync = 1 WHERE id = :bookmarkId")
    suspend fun updateNote(bookmarkId: String, note: String?)
    
    @Query("DELETE FROM bookmarks WHERE id = :bookmarkId")
    suspend fun deleteBookmark(bookmarkId: String)
    
    @Query("DELETE FROM bookmarks WHERE bookId = :bookId")
    suspend fun deleteBookmarksForBook(bookId: String)

    // ==================== Sync Support ====================
    
    @Query("SELECT * FROM bookmarks WHERE needsSync = 1")
    suspend fun getUnsyncedBookmarks(): List<BookmarkEntity>
    
    @Query("UPDATE bookmarks SET needsSync = 0 WHERE id = :bookmarkId")
    suspend fun markAsSynced(bookmarkId: String)
    
    @Query("UPDATE bookmarks SET needsSync = 0 WHERE id IN (:bookmarkIds)")
    suspend fun markMultipleAsSynced(bookmarkIds: List<String>)
}
