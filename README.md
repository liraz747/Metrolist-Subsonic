<div align="center">
<img src="https://github.com/mostafaalagamy/Metrolist/blob/main/fastlane/metadata/android/en-US/images/icon.png" width="160" height="160" style="display: block; margin: 0 auto"/>
<h1>Metrolist-Subsonic</h1>
<p>Subsonic client for Android</p>
<p><em>A fork of <a href="https://github.com/mostafaalagamy/Metrolist">Metrolist</a> with Subsonic API integration</em></p>

[![Latest release](https://img.shields.io/github/v/release/liraz747/Metrolist-Subsonic?style=for-the-badge)](https://github.com/liraz747/Metrolist-Subsonic/releases)
[![GitHub license](https://img.shields.io/github/license/liraz747/Metrolist-Subsonic?style=for-the-badge)](https://github.com/liraz747/Metrolist-Subsonic/blob/main/LICENSE)
[![GitHub forks](https://img.shields.io/github/forks/liraz747/Metrolist-Subsonic?style=for-the-badge)](https://github.com/liraz747/Metrolist-Subsonic/network)
[![GitHub stars](https://img.shields.io/github/stars/liraz747/Metrolist-Subsonic?style=for-the-badge)](https://github.com/liraz747/Metrolist-Subsonic/stargazers)
</div>

<div style="padding: 16px; margin: 16px 0; background-color: #E3F2FD; border-left: 6px solid #2196F3; border-radius: 4px;">
<h2 style="margin: 0;"><strong>About This Fork</strong></h2>
This is a fork of <a href="https://github.com/mostafaalagamy/Metrolist">Metrolist</a> that replaces YouTube Music API integration with <strong>Subsonic API</strong> support. Connect to your own Subsonic-compatible music server (such as Airsonic, Navidrome, or Subsonic) to stream your personal music library.
</div>

<h1>Screenshots</h1>

<img src="https://github.com/mostafaalagamy/Metrolist/blob/main/fastlane/metadata/android/en-US/images/screenshots/screenshot_1.png" width="30%" />
<img src="https://github.com/mostafaalagamy/Metrolist/blob/main/fastlane/metadata/android/en-US/images/screenshots/screenshot_2.png" width="30%" />
<img src="https://github.com/mostafaalagamy/Metrolist/blob/main/fastlane/metadata/android/en-US/images/screenshots/screenshot_3.png" width="30%" />

<img src="https://github.com/mostafaalagamy/Metrolist/blob/main/fastlane/metadata/android/en-US/images/screenshots/screenshot_4.png" width="30%" />
<img src="https://github.com/mostafaalagamy/Metrolist/blob/main/fastlane/metadata/android/en-US/images/screenshots/screenshot_5.png" width="30%" />
<img src="https://github.com/mostafaalagamy/Metrolist/blob/main/fastlane/metadata/android/en-US/images/screenshots/screenshot_6.png" width="30%" />

<div align="center">
<h1>Features</h1>
</div>

- **Play any song from your Subsonic server** - Stream your entire music library
- **Background playback** - Keep listening while using other apps
- **Personalized quick picks** - Discover music based on your listening habits
- **Library management** - Organize and browse your music collection
- **Download and cache songs** - Offline playback support
- **Search functionality** - Find songs, albums, and artists quickly
- **Live lyrics** - View synchronized lyrics while playing
- **Subsonic account login** - Secure authentication with your server
- **Library syncing** - Sync songs, artists, and albums from your Subsonic server
- **Skip silence** - Automatically skip silent parts in tracks
- **Audio normalization** - Consistent volume levels
- **Adjust tempo/pitch** - Control playback speed and pitch
- **Local playlist management** - Create and manage playlists
- **Reorder songs** - Customize playlist and queue order
- **Multiple themes** - Light, Dark, Black, and Dynamic theme support
- **Sleep timer** - Auto-stop playback after a set time
- **Material 3 design** - Modern, beautiful UI
- And much more!

<div align="center">
<h1>Download</h1>

Check the [Releases](https://github.com/liraz747/Metrolist-Subsonic/releases) page for the latest APK.

</div>

<div align="center">
<h1>Setup & Configuration</h1>
</div>

### Getting Started

1. **Install the app** from the [Releases](https://github.com/liraz747/Metrolist-Subsonic/releases) page
2. **Launch the app** - You'll be prompted to log in to your Subsonic server
3. **Enter your Subsonic server details:**
   - **Server URL**: Your Subsonic server address (e.g., `https://music.example.com` or `http://192.168.1.100:4040`)
   - **Username**: Your Subsonic username
   - **Password**: Your Subsonic password
4. **Start streaming** your music!

### Supported Subsonic Servers

This app is compatible with any Subsonic-compatible server, including:
- [Subsonic](http://www.subsonic.org/)
- [Airsonic](https://airsonic.github.io/)
- [Navidrome](https://www.navidrome.org/)
- [Funkwhale](https://funkwhale.audio/)
- And other Subsonic API-compatible servers

<div align="center">
<h1>FAQ</h1>
</div>

### Q: Why Metrolist-Subsonic isn't showing in Android Auto?

1. Go to Android Auto's settings and tap multiple times on the version in the bottom to enable developer settings
2. In the three dots menu at the top-right of the screen, click "Developer settings"
3. Enable "Unknown sources"

### Q: How to scrobble music to LastFM, LibreFM, ListenBrainz or GNU FM?

Use other music scrobbler apps, I recommend [Pano Scrobbler](https://play.google.com/store/apps/details?id=com.arn.scrobble).

### Q: What Subsonic server version do I need?

This app is compatible with Subsonic API version 1.16.0 and later. Most modern Subsonic-compatible servers should work.

### Q: Can I use this with YouTube Music?

No, this fork only supports Subsonic-compatible servers. For YouTube Music support, use the original [Metrolist](https://github.com/mostafaalagamy/Metrolist) app.

<div align="center">
<h1>Development Setup</h1>
</div>

### Prerequisites

- Android Studio (latest version recommended)
- JDK 21 or later
- Android SDK

### Building from Source

1. Clone the repository:
   ```bash
   git clone https://github.com/liraz747/Metrolist-Subsonic.git
   cd Metrolist-Subsonic
   ```

2. Open the project in Android Studio

3. Build the project:
   ```bash
   ./gradlew assembleArm64Release
   ```

4. The APK will be located at:
   ```
   app/build/outputs/apk/arm64/release/app-arm64-release-unsigned.apk
   ```

### GitHub Secrets Configuration

This project uses GitHub Secrets to securely store API keys for building releases. To set up the secrets:

1. Go to your GitHub repository settings
2. Navigate to **Settings** → **Secrets and variables** → **Actions**
3. Add the following repository secrets:
   - `LASTFM_API_KEY`: Your LastFM API key
   - `LASTFM_SECRET`: Your LastFM secret key

4. Get your LastFM API credentials from: https://www.last.fm/api/account/create

**Note:** These secrets are automatically injected into the build process via GitHub Actions and are not visible in the source code.

<div align="center">
<h1>Testing</h1>
</div>

To test the Subsonic functionality:

1. Build and install the app on your device
2. On first launch, you'll be prompted to enter your Subsonic server credentials
3. Enter your server URL, username, and password
4. The app will authenticate and connect to your Subsonic server
5. You can then browse and play music from your server

For development testing, you can use a local Subsonic server or a test server instance.

<div align="center">
<h1>Contributing</h1>
</div>

Contributions are welcome! Please feel free to submit a Pull Request. For major changes, please open an issue first to discuss what you would like to change.

<div align="center">
<h1>Special Thanks</h1>
</div>

**Original Metrolist**
- [Mo Agamy](https://github.com/mostafaalagamy) - Original Metrolist creator

**InnerTune**
- [Zion Huang](https://github.com/z-huang) • [Malopieds](https://github.com/Malopieds)

**OuterTune**
- [Davide Garberi](https://github.com/DD3Boh) • [Michael Zh](https://github.com/mikooomich)

<sub>Thank you to all the amazing developers who made this project possible!</sub>

<div align="center">
<h1>License</h1>
</div>

This project is licensed under the GPL-3.0 License - see the [LICENSE](LICENSE) file for details.

<div align="center">
<h1>Disclaimer</h1>
</div>

This project and its contents are not affiliated with, funded, authorized, endorsed by, or in any way associated with:
- YouTube, Google LLC
- Metrolist Group LLC
- Subsonic.org
- Any of their affiliates and subsidiaries

Any trademark, service mark, trade name, or other intellectual property rights used in this project are owned by the respective owners.

**Forked and modified with ❤️ by [liraz747](https://github.com/liraz747)**

**Original project made with ❤️ by [Mo Agamy](https://github.com/mostafaalagamy)**
