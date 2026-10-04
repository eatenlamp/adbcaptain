/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.ipc

/**
 * Wire contract for the Messenger bridge to the paid ADB Orchestra companion.
 * The companion mirrors these constants. Bump [API_VERSION] on a breaking
 * change and reject older clients on the companion side.
 *
 * Request messages carry [KEY_COMMAND]; the service always answers with a
 * [MSG_REPLY] whose Bundle holds either [KEY_RESULT] or [KEY_ERROR].
 */
object IpcContract {
    const val API_VERSION = 1

    /** Client -> service: is the engine ready? */
    const val MSG_PING = 1

    /** Client -> service: execute the shell command in [KEY_COMMAND]. */
    const val MSG_EXECUTE = 2

    /** Service -> client: the answer to any request. */
    const val MSG_REPLY = 3

    const val KEY_COMMAND = "command"
    const val KEY_RESULT = "result"
    const val KEY_ERROR = "error"
    const val KEY_READY = "ready"
    const val KEY_API_VERSION = "apiVersion"
    const val KEY_REQUEST_ID = "requestId"
}
