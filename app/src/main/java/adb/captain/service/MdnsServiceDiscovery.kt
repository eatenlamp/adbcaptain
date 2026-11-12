/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.service

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import java.net.InetAddress

/**
 * Minimal DNS-SD browser for the services advertised by the ADB daemon of this phone.
 *
 * libadb's `AdbMdns` only reports an advertisement when the resolved address matches one of
 * the local interfaces *and* the port is already bound on loopback. Android publishes the
 * `_adb-tls-pairing._tcp` service without any host address, so that check silently drops
 * the only advertisement this feature is interested in. Here every advertisement is
 * reported and the caller decides whether it can be reached.
 */
class MdnsServiceDiscovery(
    context: Context,
    serviceType: String,
    private val listener: Listener,
) {

    interface Listener {
        fun onService(name: String, host: InetAddress?, port: Int)
        fun onLost(name: String)
    }

    private val nsdManager =
        context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val executor = ContextCompat.getMainExecutor(context)
    private val serviceType = "_$serviceType._tcp"

    private val resolving = HashSet<String>()
    private val serviceCallbacks = HashMap<String, NsdManager.ServiceInfoCallback>()
    private var running = false

    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(serviceType: String) {
            Log.i(TAG, "Discovery started for $serviceType")
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.w(TAG, "Discovery start failed for $serviceType: $errorCode")
        }

        override fun onDiscoveryStopped(serviceType: String) {
            Log.i(TAG, "Discovery stopped for $serviceType")
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.w(TAG, "Discovery stop failed for $serviceType: $errorCode")
        }

        override fun onServiceFound(serviceInfo: NsdServiceInfo) {
            val name = serviceInfo.serviceName ?: return
            if (!resolving.add(name)) return
            Log.i(TAG, "Found $name for $serviceType")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                registerCallback(serviceInfo)
            } else {
                resolveLegacy(serviceInfo)
            }
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo) {
            val name = serviceInfo.serviceName ?: return
            resolving.remove(name)
            unregisterCallback(name)
            Log.i(TAG, "Lost $name for $serviceType")
            listener.onLost(name)
        }
    }

    fun start() {
        if (running) return
        running = true
        runCatching {
            nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        }.onFailure { Log.w(TAG, "Discovery failed for $serviceType", it) }
    }

    fun stop() {
        if (!running) return
        running = false
        runCatching { nsdManager.stopServiceDiscovery(discoveryListener) }
        serviceCallbacks.keys.toList().forEach(::unregisterCallback)
        resolving.clear()
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun registerCallback(serviceInfo: NsdServiceInfo) {
        val name = serviceInfo.serviceName ?: return
        val callback = object : NsdManager.ServiceInfoCallback {
            override fun onServiceInfoCallbackRegistrationFailed(errorCode: Int) {
                Log.w(TAG, "ServiceInfo callback failed for $name: $errorCode")
                resolving.remove(name)
                serviceCallbacks.remove(name)
                resolveLegacy(serviceInfo)
            }

            override fun onServiceUpdated(info: NsdServiceInfo) {
                report(name, info.port, info.hostAddresses.firstOrNull())
            }

            override fun onServiceLost() {
                resolving.remove(name)
                unregisterCallback(name)
                listener.onLost(name)
            }

            override fun onServiceInfoCallbackUnregistered() {
                resolving.remove(name)
                serviceCallbacks.remove(name)
            }
        }
        serviceCallbacks[name] = callback
        runCatching {
            nsdManager.registerServiceInfoCallback(serviceInfo, executor, callback)
        }.onFailure {
            Log.w(TAG, "Cannot watch $name", it)
            serviceCallbacks.remove(name)
            resolving.remove(name)
            resolveLegacy(serviceInfo)
        }
    }

    @Suppress("DEPRECATION")
    private fun resolveLegacy(serviceInfo: NsdServiceInfo) {
        val name = serviceInfo.serviceName ?: return
        runCatching {
            nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {
                    Log.w(TAG, "Resolve failed for $name: $errorCode")
                    resolving.remove(name)
                }

                override fun onServiceResolved(info: NsdServiceInfo) {
                    report(name, info.port, info.host)
                }
            })
        }.onFailure {
            Log.w(TAG, "Resolve call failed for $name", it)
            resolving.remove(name)
        }
    }

    @Suppress("DEPRECATION")
    private fun report(name: String, port: Int, host: InetAddress?) {
        if (port <= 0) return
        Log.i(TAG, "Resolved $name to ${host?.hostAddress ?: "no host"}:$port")
        listener.onService(name, host, port)
    }

    private fun unregisterCallback(name: String) {
        val callback = serviceCallbacks.remove(name) ?: return
        runCatching { nsdManager.unregisterServiceInfoCallback(callback) }
    }

    companion object {
        private const val TAG = "MdnsDiscovery"

        const val SERVICE_TYPE_TLS_PAIRING = "adb-tls-pairing"
        const val SERVICE_TYPE_TLS_CONNECT = "adb-tls-connect"
    }
}
