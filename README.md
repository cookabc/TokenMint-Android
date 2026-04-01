# TokenMint (Android)

A clean and elegant TOTP authenticator for Android. Built with Jetpack Compose and Material 3.

## Features

- **AES-256-GCM Encryption** — tokens stored in encrypted vault with Android Keystore key management
- **Biometric Unlock** — Fingerprint / Face with device credential fallback
- **QR Code Scanning** — CameraX + ML Kit for instant account setup
- **Manual Entry** — add tokens with issuer, account, secret, and advanced TOTP settings
- **One-Tap Copy** — tap to copy current TOTP code with haptic feedback
- **Pin Favorites** — pin frequently used tokens to the top
- **Search** — instant filtering across all accounts
- **Import/Export** — JSON vault backup and restore
- **Settings** — biometric toggle, haptic toggle, theme selection
- **Localization** — English and Simplified Chinese

## Tech Stack

| Item          | Detail                                           |
| ------------- | ------------------------------------------------ |
| Language      | Kotlin                                           |
| UI            | Jetpack Compose + Material 3                     |
| Architecture  | MVVM — ViewModel + StateFlow, Repository pattern |
| Navigation    | Navigation Compose                               |
| Security      | Android Keystore + AES-256-GCM encryption        |
| Biometric     | AndroidX Biometric API                           |
| Camera        | CameraX + ML Kit barcode scanning                |
| Serialization | Kotlinx Serialization                            |
| State         | StateFlow + ViewModel                            |
| Testing       | JUnit, Kotlin Coroutines Test, AndroidX Test     |

## Getting Started

```bash
# Clone and open in Android Studio
# Wait for Gradle sync to complete
# Run on emulator or device (API 26+)
```

### Prerequisites

- Android Studio Ladybug or newer
- JDK 17+
- Android SDK API 26+

## Project Structure

```
app/src/main/java/com/chuangcius/tokenmint/
├── MainActivity.kt               # Entry point with navigation
├── TokenMintApp.kt               # Application class
├── data/
│   ├── model/                    # Token, Vault data models
│   └── repository/
│       └── VaultRepository.kt    # Encrypted vault data access
├── service/
│   ├── BiometricService.kt       # Biometric authentication
│   ├── KeystoreService.kt        # Key management
│   └── TOTPService.kt            # TOTP code generation
├── ui/
│   ├── components/               # Reusable UI components
│   ├── screens/                  # Lock, TokenList, AddToken, Scanner, Settings
│   ├── theme/                    # Material theme configuration
│   └── viewmodels/               # MVVM ViewModels
└── util/                         # Utility classes
```

## Security

- Vault encrypted with AES-256-GCM via Android Keystore
- Biometric authentication required on app launch
- Auto-lock vault when app goes to background

## License

Private
