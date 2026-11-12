/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat

/**
 * Receives the pairing code typed into the notification shade and hands it to the service.
 * Going through a broadcast (instead of a service action directly) is required for RemoteInput
 * and keeps the flow working even if the process was killed while the shade was open.
 */
class PairingCodeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val code = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(WirelessPairingService.EXTRA_CODE)
            ?.toString()
            ?.filter(Char::isDigit)
            .orEmpty()
        // Android pairing codes are exactly six digits; anything else is a stray send.
        if (code.length != PAIRING_CODE_LENGTH) return
        val service = Intent(context, WirelessPairingService::class.java)
            .setAction(WirelessPairingService.ACTION_SUBMIT_CODE)
            .putExtra(WirelessPairingService.EXTRA_CODE, code)
        ContextCompat.startForegroundService(context, service)
    }

    companion object {
        const val ACTION_CODE = "adb.captain.action.WIRELESS_PAIRING_INPUT"
        private const val PAIRING_CODE_LENGTH = 6
    }
}
