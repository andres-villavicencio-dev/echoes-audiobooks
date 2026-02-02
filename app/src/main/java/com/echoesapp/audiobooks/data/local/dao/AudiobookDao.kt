package com.echoesapp.audiobooks.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.echoesapp.audiobooks.data.local.entity.AudiobookEntity
import com.echoesapp.audiobooks.data.local.entity.ChapterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AudiobookDao {

    // Audiobook operations
    @Query("SELECT * FROM audiobooks ORDER BY title ASC")
    fun getAllAudiobooks(): Flow<List<AudiobookEntity>>

    @Query("SELECT * FROM audiobooks WHERE id = :id")
    suspend fun getAudiobookById(id: String): AudiobookEntity?

    @Query("SELECT * FROM audiobooks WHERE id = :id")
    fun observeAudiobookById(id: String): Flow<AudiobookEntity?>

    @Query("SELECT * FROM audiobooks WHERE category = :category ORDER BY title ASC")
    fun getAudiobooksByCategory(category: String): Flow<List<AudiobookEntity>>

    @Query("SELECT * FROM audiobooks WHERE isDownloaded = 1 ORDER BY title ASC")
    fun getDownloadedAudiobooks(): Flow<List<AudiobookEntity>>

    @Query("SELECT * FROM audiobooks WHERE title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%'")
    fun searchAudiobooks(query: String): Flow<List<AudiobookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudiobook(audiobook: AudiobookEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudiobooks(audiobooks: List<AudiobookEntity>)

    @Update
    suspend fun updateAudiobook(audiobook: AudiobookEntity)

    @Query("UPDATE audiobooks SET isDownloaded = :isDownloaded WHERE id = :id")
    suspend fun updateDownloadStatus(id: String, isDownloaded: Boolean)

    @Delete
    suspend fun deleteAudiobook(audiobook: AudiobookEntity)

    @Query("DELETE FROM audiobooks WHERE id = :id")
    suspend fun deleteAudiobookById(id: String)

    @Query("DELETE FROM audiobooks")
    suspend fun deleteAllAudiobooks()

    // Chapter operations
    @Query("SELECT * FROM chapters WHERE audiobookId = :audiobookId ORDER BY chapterIndex ASC")
    suspend fun getChaptersForAudiobook(audiobookId: String): List<ChapterEntity>

    @Query("SELECT * FROM chapters WHERE audiobookId = :audiobookId ORDER BY chapterIndex ASC")
    fun observeChaptersForAudiobook(audiobookId: String): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE id = :chapterId")
    suspend fun getChapterById(chapterId: String): ChapterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>)

    @Query("DELETE FROM chapters WHERE audiobookId = :audiobookId")
    suspend fun deleteChaptersForAudiobook(audiobookId: String)

    // Combined operations
    @Transaction
    suspend fun insertAudiobookWithChapters(
        audiobook: AudiobookEntity,
        chapters: List<ChapterEntity>,
    ) {
        insertAudiobook(audiobook)
        insertChapters(chapters)
    }

    @Transaction
    suspend fun insertAudiobooksWithChapters(
        audiobooksWithChapters: List<Pair<AudiobookEntity, List<ChapterEntity>>>,
    ) {
        audiobooksWithChapters.forEach { (audiobook, chapters) ->
            insertAudiobook(audiobook)
            insertChapters(chapters)
        }
    }
}
