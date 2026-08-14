package com.faceaccess.app.overlay

/**
 * Vi tri highlight hien tai trong danh sach quet. Thuan Kotlin, khong phu
 * thuoc Android, de kiem thu thu tu quay vong va anh xa cu chi -> muc.
 */
class ScanController(initialActions: List<ScanAction> = emptyList()) {
    private var actions: List<ScanAction> = initialActions
    private var index = 0

    val size: Int get() = actions.size
    val currentIndex: Int get() = index
    val current: ScanAction? get() = actions.getOrNull(index)

    /** Thay danh sach quet (vi du sau khi tai xong danh sach app ghim). */
    fun setActions(newActions: List<ScanAction>) {
        actions = newActions
        index = if (newActions.isEmpty()) 0 else index.coerceIn(0, newActions.size - 1)
    }

    fun moveNext() {
        if (actions.isEmpty()) return
        index = (index + 1) % actions.size
    }

    fun movePrevious() {
        if (actions.isEmpty()) return
        index = (index - 1 + actions.size) % actions.size
    }

    fun reset() {
        index = 0
    }
}
