void error_fn() {
	ERROR: goto ERROR;
}

int __MONITOR_START_TRANSITION = 0;
int __MONITOR_END_TRANSITION = 0;

#define __MONITOR_START_TRANSITION __MONITOR_START_TRANSITION = __MONITOR_START_TRANSITION
#define __MONITOR_END_TRANSITION __MONITOR_END_TRANSITION = __MONITOR_END_TRANSITION 

int k;

int expensive() {
	int result = k;

	int mybool = nondet_int();
	if(mybool) {
        	result += 26;
	} else {
		result -= 13;
	}
	if(!mybool) {
	        result += 26;
        } else {
		result -= 13;
	}
	if(mybool) {
        	result += 26;
	} else {
		result -= 13;
	}
	if(!mybool) {
	        result += 26;
        } else {
		result -= 13;
	}
	if(mybool) {
        	result += 26;
	} else {
		result -= 13;
	}
	if(!mybool) {
	        result += 26;
        } else {
		result -= 13;
	}
	if(mybool) {
        	result += 26;
	} else {
		result -= 13;
	}
	if(!mybool) {
	        result += 26;
        } else {
		result -= 13;
	}
	if(mybool) {
        	result += 26;
	} else {
		result -= 13;
	}
	if(!mybool) {
	        result += 26;
        } else {
		result -= 13;
	}

	return result > 0;
}

int checkProgramInvariant() {
	return !(expensive() && (k < 0 || k > 100));
}
