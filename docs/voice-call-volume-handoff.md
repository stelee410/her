# Voice Call Volume Handoff

## Context

The first voice playback after app launch is still too loud. Lowering PCM/TTS
gain in the app did not solve it, because the audible output is routed through
Android's call/communication volume path once Bluetooth HFP/SCO or
`USAGE_VOICE_COMMUNICATION` becomes active.

The user observed that the volume stays loud until they manually adjust the
call volume. After that manual adjustment, playback follows the expected call
volume.

## Goal

Respect the system's current call volume for the active communication route
instead of forcing a fixed app-side gain. The app should avoid the first-use
"default loud call volume" behavior by synchronizing Android's call volume
after the voice route is established.

Do not start by adding another PCM gain layer. The next attempt should operate
on `AudioManager.STREAM_VOICE_CALL` at the right lifecycle moment.

## Current Relevant Code

- `MainActivity.java`
  - Creates `PcmPlayer` for realtime assistant audio.
  - Creates `GatewayTtsPlayer` for gateway TTS, including initialization TTS and tool TTS.
  - Selects voice audio routing through `selectVoiceAudioAdapter()`.
  - Calls `ensureOutputAudioAdapter()` before realtime output playback.
  - Calls `startMicCapture(...)` before voice input.

- `VoiceAudioRouteManager.java`
  - Owns Bluetooth HFP/SCO / communication route setup.
  - This is likely the best place to know when the communication route is active.

- `BluetoothHfpVoiceAudioAdapter.java`
  - Bridges voice input/output lifecycle to the Bluetooth HFP route.

- `PcmPlayer.java`
  - Writes realtime PCM audio to `AudioTrack`.

- `GatewayTtsPlayer.java`
  - Streams gateway TTS PCM or falls back to file playback.

## Proposed Approach

Add a small route-volume synchronizer that runs only after the communication
route is active:

1. When entering voice input/output using Bluetooth HFP/SCO or voice
   communication playback, wait until `AudioManager.MODE_IN_COMMUNICATION` and
   the communication device/SCO route have been requested.
2. Read `AudioManager.STREAM_VOICE_CALL`.
3. Re-apply that same value with `setStreamVolume(...)`.
4. Post one or two delayed re-applications, for example after 250 ms and
   700 ms, because Android may overwrite the stream volume during route
   transition.
5. Do not change media volume.
6. Do not force a hardcoded low volume unless the same-loud-first-use bug still
   happens after re-applying the current call volume.

Example implementation shape:

```java
AudioManager audio = getSystemService(AudioManager.class);
int current = audio.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
audio.setStreamVolume(AudioManager.STREAM_VOICE_CALL, current, 0);
main.postDelayed(() -> {
    int again = audio.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
    audio.setStreamVolume(AudioManager.STREAM_VOICE_CALL, again, 0);
}, 250);
```

If this simply re-applies the already-too-loud default value on first use, then
the next fallback is to store a user preference inside the app:

- On every voice session start, apply the last known preferred call volume.
- If the user adjusts the call volume while the app is active, capture and save
  the new `STREAM_VOICE_CALL` value.

## Android Permissions

Check that `AndroidManifest.xml` has:

```xml
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
```

If it is absent, add it.

## Verification Plan

Use the phone flavor, not tablet:

```sh
gradle :app:testPhoneDebugUnitTest
gradle :app:installPhoneDebug
```

Before installing, confirm phone APK does not include digital-avatar assets:

```sh
unzip -l app/build/outputs/apk/phone/debug/app-phone-debug.apk | rg "assets/|\\.mp4|\\.webm" || true
```

Device verification:

1. Install `phoneDebug`.
2. Reboot or force-stop the app to simulate first launch.
3. Connect the Bluetooth headset.
4. Enter voice mode and let the first assistant audio play.
5. Confirm volume uses the current call volume without needing a manual volume
   key press.
6. Check logs for route timing:

```sh
adb -s ROTATE01013093 logcat -d -v time | rg -i "HerRealtime|AudioManager|voice call|bt_sco|communication|AudioTrack|gateway tts"
```

## Notes For Next Agent

- The repo has no `./gradlew`; use system `gradle`.
- The connected device in recent runs was `ROTATE01013093`.
- Keep using `phoneDebug`; the user explicitly said this is a phone and does
  not need digital-avatar assets.
- There is a dirty working tree with prior voice routing and UI changes. Do not
  revert unrelated files.
