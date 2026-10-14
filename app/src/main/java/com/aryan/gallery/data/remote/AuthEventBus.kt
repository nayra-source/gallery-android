package com.aryan.gallery.data.remote

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Event bus signaling when HTTP 401 Unauthorized occurs due to a revoked or expired device token.
 */
@Singleton
class AuthEventBus @Inject constructor() {

    private val _unauthorizedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val unauthorizedEvents: SharedFlow<Unit> = _unauthorizedEvents.asSharedFlow()

    fun emitUnauthorized() {
        _unauthorizedEvents.tryEmit(Unit)
    }
}
