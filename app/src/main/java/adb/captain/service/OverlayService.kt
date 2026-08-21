package adb.captain.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.res.ResourcesCompat
import adb.captain.R
import adb.captain.ShizukuManager
import adb.captain.domain.usecase.SideloadUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Плавающий оверлей для быстрых скриншотов и записи экрана из любого приложения.
 */
@AndroidEntryPoint
class OverlayService : Service() {

    @Inject lateinit var useCase: SideloadUseCase

    private lateinit var windowManager: WindowManager
    private lateinit var rootView: LinearLayout
    private lateinit var menuPanel: LinearLayout
    private lateinit var recordBtn: LinearLayout
    private lateinit var recordIcon: ImageView
    private lateinit var recordLabel: TextView
    private var overlayParams: WindowManager.LayoutParams? = null

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var recording = false

    private var startTouchX = 0
    private var startTouchY = 0
    private var startParamX = 0
    private var startParamY = 0
    private var dragged = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        buildOverlay()
        startForegroundNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        serviceScope.cancel()
        overlayParams?.let { params ->
            runCatching { windowManager.removeView(rootView) }
        }
        super.onDestroy()
    }

    private fun startForegroundNotification() {
        val channelId = "overlay_channel"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                getString(R.string.overlay_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            )
            nm.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_camera)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.overlay_notification))
            .setOngoing(true)
            .build()
        startForeground(1, notification)
    }

    private fun buildOverlay() {
        val bubble = makeBubble()
        bubble.setOnTouchListener { _, event ->
            handleBubbleTouch(event, bubble)
            true
        }

        menuPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(dp(4), dp(6), dp(4), dp(6))
            background = roundedBackground(0xF21C1C2E.toInt(), dp(18), stroke = 1 to 0x26FFFFFF)
            elevation = dp(12).toFloat()
            addView(makeMenuItem(R.drawable.ic_camera, R.string.overlay_shot) { takeScreenshot() })
            recordBtn = makeMenuItem(R.drawable.ic_videocam, R.string.overlay_record) { toggleRecording() }
            addView(recordBtn)
            addView(makeMenuItem(R.drawable.ic_close, R.string.overlay_close) { stopSelf() })
        }

        rootView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(2), dp(2), dp(2), dp(2))
            addView(bubble)
            addView(menuPanel)
        }

        val wm = windowManager.defaultDisplay.width
        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        overlayParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = wm - dp(70)
            y = dp(200)
        }
        runCatching { windowManager.addView(rootView, overlayParams) }
            .onFailure { toast(getString(R.string.overlay_start_failed)) }
    }

    private fun makeBubble(): TextView = TextView(this).apply {
        text = getString(R.string.overlay_bubble)
        textSize = 16f
        typeface = ResourcesCompat.getFont(this@OverlayService, R.font.inter_bold) ?: Typeface.DEFAULT_BOLD
        setTextColor(0xFFFFFFFF.toInt())
        gravity = Gravity.CENTER
        minWidth = dp(56)
        minHeight = dp(56)
        setPadding(dp(12), dp(8), dp(12), dp(8))
        background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(0xFF22C1C3.toInt(), 0xFF3A7BD5.toInt(), 0xFF6A11CB.toInt())
        ).apply {
            cornerRadius = dp(18).toFloat()
            setStroke(dp(1), 0x40FFFFFF)
        }
        elevation = dp(10).toFloat()
    }

    private fun makeMenuItem(iconRes: Int, textRes: Int, onClick: () -> Unit): LinearLayout {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            contentDescription = getString(textRes)
            setPadding(dp(14), dp(11), dp(20), dp(11))
            isClickable = true
            isFocusable = true
            background = RippleDrawable(
                ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_pressed), intArrayOf()),
                    intArrayOf(0x33FFFFFF.toInt(), Color.TRANSPARENT)
                ),
                roundedBackground(Color.TRANSPARENT, dp(14)),
                null
            )
            setOnClickListener { onClick() }
        }

        val icon = ImageView(this).apply {
            setImageResource(iconRes)
            imageTintList = ColorStateList.valueOf(0xD9FFFFFF.toInt())
            layoutParams = LinearLayout.LayoutParams(dp(20), dp(20))
        }
        item.addView(icon)

        val label = TextView(this).apply {
            text = getString(textRes)
            textSize = 14f
            typeface = ResourcesCompat.getFont(this@OverlayService, R.font.inter_medium)
            setTextColor(0xE6FFFFFF.toInt())
            setPadding(dp(10), 0, 0, 0)
        }
        item.addView(label)

        if (textRes == R.string.overlay_record) {
            recordIcon = icon
            recordLabel = label
        }
        return item
    }

    private fun handleBubbleTouch(event: MotionEvent, bubble: View) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startTouchX = event.rawX.toInt()
                startTouchY = event.rawY.toInt()
                overlayParams?.let {
                    startParamX = it.x
                    startParamY = it.y
                }
                dragged = false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX.toInt() - startTouchX)
                val dy = (event.rawY.toInt() - startTouchY)
                if (abs(dx) > dp(8) || abs(dy) > dp(8)) {
                    dragged = true
                    overlayParams?.let {
                        it.x = startParamX + dx
                        it.y = startParamY + dy
                        runCatching { windowManager.updateViewLayout(rootView, it) }
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                if (!dragged) {
                    bubble.performClick()
                    bubble.animate().scaleX(0.9f).scaleY(0.9f).setDuration(60)
                        .withEndAction {
                            bubble.animate().scaleX(1f).scaleY(1f).setDuration(60).start()
                        }.start()
                    menuPanel.visibility =
                        if (menuPanel.visibility == View.GONE) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun roundedBackground(
        color: Int,
        radius: Int,
        stroke: Pair<Int, Int>? = null
    ): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
            stroke?.let { setStroke(it.first, it.second) }
        }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun takeScreenshot() {
        if (!ShizukuManager.isShizukuRunning()) {
            toast(getString(R.string.shizuku_not_running))
            return
        }
        closeMenu()
        serviceScope.launch {
            val path = useCase.takeScreenshot()
            mainHandler.post { toast(getString(R.string.overlay_shot_done, path)) }
        }
    }

    private fun toggleRecording() {
        if (!ShizukuManager.isShizukuRunning()) {
            toast(getString(R.string.shizuku_not_running))
            return
        }
        recording = !recording
        val tint = if (recording) 0xFFFF5252.toInt() else 0xD9FFFFFF.toInt()
        recordIcon.setImageResource(if (recording) R.drawable.ic_stop else R.drawable.ic_videocam)
        recordIcon.imageTintList = ColorStateList.valueOf(tint)
        recordLabel.text = if (recording) getString(R.string.overlay_stop) else getString(R.string.overlay_record)
        recordLabel.setTextColor(if (recording) 0xFFFF5252.toInt() else 0xE6FFFFFF.toInt())
        serviceScope.launch {
            if (recording) {
                useCase.startScreenRecording()
            } else {
                useCase.stopScreenRecording()
            }
        }
    }

    private fun closeMenu() {
        menuPanel.visibility = View.GONE
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
