package com.aryan.gallery.data.trash

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aryan.gallery.data.media.MediaOverrideDao
import com.aryan.gallery.data.media.MediaOverrideEntity

@Database(entities = [TrashEntity::class, MediaOverrideEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trashDao(): TrashDao
    abstract fun mediaOverrideDao(): MediaOverrideDao
}
