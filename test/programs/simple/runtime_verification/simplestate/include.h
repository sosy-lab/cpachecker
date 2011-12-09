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
	result += 13;
	result += 13;
	result += 13;
	result += 13;
	result += 13;
	result += 13;
	result += 13;
	result += 13;
	return result > 0;
}

int checkProgramInvariant() {
	return !(expensive() && (k < 0 || k > 100));
}
