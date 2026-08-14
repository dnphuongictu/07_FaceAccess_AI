package com.faceaccess.app.action

import com.faceaccess.app.overlay.ScanAction
import com.faceaccess.app.overlay.ScanController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScanControllerTest {

    private val actions = listOf(
        ScanAction.OpenApp("com.example.phone", "Điện thoại"),
        ScanAction.OpenApp("com.example.sms", "Tin nhắn"),
        ScanAction.Back,
        ScanAction.Home,
        ScanAction.MediaPlayPause,
        ScanAction.EmergencyStop,
    )

    @Test
    fun `starts at first action`() {
        val controller = ScanController(actions)

        assertEquals(actions[0], controller.current)
        assertEquals(0, controller.currentIndex)
    }

    @Test
    fun `moveNext advances through the list in order`() {
        val controller = ScanController(actions)

        controller.moveNext()
        assertEquals(actions[1], controller.current)
        controller.moveNext()
        assertEquals(actions[2], controller.current)
    }

    @Test
    fun `moveNext wraps around from the last action to the first`() {
        val controller = ScanController(actions)
        repeat(actions.size) { controller.moveNext() }

        assertEquals(actions[0], controller.current)
    }

    @Test
    fun `movePrevious wraps around from the first action to the last`() {
        val controller = ScanController(actions)

        controller.movePrevious()

        assertEquals(actions.last(), controller.current)
    }

    @Test
    fun `movePrevious then moveNext returns to the original action`() {
        val controller = ScanController(actions)
        controller.moveNext()
        controller.moveNext()

        controller.movePrevious()

        assertEquals(actions[1], controller.current)
    }

    @Test
    fun `current is null when action list is empty`() {
        val controller = ScanController(emptyList())

        assertNull(controller.current)
        controller.moveNext()
        controller.movePrevious()
        assertNull(controller.current)
    }

    @Test
    fun `setActions clamps current index into the new list bounds`() {
        val controller = ScanController(actions)
        repeat(actions.size - 1) { controller.moveNext() } // index = actions.size - 1 (cuoi danh sach)

        controller.setActions(listOf(ScanAction.Back, ScanAction.Home))

        assertEquals(ScanAction.Home, controller.current)
        assertEquals(1, controller.currentIndex)
    }

    @Test
    fun `scan action mapped values match gesture_event schema enum`() {
        assertEquals("back", ScanAction.Back.actionMappedValue)
        assertEquals("home", ScanAction.Home.actionMappedValue)
        assertEquals("media_play_pause", ScanAction.MediaPlayPause.actionMappedValue)
        assertEquals("emergency_stop", ScanAction.EmergencyStop.actionMappedValue)
        assertEquals("open_app", ScanAction.OpenApp("com.example.app", "App").actionMappedValue)
    }
}
