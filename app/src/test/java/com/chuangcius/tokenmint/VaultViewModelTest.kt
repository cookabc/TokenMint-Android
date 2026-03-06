package com.chuangcius.tokenmint

import com.chuangcius.tokenmint.data.model.Token
import com.chuangcius.tokenmint.data.model.Vault
import com.chuangcius.tokenmint.data.repository.VaultRepository
import com.chuangcius.tokenmint.ui.viewmodels.VaultState
import com.chuangcius.tokenmint.ui.viewmodels.VaultViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VaultViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeVaultRepository
    private lateinit var viewModel: VaultViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeVaultRepository()
        viewModel = VaultViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial load sets Success state with empty tokens`() = runTest {
        advanceUntilIdle()
        val state = viewModel.state.value
        assertTrue(state is VaultState.Success)
        assertEquals(0, (state as VaultState.Success).tokens.size)
    }

    @Test
    fun `addToken adds to success state`() = runTest {
        advanceUntilIdle() // complete initial load
        viewModel.addToken(Token(issuer = "GitHub", secret = "JBSWY3DPEHPK3PXP"))
        advanceUntilIdle()

        val state = viewModel.state.value as VaultState.Success
        assertEquals(1, state.tokens.size)
        assertEquals("GitHub", state.tokens[0].issuer)
    }

    @Test
    fun `deleteToken removes from state`() = runTest {
        advanceUntilIdle()
        val token = Token(issuer = "GitHub", secret = "JBSWY3DPEHPK3PXP")
        viewModel.addToken(token)
        advanceUntilIdle()

        val addedToken = (viewModel.state.value as VaultState.Success).tokens[0]
        viewModel.deleteToken(addedToken)
        advanceUntilIdle()

        val state = viewModel.state.value as VaultState.Success
        assertEquals(0, state.tokens.size)
    }

    @Test
    fun `togglePin toggles isPinned`() = runTest {
        advanceUntilIdle()
        val token = Token(issuer = "GitHub", secret = "JBSWY3DPEHPK3PXP", isPinned = false)
        viewModel.addToken(token)
        advanceUntilIdle()

        val addedToken = (viewModel.state.value as VaultState.Success).tokens[0]
        viewModel.togglePin(addedToken)
        advanceUntilIdle()

        val state = viewModel.state.value as VaultState.Success
        assertTrue(state.tokens[0].isPinned)
    }

    @Test
    fun `load failure sets Error state`() = runTest {
        fakeRepository.shouldFail = true
        viewModel.loadVault()
        advanceUntilIdle()

        assertTrue(viewModel.state.value is VaultState.Error)
    }

    @Test
    fun `exportVault returns JSON`() = runTest {
        advanceUntilIdle()
        viewModel.addToken(Token(issuer = "Test", secret = "JBSWY3DPEHPK3PXP"))
        advanceUntilIdle()

        val json = viewModel.exportVault()
        assertTrue(json.contains("Test"))
        assertTrue(json.contains("JBSWY3DPEHPK3PXP"))
    }

    @Test
    fun `importVault adds new tokens`() = runTest {
        advanceUntilIdle()
        val json = """[{"issuer":"Imported","secret":"JBSWY3DPEHPK3PXP","account":"","label":"","digits":6,"period":30,"algorithm":"sha1","sortOrder":0,"isPinned":false}]"""

        var importedCount = -1
        viewModel.importVault(json) { result ->
            importedCount = result.getOrDefault(0)
        }
        advanceUntilIdle()

        assertEquals(1, importedCount)
        val state = viewModel.state.value as VaultState.Success
        assertEquals(1, state.tokens.size)
    }
}

/**
 * In-memory VaultRepository for testing.
 */
private class FakeVaultRepository : VaultRepository(context = null) {
    var shouldFail = false
    private var vault = Vault()

    override suspend fun load(): Result<Vault> {
        return if (shouldFail) {
            Result.failure(Exception("Mock load error"))
        } else {
            Result.success(vault)
        }
    }

    override suspend fun save(vault: Vault): Result<Unit> {
        return if (shouldFail) {
            Result.failure(Exception("Mock save error"))
        } else {
            this.vault = vault
            Result.success(Unit)
        }
    }
}
