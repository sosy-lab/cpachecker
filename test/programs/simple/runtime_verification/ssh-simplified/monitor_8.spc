#include "include.h"

global int __MONITOR_STATE_state = 0;

event {
  pattern { $? = ssl3_get_client_hello(); }
  action { 
	__MONITOR_START_TRANSITION;
	if(__MONITOR_STATE_state == 0) {
		__MONITOR_STATE_state = 1;
	} 
	__MONITOR_END_TRANSITION;
  }
}

event {
  pattern { $? = ssl3_send_server_hello(); }
  action { 
	__MONITOR_START_TRANSITION;
	if(__MONITOR_STATE_state == 1) {
		__MONITOR_STATE_state = 2;
	} 
	__MONITOR_END_TRANSITION;
  }
}

event {
  pattern { $? = ssl3_send_server_certificate(); }
  action { 
	__MONITOR_START_TRANSITION;
	if(__MONITOR_STATE_state == 2) {
		__MONITOR_STATE_state = 3;
	} 
	__MONITOR_END_TRANSITION;
  }
}

event {
  pattern { $? = ssl3_send_server_key_exchange(); }
  action { 
	__MONITOR_START_TRANSITION;
	if(__MONITOR_STATE_state == 3) {
		__MONITOR_STATE_state = 4;
	} 
	__MONITOR_END_TRANSITION;
  }
}

event {
  pattern { $? = ssl3_send_certificate_request(); }
  action { 
	__MONITOR_START_TRANSITION;
	if(__MONITOR_STATE_state == 4) {
		__MONITOR_STATE_state = 5;
	} 
	__MONITOR_END_TRANSITION;
  }
}

event {
  pattern { $? =ssl3_check_client_hello(); }
  action { 
	__MONITOR_START_TRANSITION;
	if(__MONITOR_STATE_state == 5) {
		__MONITOR_STATE_state = 6;
	} 
	__MONITOR_END_TRANSITION;
  }
}

event {
  pattern { $? = ssl3_get_client_certificate(); }
  action { 
	__MONITOR_START_TRANSITION;
	if(__MONITOR_STATE_state == 6) {
		__MONITOR_STATE_state = 7;
	} 
	__MONITOR_END_TRANSITION;
  }
}

event {
  pattern { $? = ssl3_get_client_key_exchange(); }
  action { 
	__MONITOR_START_TRANSITION;
	if(__MONITOR_STATE_state == 7) {
		__MONITOR_STATE_state = 8;
	} 
	__MONITOR_END_TRANSITION;
  }
}

event {
  pattern { $? = ssl3_get_cert_verify(); }
  action { 
	__MONITOR_START_TRANSITION;
	if(__MONITOR_STATE_state == 8) {
		__MONITOR_STATE_state = 9;
	} 
	__MONITOR_END_TRANSITION;
  }
}

event {
  pattern { $? = ssl3_get_finished(); }
  action { 
	__MONITOR_START_TRANSITION;
	if(__MONITOR_STATE_state == 9) {
		__MONITOR_STATE_state = 10;
	} 
	else if(__MONITOR_STATE_state == 12) {
		__MONITOR_STATE_state = 13;
	} 
	else if(__MONITOR_STATE_state == 15) {
		__MONITOR_STATE_state = 16;
	} 
	else if(__MONITOR_STATE_state == 18) {
		__MONITOR_STATE_state = 19;
	} 
	else if(__MONITOR_STATE_state == 21) {
		error_fn();
	}
	__MONITOR_END_TRANSITION;
  }
}

event {
  pattern { $? = ssl3_send_change_cipher_spec(); }
  action { 
	__MONITOR_START_TRANSITION;
	if(__MONITOR_STATE_state == 10) {
		__MONITOR_STATE_state = 11;
	} 
	else if(__MONITOR_STATE_state == 13) {
		__MONITOR_STATE_state = 14;
	} 
	else if(__MONITOR_STATE_state == 16) {
		__MONITOR_STATE_state = 17;
	} 
	else if(__MONITOR_STATE_state == 19) {
		__MONITOR_STATE_state = 20;
	} 
	__MONITOR_END_TRANSITION;
  }
}


event {
  pattern { $? = ssl3_send_finished(); }
  action { 
	__MONITOR_START_TRANSITION;
	if(__MONITOR_STATE_state == 11) {
		__MONITOR_STATE_state = 12;
	} 
	else if(__MONITOR_STATE_state == 14) {
		__MONITOR_STATE_state = 15;
	} 
	else if(__MONITOR_STATE_state == 17) {
		__MONITOR_STATE_state = 18;
	} 
	else if(__MONITOR_STATE_state == 20) {
		__MONITOR_STATE_state = 21;
	} 
	__MONITOR_END_TRANSITION;
  }
}
