#!/system/bin/sh
MODDIR=${0%/*}
/system/bin/sh "$MODDIR/action.sh" stop >/dev/null 2>&1
