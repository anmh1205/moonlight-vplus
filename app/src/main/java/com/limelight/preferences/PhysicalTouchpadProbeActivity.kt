package com.limelight.preferences

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.limelight.R
import com.limelight.binding.input.touchpad.TouchpadInputDiagnostics

/** Uncaptured Android pointer-event probe for comparison with streaming dispatch. */
class PhysicalTouchpadProbeActivity : Activity() {
    private lateinit var reportView: TextView
    private var lastRefreshMs = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.title_touchpad_test_area)
        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        val guidance = TextView(this).apply {
            setText(R.string.summary_touchpad_test_area)
            textSize = 16f
        }
        root.addView(guidance)
        val clear = Button(this).apply {
            setText(R.string.action_touchpad_test_clear)
            setOnClickListener {
                TouchpadInputDiagnostics.clear(this@PhysicalTouchpadProbeActivity)
                updateReport()
            }
        }
        root.addView(clear, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT))
        val copy = Button(this).apply {
            setText(R.string.action_touchpad_test_copy)
            setOnClickListener {
                val report = TouchpadInputDiagnostics.report(this@PhysicalTouchpadProbeActivity)
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Android physical touchpad trace", report))
                Toast.makeText(this@PhysicalTouchpadProbeActivity,
                    R.string.toast_touchpad_diagnostics_copied, Toast.LENGTH_SHORT).show()
            }
        }
        root.addView(copy, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT))
        reportView = TextView(this).apply {
            textSize = 12f
            setTextIsSelectable(true)
        }
        root.addView(reportView)
        setContentView(ScrollView(this).apply { addView(root) })
        updateReport()
    }

    private fun updateReport() {
        if (::reportView.isInitialized) reportView.text = TouchpadInputDiagnostics.report(this)
    }

    private fun capture(event: MotionEvent, route: String) {
        TouchpadInputDiagnostics.record(this, event, route)
        val now = SystemClock.uptimeMillis()
        if (now - lastRefreshMs >= 500L ||
            event.actionMasked == MotionEvent.ACTION_UP ||
            event.actionMasked == MotionEvent.ACTION_BUTTON_RELEASE) {
            lastRefreshMs = now
            updateReport()
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        capture(event, "outside.touch")
        return super.dispatchTouchEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        capture(event, "outside.generic")
        return super.dispatchGenericMotionEvent(event)
    }
}
