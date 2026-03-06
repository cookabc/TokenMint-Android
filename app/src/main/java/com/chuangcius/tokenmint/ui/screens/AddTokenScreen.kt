package com.chuangcius.tokenmint.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.chuangcius.tokenmint.R
import com.chuangcius.tokenmint.data.model.TOTPAlgorithm
import com.chuangcius.tokenmint.data.model.Token
import com.chuangcius.tokenmint.service.TOTPService
import com.chuangcius.tokenmint.ui.components.BackButton

/**
 * Manual token entry form with issuer, account, secret, and advanced TOTP settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTokenScreen(
    onSave: (Token) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var issuer by rememberSaveable { mutableStateOf("") }
    var account by rememberSaveable { mutableStateOf("") }
    var secret by rememberSaveable { mutableStateOf("") }
    var digits by rememberSaveable { mutableIntStateOf(6) }
    var period by rememberSaveable { mutableIntStateOf(30) }
    var algorithm by rememberSaveable { mutableStateOf(TOTPAlgorithm.SHA1) }

    val isValid = issuer.isNotBlank() && secret.isNotBlank() && TOTPService.isValidBase32(secret)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_token)) },
                navigationIcon = { BackButton(onClick = onBack) },
                actions = {
                    IconButton(
                        onClick = {
                            val token = Token(
                                issuer = issuer.trim(),
                                account = account.trim(),
                                secret = secret.trim(),
                                digits = digits,
                                period = period,
                                algorithm = algorithm
                            )
                            onSave(token)
                        },
                        enabled = isValid
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.save)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Account Info
            SectionHeader(stringResource(R.string.account_info))

            OutlinedTextField(
                value = issuer,
                onValueChange = { issuer = it },
                label = { Text(stringResource(R.string.issuer_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = account,
                onValueChange = { account = it },
                label = { Text(stringResource(R.string.account_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Secret Key
            SectionHeader(stringResource(R.string.secret_key))

            OutlinedTextField(
                value = secret,
                onValueChange = { secret = it },
                label = { Text(stringResource(R.string.secret_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters
                ),
                isError = secret.isNotEmpty() && !TOTPService.isValidBase32(secret),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Advanced
            SectionHeader(stringResource(R.string.advanced))

            DropdownPicker(
                label = stringResource(R.string.digits),
                options = listOf(6 to "6", 8 to "8"),
                selected = digits,
                onSelect = { digits = it }
            )

            Spacer(modifier = Modifier.height(8.dp))

            DropdownPicker(
                label = stringResource(R.string.period),
                options = listOf(30 to "30s", 60 to "60s"),
                selected = period,
                onSelect = { period = it }
            )

            Spacer(modifier = Modifier.height(8.dp))

            AlgorithmPicker(
                selected = algorithm,
                onSelect = { algorithm = it }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> DropdownPicker(
    label: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.first { it.first == selected }.second

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (value, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        onSelect(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlgorithmPicker(
    selected: TOTPAlgorithm,
    onSelect: (TOTPAlgorithm) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selected.name,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.algorithm)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            TOTPAlgorithm.entries.forEach { algo ->
                DropdownMenuItem(
                    text = { Text(algo.name) },
                    onClick = {
                        onSelect(algo)
                        expanded = false
                    }
                )
            }
        }
    }
}
