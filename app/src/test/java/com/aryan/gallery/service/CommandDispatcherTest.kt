package com.aryan.gallery.service

import com.aryan.gallery.service.handlers.DeleteMediaHandler
import com.aryan.gallery.service.handlers.GetFullMediaHandler
import com.aryan.gallery.service.handlers.GetThumbnailHandler
import com.aryan.gallery.service.handlers.ListMediaHandler
import com.aryan.gallery.service.handlers.RenameMediaHandler
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class CommandDispatcherTest {

    private val listMediaHandler: ListMediaHandler = mock()
    private val getThumbnailHandler: GetThumbnailHandler = mock()
    private val getFullMediaHandler: GetFullMediaHandler = mock()
    private val deleteMediaHandler: DeleteMediaHandler = mock()
    private val renameMediaHandler: RenameMediaHandler = mock()

    private lateinit var dispatcher: CommandDispatcher

    @Before
    fun setUp() {
        whenever(listMediaHandler.supportedType).thenReturn("LIST_MEDIA")
        whenever(getThumbnailHandler.supportedType).thenReturn("GET_THUMBNAIL")
        whenever(getFullMediaHandler.supportedType).thenReturn("GET_FULL_MEDIA")
        whenever(deleteMediaHandler.supportedType).thenReturn("DELETE_MEDIA")
        whenever(renameMediaHandler.supportedType).thenReturn("RENAME_MEDIA")

        dispatcher = CommandDispatcher(
            listMediaHandler,
            getThumbnailHandler,
            getFullMediaHandler,
            deleteMediaHandler,
            renameMediaHandler
        )
    }

    @Test
    fun `dispatch routes LIST_MEDIA command to listMediaHandler`() = runTest {
        val expectedResult = DeviceCommandResultEnvelope("cmd-1", "ok", "{}")
        whenever(listMediaHandler.handleCommand(any())).thenReturn(expectedResult)

        val envelope = DeviceCommandEnvelope("cmd-1", "LIST_MEDIA", null)
        val result = dispatcher.dispatch(envelope)

        assertEquals("ok", result.status)
        assertEquals("cmd-1", result.cmdId)
    }

    @Test
    fun `dispatch with unknown command type returns error status envelope without crashing`() = runTest {
        val envelope = DeviceCommandEnvelope("cmd-99", "UNKNOWN_FUTURE_CMD", null)
        val result = dispatcher.dispatch(envelope)

        assertEquals("error", result.status)
        assertEquals("cmd-99", result.cmdId)
        assertEquals("Command type 'UNKNOWN_FUTURE_CMD' is not supported", result.message)
    }
}
