# Record — LockRec Universal v0.2 beta

Experimental root-manager module for recording your own lock-screen animations. KernelSU is the first target; other root managers are untested. No LSPosed, boot service, secure-layer capture, hidden recording, networking, or firmware modifications.

Install the module ZIP through KernelSU, reboot if requested, then tap its Action button while unlocked. Confirm the LockRec notification, lock the phone, wake it or test AOD. Tap Action again while unlocked to stop, or wait for the 60-second limit. Output is Movies/LockRec. If the notification is unavailable, the module declines to start. The notification is a shell notification, not a foreground-service indicator; do not dismiss it while recording. Avoid recording credentials or other people's private information.

Default performance mode limits the short edge to 720 pixels and uses 10 Mbps. Native mode uses 18 Mbps. Optional root command: `sh /data/adb/modules/lockrec_universal/action.sh start native`. To stop: `sh /data/adb/modules/lockrec_universal/action.sh stop`. If stale session remains after a crash/reboot, reinstall module. Actual FPS and AOD depend on device/firmware. 60 FPS target, not guarantee. Set high refresh rate manually. Black frames with display off are expected. No frame interpolation. This is a script-based module, not APK.

Package with `zip LockRec-Universal-v0.2-beta.zip module.prop action.sh customize.sh uninstall.sh README.md skip_mount`.

## v0.2
Uses the shell UID for recording notifications, with root fallback. Recording remains blocked if neither confirms posting. Diagnostics are in notification.log. Upgrade over v0.1 with no recording active.
