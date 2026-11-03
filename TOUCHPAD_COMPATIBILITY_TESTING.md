# Physical keyboard touchpad compatibility — experimental branch

Based on Moonlight V+ 12.14.4 and the unmerged compatibility implementation in upstream PR #359.
This is an isolated testing branch, not an official Moonlight V+ release.

## Test configuration
- In Settings > Input > Mouse: turn **Tablet touchpad support** OFF when using compatibility mode.
- Choose the Android tablet keyboard's physical trackpad in the **Touchpad compatibility devices** selector.
- Enable the compatibility trackpad and verify movement, tap-to-click, two-finger scroll, two-finger right click, and press-and-drag.
- The compatible trackpad reports standard mouse packets to Apollo, Sunshine, or Foundation Sunshine.
- Verify that a separately attached mouse and touchscreen still work and clicks are not doubled.

## Validation
The GitHub Actions artifact is a debug-signed side-by-side APK, not a signed production release.
Success in CI proves compilation/tests, not operation on a specific Android 13/MIUI touchpad.
Do not merge to master until tested on hardware and any CI failures are resolved.
