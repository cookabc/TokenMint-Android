package com.chuangcius.tokenmint.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.chuangcius.tokenmint.TokenMintApp
import com.chuangcius.tokenmint.data.model.Token
import com.chuangcius.tokenmint.data.model.Vault
import com.chuangcius.tokenmint.data.repository.VaultRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID

// MARK: - State

sealed class VaultState {
    data object Loading : VaultState()
    data class Success(val tokens: List<Token>) : VaultState()
    data class Error(val message: String) : VaultState()
}

// MARK: - ViewModel

class VaultViewModel(
    private val repository: VaultRepository
) : ViewModel() {

    private val _state = MutableStateFlow<VaultState>(VaultState.Loading)
    val state: StateFlow<VaultState> = _state.asStateFlow()

    private val _snackbarEvent = MutableSharedFlow<String>()
    val snackbarEvent: SharedFlow<String> = _snackbarEvent.asSharedFlow()

    private var vault = Vault()

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    init {
        loadVault()
    }

    fun loadVault() {
        _state.value = VaultState.Loading
        viewModelScope.launch {
            repository.load()
                .onSuccess {
                    vault = it
                    _state.value = VaultState.Success(sortedTokens())
                }
                .onFailure {
                    _state.value = VaultState.Error(it.message ?: "Unknown error")
                }
        }
    }

    fun addToken(token: Token) {
        viewModelScope.launch {
            val newToken = token.copy(
                sortOrder = vault.tokens.size,
                updatedAt = Instant.now()
            )
            val updated = vault.copy(
                tokens = vault.tokens + newToken,
                vaultVersion = vault.vaultVersion + 1,
                updatedAt = Instant.now()
            )
            repository.save(updated)
                .onSuccess {
                    vault = updated
                    _state.value = VaultState.Success(sortedTokens())
                }
                .onFailure {
                    _snackbarEvent.emit(it.message ?: "Failed to save")
                }
        }
    }

    fun deleteToken(token: Token) {
        viewModelScope.launch {
            val updated = vault.copy(
                tokens = vault.tokens.filter { it.id != token.id },
                vaultVersion = vault.vaultVersion + 1,
                updatedAt = Instant.now()
            )
            repository.save(updated)
                .onSuccess {
                    vault = updated
                    _state.value = VaultState.Success(sortedTokens())
                }
                .onFailure {
                    _snackbarEvent.emit(it.message ?: "Failed to delete")
                }
        }
    }

    fun updateToken(token: Token) {
        viewModelScope.launch {
            val tokens = vault.tokens.map {
                if (it.id == token.id) token.copy(updatedAt = Instant.now()) else it
            }
            val updated = vault.copy(
                tokens = tokens,
                vaultVersion = vault.vaultVersion + 1,
                updatedAt = Instant.now()
            )
            repository.save(updated)
                .onSuccess {
                    vault = updated
                    _state.value = VaultState.Success(sortedTokens())
                }
                .onFailure {
                    _snackbarEvent.emit(it.message ?: "Failed to update")
                }
        }
    }

    fun togglePin(token: Token) {
        updateToken(token.copy(isPinned = !token.isPinned))
    }

    fun reorderTokens(tokens: List<Token>) {
        viewModelScope.launch {
            val reordered = tokens.mapIndexed { index, token ->
                token.copy(sortOrder = index)
            }
            val updated = vault.copy(
                tokens = reordered,
                vaultVersion = vault.vaultVersion + 1,
                updatedAt = Instant.now()
            )
            repository.save(updated)
                .onSuccess {
                    vault = updated
                    _state.value = VaultState.Success(sortedTokens())
                }
                .onFailure {
                    _snackbarEvent.emit(it.message ?: "Failed to reorder")
                }
        }
    }

    /** Export vault tokens as JSON string. */
    fun exportVault(): String {
        return try {
            json.encodeToString(
                kotlinx.serialization.builtins.ListSerializer(Token.serializer()),
                vault.tokens
            )
        } catch (_: Exception) {
            "[]"
        }
    }

    /** Import tokens from JSON string. Returns count of new tokens imported. */
    fun importVault(jsonString: String, onResult: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            try {
                val tokens = json.decodeFromString(
                    kotlinx.serialization.builtins.ListSerializer(Token.serializer()),
                    jsonString
                )
                val existingIds = vault.tokens.map { it.id }.toSet()
                val newTokens = tokens.filter { it.id !in existingIds }

                if (newTokens.isEmpty()) {
                    onResult(Result.success(0))
                    return@launch
                }

                val updated = vault.copy(
                    tokens = vault.tokens + newTokens.mapIndexed { index, token ->
                        token.copy(sortOrder = vault.tokens.size + index)
                    },
                    vaultVersion = vault.vaultVersion + 1,
                    updatedAt = Instant.now()
                )
                repository.save(updated)
                    .onSuccess {
                        vault = updated
                        _state.value = VaultState.Success(sortedTokens())
                        onResult(Result.success(newTokens.size))
                    }
                    .onFailure {
                        onResult(Result.failure(it))
                    }
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    val tokenCount: Int get() = vault.tokens.size

    private fun sortedTokens(): List<Token> {
        return vault.tokens.sortedWith(
            compareByDescending<Token> { it.isPinned }
                .thenBy { it.sortOrder }
        )
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TokenMintApp
                VaultViewModel(app.vaultRepository)
            }
        }
    }
}
