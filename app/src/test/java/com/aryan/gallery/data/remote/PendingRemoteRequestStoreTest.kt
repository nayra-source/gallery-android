package com.aryan.gallery.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class PendingRemoteRequestStoreTest {

    private lateinit var store: PendingRemoteRequestStore

    @Before
    fun setUp() {
        store = PendingRemoteRequestStore()
    }

    @Test
    fun `stageDelete stores request and clearDelete resets it`() {
        val deleteReq = store.stageDelete(listOf(101L, 102L))
        assertNotNull(store.pendingDelete.value)
        assertEquals(listOf(101L, 102L), store.pendingDelete.value?.mediaIds)

        store.clearDelete()
        assertNull(store.pendingDelete.value)
    }

    @Test
    fun `stageRename stores request and clearRename resets it`() {
        val renameReq = store.stageRename(101L, "new_name.jpg")
        assertNotNull(store.pendingRename.value)
        assertEquals(101L, store.pendingRename.value?.mediaId)
        assertEquals("new_name.jpg", store.pendingRename.value?.newName)

        store.clearRename()
        assertNull(store.pendingRename.value)
    }
}
