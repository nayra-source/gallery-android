package com.aryan.gallery.ui.gallery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SelectionStateTest {

    private lateinit var selectionState: SelectionState

    @Before
    fun setUp() {
        selectionState = SelectionState()
    }

    @Test
    fun `enterSelectionMode sets mode true and selects initial id`() {
        selectionState.enterSelectionMode(100L)
        assertTrue(selectionState.isSelectionMode)
        assertTrue(selectionState.isSelected(100L))
        assertEquals(1, selectionState.selectedIds.size)
    }

    @Test
    fun `toggleSelection adds and removes ids correctly`() {
        selectionState.enterSelectionMode(100L)
        selectionState.toggleSelection(200L)
        assertEquals(2, selectionState.selectedIds.size)
        assertTrue(selectionState.isSelected(200L))

        selectionState.toggleSelection(200L)
        assertEquals(1, selectionState.selectedIds.size)
        assertFalse(selectionState.isSelected(200L))
    }

    @Test
    fun `toggling last item exits selection mode`() {
        selectionState.enterSelectionMode(100L)
        selectionState.toggleSelection(100L)
        assertFalse(selectionState.isSelectionMode)
        assertTrue(selectionState.selectedIds.isEmpty())
    }

    @Test
    fun `clearSelection resets selection state`() {
        selectionState.enterSelectionMode(100L)
        selectionState.toggleSelection(200L)
        selectionState.clearSelection()

        assertFalse(selectionState.isSelectionMode)
        assertTrue(selectionState.selectedIds.isEmpty())
    }
}
