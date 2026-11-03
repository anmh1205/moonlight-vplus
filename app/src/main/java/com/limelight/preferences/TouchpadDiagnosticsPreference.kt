package com.limelight.preferences

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.AttributeSet
import android.widget.Toast
import androidx.preference.Preference
import com.limelight.R
import com.limelight.binding.input.touchpad.TouchpadInputDiagnostics

/** The user can copy a compact trace without USB debugging or root access. */
class TouchpadDiagnosticsPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : Preference(context, attrs) {
    init {
        setOnPreferenceClickListener {
            val report = TouchpadInputDiagnostics.report(context)
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Moonlight V+ touchpad input trace", report))
            Toast.makeText(context, R.string.toast_touchpad_diagnostics_copied, Toast.LENGTH_SHORT).show()
            true
        }
    }

    override fun onAttached() {
        super.onAttached()
        val trace = TouchpadInputDiagnostics.report(context)
        summary = context.getString(R.string.summary_touchpad_diagnostics) + "\n" +
            trace.lineSequence().take(3).joinToString("\n")
    }
}
