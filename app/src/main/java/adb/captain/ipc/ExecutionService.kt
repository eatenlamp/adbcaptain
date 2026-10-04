/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.ipc

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import adb.captain.ShizukuManager
import adb.captain.domain.repository.AdbRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Lets the paid ADB Orchestra companion run shell commands through the same
 * Shizuku engine the core uses, without shipping any AGPL code in the
 * companion. Bind with [ACTION] and the `adb.captain.permission.EXECUTE_COMMANDS`
 * permission declared by ADB Captain; [CallerVerifier] then pins the caller.
 *
 * Messenger/Bundle is used instead of AIDL on purpose: the native aidl tool
 * cannot write its dependency file when the project path contains non-ASCII
 * characters, which is the norm on this workstation.
 */
@AndroidEntryPoint
class ExecutionService : Service() {

    @Inject
    lateinit var repository: AdbRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var messenger: Messenger

    override fun onCreate() {
        super.onCreate()
        val handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    IpcContract.MSG_PING -> handlePing(msg)
                    IpcContract.MSG_EXECUTE -> handleExecute(msg)
                    else -> super.handleMessage(msg)
                }
            }
        }
        messenger = Messenger(handler)
    }

    override fun onBind(intent: Intent?): IBinder = messenger.binder

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun handlePing(msg: Message) {
        val allowed = CallerVerifier.isAllowed(this, msg.sendingUid)
        reply(msg, Bundle().apply {
            putInt(IpcContract.KEY_API_VERSION, IpcContract.API_VERSION)
            putBoolean(IpcContract.KEY_READY, allowed && shizukuReady())
        })
    }

    private fun handleExecute(msg: Message) {
        if (!CallerVerifier.isAllowed(this, msg.sendingUid)) {
            reply(msg, Bundle().apply { putString(IpcContract.KEY_ERROR, "caller not allowed") })
            return
        }
        val command = msg.data?.getString(IpcContract.KEY_COMMAND).orEmpty()
        when {
            command.isBlank() ->
                reply(msg, Bundle().apply { putString(IpcContract.KEY_RESULT, "") })
            command.length > MAX_COMMAND_LENGTH ->
                reply(msg, Bundle().apply { putString(IpcContract.KEY_ERROR, "command too long") })
            else -> scope.launch {
                val result = runCatching { repository.executeCommand(command) }
                    .getOrElse { "Error: ${it.message}" }
                reply(msg, Bundle().apply { putString(IpcContract.KEY_RESULT, result) })
            }
        }
    }

    private fun reply(request: Message, payload: Bundle) {
        val replyTo = request.replyTo ?: return
        val requestId = request.data?.getInt(IpcContract.KEY_REQUEST_ID, -1) ?: -1
        val response = Message.obtain(null, IpcContract.MSG_REPLY).apply {
            data = Bundle(payload).apply { putInt(IpcContract.KEY_REQUEST_ID, requestId) }
        }
        try {
            replyTo.send(response)
        } catch (e: RemoteException) {
            // The client disconnected before we answered.
        }
    }

    private fun shizukuReady(): Boolean =
        ShizukuManager.isShizukuRunning() && ShizukuManager.checkShizukuPermission()

    companion object {
        /** Action a client uses when binding. */
        const val ACTION = "adb.captain.action.EXECUTE"

        /** Permission the client must hold. */
        const val PERMISSION = "adb.captain.permission.EXECUTE_COMMANDS"

        private const val MAX_COMMAND_LENGTH = 4096
    }
}
