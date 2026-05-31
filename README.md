# Trace

Android Traffic Interceptor. Inspect HTTP/HTTPS traffic, mock APIs, and debug network requests. All on device, zero configuration.

## Features

- 📡 Intercept HTTP & HTTPS traffic from any app
- 🔓 TLS MITM with dynamic certificate generation
- 🎭 Mock API responses with custom status codes and delays
- 🚫 Block URLs/domains with wildcard patterns
- 📊 Real-time traffic inspection with search
- 💾 Export CA certificate for HTTPS decryption

## Tech Stack

- **Language:** Kotlin 2.0
- **UI:** Jetpack Compose, Material 3
- **Architecture:** Clean Architecture + MVVM + StateFlow
- **DI:** Hilt
- **Database:** Room + FTS4
- **Networking:** Ktor Server + OkHttp, BouncyCastle (TLS)

## Setup

1. Clone the repository
2. Open in Android Studio
3. Build and run on device/emulator
4. Grant VPN permission when prompted

## License

MIT
