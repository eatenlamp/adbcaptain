/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import adb.captain.MainActivity
import adb.captain.R
import adb.captain.domain.repository.RemoteAdbRepository
import dagger.hilt.android.AndroidEntryPoint
import java.net.InetAddress
import java.net.NetworkInterface
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Shizuku-style pairing for this very phone.
 *
 * Android only advertises the `_adb-tls-pairing._tcp` service while the "Pair device with
 * pairing code" dialog is open, and leaving Settings closes that dialog and rotates the code.
 * So instead of asking the user to retype the code in ADB Captain, the code is entered right
 * in the notification shade: the dialog stays open, the mDNS advertisement stays alive, and
 * this service does pair + connect in the background.
 *
 * The pairing endpoint is always reached over loopback, so a similarly advertised device on
 * the same Wi-Fi can never be paired by mistake.
 */
@AndroidEntryPoint
class WirelessPairingService : Service() {

    @Inject lateinit var repository: RemoteAdbRepository

    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private var pairingDiscovery: MdnsServiceDiscovery? = null
    private var connectDiscovery: MdnsServiceDiscovery? = null

    /** Pairing ports advertised while the Settings dialog is open; newest first. */
    private val pairingPorts = LinkedHashSet<Int>()

    /** Advertised address per pairing port; only a fallback for loopback. */
    private val pairingHosts = HashMap<Int, String>()

    /** Service instance name to port, so a closed dialog can drop its stale port. */
    private val pairingServiceNames = HashMap<String, Int>()

    /** Data port advertised by wireless debugging; used after a successful pairing. */
    @Volatile private var connectPort = -1

    @Volatile private var connectHost: String? = null

    @Volatile private var phase = Phase.SEARCHING
    @Volatile private var detail: String? = null
    @Volatile private var working = false

