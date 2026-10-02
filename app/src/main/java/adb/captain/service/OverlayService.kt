package adb.captain.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
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
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import adb.captain.R
import adb.captain.ShizukuManager
import adb.captain.domain.model.RecordOptions
import adb.captain.domain.model.SavedMedia
import adb.captain.domain.repository.SettingsRepository
import adb.captain.domain.usecase.SideloadUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Плавающий оверлей для быстрых скриншотов и записи экрана из любого приложения.
 */
@AndroidEntryPoint
class OverlayService : Service() {

    @Inject lateinit var useCase: SideloadUseCase
    @Inject lateinit var settings: SettingsRepository

    private lateinit var windowManager: WindowManager
    private lateinit var rootView: LinearLayout
    private lateinit var menuPanel: LinearLayout
    private lateinit var recordBtn: LinearLayout
    private lateinit var recordIcon: ImageView
    private lateinit var recordLabel: TextView
    private lateinit var previewView: ImageView
    private lateinit var qualityLabel: TextView
    private lateinit var fpsLabel: TextView
    private lateinit var hideLabel: TextView
    private var overlayParams: WindowManager.LayoutParams? = null

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var recording = false
    private var recordingPath: String? = null
    private var attachedToWindow = false
    private var lastScreenshotUri = ""
    private var hideOverlayInCapture = true
    private var recordQuality = "native"
    private var recordMaxFps = true
    private var tickJob: Job? = null

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
        attachedToWindow = true
        startForegroundNotification()
        restoreState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOT -> takeScreenshot()
            ACTION_RECORD -> if (!recording) startRecording()
            ACTION_STOP_RECORD -> if (recording) stopRecording()
            ACTION_SHARE -> shareLastScreenshot()
        }
        return START_STICKY
    }

    private fun restoreState() {
        serviceScope.launch {
            settings.getRecordQuality().collect { value ->
                recordQuality = value
                mainHandler.post { refreshToggles() }
            }
        }
        serviceScope.launch {
            settings.getRecordMaxFps().collect { value ->
                recordMaxFps = value
                mainHandler.post { refreshToggles() }
            }
        }
        serviceScope.launch {
            settings.getHideOverlayInCapture().collect { value ->
                hideOverlayInCapture = value
                mainHandler.post { refreshToggles() }
            }
        }
        serviceScope.launch {
            val uri = settings.getLastScreenshotUri().first()
            lastScreenshotUri = uri
        }
    }

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

        val openIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_camera)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.overlay_notification))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (openIntent != null) {
            builder.setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        }

        builder.addAction(
            R.drawable.ic_camera,
            getString(R.string.overlay_shot),
            servicePendingIntent(ACTION_SHOT, 1)
        )
        builder.addAction(
            R.drawable.ic_videocam,
            getString(R.string.overlay_record),
            servicePendingIntent(if (recording) ACTION_STOP_RECORD else ACTION_RECORD, 2)
        )
        builder.addAction(
            R.drawable.ic_share,
            getString(R.string.overlay_share),
            servicePendingIntent(ACTION_SHARE, 3)
        )

        startForeground(1, builder.build())
    }

    private fun servicePendingIntent(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getService(
            this,
            requestCode,
            Intent(this, OverlayService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    /**
     * Уведомление остаётся и во время записи: если оверлей скрыт,
     * остановить запись можно только отсюда.
     */
    private fun refreshNotification() {
        startForegroundNotification()
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

            previewView = ImageView(this@OverlayService).apply {
                layoutParams = LinearLayout.LayoutParams(dp(150), dp(150)).apply {
                    gravity = Gravity.CENTER
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
                visibility = View.GONE
                setBackgroundColor(0x22000000)
            }
            addView(previewView)

            addView(makeMenuItem(R.drawable.ic_camera, R.string.overlay_shot) { takeScreenshot() })
            recordBtn = makeMenuItem(R.drawable.ic_videocam, R.string.overlay_record) { toggleRecording() }
            addView(recordBtn)
            addView(makeMenuItem(R.drawable.ic_share, R.string.overlay_share) { shareLastScreenshot() })

            qualityLabel = addToggle(R.string.overlay_quality) { cycleQuality() }
            fpsLabel = addToggle(R.string.overlay_max_fps) { toggleMaxFps() }
            hideLabel = addToggle(R.string.overlay_hide_self) { toggleHideOverlay() }

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

    /**
     * Пункт меню-переключатель: слева иконка-«качели», справа текущее
     * значение. Значение хранится в SettingsManager, чтобы переживать
     * перезапуск сервиса.
     */
    private fun addToggle(titleRes: Int, onClick: () -> Unit): TextView {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            contentDescription = getString(titleRes)
            setPadding(dp(14), dp(9), dp(20), dp(9))
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
        item.addView(
            ImageView(this).apply {
                setImageResource(R.drawable.ic_toggle)
                imageTintList = ColorStateList.valueOf(0xD9FFFFFF.toInt())
                layoutParams = LinearLayout.LayoutParams(dp(20), dp(20))
            }
        )
        item.addView(
            TextView(this).apply {
                text = getString(titleRes)
                textSize = 14f
                typeface = ResourcesCompat.getFont(this@OverlayService, R.font.inter_medium)
                setTextColor(0xE6FFFFFF.toInt())
                setPadding(dp(10), 0, dp(8), 0)
            }
        )
        val value = TextView(this).apply {
            textSize = 14f
            typeface = ResourcesCompat.getFont(this@OverlayService, R.font.inter_bold)
            setTextColor(0xFF22C1C3.toInt())
        }
        item.addView(value)
        menuPanel.addView(item)
        return value
    }

    private fun attachOverlay() {
        if (attachedToWindow) return
        runCatching {
            windowManager.addView(rootView, overlayParams)
            attachedToWindow = true
        }.onFailure { toast(getString(R.string.overlay_start_failed)) }
    }

    private fun detachOverlay() {
        if (!attachedToWindow) return
        runCatching { windowManager.removeView(rootView) }
        attachedToWindow = false
    }

    /**
     * Оверлей — это обычное окно поверх экрана, поэтому он попадает и в
     * скриншот, и в запись. Прячем окно, ждём кадр, снимаем, возвращаем.
     */
    private suspend fun <T> withoutOverlay(block: suspend () -> T): T {
        if (!hideOverlayInCapture) return block()
        detachOverlay()
        delay(CAPTURE_SETTLE_MS)
        return try {
            block()
        } finally {
            delay(CAPTURE_SETTLE_MS)
            attachOverlay()
        }
    }

    private fun recordOptions(): RecordOptions {
        val base = when (recordQuality) {
            "balanced" -> RecordOptions.BALANCED
            "compact" -> RecordOptions.COMPACT
            "low" -> RecordOptions.LOW
            else -> RecordOptions.NATIVE_MAX
        }
        // screenrecord не имеет флага fps: максимум кадров = нативное
        // разрешение и повышенный битрейт, иначе — снижаем поток.
        return if (recordMaxFps) base.copy(bitRateMbps = base.bitRateMbps * 2) else base
    }

    private fun refreshToggles() {
        val qualityText = when (recordQuality) {
            "balanced" -> getString(R.string.overlay_quality_balanced)
            "compact" -> getString(R.string.overlay_quality_compact)
            "low" -> getString(R.string.overlay_quality_low)
            else -> getString(R.string.overlay_quality_native)
        }
        qualityLabel.text = qualityText
        fpsLabel.text = getString(if (recordMaxFps) R.string.on_short else R.string.off_short)
        hideLabel.text = getString(if (hideOverlayInCapture) R.string.on_short else R.string.off_short)
    }

    private fun cycleQuality() {
        recordQuality = when (recordQuality) {
            "native" -> "balanced"
            "balanced" -> "compact"
            "compact" -> "low"
            else -> "native"
        }
        refreshToggles()
        serviceScope.launch { settings.setRecordQuality(recordQuality) }
    }

    private fun toggleMaxFps() {
        recordMaxFps = !recordMaxFps
        refreshToggles()
        serviceScope.launch { settings.setRecordMaxFps(recordMaxFps) }
    }

    private fun toggleHideOverlay() {
        hideOverlayInCapture = !hideOverlayInCapture
        refreshToggles()
        serviceScope.launch { settings.setHideOverlayInCapture(hideOverlayInCapture) }
    }

    private fun shareLastScreenshot() {
        val uri = lastScreenshotUri
        if (uri.isBlank()) {
            toast(getString(R.string.overlay_share_none))
            return
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, Uri.parse(uri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(send, getString(R.string.overlay_share)))
    }

    private fun showPreview(media: SavedMedia?) {
        val uri = media?.uri ?: return
        mainHandler.post {
            if (!::previewView.isInitialized) return@post
            previewView.visibility = View.VISIBLE
            previewView.setImageURI(uri)
        }
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
            val saved = withoutOverlay { useCase.captureScreenshot() }
            if (saved == null) {
                mainHandler.post { toast(getString(R.string.capture_failed)) }
                return@launch
            }
            lastScreenshotUri = saved.uri.toString()
            settings.setLastScreenshotUri(lastScreenshotUri)
            mainHandler.post {
                toast(getString(R.string.overlay_shot_done, saved.displayPath))
                showPreview(saved)
            }
        }
    }

    private fun toggleRecording() {
        if (!ShizukuManager.isShizukuRunning()) {
            toast(getString(R.string.shizuku_not_running))
            return
        }
        closeMenu()
        if (recording) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        recording = true
        setRecordingUi(true)
        detachOverlay()
        tickJob = serviceScope.launch {
            val path = withoutOverlay { useCase.startScreenRecording(recordOptions()) }
            recordingPath = path
        }
    }

    private fun stopRecording() {
        tickJob?.cancel()
        tickJob = null
        recording = false
        setRecordingUi(false)
        val path = recordingPath
        recordingPath = null
        attachOverlay()
        serviceScope.launch {
            val saved = withoutOverlay { useCase.stopScreenRecording(path) }
            mainHandler.post {
                toast(
                    if (saved != null) getString(R.string.overlay_record_done, saved.displayPath)
                    else getString(R.string.overlay_record_failed)
                )
            }
        }
    }

    private fun setRecordingUi(active: Boolean) {
        mainHandler.post {
            if (!::recordIcon.isInitialized) return@post
            val tint = if (active) 0xFFFF5252.toInt() else 0xD9FFFFFF.toInt()
            recordIcon.setImageResource(if (active) R.drawable.ic_stop else R.drawable.ic_videocam)
            recordIcon.imageTintList = ColorStateList.valueOf(tint)
            recordLabel.text = if (active) getString(R.string.overlay_stop) else getString(R.string.overlay_record)
            recordLabel.setTextColor(if (active) 0xFFFF5252.toInt() else 0xE6FFFFFF.toInt())
        }
        refreshNotification()
    }

    private fun closeMenu() {
        menuPanel.visibility = View.GONE
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private companion object {
        const val ACTION_SHOT = "adb.captain.action.SHOT"
        const val ACTION_RECORD = "adb.captain.action.RECORD"
        const val ACTION_STOP_RECORD = "adb.captain.action.STOP_RECORD"
        const val ACTION_SHARE = "adb.captain.action.SHARE"

        /** Кадр нужен, чтобы оверлей успел исчезнуть до screencap/screenrecord. */
        const val CAPTURE_SETTLE_MS = 250L
    }
}
