package com.aryan.gallery.data.trash

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrashDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TrashEntity>)

    @Query("DELETE FROM trash_metadata WHERE mediaId = :mediaId")
    suspend fun deleteById(mediaId: Long)

    @Query("DELETE FROM trash_metadata WHERE mediaId IN (:mediaIds)")
    suspend fun deleteByIds(mediaIds: List<Long>)

    @Query("DELETE FROM trash_metadata")
    suspend fun clearAll()

    @Query("SELECT * FROM trash_metadata ORDER BY trashedAt DESC")
    fun getAllTrashItems(): Flow<List<TrashEntity>>

    @Query("SELECT * FROM trash_metadata WHERE purgeAt <= :now")
    suspend fun getExpiredTrashItems(now: Long = System.currentTimeMillis()): List<TrashEntity>

    @Query("SELECT mediaId FROM trash_metadata")
    suspend fun getTrashedMediaIds(): List<Long>

    @Query("SELECT mediaId FROM trash_metadata")
    fun getTrashedMediaIdsFlow(): Flow<List<Long>>
}
