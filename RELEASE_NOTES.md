# Release v0.1 - Initial Release

## Overview

This is the initial release of Metrolist-Subsonic, a fork of Metrolist with Subsonic API integration.

## Features

- Stream music from your Subsonic-compatible server (Subsonic, Airsonic, Navidrome, Funkwhale, etc.)
- Background playback with Media3 support
- Library syncing and management
- Offline playback with download/cache support
- Live lyrics support
- Local playlist management
- Material 3 design with multiple themes (Light, Dark, Black, Dynamic)
- Sleep timer, skip silence, audio normalization, tempo/pitch adjustment
- Android Auto support

## Testing Instructions

### Test Locally

1. Clone the repository:
   ```bash
   git clone https://github.com/liraz747/Metrolist-Subsonic.git
   cd Metrolist-Subsonic
   ```

2. Build the APK:
   ```bash
   ./gradlew assembleUniversalRelease
   ```

3. The unsigned APK will be available at:
   ```
   app/build/outputs/apk/universal/release/app-universal-release-unsigned.apk
   ```

4. Install on your device:
   ```bash
   adb install app/build/outputs/apk/universal/release/app-universal-release-unsigned.apk
   ```

### Test via GitHub Actions

1. Push a tag starting with `v` (e.g., `v0.1`) to trigger the release workflow:
   ```bash
   git tag -a v0.1 -m "Release v0.1"
   git push origin v0.1
   ```

2. Alternatively, manually trigger the workflow via GitHub Actions UI:
   - Go to Actions → Release Workflow
   - Click "Run workflow"
   - Select the branch
   - Click "Run workflow"

3. The workflow will:
   - Build release APKs for all architectures (arm64, armeabi, x86, x86_64, universal)
   - Sign the APKs (if signing credentials are configured)
   - Create a GitHub Release
   - Upload the APKs as release assets

## Known Limitations

- Release signing requires `KEYSTORE`, `KEY_ALIAS`, `KEYSTORE_PASSWORD`, and `KEY_PASSWORD` secrets to be configured in GitHub
- Without signing secrets, unsigned APKs will be produced
- LastFM integration requires `LASTFM_API_KEY` and `LASTFM_SECRET` secrets

## Subsonic Server Compatibility

This app is compatible with Subsonic API version 1.16.0 and later. Tested with:
- Subsonic
- Airsonic
- Navidrome

## Feedback and Issues

Please report issues or provide feedback at: https://github.com/liraz747/Metrolist-Subsonic/issues
