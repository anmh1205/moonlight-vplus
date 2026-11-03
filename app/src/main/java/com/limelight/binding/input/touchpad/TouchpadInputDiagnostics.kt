package com.limelight.binding.input.touchpad

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import com.limelight.BuildConfig

/**
 * Debug-build-only, opt-in inspection by the user via Settings > Input > Mouse.
 * Stores device/source/action metadata, never pointer coordinates or key content.
 */
object TouchpadInputDiagnostics {
    private const val PREFS = "touchpad_input_diagnostics"
    private const val KEY = "recent_events"
    private const val MAX_LINES = 125
    private var lastMoveMs = 0L
    private var lastMoveDevice = Int.MIN_VALUE
    private var lastScrollMs = 0L
    private var lastOutputMs = 0L
    private var lastOutputName = ""
    private var lastStateMs = 0L
    private var lastState = ""

    @Synchronized
    fun record(context: Context, event: MotionEvent, stage: String) {
        if (!BuildConfig.DEBUG && !BuildConfig.TOUCHPAD_TRACE_ENABLED) return
        val device = event.device
        val action = event.actionMasked
        // Sample continuous movement; always retain click, contacts, and scroll.
        val moving = action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_HOVER_MOVE
        val now = SystemClock.uptimeMillis()
        if (moving && event.deviceId == lastMoveDevice && now - lastMoveMs < 400L) return
        if (action == MotionEvent.ACTION_SCROLL) {
            if (now - lastScrollMs < 100L) return
            lastScrollMs = now
        }
        if (moving) {
            lastMoveMs = now
            lastMoveDevice = event.deviceId
        }
        val source = "0x" + event.source.toString(16)
        val deviceSources = "0x" + (device?.sources ?: 0).toString(16)
        val classification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) event.classification else -1
        val deviceName = device?.name?.replace('\n', ' ')?.take(48) ?: "(no device)"
        val descriptor = device?.descriptor?.hashCode()?.toUInt()?.toString(16) ?: "-"
        val selected = TouchpadCompatibilityDevices.contains(context, device)
        val vscroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
        val hscroll = event.getAxisValue(MotionEvent.AXIS_HSCROLL)
        val line = "$now $stage id=${event.deviceId} name=$deviceName desc=$descriptor selected=$selected " +
            "src=$source devsrc=$deviceSources action=${MotionEvent.actionToString(action)} " +
            "tool=${if (event.pointerCount > 0) event.getToolType(0) else -1} pointers=${event.pointerCount} " +
            "buttons=${event.buttonState} cls=$classification flags=0x${event.flags.toString(16)} " +
            "vScroll=$vscroll hScroll=$hscroll"
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val recent = prefs.getString(KEY, "")!!.lineSequence().filter { it.isNotBlank() }.takeLastCompat(MAX_LINES - 1).toList()
        prefs.edit().putString(KEY, (recent + line).joinToString("\n")).apply()
    }

    /**
     * Separates Android event delivery from stream ownership and pointer capture.
     * Does not record motion coordinates or keyboard data.
     */
    @Synchronized
    fun recordState(
        context: Context,
        grabbed: Boolean,
        focus: Boolean,
        capture: Boolean,
        nativeMousePointer: Boolean,
        nativeTouchpad: Boolean,
    ) {
        if (!BuildConfig.DEBUG && !BuildConfig.TOUCHPAD_TRACE_ENABLED) return
        val state = "grab=$grabbed focus=$focus pointerInput=$capture " +
            "nativeMousePointer=$nativeMousePointer nativeTouchpad=$nativeTouchpad"
        val now = SystemClock.uptimeMillis()
        if (state == lastState && now - lastStateMs < 1600L) return
        lastState = state
        lastStateMs = now
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lines = prefs.getString(KEY, "")!!.lineSequence().filter { it.isNotBlank() }
            .takeLastCompat(MAX_LINES - 1).toList()
        prefs.edit().putString(KEY, (lines + "$now STATE $state").joinToString("\n")).apply()
    }

    @Synchronized
    fun recordOutput(context: Context, eventName: String) {
        if (!BuildConfig.DEBUG && !BuildConfig.TOUCHPAD_TRACE_ENABLED) return
        val now = SystemClock.uptimeMillis()
        if (eventName in setOf("Position", "Move", "Scroll", "Scroll(Android ACTION_SCROLL)") &&
            eventName == lastOutputName && now - lastOutputMs < 180L) return
        lastOutputMs = now
        lastOutputName = eventName
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val recent = prefs.getString(KEY, "")!!.lineSequence().filter { it.isNotBlank() }
            .takeLastCompat(MAX_LINES - 1).toList()
        val line = "$now OUT ${eventName.take(48)}"
        prefs.edit().putString(KEY, (recent + line).joinToString("\n")).apply()
    }

    @Synchronized
    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).apply()
        lastMoveMs = 0L
        lastMoveDevice = Int.MIN_VALUE
        lastScrollMs = 0L
        lastOutputMs = 0L
        lastOutputName = ""
        lastStateMs = 0L
        lastState = ""
    }

    @Synchronized
    fun report(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val selected = TouchpadCompatibilityDevices.selected(context)
        val devices = InputDevice.getDeviceIds().asSequence().mapNotNull { InputDevice.getDevice(it) }
            .joinToString("\n") { dev ->
                "device id=${dev.id} name=${dev.name.take(50)} desc=${dev.descriptor.hashCode().toUInt().toString(16)} " +
                    "src=0x${dev.sources.toString(16)} virtual=${dev.isVirtual} selected=${dev.descriptor in selected}"
            }
        return "Moonlight V+ physical touchpad diagnostics (no coordinates or typed text)\n" +
            "traceEnabled=${BuildConfig.DEBUG || BuildConfig.TOUCHPAD_TRACE_ENABLED}\n" +
            "Selected descriptors: ${selected.size}; Android API: ${Build.VERSION.SDK_INT}\n" +
            "Connected devices:\n$devices\nRecent input events (latest at bottom):\n" +
            (prefs.getString(KEY, "")?.ifBlank { "(none captured)" } ?: "(none captured)")
    }

    private fun <T> Sequence<T>.takeLastCompat(n: Int): Sequence<T> = toList().takeLast(n).asSequence()
}
