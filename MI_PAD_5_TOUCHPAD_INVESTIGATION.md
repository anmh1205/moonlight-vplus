# Mi Pad 5 physical keyboard touchpad — root-cause investigation

## Purpose
This is a *field diagnostic*, not a verified fix for MIUI 14 / Android 13.
Original baseline V+ 12.14.4, plus experimental PR #359 compatibility logic from the separate
development branch. The APK does not rely on Foundation Sunshine extensions.

## What has been verified
- The 12.14.4 compatibility-port APK compiles, but the user has confirmed
  tap-to-click/two-finger scroll still fail on Xiaomi Mi Pad 5.
- In the earlier release the trace code was incorrectly gated on BuildConfig.DEBUG.
  This field diagnostic enables it explicitly in signed Release variants.
- The selected-device compatibility route is not universal: when input device descriptors
  or event sources differ from expectations, no compatibility handler may run.

## Field procedure (no root / no ADB)
1. Install the isolated APK (package com.limelight.qiin.touchpadprobe). It is a different
   application from V+ and the earlier trial. Pair your Apollo/Sunshine host.
2. Under Settings > Input > Mouse, turn OFF "Tablet touchpad support".
   Open "Touchpad compatibility devices" and select the physical keyboard touchpad
   (try entries associated with the keyboard, if multiple); reconnect the stream.
3. In the stream: move the cursor, tap once, tap twice, click the physical button,
   scroll with two fingers, and tap with two fingers.
4. Return to Settings > Input > Mouse and tap "Copy physical touchpad input trace".
5. Paste the report in the issue/chat without changing entries. The trace omits
   typed keys and raw pointer coordinates; it includes input-device names and a
   short history of action classification and emitted mouse packet names.

## Interpretation
- No event for the physical input but other motion events present: loss before Activity,
  or pointer capture intercepts events.
- Input events show selected=false: compatibility routing has not activated.
- Input events and selected=true, but no OUT mouse action: gesture classification/dispatch.
- OUT Click/Scroll but host ignores action: downstream packet or host path.
- STATE pointerInput=false: capture state is suppressing delivery.
- All tap/scroll events emitted but application ignores: verify host input focus.

## Completion criteria
Do NOT call this fixed until Mi Pad 5 validates tap, double tap, two-finger tap/scroll,
physical press, drag, and regression of external mouse/onscreen touchscreen. Passing CI
alone only establishes build and test validity.

## References
- Artemis: https://github.com/ClassicOldSong/moonlight-android
- Upstream compatibility PR: https://github.com/qiin2333/moonlight-vplus/pull/359
- Xiaomi pointer-mode regression: https://github.com/qiin2333/moonlight-vplus/issues/384

## Compare uncaptured vs streamed physical touchpad input (v2)
The Settings > Input > Mouse page has **Test physical touchpad input (without streaming)**.
This screen intentionally does not request pointer capture.
1. Open it, press Clear event trace, test keyboard touchpad movement, tap, physical
   click, two-finger scroll, two-finger tap. Press Copy Android touchpad event trace
   and retain the text.
2. Connect to the host, repeat these actions on the streamed desktop.
3. Disconnect, return to Settings > Input > Mouse > Copy physical touchpad input trace.

Compare device ID, descriptor hash, source, tool, classification, action, selected,
STATE pointerInput and OUT send attempts. If the Android event exists only outside the
stream, focus/pointer capture or local UI dispatch is the first divergent boundary.
If it reaches the stream but has selected=false, device matching/activation is faulty.
If it is selected and no OUT event follows, classification and gesture mapping are faulty.

The diagnostic does NOT record keyboard keystrokes or raw touch coordinates.
It does not prove that an attempted send was received by the host.
