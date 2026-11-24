#!/system/bin/sh
SKIPMOUNT=true
ui_print 'LockRec Universal v0.3 beta'
ui_print 'Manual Action starts/stops a bounded 60-second recording.'
ui_print 'No boot recording or system patches.'
set_perm "$MODPATH/action.sh" 0 0 0755
