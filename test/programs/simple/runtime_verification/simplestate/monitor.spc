#include "include.h"

event {
  after
  pattern { $? = $?; }
  action { 
	__MONITOR_START_TRANSITION;
	if(!checkProgramInvariant()) {
		error_fn();
	}
	__MONITOR_END_TRANSITION;
  }
}
