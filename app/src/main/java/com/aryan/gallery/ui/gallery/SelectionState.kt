package com.aryan.gallery.ui.gallery

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * State holder for grid item multi-selection mode.
 */
class SelectionState {
    var isSelectionMode by mutableStateOf(false)
        private set

    var selectedIds by mutableStateOf<Set<Long>>(emptySet())
        private set

    fun enterSelectionMode(initialId: Long) {
        isSelectionMode = true
        selectedIds = setOf(initialId)
    }

    fun toggleSelection(id: Long) {
        val current = selectedIds.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        selectedIds = current
        if (current.isEmpty()) {
            isSelectionMode = false
        }
    }

    fun clearSelection() {
        selectedIds = emptySet()
        isSelectionMode = false
    }

    fun isSelected(id: Long): Boolean = selectedIds.contains(id)
}
