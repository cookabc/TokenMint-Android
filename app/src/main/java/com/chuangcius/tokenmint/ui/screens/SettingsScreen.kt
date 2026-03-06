package com.chuangcius.tokenmint.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.chuangcius.tokenmint.R
import com.chuangcius.tokenmint.service.BiometricService
import com.chuangcius.tokenmint.ui.components.BackButton
import com.chuangcius.tokenmint.ui.viewmodels.VaultViewModel

/**
 * Settings screen: biometric toggle, haptic/theme/language prefs,
 * export/import vault, and about section.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: VaultViewModel,
    onBack: () -> Unit,
    onLaunchImportFilePicker: ((Uri?) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }

    var biometricEnabled by remember {
        mutableStateOf(prefs.getBoolean("biometric_enabled", true))
    }
    var hapticEnabled by remember {
        mutableStateOf(prefs.getBoolean("haptic_enabled", true))
    }
    var selectedTheme by remember {
        mutableIntStateOf(prefs.getInt("theme", 0)) // 0=System, 1=Light, 2=Dark
    }

    var importAlertMessage by remember { mutableStateOf<String?>(null) }

    val activity = context as? FragmentActivity
    val canBiometric = remember {
        activity?.let { BiometricService.canAuthenticate(it) } ?: false
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = { BackButton(onClick = onBack) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Security section
            if (canBiometric) {
                SectionTitle(stringResource(R.string.security))
                SwitchRow(
                    label = stringResource(R.string.biometric_unlock),
                    checked = biometricEnabled,
                    onCheckedChange = {
                        biometricEnabled = it
                        prefs.edit().putBoolean("biometric_enabled", it).apply()
                    }
                )
                HorizontalDivider()
            }

            // Preferences section
            SectionTitle(stringResource(R.string.preferences))
            SwitchRow(
                label = stringResource(R.string.haptic_feedback),
                checked = hapticEnabled,
                onCheckedChange = {
                    hapticEnabled = it
                    prefs.edit().putBoolean("haptic_enabled", it).apply()
                }
            )
            HorizontalDivider()

            ThemeRow(
                selected = selectedTheme,
                onSelect = {
                    selectedTheme = it
                    prefs.edit().putInt("theme", it).apply()
                }
            )
            HorizontalDivider()

            // Data section
            SectionTitle(stringResource(R.string.data))
            ClickableRow(
                label = stringResource(R.string.export_vault),
                icon = { Icon(Icons.Default.FileUpload, contentDescription = null) },
                onClick = {
                    val json = viewModel.exportVault()
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, json)
                        type = "application/json"
                    }
                    context.startActivity(
                        Intent.createChooser(sendIntent, context.getString(R.string.export_vault))
                    )
                }
            )
            HorizontalDivider()
            ClickableRow(
                label = stringResource(R.string.import_vault),
                icon = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                onClick = {
                    onLaunchImportFilePicker { uri ->
                        if (uri == null) return@onLaunchImportFilePicker
                        try {
                            val json = context.contentResolver.openInputStream(uri)
                                ?.bufferedReader()?.use { it.readText() }
                                ?: return@onLaunchImportFilePicker
                            viewModel.importVault(json) { result ->
                                result
                                    .onSuccess { count ->
                                        importAlertMessage =
                                            context.getString(R.string.import_success, count)
                                    }
                                    .onFailure { e ->
                                        importAlertMessage = context.getString(
                                            R.string.import_failed,
                                            e.message ?: "Unknown"
                                        )
                                    }
                            }
                        } catch (e: Exception) {
                            importAlertMessage =
                                context.getString(R.string.import_failed, e.message ?: "Unknown")
                        }
                    }
                }
            )
            HorizontalDivider()

            // About section
            SectionTitle(stringResource(R.string.about))
            InfoRow(
                label = stringResource(R.string.version),
                value = getAppVersion(context)
            )
            HorizontalDivider()
            InfoRow(
                label = stringResource(R.string.tokens_count),
                value = viewModel.tokenCount.toString()
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Import result alert
    importAlertMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { importAlertMessage = null },
            title = { Text(stringResource(R.string.import_complete)) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { importAlertMessage = null }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }
}

// MARK: - Composable Helpers

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ThemeRow(
    selected: Int,
    onSelect: (Int) -> Unit
) {
    val themes = listOf(
        stringResource(R.string.theme_system),
        stringResource(R.string.theme_light),
        stringResource(R.string.theme_dark)
    )
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.theme),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = themes[selected],
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }

    if (expanded) {
        AlertDialog(
            onDismissRequest = { expanded = false },
            title = { Text(stringResource(R.string.theme)) },
            text = {
                Column {
                    themes.forEachIndexed { index, label ->
                        TextButton(
                            onClick = {
                                onSelect(index)
                                expanded = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
private fun ClickableRow(
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

private fun getAppVersion(context: Context): String {
    return try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
    } catch (_: Exception) {
        "1.0"
    }
}
