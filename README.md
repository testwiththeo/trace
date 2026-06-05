<div align="center">
  <h1 align="center">Trace</h1>
  <p align="center">
    <strong>Android Traffic Interceptor for Developers</strong>
    <br />
    Intercept. Inspect. Mock. Debug APIs.
    <br />
    All on device. Zero desktop setup.
  </p>

  <p align="center">
    <img src="https://img.shields.io/badge/Kotlin-2.0-blue?logo=kotlin" alt="Kotlin" />
    <img src="https://img.shields.io/badge/Compose-BOM-4285F4?logo=jetpackcompose" alt="Compose" />
    <img src="https://img.shields.io/badge/API-26%2B-brightgreen" alt="API 26+" />
    <img src="https://img.shields.io/badge/License-MIT-yellow" alt="MIT" />
    <img src="https://img.shields.io/badge/build-passing-brightgreen" alt="Build" />
  </p>
</div>

Trace is an on-device network traffic interceptor for Android. Capture HTTP/HTTPS requests from any app, mock API responses, block URLs, inject delays, and inspect full request/response details. No USB cable, no desktop proxy, no Wi-Fi configuration.

## The Problem

Testing API integrations on Android means juggling multiple tools. Charles Proxy needs USB tethering or Wi-Fi setup. Postman can't intercept traffic from other apps. Mock servers require code changes and rebuilding. You lose time switching contexts between the app, proxy logs, and mock configurations.

Worse, reproducing edge cases is painful. Simulating slow networks, error responses, or blocked endpoints means editing backend code or complex proxy rules. Testing offline scenarios requires airplane mode. None of these workflows are fast.

## What Trace Does

Trace runs directly on your Android device as a VPN-based proxy. No computer required.

Tap START to begin capturing. All HTTP/HTTPS traffic from every app routes through Trace automatically. Inspect requests in real-time, create mock rules to return custom responses, block specific URLs, or inject artificial delays to test loading states.

**Debug APIs without leaving your device.**

## Features

Trace is built around the workflow developers repeat when debugging network issues.

| Area | Capability |
|------|------------|
| Capture | VPN-based interception, local HTTP/HTTPS proxy, real-time traffic logging, full request/response bodies, status codes, headers, and duration. |
| Inspection | Traffic detail viewer, search with debounce, color-coded HTTP methods, expandable headers and bodies, JSON pretty-print. |
| Mocking | URL pattern matching with wildcards, custom response status codes and bodies, per-rule delays, priority-ordered rules. |
| Blocking | URL and domain blocking with wildcard patterns, instant 502 response, blocklist management. |
| Delays | Artificial latency injection for testing loading states and timeouts, per-rule configuration, 0-10 second range. |
| TLS | HTTPS interception with dynamic certificate generation, BouncyCastle CA, per-hostname leaf certificates, TLS MITM. |
| Real-time | Live traffic count, instant UI updates, capture status indicator, notification when active. |
| Privacy | Local-first storage. No account, no backend, no cloud sync. All data on device. |

| Pain Point | How Trace Helps |
|------------|-----------------|
| USB cables and Wi-Fi proxy setup | Runs entirely on device via VPN. No external tools. |
| Can't mock without code changes | Mock rules intercept traffic before it leaves the device. |
| Hard to simulate slow networks | Delay injector adds configurable latency per URL pattern. |
| Blocking competitors or tracking URLs | BlocklistFilter stops requests instantly with wildcard support. |
| Testing HTTPS APIs | Dynamic cert generation intercepts encrypted traffic transparently. |
| Debugging takes too long | Real-time traffic inspection shows exactly what's being sent and received. |

## Quick Start

```bash
git clone https://github.com/testwiththeo/trace.git
cd trace
./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Open the app, tap START, grant VPN permission, and start browsing. Traffic appears in the log instantly.

## Architecture

Trace uses Clean Architecture with three layers: presentation, domain, and data. The VPN service and proxy run in the data/infrastructure layer, engines (mock, block, delay) in the domain layer, and Compose screens in the presentation layer.

```text
presentation
  -> ViewModels with StateFlow
  -> Compose screens
  -> Navigation

domain
  -> Models (CapturedTraffic, MockRule)
  -> Repository interfaces
  -> Engine logic (Mock, Block, Delay, Capture)

data
  -> Room database with FTS4
  -> Repository implementations
  -> VPN Service + Proxy Server
  -> TLS Interceptor (BouncyCastle)

proxy
  -> TraceVpnService (VPN tunnel)
  -> Tunneller (packet reader)
  -> LocalProxyServer (HTTP/HTTPS proxy)
  -> TlsInterceptor (dynamic certs)

engine
  -> TrafficCapturer (HTTP parsing)
  -> MockEngine (pattern matching + custom responses)
  -> BlocklistFilter (URL/domain blocking)
  -> DelayInjector (artificial latency)
