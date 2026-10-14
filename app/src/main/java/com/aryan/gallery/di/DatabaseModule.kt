package com.aryan.gallery.di

import android.content.Context
import androidx.room.Room
import com.aryan.gallery.data.trash.AppDatabase
import com.aryan.gallery.data.trash.TrashDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "gallery_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideTrashDao(appDatabase: AppDatabase): TrashDao {
        return appDatabase.trashDao()
    }
}
