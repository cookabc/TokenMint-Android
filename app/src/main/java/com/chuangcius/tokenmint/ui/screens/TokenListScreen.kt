package com.chuangcius.tokenmint.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chuangcius.tokenmint.R
import com.chuangcius.tokenmint.data.model.Token
import com.chuangcius.tokenmint.ui.components.TokenRow
import com.chuangcius.tokenmint.ui.theme.TokenMintAccent
import com.chuangcius.tokenmint.ui.viewmodels.VaultState
import com.chuangcius.tokenmint.ui.viewmodels.VaultViewModel

/**
 * Main screen showing all TOTP tokens with search, swipe actions, and navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TokenListScreen(
    viewModel: VaultViewModel,
    onNavigateToAdd: () -> Unit,
    onNavigateToScan: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showAddMenu by remember { mutableStateOf(false) }

    // Collect snackbar events
    LaunchedEffect(Unit) {
        viewModel.snackbarEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.cd_settings)
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showAddMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.cd_add_token)
                            )
                        }
                        DropdownMenu(
                            expanded = showAddMenu,
                            onDismissRequest = { showAddMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.add_manually)) },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Add, contentDescription = null)
                                },
                                onClick = {
                                    showAddMenu = false
                                    onNavigateToAdd()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.scan_qr_code)) },
                                leadingIcon = {
                                    Icon(Icons.Outlined.QrCodeScanner, contentDescription = null)
                                },
                                onClick = {
                                    showAddMenu = false
                                    onNavigateToScan()
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        when (val s = state) {
            is VaultState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is VaultState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = s.message,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { viewModel.loadVault() }) {
                        Text(stringResource(R.string.error_retry))
                    }
                }
            }

            is VaultState.Success -> {
                TokenListContent(
                    tokens = s.tokens,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onDeleteToken = { viewModel.deleteToken(it) },
                    onTogglePin = { viewModel.togglePin(it) },
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun TokenListContent(
    tokens: List<Token>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onDeleteToken: (Token) -> Unit,
    onTogglePin: (Token) -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredTokens = remember(tokens, searchQuery) {
        if (searchQuery.isBlank()) {
            tokens
        } else {
            val query = searchQuery.lowercase()
            tokens.filter {
                it.issuer.lowercase().contains(query) ||
                    it.account.lowercase().contains(query)
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Search bar
        if (tokens.isNotEmpty()) {
            SearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange
            )
        }

        if (tokens.isEmpty()) {
            EmptyState()
        } else if (filteredTokens.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No results",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            TokenList(
                tokens = filteredTokens,
                onDelete = onDeleteToken,
                onTogglePin = onTogglePin
            )
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(R.string.search_tokens)) },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            unfocusedContainerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedIndicatorColor = MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TokenList(
    tokens: List<Token>,
    onDelete: (Token) -> Unit,
    onTogglePin: (Token) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(items = tokens, key = { it.id }) { token ->
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = { value ->
                    when (value) {
                        SwipeToDismissBoxValue.EndToStart -> {
                            onDelete(token)
                            true
                        }
                        SwipeToDismissBoxValue.StartToEnd -> {
                            onTogglePin(token)
                            false // Reset the swipe, don't stay dismissed
                        }
                        SwipeToDismissBoxValue.Settled -> false
                    }
                }
            )

            SwipeToDismissBox(
                state = dismissState,
                backgroundContent = {
                    val direction = dismissState.dismissDirection

                    val bgColor by animateColorAsState(
                        when (direction) {
                            SwipeToDismissBoxValue.EndToStart ->
                                MaterialTheme.colorScheme.errorContainer
                            SwipeToDismissBoxValue.StartToEnd ->
                                TokenMintAccent.copy(alpha = 0.2f)
                            else -> Color.Transparent
                        },
                        label = "swipe-bg"
                    )

                    val alignment = when (direction) {
                        SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                        else -> Alignment.CenterStart
                    }

                    val icon = when (direction) {
                        SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                        else -> Icons.Default.PushPin
                    }

                    val tint = when (direction) {
                        SwipeToDismissBoxValue.EndToStart ->
                            MaterialTheme.colorScheme.onErrorContainer
                        else -> TokenMintAccent
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(bgColor)
                            .padding(horizontal = 20.dp),
                        contentAlignment = alignment
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = tint
                        )
                    }
                },
                enableDismissFromStartToEnd = true,
                enableDismissFromEndToStart = true
            ) {
                TokenRow(
                    token = token,
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                )
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.no_tokens_title),
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.no_tokens_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}