    private var workJob: Job? = null
    private var searchTimeout: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForegroundCompat(buildNotification(alert = true))
        startDiscovery()
        armSearchTimeout()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SUBMIT_CODE -> {
                val code = intent.getStringExtra(EXTRA_CODE).orEmpty()
                if (code.isNotEmpty()) submitCode(code)
            }
            ACTION_CANCEL -> stopPairing()
            else -> {
                if (!working) {
                    startDiscovery()
                    notify(alert = phase != Phase.READY)
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        runCatching { pairingDiscovery?.stop() }
        runCatching { connectDiscovery?.stop() }
        pairingDiscovery = null
        connectDiscovery = null
        searchTimeout?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun startDiscovery() {
        if (pairingDiscovery == null) {
            pairingDiscovery = runCatching {
                MdnsServiceDiscovery(
                    this,
                    MdnsServiceDiscovery.SERVICE_TYPE_TLS_PAIRING,
                    object : MdnsServiceDiscovery.Listener {
                        override fun onService(name: String, host: InetAddress?, port: Int) {
                            onPairingPort(name, host?.hostAddress, port)
                        }

                        override fun onLost(name: String) = onPairingLost(name)
                    }
                ).also { it.start() }
            }.onFailure { Log.w(TAG, "Pairing discovery failed", it) }.getOrNull()
        }
        if (connectDiscovery == null) {
            connectDiscovery = runCatching {
                MdnsServiceDiscovery(
                    this,
                    MdnsServiceDiscovery.SERVICE_TYPE_TLS_CONNECT,
                    object : MdnsServiceDiscovery.Listener {
                        override fun onService(name: String, host: InetAddress?, port: Int) {
                            onConnectPort(host?.hostAddress, port)
                        }

                        override fun onLost(name: String) = Unit
                    }
                ).also { it.start() }
            }.onFailure { Log.w(TAG, "Connect discovery failed", it) }.getOrNull()
        }
    }

    private fun onPairingPort(name: String, host: String?, port: Int) {
        if (port <= 0) return
        if (!isThisDevice(host)) {
            Log.i(TAG, "Ignoring pairing advertisement from $host:$port")
            return
        }
        val fresh = synchronized(pairingPorts) {
            val added = pairingPorts.remove(port)
            pairingPorts.add(port)
            pairingHosts[port] = host.orEmpty()
            pairingServiceNames[name] = port
            !added
        }
        Log.i(TAG, "Pairing service advertised on port $port (new=$fresh)")
        mainHandler.post {
            if (!working && phase != Phase.CONNECTED) {
                val wasSearching = phase == Phase.SEARCHING
                phase = Phase.READY
                detail = null
                if (fresh || wasSearching) notify(alert = true)
            }
        }
    }

    private fun onPairingLost(name: String) {
        val port = synchronized(pairingPorts) {
            val known = pairingServiceNames.remove(name) ?: return
            pairingPorts.remove(known)
            pairingHosts.remove(known)
            known
        }
        Log.i(TAG, "Pairing service on port $port went away")
    }

    private fun onConnectPort(host: String?, port: Int) {
        if (port <= 0) return
        connectPort = port
        connectHost = host
        Log.i(TAG, "Connect service advertised on port $port")
    }

    /**
     * Only the daemon of this phone is useful here. Another device on the same Wi-Fi may
     * advertise a pairing service at the same time; its port would never answer on loopback.
     */
    private fun isThisDevice(host: String?): Boolean {
        if (host.isNullOrBlank()) return true
        val addresses = runCatching {
            NetworkInterface.getNetworkInterfaces().toList()
                .flatMap { it.inetAddresses.toList() }
                .mapNotNull { it.hostAddress?.substringBefore('%') }
        }.getOrNull()
        // If the interfaces cannot be enumerated, accept rather than break the flow.
        if (addresses.isNullOrEmpty()) return true
        return host.substringBefore('%') in addresses
    }

    private fun submitCode(code: String) {
        if (working) return
        working = true
        searchTimeout?.cancel()
        phase = Phase.PAIRING
        detail = null
        notify(alert = false)

        workJob = scope.launch {
            val candidates = awaitPairingPorts()
            if (candidates.isEmpty()) {
                finishWithError(getString(R.string.wireless_pair_notify_expired))
                return@launch
            }

            var paired = false
            for (round in 0 until PAIR_ROUNDS) {
                val ports = if (round == 0) candidates else snapshotPairingPorts()
                for (port in ports) {
                    paired = tryPair(port, code)
                    if (paired) break
                }
                if (paired) break
                // A reopened dialog gets a fresh port; give discovery a moment to report it.
                delay(PAIR_RETRY_MS)
            }
            if (!paired) {
                finishWithError(getString(R.string.wireless_pair_notify_bad_code))
                return@launch
            }

            mainHandler.post {
                if (!working) return@post
                phase = Phase.CONNECTING
                notify(alert = false)
            }

            val dataPort = awaitConnectPort()
            if (dataPort <= 0) {
                finishWithError(getString(R.string.wireless_pair_notify_connect_failed))
                return@launch
            }

            var connected = false
            var connectedAddress: String? = null
            var attempts = 0
            val addresses = listOfNotNull(LOOPBACK, connectHost)
                .filter { it.isNotBlank() }
                .distinct()
            while (attempts < CONNECT_ATTEMPTS && !connected) {
                for (address in addresses) {
                    connected = repository.connect(address, dataPort).isSuccess
                    if (connected) {
                        connectedAddress = address
                        break
                    }
                }
                if (!connected) {
                    attempts++
                    if (attempts < CONNECT_ATTEMPTS) delay(CONNECT_RETRY_MS)
                }
            }
            if (connected) {
                finishConnected(connectedAddress ?: LOOPBACK, dataPort)
            } else {
                finishWithError(getString(R.string.wireless_pair_notify_connect_failed))
            }
        }
    }

    private suspend fun tryPair(port: Int, code: String): Boolean {
        val result = repository.pair(LOOPBACK, port, code)
        if (result.isSuccess) return true
        // Some OEM daemons are only reachable on the Wi-Fi address; retry there.
        val host = synchronized(pairingPorts) { pairingHosts[port] }
        if (!host.isNullOrBlank() && host != LOOPBACK) {
            Log.w(TAG, "Loopback pairing on $port failed, retrying on $host")
            if (repository.pair(host, port, code).isSuccess) return true
        }
        Log.w(TAG, "Pairing on $port failed", result.exceptionOrNull())
        return false
    }

    private fun snapshotPairingPorts(): List<Int> =
        synchronized(pairingPorts) { pairingPorts.toList().asReversed() }

    private suspend fun awaitPairingPorts(): List<Int> {
        val deadline = SystemClock.elapsedRealtime() + PORT_WAIT_MS
        while (SystemClock.elapsedRealtime() < deadline) {
            val ports = snapshotPairingPorts()
            if (ports.isNotEmpty()) return ports
            delay(POLL_MS)
        }
        return emptyList()
    }

    private suspend fun awaitConnectPort(): Int {
        val deadline = SystemClock.elapsedRealtime() + PORT_WAIT_MS
        while (SystemClock.elapsedRealtime() < deadline) {
            val port = connectPort
            if (port > 0) return port
            delay(POLL_MS)
        }
        return -1
    }

    private fun finishConnected(host: String, port: Int) {
        mainHandler.post {
            if (!working) return@post
            working = false
            phase = Phase.CONNECTED
            detail = getString(R.string.wireless_pair_notify_connected, "$host:$port")
            notify(alert = true)
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
            stopSelf()
        }
    }

    private fun finishWithError(message: String) {
        mainHandler.post {
            if (!working) return@post
            working = false
            phase = Phase.FAILED
            detail = message
            notify(alert = true)
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
            stopSelf()
        }
    }

    private fun stopPairing() {
        workJob?.cancel()
        workJob = null
        searchTimeout?.cancel()
        working = false
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        notificationManager.cancel(NOTIFICATION_ID)
        stopSelf()
    }

    private fun armSearchTimeout() {
        searchTimeout?.cancel()
        searchTimeout = scope.launch {
            delay(SEARCH_TIMEOUT_MS)
            mainHandler.post {
                if (!working && phase == Phase.SEARCHING) {
                    phase = Phase.FAILED
                    detail = getString(R.string.wireless_pair_notify_timeout)
                    notify(alert = true)
                    ServiceCompat.stopForeground(
                        this@WirelessPairingService,
                        ServiceCompat.STOP_FOREGROUND_DETACH
                    )
                    stopSelf()
                }
            }
        }
    }

    private fun notify(alert: Boolean) {
        notificationManager.notify(NOTIFICATION_ID, buildNotification(alert))
    }

    private fun buildNotification(alert: Boolean): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_wireless)
            .setContentTitle(getString(R.string.wireless_title))
            .setContentText(notificationText())
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationText()))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // Updates must not re-alert, but arriving at the code prompt should.
            .setOnlyAlertOnce(!alert)
            .setOngoing(phase != Phase.CONNECTED && phase != Phase.FAILED)
            .setAutoCancel(phase == Phase.CONNECTED)
            .setContentIntent(appPendingIntent())

        when (phase) {
            Phase.SEARCHING -> builder.addAction(
                0,
                getString(R.string.wireless_pair_notify_open_settings),
                settingsPendingIntent()
            )
            Phase.READY -> builder.addAction(codeAction())
            Phase.FAILED -> {
                builder.addAction(codeAction())
                builder.addAction(
                    0,
                    getString(R.string.wireless_pair_notify_open_settings),
                    settingsPendingIntent()
                )
            }
            Phase.CONNECTED -> builder.addAction(
                0,
                getString(R.string.wireless_pair_notify_open_app),
                appPendingIntent()
            )
            Phase.PAIRING, Phase.CONNECTING -> Unit
        }

        if (phase != Phase.CONNECTED && phase != Phase.FAILED) {
            builder.addAction(
                0,
                getString(R.string.wireless_pair_notify_cancel),
                cancelPendingIntent()
            )
        }

        return builder.build()
    }

    private fun notificationText(): String = when (phase) {
        Phase.SEARCHING -> getString(R.string.wireless_pair_notify_searching)
        Phase.READY -> getString(R.string.wireless_pair_notify_ready)
        Phase.PAIRING -> getString(R.string.wireless_pair_notify_pairing)
        Phase.CONNECTING -> getString(R.string.wireless_pair_notify_connecting)
        Phase.CONNECTED -> detail ?: getString(R.string.wireless_pair_notify_connected, LOOPBACK)
        Phase.FAILED -> detail ?: getString(R.string.wireless_pair_notify_bad_code)
    }

    private fun codeAction(): NotificationCompat.Action {
        val input = RemoteInput.Builder(EXTRA_CODE)
            .setLabel(getString(R.string.wireless_pairing_code))
            .build()
        val intent = Intent(this, PairingCodeReceiver::class.java)
            .setAction(PairingCodeReceiver.ACTION_CODE)
        // RemoteInput results are attached to the Intent by the system, so this one
        // PendingIntent has to be mutable. It is explicit and non-exported.
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            REQUEST_CODE_INPUT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        return NotificationCompat.Action.Builder(
            R.drawable.ic_wireless,
            getString(R.string.wireless_pair_notify_enter_code),
            pendingIntent
        )
            .addRemoteInput(input)
            .setAllowGeneratedReplies(false)
            .build()
    }

    private fun appPendingIntent(): PendingIntent {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
            ?: Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            this,
            REQUEST_CODE_APP,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun settingsPendingIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        REQUEST_CODE_SETTINGS,
        Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun cancelPendingIntent(): PendingIntent = PendingIntent.getService(
        this,
        REQUEST_CODE_CANCEL,
        Intent(this, WirelessPairingService::class.java).setAction(ACTION_CANCEL),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.wireless_pair_notify_channel),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.wireless_pair_notify_channel_desc)
            setShowBadge(true)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun startForegroundCompat(notification: Notification) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    private enum class Phase { SEARCHING, READY, PAIRING, CONNECTING, CONNECTED, FAILED }

    companion object {
        private const val TAG = "WirelessPairing"

        private const val CHANNEL_ID = "wireless_pairing"
        private const val NOTIFICATION_ID = 0x41

        const val ACTION_START = "adb.captain.action.WIRELESS_PAIRING_START"
        const val ACTION_SUBMIT_CODE = "adb.captain.action.WIRELESS_PAIRING_CODE"
        private const val ACTION_CANCEL = "adb.captain.action.WIRELESS_PAIRING_CANCEL"
        const val EXTRA_CODE = "pairing_code"

        private const val REQUEST_CODE_INPUT = 11
        private const val REQUEST_CODE_APP = 12
        private const val REQUEST_CODE_SETTINGS = 13
        private const val REQUEST_CODE_CANCEL = 14

        /** The daemon of this very phone is always reachable over loopback. */
        private const val LOOPBACK = "127.0.0.1"

        private const val PORT_WAIT_MS = 10_000L
        private const val POLL_MS = 200L
        private const val PAIR_ROUNDS = 3
        private const val PAIR_RETRY_MS = 800L
        private const val SEARCH_TIMEOUT_MS = 10 * 60 * 1000L
        private const val CONNECT_ATTEMPTS = 4
        private const val CONNECT_RETRY_MS = 1_500L

        fun start(context: Context) {
            val intent = Intent(context, WirelessPairingService::class.java)
                .setAction(ACTION_START)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
