package com.echoesapp.audiobooks.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * Supabase PostgREST API for Echoes audiobooks.
 * 
 * Supabase REST uses query params for filtering:
 * - eq. (equals), neq. (not equals)
 * - gt., gte., lt., lte. (comparisons)
 * - like., ilike. (pattern matching)
 * - in. (in list)
 * 
 * Pagination via Range header or limit/offset.
 */
interface EchoesApi {

    @GET("rest/v1/audiobooks")
    suspend fun getAudiobooks(
        @Query("select") select: String = "*",
        @Query("category") category: String? = null,
        @Query("order") order: String = "created_at.desc",
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
    ): Response<List<AudiobookDto>>

    @GET("rest/v1/audiobooks")
    suspend fun getAudiobook(
        @Query("id") id: String, // Pass as "eq.{uuid}"
        @Query("select") select: String = "*",
    ): Response<List<AudiobookDto>>

    @GET("rest/v1/chapters")
    suspend fun getChapters(
        @Query("audiobook_id") audiobookId: String, // Pass as "eq.{uuid}"
        @Query("select") select: String = "*",
        @Query("order") order: String = "chapter_index.asc",
    ): Response<List<ChapterDto>>

    @GET("rest/v1/audiobooks")
    suspend fun searchAudiobooks(
        @Query("title") titleSearch: String, // Pass as "ilike.*query*"
        @Query("select") select: String = "*",
        @Query("limit") limit: Int = 20,
    ): Response<List<AudiobookDto>>

    @GET("rest/v1/audiobooks")
    suspend fun getFeaturedAudiobooks(
        @Query("is_featured") isFeatured: String = "eq.true",
        @Query("select") select: String = "*",
        @Query("limit") limit: Int = 10,
    ): Response<List<AudiobookDto>>

    companion object {
        const val BASE_URL = "https://mutellbwriokkfqrztgl.supabase.co/"
        const val ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im11dGVsbGJ3cmlva2tmcXJ6dGdsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzgzMjcyMDAsImV4cCI6MjA1MzkwMzIwMH0.placeholder" // Will be set properly
        const val STORAGE_URL = "https://mutellbwriokkfqrztgl.supabase.co/storage/v1/object/public/audio/"
    }
}
