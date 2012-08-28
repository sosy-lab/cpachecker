#include "include.h"

global int __MONITOR_STATE_state = 0;


event {
  pattern { $? = ssl3_client_hello(); }
  action { 
	__MONITOR_START_TRANSITION;
	if(__MONITOR_STATE_state == 0) {
		__MONITOR_STATE_state = 1;
	} 
	__MONITOR_END_TRANSITION;
  }
}

event {
  pattern { $? = ssl3_get_server_hello(); }
  action { 
	__MONITOR_START_TRANSITION;
	if(__MONITOR_STATE_state == 1) {
		__MONITOR_STATE_state = 2;
	} 
	__MONITOR_END_TRANSITION;
  }
}


event {
  pattern { $? = ssl3_get_server_certificate(); }
  action { 
	__MONITOR_START_TRANSITION;
	if(__MONITOR_STATE_state == 2) {
		__MONITOR_STATE_state = 3;
	} 
	__MONITOR_END_TRANSITION;
  }
}


event {
  pattern { $? = ssl3_get_key_exchange(); }
  action { 
	__MONITOR_START_TRANSITION;
	if(__MONITOR_STATE_state == 3) {
		__MONITOR_STATE_state = 4;
	} 
	__MONITOR_END_TRANSITION;
  }
}


event {
  pattern { $? = ssl3_get_certificate_request(); }
  action { 
	__MONITOR_START_TRANSITION;
	if(__MONITOR_STATE_state == 5) {
		error_fn();
	} 
	__MONITOR_END_TRANSITION;
  }
}


event {
  pattern { $? = ssl3_get_server_done(); }
  action { 
	__MONITOR_START_TRANSITION;
	if(__MONITOR_STATE_state == 4) {
		__MONITOR_STATE_state = 5;
	} 
	__MONITOR_END_TRANSITION;
  }
}


