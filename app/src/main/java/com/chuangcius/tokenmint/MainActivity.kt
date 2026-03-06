package com.chuangcius.tokenmint

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.chuangcius.tokenmint.service.BiometricService
import com.chuangcius.tokenmint.ui.screens.AddTokenScreen
import com.chuangcius.tokenmint.ui.screens.LockScreen
import com.chuangcius.tokenmint.ui.screens.ScannerScreen
import com.chuangcius.tokenmint.ui.screens.SettingsScreen
import com.chuangcius.tokenmint.ui.screens.TokenListScreen
import com.chuangcius.tokenmint.ui.theme.TokenMintTheme
import com.chuangcius.tokenmint.ui.viewmodels.VaultViewModel

class MainActivity : FragmentActivity() {

    private companion object {
        const val CAMERA_PERMISSION_REQUEST_CODE = 1001
    }

    private val viewModel: VaultViewModel by viewModels { VaultViewModel.Factory }

    private var isLocked by mutableStateOf(true)

    private var _hasCameraPermission by mutableStateOf(false)
    private var isHandlingSystemPermissionDialog = false

    // File import picker — same reason
    private var importFileCallback: ((Uri?) -> Unit)? = null
    private val importFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        importFileCallback?.invoke(uri)
        importFileCallback = null
    }

    private fun launchImportFilePicker(callback: (Uri?) -> Unit) {
        importFileCallback = callback
        importFileLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Skip lock if biometric not enabled
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val biometricEnabled = prefs.getBoolean("biometric_enabled", true)
        if (!biometricEnabled || !BiometricService.canAuthenticate(this)) {
            isLocked = false
        }

        // Initialise camera permission state
        _hasCameraPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        setContent {
            TokenMintTheme {
                if (isLocked) {
                    LockScreen(
                        onAuthenticate = { promptBiometric() }
                    )
                } else {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "tokenList",
                        enterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Start,
                                tween(300)
                            ) + fadeIn(tween(300))
                        },
                        exitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Start,
                                tween(300)
                            ) + fadeOut(tween(300))
                        },
                        popEnterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.End,
                                tween(300)
                            ) + fadeIn(tween(300))
                        },
                        popExitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.End,
                                tween(300)
                            ) + fadeOut(tween(300))
                        }
                    ) {
                        composable("tokenList") {
                            TokenListScreen(
                                viewModel = viewModel,
                                onNavigateToAdd = { navController.navigate("addToken") },
                                onNavigateToScan = { navController.navigate("scanner") },
                                onNavigateToSettings = { navController.navigate("settings") }
                            )
                        }
                        composable("addToken") {
                            AddTokenScreen(
                                onSave = { token ->
                                    viewModel.addToken(token)
                                    navController.popBackStack()
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("scanner") {
                            ScannerScreen(
                                hasCameraPermission = _hasCameraPermission,
                                onRequestCameraPermission = {
                                    requestCameraPermissionCompat()
                                },
                                onTokenScanned = { token ->
                                    viewModel.addToken(token)
                                    navController.popBackStack()
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onLaunchImportFilePicker = { callback ->
                                    launchImportFilePicker(callback)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (isHandlingSystemPermissionDialog) return
        // Re-lock when app goes to background
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        if (prefs.getBoolean("biometric_enabled", true) &&
            BiometricService.canAuthenticate(this)
        ) {
            isLocked = true
        }
    }

    private fun promptBiometric() {
        BiometricService.authenticate(
            activity = this,
            title = getString(R.string.app_name),
            subtitle = getString(R.string.unlock_vault),
            onSuccess = { isLocked = false },
            onError = { /* stay locked */ }
        )
    }

    private fun requestCameraPermissionCompat() {
        isHandlingSystemPermissionDialog = true
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            isHandlingSystemPermissionDialog = false
            _hasCameraPermission = grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
        }
    }
}
