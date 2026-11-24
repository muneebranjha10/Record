#!/system/bin/sh
# Explicitly started, user-controlled recordings only. Never request secure layers.
MODDIR=${0%/*}
[ -d "$MODDIR" ] || exit 1
STATE="$MODDIR/session"
OUTDIR=/sdcard/Movies/LockRec
umask 077
notify() {
  case "$1" in
    'Recording active. Use LockRec controls to stop.'|'Recording ended. Check Movies/LockRec.'|'Recording failed. Check module last.log.') ;;
    *) return 1;;
  esac
  result=$(su 2000 -c "/system/bin/cmd notification post -v -t 'LockRec Universal' lockrec '$1'" 2>&1)
  if printf '%s\n' "$result" | grep -Eq '^[[:space:]]*posted:[[:space:]]*$'; then
    printf '%s\n' 'Notification posted via shell UID.' > "$MODDIR/notification.log"
    return 0
  fi
  printf '%s\n' "$result" > "$MODDIR/notification.log"
  result=$(/system/bin/cmd notification post -v -t 'LockRec Universal' lockrec "$1" 2>&1)
  if printf '%s\n' "$result" | grep -Eq '^[[:space:]]*posted:[[:space:]]*$'; then
    printf '%s\n' 'Notification posted via root.' >> "$MODDIR/notification.log"
    return 0
  fi
  printf '%s\n' "$result" >> "$MODDIR/notification.log"
  return 1
}
active() {
  [ -r "$STATE/pid" ] && [ -r "$STATE/output" ] || return 1
  pid=$(cat "$STATE/pid"); file=$(cat "$STATE/output")
  case "$pid" in ''|*[!0-9]*) return 1;; esac
  case "$file" in "$OUTDIR"/LockRec_*.mp4) ;; *) return 1;; esac
  [ -r "/proc/$pid/cmdline" ] || return 1
  tr '\000' '\n' < "/proc/$pid/cmdline" | grep -Fxq /system/bin/screenrecord &&
  tr '\000' '\n' < "/proc/$pid/cmdline" | grep -Fxq "$file"
}
stop() {
  if active; then
    kill -INT "$pid" || return 1
    echo 'Stopping; wait for MP4 finalization.'
  else
    echo 'No active recording.'
  fi
}
worker() {
  profile=$1; file=$2
  if [ "$profile" = performance ]; then
    size=$(/system/bin/wm size | tail -n 1 | awk '{print $NF}')
    w=${size%x*}; h=${size#*x}
    case "$w:$h" in *[!0-9:]*|:*|*:) w=0; h=0;; esac
    if [ "$w" -gt 0 ] && [ "$h" -gt 0 ]; then
      if [ "$w" -le "$h" ]; then
        nw=$((w<720?w/2*2:720)); nh=$((h*nw/w/2*2))
      else
        nh=$((h<720?h/2*2:720)); nw=$((w*nh/h/2*2))
      fi
      set -- --size "${nw}x${nh}" --bit-rate 10000000
    else
      set -- --bit-rate 10000000
    fi
  else
    set -- --bit-rate 20000000
  fi
  # Use zero only when the device explicitly advertises unlimited duration.
  limit=180
  if /system/bin/screenrecord --help 2>&1 | grep -qi 'remove the time limit'; then
    limit=0
  fi
  /system/bin/screenrecord --verbose --time-limit "$limit" "$@" "$file" > "$MODDIR/last.log" 2>&1 &
  child=$!
  printf '%s\n' "$child" > "$STATE/pid"
  wait "$child"; rc=$?
  if [ -s "$file" ]; then
    chmod 0644 "$file" 2>/dev/null
    /system/bin/am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d "file://$file" >/dev/null 2>&1
    notify 'Recording ended. Check Movies/LockRec.' || :
  else
    rm -f "$file"
    notify 'Recording failed. Check module last.log.' || :
  fi
  rm -rf "$STATE"
  exit "$rc"
}
# Open the optional controller only on a manual manager Action (no arguments).
if [ "$#" -eq 0 ] && /system/bin/pm path org.lockrec.controller 2>/dev/null | grep -q '^package:'; then
  if /system/bin/am start --user current -n org.lockrec.controller/.MainActivity --ez from_module true >/dev/null 2>&1; then
    echo 'Controller opened. Use Start, Pause, End or Hide.'
    exit 0
  fi
  echo 'Controller launch failed; open it manually.'
  exit 1
fi
case "${1:-toggle}" in
  worker) worker "$2" "$3" ;;
  stop) stop ;;
  status) if active; then echo 'Recording'; elif [ -d "$STATE" ]; then echo 'Finalizing'; else echo 'Idle'; fi ;;
  toggle|start)
    if active; then
      [ "${1:-toggle}" = toggle ] && stop || echo 'Already recording.'
      exit 0
    fi
    if [ -d "$STATE" ]; then
      echo 'Previous session still finalizing or interrupted. Reinstall module if state persists.'
      exit 1
    fi
    profile=${2:-native}
    case "$profile" in performance|native) ;; *) echo 'Invalid profile'; exit 1;; esac
    [ -x /system/bin/screenrecord ] || { echo 'Native screenrecord not available'; exit 1; }
    mkdir -p "$OUTDIR" || exit 1
    mkdir "$STATE" || exit 1
    file="$OUTDIR/LockRec_$(date +%Y%m%d_%H%M%S)_$$.mp4"
    printf '%s\n' "$file" > "$STATE/output"
    if ! notify 'Recording active. Use LockRec controls to stop.'; then
      rm -rf "$STATE"; echo 'Recording not started: visible notification could not be verified.'; exit 1
    fi
    nohup /system/bin/sh "$MODDIR/action.sh" worker "$profile" "$file" </dev/null >/dev/null 2>&1 &
    echo 'Recording requested. Confirm recording notification; lock the phone to test.'
    echo "Output: $file"
    ;;
  *) echo 'Usage: action.sh [toggle|start|stop|status] [performance|native]'; exit 1;;
esac

