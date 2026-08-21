package adb.captain.presentation.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import adb.captain.R
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import adb.captain.ShizukuManager

@Composable
fun SettingsScreen(
    onOpenHelp: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val isAutoComplete by viewModel.isAutoCompleteEnabled.collectAsState()
    val currentLanguage by viewModel.language.collectAsState()
    
    var shizukuStatus by remember { mutableStateOf(ShizukuManager.isShizukuRunning()) }
    LaunchedEffect(Unit) {
        while(true) {
            shizukuStatus = ShizukuManager.isShizukuRunning()
            kotlinx.coroutines.delay(3000)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(stringResource(R.string.settings_help), style = MaterialTheme.typography.titleMedium)

        OutlinedButton(
            onClick = onOpenHelp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.AutoMirrored.Filled.Help, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.settings_open_help))
        }

        HorizontalDivider()

        Text(stringResource(R.string.settings_appearance), style = MaterialTheme.typography.titleMedium)
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.settings_dark_theme), modifier = Modifier.weight(1f))
            Switch(checked = isDarkTheme, onCheckedChange = { viewModel.setDarkTheme(it) })
        }

        HorizontalDivider()

        Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.titleMedium)

        val supportedLanguages = listOf(
            "en" to "English",
            "ru" to "Русский",
            "de" to "Deutsch",
            "es" to "Español",
            "fr" to "Français",
            "it" to "Italiano",
            "pt" to "Português",
            "pl" to "Polski",
            "uk" to "Українська",
            "nl" to "Nederlands",
            "tr" to "Türkçe",
            "cs" to "Čeština",
            "zh" to "中文",
            "ja" to "日本語",
            "hi" to "हिन्दी"
        )

        supportedLanguages.forEach { (code, name) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, modifier = Modifier.weight(1f))
                RadioButton(selected = currentLanguage == code, onClick = { viewModel.setLanguage(code) })
            }
        }

        HorizontalDivider()

        Text(stringResource(R.string.settings_features), style = MaterialTheme.typography.titleMedium)
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.settings_auto_complete), modifier = Modifier.weight(1f))
            Switch(checked = isAutoComplete, onCheckedChange = { viewModel.setAutoCompleteEnabled(it) })
        }

        HorizontalDivider()

        Text(stringResource(R.string.settings_shizuku), style = MaterialTheme.typography.titleMedium)
        
        Surface(
            color = if (shizukuStatus) Color(0xFF4CAF50).copy(alpha = 0.1f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (shizukuStatus) stringResource(R.string.settings_shizuku_status_running) else stringResource(R.string.settings_shizuku_status_not_running),
                modifier = Modifier.padding(12.dp),
                color = if (shizukuStatus) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.settings_version), style = MaterialTheme.typography.bodySmall)
        }
    }
}
