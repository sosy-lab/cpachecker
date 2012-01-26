#include "include.h"

global int __MONITOR_STATE_executed_threads = 0;

event {
  pattern { t1_started(); }
  action { 
	__MONITOR_START_TRANSITION;
	if(__MONITOR_STATE_executed_threads == 1 || __MONITOR_STATE_executed_threads == 12 || __MONITOR_STATE_executed_threads == 13) {
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
	else if(__MONITOR_STATE_executed_threads == 23) {
		__MONITOR_STATE_executed_threads = 0; 
	}
	__MONITOR_END_TRANSITION;
  }
}

event {
  pattern { t2_started(); }
  action { 
	__MONITOR_START_TRANSITION;
	if(__MONITOR_STATE_executed_threads == 2 || __MONITOR_STATE_executed_threads == 12 || __MONITOR_STATE_executed_threads == 23) {
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
	else if(__MONITOR_STATE_executed_threads == 13) {
		__MONITOR_STATE_executed_threads = 0; 
	}
	__MONITOR_END_TRANSITION;
  }
}

event {
  pattern { t3_started(); }
  action { 
	__MONITOR_START_TRANSITION;
	if(__MONITOR_STATE_executed_threads == 3 || __MONITOR_STATE_executed_threads == 13 || __MONITOR_STATE_executed_threads == 23) {
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
	else if(__MONITOR_STATE_executed_threads == 12) {
		__MONITOR_STATE_executed_threads = 0; 
	}
	__MONITOR_END_TRANSITION;
  }
}
