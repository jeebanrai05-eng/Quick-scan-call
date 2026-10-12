package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R

class QuickScanWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_quick_scan)

            // PendingIntent for Quick OCR Scan
            val scanIntent = Intent(context, MainActivity::class.java).apply {
                action = "com.example.ACTION_QUICK_SCAN"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("EXTRA_NAV_TARGET", "quick_scan")
            }
            val scanPendingIntent = PendingIntent.getActivity(
                context,
                1001,
                scanIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_quick_scan, scanPendingIntent)

            // PendingIntent for T9 Dialer
            val dialerIntent = Intent(context, MainActivity::class.java).apply {
                action = "com.example.ACTION_T9_DIALER"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("EXTRA_NAV_TARGET", "search_t9")
            }
            val dialerPendingIntent = PendingIntent.getActivity(
                context,
                1002,
                dialerIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_dialer, dialerPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