```

## Project Structure

```text
app/src/main/java/com/trace/app/
|-- TraceApplication.kt
|-- data/
|   |-- db/
|   |-- repository/
|-- di/
|-- domain/
|   |-- model/
|   |-- repository/
|-- engine/
|   |-- TrafficCapturer.kt
|   |-- MockEngine.kt
|   |-- BlocklistFilter.kt
|   |-- DelayInjector.kt
|-- presentation/
|   |-- navigation/
|   |-- screen/
|   |   |-- home/
|   |   |-- trafficlog/
|   |   |-- trafficdetail/
|   |   |-- mockrules/
|   |   |-- settings/
|   `-- theme/
`-- proxy/
    |-- TraceVpnService.kt
    |-- Tunneller.kt
    |-- LocalProxyServer.kt
    |-- TlsInterceptor.kt
    |-- TlsUtils.kt
```

## Product Flow

1. Open Trace and tap START.
2. Grant VPN permission when prompted.
3. Browse any app or website.
4. Return to Trace to inspect captured traffic.
5. Tap any request to see full details.
6. Create mock rules to override specific endpoints.
7. Block unwanted URLs or domains.
8. Stop capture when done.

## Screens

| Screen | What it does |
|--------|-------------|
| Home | Start/stop capture, view traffic count, quick navigation to other screens. |
| Traffic Log | Real-time list of captured requests with search, color-coded methods, and status codes. |
| Traffic Detail | Full request/response viewer with headers, bodies, status, duration, and timing. |
| Mock Rules | Create, edit, enable/disable, and delete URL pattern matching rules. |
| Settings | CA certificate management, export, regenerate, clear data, and app info. |

## Tech Stack

| Layer | Tools |
|-------|-------|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose, Material 3, Navigation Compose |
| Architecture | Clean Architecture, MVVM, StateFlow |
| Dependency Injection | Hilt |
| Persistence | Room, FTS4, DataStore Preferences |
| Proxy | Raw ServerSocket, Ktor Client + OkHttp |
| TLS | Bouncy Castle (CA cert generation, dynamic leaf certs) |
| Networking | Ktor Client (OkHttp engine) for request forwarding |
| Testing | JUnit 5, MockK, Kotest, Robolectric |
| Build | Gradle, KSP |

## Requirements

- Android Studio with Kotlin 2.0 support
- JDK 17
- Android SDK 34
- Android device or emulator running API 26+

## Development

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
```

Install the debug APK:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## How It Works

```text
App sends HTTP request
    |
    v
Android VPN Service routes traffic
    |
    v
TUN interface captures packets
    |
    v
LocalProxyServer accepts connection
    |
    +-- Check BlocklistFilter -> 502 if blocked
    |
    +-- Check MockEngine -> Return mock if matched
    |
    +-- Forward to real server via OkHttp
    |
    +-- Apply DelayInjector
    |
    +-- Save to Room database
    |
    v
Response returned to app
```

For HTTPS traffic:

```text
App sends CONNECT hostname:443
    |
    v
Proxy responds 200 Connection Established
    |
    v
TLS handshake with client (our fake cert)
    |
    v
TLS handshake with real server (real cert)
    |
    v
Decrypted traffic relayed through proxy
    |
    v
Full inspection of HTTP inside TLS
```

## Testing

Run all tests:

```bash
./gradlew testDebugUnitTest
```

The test suite covers:

- MockEngine URL pattern matching and response generation
- BlocklistFilter exact and wildcard domain matching
- DelayInjector configuration and clamping
- TrafficCapturer HTTP parsing and body truncation

## Permissions

Trace uses Android permissions for traffic interception:

| Permission | Why it is needed |
|------------|------------------|
| INTERNET | Forward requests to real servers and receive responses. |
| ACCESS_NETWORK_STATE | Monitor network availability. |
| FOREGROUND_SERVICE | Keep VPN service alive while capturing. |
| POST_NOTIFICATIONS | Show active capture notification on Android 13+. |
| VPN_SERVICE | System dialog to grant VPN tunnel permission. |

HTTPS interception requires installing the CA certificate in Android Settings > Security > Encryption & credentials > Install a certificate > CA certificate.

## Privacy

Trace is local-first by design.

- All traffic data is stored in the Room database on device.
- No data is sent to external servers.
- There is no sign-in, backend service, telemetry, or analytics.
- The CA certificate is generated on device and never leaves it.
- Uninstalling the app removes all captured data.

## Roadmap

- [x] VPN-based HTTP/HTTPS traffic interception
- [x] TLS MITM with dynamic certificate generation
- [x] Mock engine with URL pattern matching
- [x] Blocklist filter with wildcard support
- [x] Delay injection for latency simulation
- [x] Real-time traffic log with search
- [x] Request/response detail viewer
- [x] CA certificate export and management
- [x] Unit tests for engine components
- [ ] Export requests as cURL commands
- [ ] Export sessions as HAR files
- [ ] Replay captured requests
- [ ] App package detection (which app made the request)
- [ ] Session management
- [ ] CI/CD with GitHub Actions
- [ ] Pull-to-refresh on traffic log
- [ ] Filter chips for method and status code

## Contributing

Contributions are welcome. Keep changes focused and open a pull request.

```bash
git checkout -b feat/your-feature
```

Before committing:

```bash
./gradlew testDebugUnitTest
```

Use Conventional Commits:

```bash
git commit -m "feat: add export template preview"
git push origin feat/your-feature
```

## License

Trace is released under the MIT License.

---

<div align="center">
  <strong>Trace</strong>
  <br />
  Built for developers who debug APIs without leaving their device.
</div>
