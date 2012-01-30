#include "include.h"

global int __MONITOR_STATE_executed_threads = 0;

event {
  pattern { t1_started(); }
  action { 
	__MONITOR_START_TRANSITION;
	if(__MONITOR_STATE_executed_threads == 1 || __MONITOR_STATE_executed_threads == 12 || __MONITOR_STATE_executed_threads == 13 || __MONITOR_STATE_executed_threads == 14 || __MONITOR_STATE_executed_threads == 123 || __MONITOR_STATE_executed_threads == 124 || __MONITOR_STATE_executed_threads == 134) {
		error_fn();
	} 
	else if(__MONITOR_STATE_executed_threads == 0) {
		__MONITOR_STATE_executed_threads = 1; 
	}
	else if(__MONITOR_STATE_executed_threads == 2) {
		__MONITOR_STATE_executed_threads = 12; 
	}
	else if(__MONITOR_STATE_executed_threads == 3) {
		__MONITOR_STATE_executed_threads = 13; 
	}
	else if(__MONITOR_STATE_executed_threads == 4) {
		__MONITOR_STATE_executed_threads = 14; 
	}
	else if(__MONITOR_STATE_executed_threads == 23) {
		__MONITOR_STATE_executed_threads = 123; 
	}
	else if(__MONITOR_STATE_executed_threads == 24) {
		__MONITOR_STATE_executed_threads = 124; 
	}
	else if(__MONITOR_STATE_executed_threads == 34) {
		__MONITOR_STATE_executed_threads = 134; 
	}
	else if(__MONITOR_STATE_executed_threads == 234) {
		__MONITOR_STATE_executed_threads = 0; 
	}
	__MONITOR_END_TRANSITION;
  }
}

event {
  pattern { t2_started(); }
  action { 
	__MONITOR_START_TRANSITION;
	if(__MONITOR_STATE_executed_threads == 2 || __MONITOR_STATE_executed_threads == 12 || __MONITOR_STATE_executed_threads == 23 || __MONITOR_STATE_executed_threads == 24 || __MONITOR_STATE_executed_threads == 123 || __MONITOR_STATE_executed_threads == 124 || __MONITOR_STATE_executed_threads == 234) {
		error_fn();
	} 
	else if(__MONITOR_STATE_executed_threads == 0) {
		__MONITOR_STATE_executed_threads = 2; 
	}
	else if(__MONITOR_STATE_executed_threads == 1) {
		__MONITOR_STATE_executed_threads = 12; 
	}
	else if(__MONITOR_STATE_executed_threads == 3) {
		__MONITOR_STATE_executed_threads = 23; 
	}
	else if(__MONITOR_STATE_executed_threads == 4) {
		__MONITOR_STATE_executed_threads = 24; 
	}
	else if(__MONITOR_STATE_executed_threads == 13) {
		__MONITOR_STATE_executed_threads = 123; 
	}
	else if(__MONITOR_STATE_executed_threads == 14) {
		__MONITOR_STATE_executed_threads = 124; 
	}
	else if(__MONITOR_STATE_executed_threads == 34) {
		__MONITOR_STATE_executed_threads = 234; 
	}
	else if(__MONITOR_STATE_executed_threads == 134) {
		__MONITOR_STATE_executed_threads = 0; 
	}
	__MONITOR_END_TRANSITION;
  }
}

event {
  pattern { t3_started(); }
  action { 
	__MONITOR_START_TRANSITION;
	if(__MONITOR_STATE_executed_threads == 3 || __MONITOR_STATE_executed_threads == 13 || __MONITOR_STATE_executed_threads == 23 || __MONITOR_STATE_executed_threads == 34 || __MONITOR_STATE_executed_threads == 123 || __MONITOR_STATE_executed_threads == 134 || __MONITOR_STATE_executed_threads == 234) {
		error_fn();
	} 
	else if(__MONITOR_STATE_executed_threads == 0) {
		__MONITOR_STATE_executed_threads = 3; 
	}
	else if(__MONITOR_STATE_executed_threads == 1) {
		__MONITOR_STATE_executed_threads = 13; 
	}
	else if(__MONITOR_STATE_executed_threads == 2) {
		__MONITOR_STATE_executed_threads = 23; 
	}
	else if(__MONITOR_STATE_executed_threads == 4) {
		__MONITOR_STATE_executed_threads = 34; 
	}
	else if(__MONITOR_STATE_executed_threads == 12) {
		__MONITOR_STATE_executed_threads = 123; 
	}
	else if(__MONITOR_STATE_executed_threads == 14) {
		__MONITOR_STATE_executed_threads = 134; 
	}
	else if(__MONITOR_STATE_executed_threads == 24) {
		__MONITOR_STATE_executed_threads = 234; 
	}
	else if(__MONITOR_STATE_executed_threads == 124) {
		__MONITOR_STATE_executed_threads = 0; 
	}
	__MONITOR_END_TRANSITION;
  }
}

event {
  pattern { t4_started(); }
  action { 
	__MONITOR_START_TRANSITION;
	if(__MONITOR_STATE_executed_threads == 4 || __MONITOR_STATE_executed_threads == 14 || __MONITOR_STATE_executed_threads == 24 || __MONITOR_STATE_executed_threads == 34 || __MONITOR_STATE_executed_threads == 124 || __MONITOR_STATE_executed_threads == 134 || __MONITOR_STATE_executed_threads == 234) {
		error_fn();
	} 
	else if(__MONITOR_STATE_executed_threads == 0) {
		__MONITOR_STATE_executed_threads = 4; 
	}
	else if(__MONITOR_STATE_executed_threads == 1) {
		__MONITOR_STATE_executed_threads = 14; 
	}
	else if(__MONITOR_STATE_executed_threads == 2) {
		__MONITOR_STATE_executed_threads = 24; 
	}
	else if(__MONITOR_STATE_executed_threads == 3) {
		__MONITOR_STATE_executed_threads = 34; 
	}
	else if(__MONITOR_STATE_executed_threads == 12) {
		__MONITOR_STATE_executed_threads = 124; 
	}
	else if(__MONITOR_STATE_executed_threads == 13) {
		__MONITOR_STATE_executed_threads = 134; 
	}
	else if(__MONITOR_STATE_executed_threads == 23) {
		__MONITOR_STATE_executed_threads = 234; 
	}
	else if(__MONITOR_STATE_executed_threads == 123) {
		__MONITOR_STATE_executed_threads = 0; 
	}
	__MONITOR_END_TRANSITION;
  }
}
