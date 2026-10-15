package com.aryan.gallery.data.media

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "MediaSync"

/**
 * Singleton ContentObserver monitoring local MediaStore image & video mutations.
 * Emits change events on [mediaChanges] to trigger instant Jetpack Paging invalidation and UI updates.
 */
@Singleton
class MediaStoreObserver @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val _mediaChanges = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val mediaChanges: SharedFlow<Unit> = _mediaChanges.asSharedFlow()

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            Log.d(TAG, "MediaStoreObserver detected change. Emitting mediaChanges event...")
            scope.launch {
                _mediaChanges.emit(Unit)
            }
        }
    }

    init {
        register()
    }

    fun register() {
        try {
            context.contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                observer
            )
            context.contentResolver.registerContentObserver(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                true,
                observer
            )
            Log.d(TAG, "MediaStoreObserver successfully registered.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register MediaStoreObserver", e)
        }
    }

    fun notifyMediaChanged() {
        Log.d(TAG, "Manual notifyMediaChanged triggered. Emitting event...")
        scope.launch {
            _mediaChanges.emit(Unit)
        }
    }
}
