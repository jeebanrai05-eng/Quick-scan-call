package com.example.utils

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity
import com.example.R

object ShortcutHelper {

    fun isPinShortcutSupported(context: Context): Boolean {
        return ShortcutManagerCompat.isRequestPinShortcutSupported(context)
    }

    fun pinQuickScannerShortcut(context: Context) {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            Toast.makeText(
                context,
                "Launcher does not support pinning shortcuts. Long-press app icon instead.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val scanIntent = Intent(context, MainActivity::class.java).apply {
            action = "com.example.ACTION_QUICK_SCAN"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAV_TARGET", "quick_scan")
        }

        val pinShortcutInfo = ShortcutInfoCompat.Builder(context, "pinned_quick_ocr_scan")
            .setShortLabel("Quick Scan")
            .setLongLabel("Quick OCR Number Scanner")
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
            .setIntent(scanIntent)
            .build()

        val success = ShortcutManagerCompat.requestPinShortcut(context, pinShortcutInfo, null)
        if (success) {
            Toast.makeText(context, "Quick Scanner shortcut requested! Check your Home Screen.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Could not add shortcut.", Toast.LENGTH_SHORT).show()
        }
    }
}
