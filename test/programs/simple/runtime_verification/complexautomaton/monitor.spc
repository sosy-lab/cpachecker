#include "include.h"

global int __MONITOR_STATE_lockStatus = -1;

event {
  pattern { init(); }
  action {
	__MONITOR_START_TRANSITION;
	if(__MONITOR_STATE_lockStatus != -1) {
		error_fn();
	} else {
		__MONITOR_STATE_lockStatus = 0;
	}
	__MONITOR_END_TRANSITION;
}
}

event {
  pattern { lock(); }
  action { 
	__MONITOR_START_TRANSITION;
	if(__MONITOR_STATE_lockStatus == 1) {
		error_fn();
	} else {
		__MONITOR_STATE_lockStatus = 1; 
	}
	__MONITOR_END_TRANSITION;
  }
}

event {
  pattern { unlock(); }
  action { 
	__MONITOR_START_TRANSITION;
	if(__MONITOR_STATE_lockStatus == 0) {
		error_fn();
	} else {
		__MONITOR_STATE_lockStatus = 0; 
	}
	__MONITOR_END_TRANSITION;
  }
}
