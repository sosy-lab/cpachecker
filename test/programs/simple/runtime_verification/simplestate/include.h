void error_fn() {
	ERROR: goto ERROR;
}

void __MONITOR_END_TRANSITION() {

}

void __MONITOR_START_TRANSITION() {

}

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
