/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.presentation.screens.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import adb.captain.R
import adb.captain.domain.model.ProStatus

private const val TAG = "ProSection"

/**
 * Paid features of the store build, and the way to support the author.
 */
@Composable
fun ProSection(
    viewModel: ProViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val status by viewModel.status.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Text(stringResource(R.string.pro_title), style = MaterialTheme.typography.titleMedium)

    if (status == ProStatus.PRO) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.LockOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(R.string.pro_active),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    } else {
        products.forEach { product ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(product.title, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.pro_features),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            product.formattedPrice,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Button(onClick = { viewModel.purchase(product.id) }) {
                            Text(stringResource(R.string.pro_buy))
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    Spacer(Modifier.height(8.dp))

    OutlinedButton(
        onClick = { openStorePage(context) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.Favorite, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.pro_donate))
    }
}

/**
 * Opens the listing of the app in the store. The store handles the market
 * scheme when it is installed, otherwise the web site is used.
 */
private fun openStorePage(context: Context) {
    val market = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("market://details?id=${context.packageName}")
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(market)
    } catch (error: ActivityNotFoundException) {
        Log.w(TAG, "No store app installed, opening the web site", error)
        val web = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.rustore.ru/")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(web) }
    }
}
