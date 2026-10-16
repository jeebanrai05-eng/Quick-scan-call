package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.QuickCallApp
import com.example.utils.PermissionHelper
import kotlinx.coroutines.launch

/**
 * OverlayService displays a high-performance floating caller ID window
 * on top of incoming calls using the WindowManager API.
 */
class OverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_HIDE_OVERLAY) {
            removeOverlay()
            stopSelf()
            return START_NOT_STICKY
        }

        // Android requires foreground service notification immediately
        val notification = createServiceNotification()
        startForeground(NOTIFICATION_ID, notification)

        val phoneNumber = intent?.getStringExtra(EXTRA_PHONE_NUMBER) ?: "Unknown"
        val callerName = intent?.getStringExtra(EXTRA_CALLER_NAME) ?: "Unknown Caller"
        val category = intent?.getStringExtra(EXTRA_CATEGORY) ?: "Call"
        val spamScore = intent?.getIntExtra(EXTRA_SPAM_SCORE, 0) ?: 0
        val isSpam = intent?.getBooleanExtra(EXTRA_IS_SPAM, false) ?: false
        val city = intent?.getStringExtra(EXTRA_CITY) ?: "Local"
        val carrier = intent?.getStringExtra(EXTRA_CARRIER) ?: "Carrier"

        showFloatingOverlay(
            phoneNumber = phoneNumber,
            callerName = callerName,
            category = category,
            spamScore = spamScore,
            isSpam = isSpam,
            city = city,
            carrier = carrier
        )

        return START_NOT_STICKY
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showFloatingOverlay(
        phoneNumber: String,
        callerName: String,
        category: String,
        spamScore: Int,
        isSpam: Boolean,
        city: String,
        carrier: String
    ) {
        if (!PermissionHelper.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        removeOverlay()

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = 120
        }

        val density = resources.displayMetrics.density

        val root = FrameLayout(this).apply {
            setPadding((16 * density).toInt(), (12 * density).toInt(), (16 * density).toInt(), (12 * density).toInt())
        }

        // Modern Cyber Dark / Neon / Spam Red Glassmorphic Card View
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val p = (18 * density).toInt()
            setPadding(p, p, p, p)

            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 24 * density
                setColor(if (isSpam) Color.parseColor("#F0190812") else Color.parseColor("#F0081224"))
                setStroke(
                    (2 * density).toInt(),
                    if (isSpam) Color.parseColor("#FFFF3366") else Color.parseColor("#FF00FF66")
                )
            }
            background = bg
            elevation = 20 * density
        }

        // 1. Top Drag Handle Pill Indicator
        val dragHandle = View(this).apply {
            val handleWidth = (44 * density).toInt()
            val handleHeight = (4 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(handleWidth, handleHeight).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = (12 * density).toInt()
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 4 * density
                setColor(if (isSpam) Color.parseColor("#80FF3366") else Color.parseColor("#8000FF66"))
            }
        }
        card.addView(dragHandle)

        // 2. Header Row: Status Badge & Close Button
        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val badge = TextView(this).apply {
            text = if (isSpam) "🚨 HIGH RISK SPAM DETECTED" else "⚡ QUICK CALL ID"
            setTextColor(if (isSpam) Color.parseColor("#FFFF3366") else Color.parseColor("#FF00FF66"))
            textSize = 11.5f
            setTypeface(null, Typeface.BOLD)
            val badgeBg = GradientDrawable().apply {
                cornerRadius = 8 * density
                setColor(if (isSpam) Color.parseColor("#33FF3366") else Color.parseColor("#2600FF66"))
            }
            background = badgeBg
            setPadding((10 * density).toInt(), (4 * density).toInt(), (10 * density).toInt(), (4 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
        }

        val closeBtn = TextView(this).apply {
            text = "✕"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setPadding((12 * density).toInt(), (4 * density).toInt(), (12 * density).toInt(), (4 * density).toInt())
            setOnClickListener {
                removeOverlay()
                stopSelf()
            }
        }

        topRow.addView(badge)
        topRow.addView(closeBtn)
        card.addView(topRow)

        // 3. Caller Details
        val nameView = TextView(this).apply {
            text = callerName
            setTextColor(Color.WHITE)
            textSize = 21f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, (12 * density).toInt(), 0, (2 * density).toInt())
        }
        card.addView(nameView)

        val numberView = TextView(this).apply {
            text = phoneNumber
            setTextColor(Color.parseColor("#00F0FF"))
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, (6 * density).toInt())
        }
        card.addView(numberView)

        val metaView = TextView(this).apply {
            text = "📍 $city • 📡 $carrier • 🏷️ $category"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 12.5f
            setPadding(0, 0, 0, (14 * density).toInt())
        }
        card.addView(metaView)

        // 4. Spam Alert warning if applicable
        if (isSpam) {
            val spamAlert = TextView(this).apply {
                text = "⚠️ Threat Rating: $spamScore/100. Community reports flag this caller as suspicious."
                setTextColor(Color.parseColor("#FFA0B0"))
                textSize = 12f
                val alertBg = GradientDrawable().apply {
                    cornerRadius = 8 * density
                    setColor(Color.parseColor("#2BFF3366"))
                }
                background = alertBg
                setPadding((10 * density).toInt(), (6 * density).toInt(), (10 * density).toInt(), (6 * density).toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = (14 * density).toInt()
                }
            }
            card.addView(spamAlert)
        }

        // 5. Action Buttons Row
        val actionsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val btnOpenApp = Button(this).apply {
            text = "Open Details"
            setTextColor(Color.parseColor("#050810"))
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            val btnBg = GradientDrawable().apply {
                cornerRadius = 12 * density
                setColor(Color.parseColor("#FF00FF66"))
            }
            background = btnBg
            layoutParams = LinearLayout.LayoutParams(0, (42 * density).toInt(), 1f).apply {
                rightMargin = (8 * density).toInt()
            }
            setOnClickListener {
                val appIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("EXTRA_NAV_TARGET", "caller_popup")
                    putExtra("EXTRA_PHONE_NUMBER", phoneNumber)
                    putExtra("EXTRA_CALLER_NAME", callerName)
                }
                startActivity(appIntent)
                removeOverlay()
                stopSelf()
            }
        }

        val btnBlock = Button(this).apply {
            text = if (isSpam) "Block & Report" else "Quick Block"
            setTextColor(Color.WHITE)
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            val btnBg = GradientDrawable().apply {
                cornerRadius = 12 * density
                setColor(Color.parseColor("#FFCC0033"))
            }
            background = btnBg
            layoutParams = LinearLayout.LayoutParams(0, (42 * density).toInt(), 1f)
            setOnClickListener {
                val app = applicationContext as QuickCallApp
                app.applicationScope.launch {
                    app.repository.reportSpam(phoneNumber, "Blocked from floating overlay", callerName)
                }
                Toast.makeText(context, "Number $phoneNumber blocked & reported", Toast.LENGTH_SHORT).show()
                removeOverlay()
                stopSelf()
            }
        }

        actionsRow.addView(btnOpenApp)
        actionsRow.addView(btnBlock)
        card.addView(actionsRow)

        // 6. Smooth 2D Drag-to-Reposition on Header / Drag Handle
        val dragTouchListener = object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                if (event == null) return false
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        try {
                            windowManager?.updateViewLayout(root, params)
                        } catch (_: Exception) {}
                        return true
                    }
                }
                return false
            }
        }

        dragHandle.setOnTouchListener(dragTouchListener)
        topRow.setOnTouchListener(dragTouchListener)

        root.addView(card)
        overlayView = root

        try {
            windowManager?.addView(root, params)
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun removeOverlay() {
        overlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (_: Exception) {}
            overlayView = null
        }
    }

    private fun createServiceNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, QuickCallApp.CHANNEL_OVERLAY_SERVICE)
            .setContentTitle("Quick Call Overlay Active")
            .setContentText("Identifying callers and shielding from spam")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 8801

        const val ACTION_SHOW_OVERLAY = "com.example.action.SHOW_OVERLAY"
        const val ACTION_HIDE_OVERLAY = "com.example.action.HIDE_OVERLAY"

        const val EXTRA_PHONE_NUMBER = "extra_phone_number"
        const val EXTRA_CALLER_NAME = "extra_caller_name"
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_SPAM_SCORE = "extra_spam_score"
        const val EXTRA_IS_SPAM = "extra_is_spam"
        const val EXTRA_CITY = "extra_city"
        const val EXTRA_CARRIER = "extra_carrier"
    }
}
