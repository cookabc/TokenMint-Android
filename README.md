# TokenMint (Android)

A clean and elegant TOTP authenticator for Android, ported from the iOS version. Provides a secure, touch-first two-factor authentication experience. Built with Jetpack Compose and Material 3.

## Features

- **AES-256-GCM Encryption** — tokens stored in encrypted vault with Android Keystore key management
- **Biometric Unlock** — Fingerprint / Face with device credential fallback
- **QR Code Scanning** — CameraX + ML Kit for instant account setup
- **Manual Entry** — add tokens with issuer, account, secret, and advanced TOTP settings
- **One-Tap Copy** — tap to copy current TOTP code with haptic feedback
- **Pin Favorites** — pin frequently used tokens to the top
- **Search** — instant filtering across all accounts
- **Import/Export** — JSON vault backup and restore (compatible with iOS version)
- **Settings** — biometric toggle, haptic toggle, theme selection
- **Localization** — English and Simplified Chinese

## Tech Stack

| Item | Detail |
|------|--------|
| Language | Kotlin 2.1.0 |
| UI | Jetpack Compose (BOM 2025.02.00) + Material 3 |
| Target | Android 8.0+ (API 26), compiled against API 35 |
| Persistence | AES-256-GCM encrypted file + Android Keystore |
| Architecture | MVVM — ViewModel + StateFlow + sealed state class |
| QR Scanner | CameraX 1.3.1 + ML Kit Barcode Scanning 17.2.0 |
| Biometric | AndroidX Biometric 1.1.0 |
| Serialization | kotlinx-serialization-json 1.7.3 |
| Navigation | Navigation Compose 2.8.5 |
| Build System | Gradle 8.13 + AGP 8.13.2 |

## Getting Started

```bash
# Clone and build
cd 042_TokenMint-Android
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Run unit tests
./gradlew testDebugUnitTest
```

## Project Structure

```
app/src/main/java/com/chuangcius/tokenmint/
├── TokenMintApp.kt                   # Application class
├── MainActivity.kt                   # Single Activity + NavHost
├── data/
│   ├── model/Models.kt               # Token, Vault, EncryptedVault
│   └── repository/VaultRepository.kt # AES-GCM encrypt/decrypt vault
├── service/
│   ├── BiometricService.kt           # BiometricPrompt wrapper
│   ├── KeystoreService.kt            # Android Keystore key management
│   └── TOTPService.kt                # RFC 6238 TOTP generation
├── ui/
│   ├── components/
│   │   ├── BackButton.kt
│   │   ├── CountdownRing.kt          # Canvas circular countdown
│   │   └── TokenRow.kt               # Token list item
│   ├── screens/
│   │   ├── AddTokenScreen.kt         # Manual token entry form
│   │   ├── LockScreen.kt             # Biometric lock screen
│   │   ├── ScannerScreen.kt          # CameraX + ML Kit QR scanner
│   │   ├── SettingsScreen.kt         # App settings
│   │   └── TokenListScreen.kt        # Main token list
│   ├── theme/
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   └── viewmodels/
│       └── VaultViewModel.kt         # Central state management
└── util/
    └── AppError.kt                   # Sealed error class

app/src/main/res/
├── values/strings.xml                # English strings
├── values-zh-rCN/strings.xml         # Simplified Chinese
├── values/colors.xml
└── values/themes.xml
```

## iOS Compatibility

The vault export format is JSON-compatible with the iOS version. You can export from iOS and import on Android (and vice versa) via the Settings screen.

## License

Same as the iOS version.
