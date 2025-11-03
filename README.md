# SecureShellV

SecureShellV is an Android-based VPN solution designed to provide secure, reliable, and configurable network tunneling through SSH.
It enables users to safely browse the internet—even on untrusted networks—while offering a polished UI, account management, app filtering, server switching, and multi-language support.

This project demonstrates experience in Android networking, VPNService, secure communications, UI/UX, and client–server integration.

## Features

✅ V2Ray tunneling (VLESS / VMess) </br>
✅ Automatic best server selection (lowest ping)</br>
✅ Fetches configs dynamically from backend</br>
✅ Stable & reliable connection under censorship</br>
✅ Account system with expiration & data tracking</br>
✅ Data usage monitoring (Used / Remaining / Total)</br>
✅ Subscription renewal & package management</br>
✅ App filtering (Include / Exclude)</br>
✅ Server list selection (e.g., Germany, Auto mode)</br>
✅ Multi-language support (English / فارسی)</br>
✅ Clean, modern UI</br>
✅ Persistent notification mode (optional)</br>

## Screenshots

Below are sample UI sections. You can insert image tags after uploading screenshots to your repo.

Login screen </br> ![Alt text](/screenshots/login.jpg?raw=true)

Home dashboard (Connected / Connecting / Disconnected) </br> 
<p float="left">
  <img src="/screenshots/home_disconnected.jpg" width="200" />
  <img src="/screenshots/home_connecting.jpg" width="200" /> 
  <img src="/screenshots/home_connected.jpg" width="200" />
</p>

Server selection page </br> ![Alt text](/screenshots/servers_tab.jpg?raw=true)

Account page (usage, expiration, user info) </br> ![Alt text](/screenshots/account_info_tab.jpg?raw=true)

Subscription selection </br> ![Alt text](/screenshots/account_renewal.jpg?raw=true)

Settings (language, app filter) </br> ![Alt text](/screenshots/settings_tab.jpg?raw=true)

App filtering UI </br> ![Alt text](/screenshots/app_filter.jpg?raw=true)

## Architecture Overview

SecureShellV consists of:

### Client (Android)

 - Fetches V2Ray configs from backend

 - Benchmarks available servers (ping test)

 - Auto-selects the lowest-latency server

 - Instantiates a V2Ray VPN tunnel via VpnService

 - Shows user info & usage stats

 - Optional app-based routing

### Backend

Provides:
- User authentication
- Server configurations (VLESS / VMess JSON)
- Subscription info (remaining traffic, expiration date)
- Package options

### Connection Flow

1. User signs in

2. App retrieves available V2Ray configurations

3. App pings each server

4. Best server is selected automatically

5. V2Ray tunnel is created via VpnService

6. Device traffic is routed securely

## Tech Stack
Category             |  Tools
:-------------------------:|:-------------------------:
Platform                   | Android
Language                   | Kotlin / Java
VPN                        | Android VpnService, V2ray Core
Networking                 | HTTPS Backend
Protocols                  | VMess, VLESS
Storage                    | SharedPreferences / Local cache
Architecture	             | MVVM
UI	                       | Google Material Design 3 Components

## Getting Started
### Prerequisites

- Android Studio
- Android 7.0+ (API 24+)
- SSH server account (for tunneling)

### Installation
```git clone https://github.com/sina-khosravi-0/SecureShellV.git```


1. Open the project in Android Studio
2. Set server configuration values (if applicable)
3.  Build & run on a physical device
4. Grant VPN permission when prompted

## Usage

1. **Sign in**
2. **Select server** (or Auto)
3. App pings all candidates & picks lowest latency
4. Tap **Connect**
5. Check real-time traffic usage & connection health
6. Optional:
    - App filter
    - Language switch
    - Renew subscription
## Security

- V2Ray protocol: encrypted + censorship resistant
- No traffic outside tunnel
- All configs delivered over HTTPS
- User-based authentication and rate limits
- Backend assists with secure config distribution

## Subscription System

Users can:

- View expiration date
- View data limits
- Renew subscription
- Select package type

Example package selection:
> 1 Month • 1 User • 80,000 Toman

## Multi-Language

Supported:

English, فارسی

User can switch languages in Settings.

## Future Improvements

- More tunneling modes (Quic, Reality, etc.)
- Faster reconnect with pre-cached configs
- Auto hot swapping configs

### 📚 What I Learned

- Implementing V2Ray protocol inside VpnService
- Dynamic config delivery + parsing
- Latency-based server selection
- Real-time usage + subscription state synchronization
- UI/UX for network-based utility apps
- App-level routing & filtering

Author: **Sina Khosravi** </br>
GitHub: https://github.com/sina-khosravi-0
