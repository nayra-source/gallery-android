package com.aryan.gallery.data.media

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MediaOverrideDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(override: MediaOverrideEntity)

    @Query("SELECT * FROM media_overrides WHERE mediaId = :mediaId")
    suspend fun getOverride(mediaId: Long): MediaOverrideEntity?

    @Query("SELECT * FROM media_overrides")
    suspend fun getAllOverrides(): List<MediaOverrideEntity>
}
