# LockRec v0.8 beta
Install the root module ZIP in KernelSU, reboot, install the companion APK, grant root and notifications. Uninstall earlier APK first if signatures differ; keep the root module. Start recording in the app, then lock your phone. The existing native screenrecord engine is retained. Videos save in Movies/LockRec. AOD is device-dependent. Protected layers are not bypassed.

Select native or a lower short-edge resolution. FPS choices appear only if the native recorder advertises a manual FPS flag and display/encoder support is reported. Stock AOSP and the tested Samsung screenrecord do not expose such a flag; Auto is the only available rate there. Actual FPS is not guaranteed. Unlimited duration on supported native recorders; otherwise 180 seconds.

The floating toolbar is optional and uses the ordinary overlay permission. Hide removes it, not the recording notification. The toolbar may be included in video while visible. Pause saves the clip; resume makes a separate clip. Do not record credentials or other people's private information. Controller flicker mitigated by only updating views and notification when state changes. Runtime testing pending.
